package it.unipv.bitFactory.dao.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import it.unipv.bitFactory.dao.interfacce.DAOException;
import it.unipv.bitFactory.dao.interfacce.MagazzinoDAO;
import it.unipv.bitFactory.model.magazzino.SoglieMagazzino;
import it.unipv.bitFactory.model.magazzino.StatoDisponibilita;
import it.unipv.bitFactory.model.magazzino.VoceMagazzino;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;

public final class SqliteMagazzinoDAO implements MagazzinoDAO {

    private final String jdbcUrl;
    private final SoglieMagazzino soglieMagazzino;

    public SqliteMagazzinoDAO(
            String percorsoDatabase,
            SoglieMagazzino soglieMagazzino
    ) {
        if (percorsoDatabase == null || percorsoDatabase.isBlank()) {
            throw new IllegalArgumentException(
                    "Il percorso del database non può essere vuoto"
            );
        }

        this.jdbcUrl = "jdbc:sqlite:" + percorsoDatabase.trim();
        this.soglieMagazzino = Objects.requireNonNull(
                soglieMagazzino,
                "Le soglie del magazzino non possono essere null"
        );
    }

    @Override
    public Optional<VoceMagazzino> trovaPerIdPezzo(String idPezzo) {
        String id = validaIdPezzo(idPezzo);

        String sql = """
                SELECT id_pezzo,
                       tipo,
                       CASE WHEN id_macchina IS NULL THEN 1 ELSE 0 END AS disponibile
                FROM pezzo
                WHERE id_pezzo = ?
                """;

        try (Connection connection = apriConnessione();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                TipoPezzo tipo = tipoJava(result.getString("tipo"));
                if (tipo == null) {
                    return Optional.empty();
                }

                int quantita = result.getInt("disponibile");

                return Optional.of(new VoceMagazzino(
                        result.getString("id_pezzo"),
                        tipo,
                        quantita,
                        soglieMagazzino.calcolaStato(quantita)
                ));
            }

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante la ricerca del pezzo in magazzino",
                    e
            );
        }
    }

    @Override
    public List<VoceMagazzino> trovaTutti() {
        Map<TipoPezzo, Integer> quantitaPerTipo =
                new EnumMap<>(TipoPezzo.class);

        for (TipoPezzo tipo : TipoPezzo.values()) {
            quantitaPerTipo.put(tipo, 0);
        }

        String sql = """
                SELECT tipo, COUNT(*) AS quantita
                FROM pezzo
                WHERE id_macchina IS NULL
                GROUP BY LOWER(TRIM(tipo))
                """;

        try (Connection connection = apriConnessione();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                TipoPezzo tipo = tipoJava(result.getString("tipo"));

                if (tipo != null) {
                    quantitaPerTipo.merge(
                            tipo,
                            result.getInt("quantita"),
                            Integer::sum
                    );
                }
            }

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante il caricamento del magazzino",
                    e
            );
        }

        List<VoceMagazzino> risultato = new ArrayList<>();

        for (TipoPezzo tipo : TipoPezzo.values()) {
            int quantita = quantitaPerTipo.get(tipo);
            StatoDisponibilita stato =
                    soglieMagazzino.calcolaStato(quantita);

            risultato.add(new VoceMagazzino(
                    "RIEPILOGO-" + tipo.name(),
                    tipo,
                    quantita,
                    stato
            ));
        }

        return risultato;
    }

    @Override
    public void aggiornaQuantita(String idPezzo, int nuovaQuantita) {
        String id = validaIdPezzo(idPezzo);

        if (nuovaQuantita < 0 || nuovaQuantita > 1) {
            throw new IllegalArgumentException(
                    "Per un singolo pezzo la quantità può essere soltanto 0 o 1"
            );
        }

        if (nuovaQuantita == 1) {
            VoceMagazzino voce = trovaPerIdPezzo(id)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Pezzo non trovato: " + id
                    ));

            if (voce.getQuantita() != 1) {
                throw new IllegalStateException(
                        "Il pezzo è montato su una macchina"
                );
            }
            return;
        }

        scartaPezzo(id);
    }

    @Override
    public List<Pezzo> trovaPezziLiberi(TipoPezzo tipoPezzo) {
        validaTipo(tipoPezzo);

        List<String> valoriTipo = valoriDatabase(tipoPezzo);
        String sql = """
                SELECT id_pezzo,
                       COALESCE(km, 0) AS km,
                       COALESCE(km_max, 0) AS km_max,
                       COALESCE(tempo_utilizzo, 0) AS tempo_utilizzo,
                       COALESCE(tempo_max, 0) AS tempo_max
                FROM pezzo
                WHERE id_macchina IS NULL
                  AND LOWER(TRIM(tipo)) IN (%s)
                ORDER BY id_pezzo
                """.formatted(segnaposto(valoriTipo.size()));

        List<Pezzo> risultato = new ArrayList<>();

        try (Connection connection = apriConnessione();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            impostaTipi(statement, 1, valoriTipo);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Pezzo pezzo = new Pezzo(
                            result.getString("id_pezzo"),
                            tipoPezzo,
                            result.getDouble("km_max"),
                            result.getInt("tempo_max")
                    );

                    pezzo.aggiornaUtilizzo(
                            result.getDouble("km"),
                            result.getInt("tempo_utilizzo")
                    );

                    risultato.add(pezzo);
                }
            }

            return risultato;

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante la ricerca dei pezzi liberi",
                    e
            );
        }
    }

    @Override
    public void aggiungiPezzi(
            TipoPezzo tipoPezzo,
            int quantita,
            double kmMax,
            int tempoMax
    ) {
        validaTipo(tipoPezzo);

        if (quantita <= 0) {
            throw new IllegalArgumentException(
                    "La quantità deve essere maggiore di zero"
            );
        }

        if (kmMax < 0 || tempoMax < 0) {
            throw new IllegalArgumentException(
                    "Km massimi e tempo massimo non possono essere negativi"
            );
        }

        String sql = """
                INSERT INTO pezzo (
                    id_pezzo,
                    tipo,
                    id_macchina,
                    km,
                    km_max,
                    tempo_utilizzo,
                    tempo_max
                )
                VALUES (?, ?, NULL, 0, ?, 0, ?)
                """;

        try (Connection connection = apriConnessione()) {
            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < quantita; i++) {
                    Pezzo pezzo = new Pezzo(tipoPezzo, kmMax, tempoMax);

                    statement.setString(1, pezzo.getIdPezzo());
                    statement.setString(2, tipoDatabase(tipoPezzo));
                    statement.setDouble(3, kmMax);
                    statement.setInt(4, tempoMax);
                    statement.addBatch();
                }

                statement.executeBatch();
                connection.commit();

            } catch (SQLException | RuntimeException e) {
                rollbackSilenzioso(connection);
                throw e;
            }

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante l'inserimento dei pezzi in magazzino",
                    e
            );
        }
    }

    @Override
    public void scartaPezzo(String idPezzo) {
        String id = validaIdPezzo(idPezzo);

        String sql = """
                DELETE FROM pezzo
                WHERE id_pezzo = ?
                  AND id_macchina IS NULL
                """;

        try (Connection connection = apriConnessione();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException(
                        "Pezzo libero non trovato: " + id
                );
            }

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante lo scarto del pezzo " + id,
                    e
            );
        }
    }

    private Connection apriConnessione() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 15000");
        } catch (SQLException e) {
            connection.close();
            throw e;
        }

        return connection;
    }

    private List<String> valoriDatabase(TipoPezzo tipoPezzo) {
        return switch (tipoPezzo) {
            case RUOTA -> List.of(
                    "pneumatico", "pneumatici", "ruota", "ruote",
                    "gomma", "gomme"
            );
            case FRENO -> List.of("freno", "freni");
            case SCOCCA -> List.of("scocca");
            case MOTORE -> List.of("motore");
            case VOLANTE -> List.of("volante");
        };
    }

    private String tipoDatabase(TipoPezzo tipoPezzo) {
        return switch (tipoPezzo) {
            case RUOTA -> "pneumatico";
            case FRENO -> "freno";
            case SCOCCA -> "scocca";
            case MOTORE -> "motore";
            case VOLANTE -> "volante";
        };
    }

    private TipoPezzo tipoJava(String valoreDatabase) {
        if (valoreDatabase == null || valoreDatabase.isBlank()) {
            return null;
        }

        return switch (valoreDatabase.trim().toLowerCase(Locale.ROOT)) {
            case "pneumatico", "pneumatici", "ruota", "ruote",
                 "gomma", "gomme" -> TipoPezzo.RUOTA;
            case "freno", "freni" -> TipoPezzo.FRENO;
            case "scocca" -> TipoPezzo.SCOCCA;
            case "motore" -> TipoPezzo.MOTORE;
            case "volante" -> TipoPezzo.VOLANTE;
            default -> null;
        };
    }

    private String segnaposto(int quantita) {
        return String.join(",", Collections.nCopies(quantita, "?"));
    }

    private void impostaTipi(
            PreparedStatement statement,
            int indiceIniziale,
            List<String> valori
    ) throws SQLException {
        int indice = indiceIniziale;

        for (String valore : valori) {
            statement.setString(indice++, valore);
        }
    }

    private void rollbackSilenzioso(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private String validaIdPezzo(String idPezzo) {
        if (idPezzo == null || idPezzo.isBlank()) {
            throw new IllegalArgumentException(
                    "L'id del pezzo non può essere vuoto"
            );
        }

        return idPezzo.trim();
    }

    private void validaTipo(TipoPezzo tipoPezzo) {
        if (tipoPezzo == null) {
            throw new IllegalArgumentException(
                    "Il tipo del pezzo non può essere null"
            );
        }
    }
}