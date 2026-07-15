package it.unipv.bitFactory.dao.sqlite;

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

import it.unipv.bitFactory.dao.interfacce.DAOException;
import it.unipv.bitFactory.dao.interfacce.LegendsDAO;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.model.veicoli.Legends;

/**
 * DAO SQLite dell'aggregato Legends.
 * Gestisce la macchina e la composizione dei pezzi montati.
 */
public final class SqliteLegendsDAO implements LegendsDAO {

    private final String jdbcUrl;

    public SqliteLegendsDAO(String percorsoDatabase) {
        if (percorsoDatabase == null || percorsoDatabase.isBlank()) {
            throw new IllegalArgumentException(
                    "Il percorso del database non può essere vuoto"
            );
        }

        this.jdbcUrl = "jdbc:sqlite:" + percorsoDatabase.trim();
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
                sincronizzaPezzi(connection, legends);
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
                    "Errore durante la lettura della macchina "
                            + idMacchina,
                    e
            );
        }
    }

    @Override
    public List<Legends> trovaTutte() {
        String sql = """
                SELECT id_macchina,
                       COALESCE(km_macchina, 0) AS km_macchina
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

            } catch (SQLException | RuntimeException e) {
                rollbackSilenzioso(connection);
                throw e;
            }

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante l'eliminazione della macchina "
                            + idMacchina,
                    e
            );
        }
    }

    private Legends caricaMacchina(
            Connection connection,
            String idMacchina
    ) throws SQLException {

        String sql = """
                SELECT id_macchina,
                       COALESCE(km_macchina, 0) AS km_macchina
                FROM macchine
                WHERE id_macchina = ?
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

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
        legends.percorriKm(result.getDouble("km_macchina"));
        return legends;
    }

    private void caricaPezzi(
            Connection connection,
            Legends legends,
            String idMacchina
    ) throws SQLException {

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

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, idMacchina);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Optional<TipoPezzo> tipo =
                            convertiTipoDalDatabase(
                                    result.getString("tipo")
                            );

                    if (tipo.isEmpty()) {
                        continue;
                    }

                    Pezzo pezzo = new Pezzo(
                            result.getString("id_pezzo"),
                            tipo.get(),
                            result.getDouble("km_max"),
                            result.getInt("tempo_max")
                    );

                    pezzo.aggiornaUtilizzo(
                            result.getDouble("km"),
                            result.getInt("tempo_utilizzo")
                    );

                    legends.aggiungiPezzo(pezzo);
                }
            }
        }
    }

    private void salvaMacchina(
            Connection connection,
            Legends legends
    ) throws SQLException {

        String sql = """
                INSERT INTO macchine (
                    id_macchina,
                    km_macchina
                )
                VALUES (?, ?)
                ON CONFLICT(id_macchina) DO UPDATE SET
                    km_macchina = excluded.km_macchina
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, validaId(legends.getId()));
            statement.setDouble(2, legends.getKmTotali());
            statement.executeUpdate();
        }
    }

    /**
     * Rende la composizione persistita uguale a quella dell'oggetto Legends.
     * Prima libera i pezzi precedentemente montati sulla macchina, poi assegna
     * quelli presenti nell'oggetto. Tutto avviene nella transazione locale
     * aperta dal metodo salva().
     */
    private void sincronizzaPezzi(
            Connection connection,
            Legends legends
    ) throws SQLException {

        String liberaVecchiSql = """
                UPDATE pezzo
                SET id_macchina = NULL
                WHERE id_macchina = ?
                """;

        try (PreparedStatement liberaVecchi =
                     connection.prepareStatement(liberaVecchiSql)) {
            liberaVecchi.setString(1, legends.getId());
            liberaVecchi.executeUpdate();
        }

        String aggiornaSql = """
                UPDATE pezzo
                SET tipo = ?,
                    id_macchina = ?,
                    km = ?,
                    km_max = ?,
                    tempo_utilizzo = ?,
                    tempo_max = ?
                WHERE id_pezzo = ?
                  AND id_macchina IS NULL
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
                String tipoDatabase =
                        convertiTipoPerDatabase(pezzo.getTipo());

                aggiorna.setString(1, tipoDatabase);
                aggiorna.setString(2, legends.getId());
                aggiorna.setDouble(3, pezzo.getKmAttuali());
                aggiorna.setDouble(4, pezzo.getKmMax());
                aggiorna.setInt(5, pezzo.getTempoAttuale());
                aggiorna.setInt(6, pezzo.getTempoMax());
                aggiorna.setString(7, pezzo.getIdPezzo());

                int righeAggiornate = aggiorna.executeUpdate();

                if (righeAggiornate == 0) {
                    try {
                        inserisci.setString(1, pezzo.getIdPezzo());
                        inserisci.setString(2, tipoDatabase);
                        inserisci.setString(3, legends.getId());
                        inserisci.setDouble(4, pezzo.getKmAttuali());
                        inserisci.setDouble(5, pezzo.getKmMax());
                        inserisci.setInt(6, pezzo.getTempoAttuale());
                        inserisci.setInt(7, pezzo.getTempoMax());
                        inserisci.executeUpdate();

                    } catch (SQLException e) {
                        throw new IllegalStateException(
                                "Il pezzo " + pezzo.getIdPezzo()
                                        + " non è disponibile",
                                e
                        );
                    }
                }
            }
        }
    }

    private Connection apriConnessione() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException e) {
            connection.close();
            throw e;
        }

        return connection;
    }

    private Optional<TipoPezzo> convertiTipoDalDatabase(
            String valoreDatabase
    ) {
        if (valoreDatabase == null || valoreDatabase.isBlank()) {
            return Optional.empty();
        }

        String tipoDb = valoreDatabase
                .trim()
                .toLowerCase(Locale.ROOT);

        return switch (tipoDb) {
            case "motore" -> Optional.of(TipoPezzo.MOTORE);
            case "freno", "freni" -> Optional.of(TipoPezzo.FRENO);
            case "pneumatico", "pneumatici", "ruota", "ruote",
                 "gomma", "gomme" -> Optional.of(TipoPezzo.RUOTA);
            case "volante" -> Optional.of(TipoPezzo.VOLANTE);
            case "scocca" -> Optional.of(TipoPezzo.SCOCCA);
            default -> Optional.empty();
        };
    }

    private String convertiTipoPerDatabase(TipoPezzo tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException(
                    "Il tipo del pezzo non può essere null"
            );
        }

        return switch (tipo) {
            case MOTORE -> "motore";
            case FRENO -> "freno";
            case RUOTA -> "pneumatico";
            case VOLANTE -> "volante";
            case SCOCCA -> "scocca";
        };
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
            // Manteniamo l'eccezione originale.
        }
    }
}
