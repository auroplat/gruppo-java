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
import java.util.Optional;

import it.unipv.bitFactory.dao.interfacce.DAOException;
import it.unipv.bitFactory.dao.interfacce.MagazzinoDAO;
import it.unipv.bitFactory.model.magazzino.SoglieMagazzino;
import it.unipv.bitFactory.model.magazzino.StatoDisponibilita;
import it.unipv.bitFactory.model.magazzino.VoceMagazzino;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;

public class SqliteMagazzinoDAO implements MagazzinoDAO {

    private final String urlDatabase;
    private final SoglieMagazzino soglieMagazzino;

    public SqliteMagazzinoDAO(
            String percorsoDatabase,
            SoglieMagazzino soglieMagazzino) {

        if (percorsoDatabase == null || percorsoDatabase.isBlank()) {
            throw new IllegalArgumentException("Il percorso del database non può essere vuoto");
        }

        if (soglieMagazzino == null) {
            throw new IllegalArgumentException("Le soglie del magazzino non possono essere null");
        }

        this.urlDatabase = "jdbc:sqlite:" + percorsoDatabase;
        this.soglieMagazzino = soglieMagazzino;

        inizializzaDatabase();
    }

    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(urlDatabase);

        try (Statement statement = conn.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }

        return conn;
    }

    private void inizializzaDatabase() {
        String creaMacchine = """
                CREATE TABLE IF NOT EXISTS macchine (
                    id_macchina TEXT PRIMARY KEY,
                    km_macchina DOUBLE NOT NULL DEFAULT 0
                )
                """;

        String creaPezzi = """
                CREATE TABLE IF NOT EXISTS pezzo (
                    id_pezzo TEXT PRIMARY KEY,
                    tipo TEXT NOT NULL,
                    id_macchina TEXT,
                    km DOUBLE NOT NULL DEFAULT 0,
                    km_max DOUBLE NOT NULL DEFAULT 0,
                    tempo_utilizzo DOUBLE NOT NULL DEFAULT 0,
                    tempo_max DOUBLE NOT NULL DEFAULT 0,
                    FOREIGN KEY (id_macchina) REFERENCES macchine(id_macchina)
                )
                """;

        try (Connection conn = getConnection();
             Statement statement = conn.createStatement()) {

            statement.execute(creaMacchine);
            statement.execute(creaPezzi);

        } catch (SQLException e) {
            throw new DAOException("Errore durante l'inizializzazione del magazzino", e );
        }
    }

    @Override
    public Optional<VoceMagazzino> trovaPerIdPezzo(String idPezzo) {
        validaIdPezzo(idPezzo);

        String sql = """
                SELECT id_pezzo,
                       tipo,
                       CASE WHEN id_macchina IS NULL THEN 1 ELSE 0 END AS disponibile
                FROM pezzo
                WHERE id_pezzo = ?
                """;

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, idPezzo.trim());

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
            throw new DAOException("Errore durante la ricerca del pezzo in magazzino", e );
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

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
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
            throw new DAOException( "Errore durante il caricamento del magazzino", e );
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
        validaIdPezzo(idPezzo);

        if (nuovaQuantita < 0 || nuovaQuantita > 1) {
            throw new IllegalArgumentException("Per un singolo pezzo la quantità può essere soltanto 0 o 1" );
        }

        if (nuovaQuantita == 1) {
            VoceMagazzino voce = trovaPerIdPezzo(idPezzo)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Pezzo non trovato: " + idPezzo
                    ));

            if (voce.getQuantita() != 1) {
                throw new IllegalStateException( "Il pezzo è montato su una macchina e non può essere reso libero direttamente" );
            }
            return;
        }

        String sql = """
                DELETE FROM pezzo
                WHERE id_pezzo = ?
                  AND id_macchina IS NULL
                """;

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, idPezzo.trim());

            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException( "Pezzo libero non trovato: " + idPezzo );
            }

        } catch (SQLException e) {
            throw new DAOException( "Errore durante la rimozione del pezzo dal magazzino", e );
        }
    }

    @Override
    public List<Pezzo> trovaPezziLiberi(TipoPezzo tipoPezzo) {
        validaTipo(tipoPezzo);

        List<String> valoriTipo = valoriDatabase(tipoPezzo);
        String sql = """
                SELECT id_pezzo,
                       tipo,
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

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

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
            throw new DAOException( "Errore durante la ricerca dei pezzi liberi", e );
        }
    }

    @Override
    public void aggiungiPezzi(
            TipoPezzo tipoPezzo,
            int quantita,
            double kmMax,
            int tempoMax) {

        validaTipo(tipoPezzo);

        if (quantita <= 0) {
            throw new IllegalArgumentException( "La quantità deve essere maggiore di zero" );
        }

        if (kmMax < 0 || tempoMax < 0) {
            throw new IllegalArgumentException( "Km massimi e tempo massimo non possono essere negativi" );
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

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                for (int i = 0; i < quantita; i++) {
                    Pezzo pezzo = new Pezzo(tipoPezzo, kmMax, tempoMax);

                    statement.setString(1, pezzo.getIdPezzo());
                    statement.setString(2, tipoDatabase(tipoPezzo));
                    statement.setDouble(3, kmMax);
                    statement.setInt(4, tempoMax);
                    statement.addBatch();
                }

                statement.executeBatch();
                conn.commit();

            } catch (SQLException | RuntimeException e) {
                rollbackSilenzioso(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new DAOException( "Errore durante l'inserimento dei pezzi in magazzino", e  );
        }
    }

    @Override
    public void creaMacchina(
            String idMacchina,
            Map<TipoPezzo, Integer> ricetta) {

        validaIdMacchina(idMacchina);

        if (ricetta == null || ricetta.isEmpty()) {
            throw new IllegalArgumentException( "La ricetta della macchina non può essere vuota" );
        }

        String id = idMacchina.trim();

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                if (esisteMacchina(conn, id)) {
                    throw new IllegalArgumentException( "Esiste già una macchina con id: " + id );
                }

                Map<TipoPezzo, List<String>> pezziDaMontare =
                        new EnumMap<>(TipoPezzo.class);

                for (Map.Entry<TipoPezzo, Integer> voce : ricetta.entrySet()) {
                    TipoPezzo tipo = voce.getKey();
                    Integer quantita = voce.getValue();

                    validaTipo(tipo);

                    if (quantita == null || quantita <= 0) {
                        throw new IllegalArgumentException( "Quantità non valida nella ricetta per " + tipo );
                    }

                    List<String> ids = trovaIdPezziLiberi(
                            conn,
                            tipo,
                            quantita
                    );

                    if (ids.size() < quantita) {
                        throw new IllegalStateException(
                                "Pezzi insufficienti per " + tipo
                                        + ": richiesti " + quantita
                                        + ", disponibili " + ids.size()
                        );
                    }

                    pezziDaMontare.put(tipo, ids);
                }

                try (PreparedStatement inserisciMacchina = conn.prepareStatement(
                        "INSERT INTO macchine (id_macchina, km_macchina) VALUES (?, 0)"
                )) {
                    inserisciMacchina.setString(1, id);
                    inserisciMacchina.executeUpdate();
                }

                try (PreparedStatement associaPezzo = conn.prepareStatement("""
                        UPDATE pezzo
                        SET id_macchina = ?
                        WHERE id_pezzo = ?
                          AND id_macchina IS NULL
                        """)) {

                    for (List<String> ids : pezziDaMontare.values()) {
                        for (String idPezzo : ids) {
                            associaPezzo.setString(1, id);
                            associaPezzo.setString(2, idPezzo);

                            if (associaPezzo.executeUpdate() != 1) {
                                throw new IllegalStateException( "Il pezzo non è più disponibile: " + idPezzo );
                            }
                        }
                    }
                }

                conn.commit();

            } catch (SQLException | RuntimeException e) {
                rollbackSilenzioso(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new DAOException( "Errore durante la creazione della macchina",  e  );
        }
    }

    @Override
    public void cambiaPezzo(
            String idMacchina,
            TipoPezzo tipoPezzo,
            String idNuovoPezzo) {

        validaIdMacchina(idMacchina);
        validaTipo(tipoPezzo);
        validaIdPezzo(idNuovoPezzo);

        String idMacchinaPulito = idMacchina.trim();
        String idNuovoPulito = idNuovoPezzo.trim();

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                if (!esisteMacchina(conn, idMacchinaPulito)) {
                    throw new IllegalArgumentException( "Macchina non trovata: " + idMacchinaPulito  );
                }

                verificaNuovoPezzo(
                        conn,
                        idNuovoPulito,
                        tipoPezzo
                );

                String idVecchioPezzo = trovaPezzoMontato(
                        conn,
                        idMacchinaPulito,
                        tipoPezzo
                ).orElseThrow(() -> new IllegalStateException( "La macchina non possiede un pezzo di tipo " + tipoPezzo ));

                try (PreparedStatement eliminaVecchio = conn.prepareStatement("""
                        DELETE FROM pezzo
                        WHERE id_pezzo = ?
                          AND id_macchina = ?
                        """)) {

                    eliminaVecchio.setString(1, idVecchioPezzo);
                    eliminaVecchio.setString(2, idMacchinaPulito);

                    if (eliminaVecchio.executeUpdate() != 1) {
                        throw new IllegalStateException( "Impossibile rimuovere il vecchio pezzo" );
                    }
                }

                try (PreparedStatement montaNuovo = conn.prepareStatement("""
                        UPDATE pezzo
                        SET id_macchina = ?
                        WHERE id_pezzo = ?
                          AND id_macchina IS NULL
                        """)) {

                    montaNuovo.setString(1, idMacchinaPulito);
                    montaNuovo.setString(2, idNuovoPulito);

                    if (montaNuovo.executeUpdate() != 1) {
                        throw new IllegalStateException( "Il nuovo pezzo non è più disponibile" );
                    }
                }

                conn.commit();

            } catch (SQLException | RuntimeException e) {
                rollbackSilenzioso(conn);
                throw e;
            }

        } catch (SQLException e) {
            throw new DAOException( "Errore durante il cambio del pezzo",  e  );
        }
    }

    private boolean esisteMacchina(
            Connection conn,
            String idMacchina) throws SQLException {

        try (PreparedStatement statement = conn.prepareStatement(
                "SELECT 1 FROM macchine WHERE id_macchina = ?"
        )) {
            statement.setString(1, idMacchina);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private List<String> trovaIdPezziLiberi(
            Connection conn,
            TipoPezzo tipoPezzo,
            int quantita) throws SQLException {

        List<String> valoriTipo = valoriDatabase(tipoPezzo);
        String sql = """
                SELECT id_pezzo
                FROM pezzo
                WHERE id_macchina IS NULL
                  AND LOWER(TRIM(tipo)) IN (%s)
                ORDER BY id_pezzo
                LIMIT ?
                """.formatted(segnaposto(valoriTipo.size()));

        List<String> ids = new ArrayList<>();

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            int indice = impostaTipi(statement, 1, valoriTipo);
            statement.setInt(indice, quantita);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ids.add(result.getString("id_pezzo"));
                }
            }
        }

        return ids;
    }

    private void verificaNuovoPezzo(
            Connection conn,
            String idPezzo,
            TipoPezzo tipoRichiesto) throws SQLException {

        String sql = """
                SELECT tipo, id_macchina
                FROM pezzo
                WHERE id_pezzo = ?
                """;

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, idPezzo);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException( "Pezzo non trovato: " + idPezzo );
                }

                if (result.getString("id_macchina") != null) {
                    throw new IllegalStateException( "Il pezzo è già montato su una macchina"  );
                }

                TipoPezzo tipoEffettivo = tipoJava(result.getString("tipo"));

                if (tipoEffettivo != tipoRichiesto) {
                    throw new IllegalArgumentException( "Il pezzo selezionato non è di tipo " + tipoRichiesto );
                }
            }
        }
    }

    private Optional<String> trovaPezzoMontato(
            Connection conn,
            String idMacchina,
            TipoPezzo tipoPezzo) throws SQLException {

        List<String> valoriTipo = valoriDatabase(tipoPezzo);
        String sql = """
                SELECT id_pezzo
                FROM pezzo
                WHERE id_macchina = ?
                  AND LOWER(TRIM(tipo)) IN (%s)
                ORDER BY
                    MAX(
                        CASE
                            WHEN COALESCE(km_max, 0) > 0
                            THEN COALESCE(km, 0) / km_max
                            ELSE 0
                        END,
                        CASE
                            WHEN COALESCE(tempo_max, 0) > 0
                            THEN COALESCE(tempo_utilizzo, 0) / tempo_max
                            ELSE 0
                        END
                    ) DESC,
                    id_pezzo
                LIMIT 1
                """.formatted(segnaposto(valoriTipo.size()));

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, idMacchina);
            impostaTipi(statement, 2, valoriTipo);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(result.getString("id_pezzo"));
                }
                return Optional.empty();
            }
        }
    }

    private String tipoDatabase(TipoPezzo tipo) {
        return switch (tipo) {
            case RUOTA -> "pneumatico";
            case FRENO -> "freno";
            default -> tipo.name().toLowerCase(Locale.ROOT);
        };
    }

    private List<String> valoriDatabase(TipoPezzo tipo) {
        return switch (tipo) {
            case RUOTA -> List.of("pneumatico", "pneumatici", "ruota", "ruote", "gomma", "gomme");
            case FRENO -> List.of("freno", "freni");
            case SCOCCA -> List.of("scocca");
            case MOTORE -> List.of("motore");
            case VOLANTE -> List.of("volante");
        };
    }

    private TipoPezzo tipoJava(String valore) {
        if (valore == null) {
            return null;
        }

        return switch (valore.trim().toLowerCase(Locale.ROOT)) {
            case "pneumatico", "pneumatici", "ruota", "ruote", "gomma", "gomme" -> TipoPezzo.RUOTA;
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

    private int impostaTipi(
            PreparedStatement statement,
            int indiceIniziale,
            List<String> valori) throws SQLException {

        int indice = indiceIniziale;

        for (String valore : valori) {
            statement.setString(indice++, valore);
        }

        return indice;
    }

    private void rollbackSilenzioso(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void validaIdPezzo(String idPezzo) {
        if (idPezzo == null || idPezzo.isBlank()) {
            throw new IllegalArgumentException( "L'id del pezzo non può essere vuoto" );
        }
    }

    private void validaIdMacchina(String idMacchina) {
        if (idMacchina == null || idMacchina.isBlank()) {
            throw new IllegalArgumentException( "L'id della macchina non può essere vuoto"  );
        }
    }

    private void validaTipo(TipoPezzo tipoPezzo) {
        if (tipoPezzo == null) {
            throw new IllegalArgumentException( "Il tipo del pezzo non può essere null"  );
        }
    }
}
