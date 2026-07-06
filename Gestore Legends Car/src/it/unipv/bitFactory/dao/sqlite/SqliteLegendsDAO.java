package it.unipv.bitFactory.dao.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import it.unipv.bitFactory.dao.DAOException;
import it.unipv.bitFactory.dao.LegendsDAO;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.model.veicoli.Legends;

public final class SqliteLegendsDAO implements LegendsDAO {

    private final String jdbcUrl;

    public SqliteLegendsDAO(String databasePath) {
        if (databasePath == null || databasePath.isBlank()) {
            throw new IllegalArgumentException(
                    "Il percorso del database non può essere vuoto"
            );
        }

        Path path = Path.of(databasePath)
                .toAbsolutePath()
                .normalize();

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(
                    "Database SQLite non trovato: " + path
            );
        }

        if (!Files.isReadable(path) || !Files.isWritable(path)) {
            throw new IllegalArgumentException(
                    "Il database deve essere leggibile e modificabile: " + path
            );
        }

        this.jdbcUrl = "jdbc:sqlite:" + path;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new DAOException(
                    "Driver SQLite JDBC non trovato",
                    e
            );
        }
    }

    @Override
    public void salva(Legends legends) {
        if (legends == null) {
            throw new IllegalArgumentException(
                    "La macchina da salvare non può essere null"
            );
        }

        try (Connection connection = apriConnessione()) {
            connection.setAutoCommit(false);

            try {
                salvaMacchina(connection, legends);
                salvaPezzi(connection, legends);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                rollbackSilenzioso(connection);
                throw e;
            }

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante il salvataggio della macchina "
                            + legends.getId(),
                    e
            );
        }
    }

    @Override
    public Optional<Legends> trovaPerId(String id) {
        String idMacchina = validaId(id);

        try (Connection connection = apriConnessione()) {
            return Optional.ofNullable(
                    caricaMacchina(connection, idMacchina)
            );
        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante la lettura della macchina " + idMacchina,
                    e
            );
        }
    }

    @Override
    public List<Legends> trovaTutte() {
        String sql = """
                SELECT id_macchina, COALESCE(km_macchina, 0) AS km_macchina
                FROM macchine
                ORDER BY id_macchina
                """;

        List<Legends> macchine = new ArrayList<>();

        try (Connection connection = apriConnessione();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                Legends legends = creaMacchinaDaResultSet(result);
                caricaPezzi(connection, legends, legends.getId());
                macchine.add(legends);
            }

            return macchine;

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante la lettura delle macchine",
                    e
            );
        }
    }

   

    @Override
    public void elimina(String id) {
        String idMacchina = validaId(id);

        String eliminaPezziSql = """
                DELETE FROM pezzo
                WHERE id_macchina = ?
                """;

        String eliminaMacchinaSql = """
                DELETE FROM macchine
                WHERE id_macchina = ?
                """;

        try (Connection connection = apriConnessione()) {
            connection.setAutoCommit(false);

            try (PreparedStatement eliminaPezzi =
                         connection.prepareStatement(eliminaPezziSql);
                 PreparedStatement eliminaMacchina =
                         connection.prepareStatement(eliminaMacchinaSql)) {

                eliminaPezzi.setString(1, idMacchina);
                eliminaPezzi.executeUpdate();

                eliminaMacchina.setString(1, idMacchina);
                eliminaMacchina.executeUpdate();

                connection.commit();

            } catch (SQLException e) {
                rollbackSilenzioso(connection);
                throw e;
            }

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante l'eliminazione della macchina " + idMacchina,
                    e
            );
        }
    }

    private Connection apriConnessione() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }

        return connection;
    }

    private Legends caricaMacchina(
            Connection connection,
            String idMacchina) throws SQLException {

        String sql = """
                SELECT id_macchina, COALESCE(km_macchina, 0) AS km_macchina
                FROM macchine
                WHERE id_macchina = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, idMacchina);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }

                Legends legends = creaMacchinaDaResultSet(result);
                caricaPezzi(connection, legends, idMacchina);
                return legends;
            }
        }
    }

    private Legends creaMacchinaDaResultSet(ResultSet result)
            throws SQLException {

        Legends legends = new Legends(
                result.getString("id_macchina")
        );

        legends.percorriKm(
                result.getDouble("km_macchina")
        );

        return legends;
    }

    private void caricaPezzi(
            Connection connection,
            Legends legends,
            String idMacchina) throws SQLException {

        String sql = """
                SELECT id_pezzo,
                       tipo,
                       COALESCE(km, 0) AS km,
                       COALESCE(km_max, 0) AS km_max,
                       COALESCE(tempo_utilizzo, 0) AS tempo_utilizzo,
                       COALESCE(tempo_max, 0) AS tempo_max
                FROM pezzo
                WHERE id_macchina = ?
                ORDER BY tipo, id_pezzo
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, idMacchina);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Optional<TipoPezzo> tipo = convertiTipoDalDatabase(
                            result.getString("tipo")
                    );

                    // Tipi non presenti nel modello Java (es. batteria,
                    // sensore) restano nel DB e non vengono caricati.
                    if (tipo.isEmpty()) {
                        continue;
                    }

                    Pezzo pezzo = new Pezzo(
                            tipo.get(),
                            result.getDouble("km_max"),
                            result.getInt("tempo_max")
                    );

                    pezzo.aggiornaUtilizzo(
                            result.getDouble("km"),
                            result.getInt("tempo_utilizzo")
                    );

                    legends.modificaPezzo(pezzo);
                }
            }
        }
    }

    private void salvaMacchina(
            Connection connection,
            Legends legends) throws SQLException {

        String sql = """
                INSERT INTO macchine (id_macchina, km_macchina)
                VALUES (?, ?)
                ON CONFLICT(id_macchina) DO UPDATE SET
                    km_macchina = excluded.km_macchina
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, validaId(legends.getId()));
            statement.setDouble(2, legends.getKmTotali());
            statement.executeUpdate();
        }
    }

    private void salvaPezzi(
            Connection connection,
            Legends legends) throws SQLException {

        String aggiornaSql = """
                UPDATE pezzo
                SET km = ?,
                    km_max = ?,
                    tempo_utilizzo = ?,
                    tempo_max = ?
                WHERE id_macchina = ?
                  AND LOWER(TRIM(tipo)) = ?
                """;

        String inserisciSql = """
                INSERT INTO pezzo (
                    id_pezzo,
                    tipo,
                    id_macchina,
                    km,
                    km_max,
                    tempo_utilizzo,
                    tempo_max
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement aggiorna =
                     connection.prepareStatement(aggiornaSql);
             PreparedStatement inserisci =
                     connection.prepareStatement(inserisciSql)) {

            for (Pezzo pezzo : legends.getTuttiPezzi()) {
                String tipoDatabase = convertiTipoPerDatabase(
                        pezzo.getTipo()
                );

                aggiorna.setDouble(1, pezzo.getKmAttuali());
                aggiorna.setDouble(2, pezzo.getKmMax());
                aggiorna.setInt(3, pezzo.getTempoAttuale());
                aggiorna.setInt(4, pezzo.getTempoMax());
                aggiorna.setString(5, legends.getId());
                aggiorna.setString(6, tipoDatabase);

                int righeAggiornate = aggiorna.executeUpdate();

                if (righeAggiornate == 0) {
                    inserisci.setString(1, generaIdPezzo());
                    inserisci.setString(2, tipoDatabase);
                    inserisci.setString(3, legends.getId());
                    inserisci.setDouble(4, pezzo.getKmAttuali());
                    inserisci.setDouble(5, pezzo.getKmMax());
                    inserisci.setInt(6, pezzo.getTempoAttuale());
                    inserisci.setInt(7, pezzo.getTempoMax());
                    inserisci.executeUpdate();
                }
            }
        }
    }

    private Optional<TipoPezzo> convertiTipoDalDatabase(
            String valoreDatabase) {

        if (valoreDatabase == null || valoreDatabase.isBlank()) {
            return Optional.empty();
        }

        String tipoDb = valoreDatabase
                .trim()
                .toLowerCase(Locale.ROOT);

        List<String> nomiEnum = switch (tipoDb) {
            case "motore" -> List.of("MOTORE");
            case "freno", "freni" -> List.of("FRENO", "FRENI");
            case "pneumatico", "pneumatici", "ruota", "gomme" ->
                    List.of("RUOTA", "PNEUMATICO", "PNEUMATICI", "GOMME");
            case "volante" -> List.of("VOLANTE");
            default -> List.of(tipoDb.toUpperCase(Locale.ROOT));
        };

        for (String nomeEnum : nomiEnum) {
            try {
                return Optional.of(TipoPezzo.valueOf(nomeEnum));
            } catch (IllegalArgumentException ignored) {
                // Prova l'eventuale nome alternativo.
            }
        }

        return Optional.empty();
    }

    private String convertiTipoPerDatabase(TipoPezzo tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException(
                    "Il tipo del pezzo non può essere null"
            );
        }

        return switch (tipo.name()) {
            case "MOTORE" -> "motore";
            case "FRENO", "FRENI" -> "freno";
            case "RUOTA", "PNEUMATICO", "PNEUMATICI", "GOMME" ->
                    "pneumatico";
            case "VOLANTE" -> "volante";
            default -> tipo.name().toLowerCase(Locale.ROOT);
        };
    }

    private String generaIdPezzo() {
        return "PZ-" + UUID.randomUUID()
                .toString()
                .replace("-", "");
    }

    private String validaId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "L'id della macchina non può essere vuoto"
            );
        }

        return id.trim();
    }

    private void rollbackSilenzioso(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Conserva l'eccezione originale.
        }
    }
}
