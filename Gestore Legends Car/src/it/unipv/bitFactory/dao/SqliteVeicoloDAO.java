package it.unipv.bitFactory.dao.veicoli;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import it.unipv.bitFactory.dao.DAOException;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.model.veicoli.Legends;
import it.unipv.bitFactory.model.veicoli.TipoVeicolo;

public class SqliteVeicoloDAO implements VeicoloDAO {

    private final String urlDatabase;

    public SqliteVeicoloDAO(String percorsoDatabase) {
        if (percorsoDatabase == null || percorsoDatabase.isBlank()) {
            throw new IllegalArgumentException("Il percorso del database non può essere vuoto");
        }

        this.urlDatabase = "jdbc:sqlite:" + percorsoDatabase;
    }

    private Connection getConnection() throws Exception {
        return DriverManager.getConnection(urlDatabase);
    }

    @Override
    public void inizializzaDatabase() {
        String creaTabellaPezzo = """
            CREATE TABLE IF NOT EXISTS pezzo (
                id_pezzo TEXT PRIMARY KEY,
                tipo_pezzo TEXT NOT NULL,
                km_max REAL NOT NULL,
                tempo_max INTEGER NOT NULL,
                km_attuali REAL NOT NULL DEFAULT 0,
                tempo_attuale INTEGER NOT NULL DEFAULT 0
            )
            """;

        String creaTabellaVeicolo = """
            CREATE TABLE IF NOT EXISTS veicolo (
                id_veicolo TEXT PRIMARY KEY,
                tipo_veicolo TEXT NOT NULL,
                km_totali REAL NOT NULL
            )
            """;

        String creaTabellaVeicoloPezzo = """
            CREATE TABLE IF NOT EXISTS veicolo_pezzo (
                id_veicolo TEXT NOT NULL,
                id_pezzo TEXT NOT NULL,
                PRIMARY KEY (id_veicolo, id_pezzo),
                FOREIGN KEY (id_veicolo) REFERENCES veicolo(id_veicolo),
                FOREIGN KEY (id_pezzo) REFERENCES pezzo(id_pezzo)
            )
            """;

        try (
                Connection conn = getConnection();
                Statement st = conn.createStatement()
        ) {
            st.execute(creaTabellaPezzo);
            st.execute(creaTabellaVeicolo);
            st.execute(creaTabellaVeicoloPezzo);

        } catch (Exception e) {
            throw new DAOException("Errore durante l'inizializzazione del database veicoli", e);
        }
    }

    @Override
    public boolean esisteVeicolo(String idVeicolo) {
        String sql = """
            SELECT COUNT(*) AS totale
            FROM veicolo
            WHERE id_veicolo = ?
            """;

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, idVeicolo);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("totale") > 0;
            }

        } catch (Exception e) {
            throw new DAOException("Errore durante il controllo dell'esistenza del veicolo", e);
        }
    }

    @Override
    public int contaPezziLiberi(TipoPezzo tipoPezzo) {
        String sql = """
            SELECT COUNT(*) AS totale
            FROM pezzo p
            WHERE p.tipo_pezzo = ?
            AND p.id_pezzo NOT IN (
                SELECT vp.id_pezzo
                FROM veicolo_pezzo vp
            )
            """;

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, tipoPezzo.name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("totale");
                }

                return 0;
            }

        } catch (Exception e) {
            throw new DAOException("Errore durante il conteggio dei pezzi liberi", e);
        }
    }

    @Override
    public List<Pezzo> trovaPezziLiberi(TipoPezzo tipoPezzo, int quantitaRichiesta) {
        if (quantitaRichiesta <= 0) {
            throw new IllegalArgumentException("La quantità richiesta deve essere maggiore di zero");
        }

        String sql = """
            SELECT p.id_pezzo, p.tipo_pezzo, p.km_max, p.tempo_max, p.km_attuali, p.tempo_attuale
            FROM pezzo p
            WHERE p.tipo_pezzo = ?
            AND p.id_pezzo NOT IN (
                SELECT vp.id_pezzo
                FROM veicolo_pezzo vp
            )
            LIMIT ?
            """;

        List<Pezzo> pezzi = new ArrayList<>();

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, tipoPezzo.name());
            ps.setInt(2, quantitaRichiesta);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pezzi.add(creaPezzoDaResultSet(rs));
                }
            }

            return pezzi;

        } catch (Exception e) {
            throw new DAOException("Errore durante la ricerca dei pezzi liberi", e);
        }
    }

    @Override
    public void salvaLegends(Legends legends) {
        if (legends == null) {
            throw new IllegalArgumentException("La Legends da salvare non può essere null");
        }

        String inserisciVeicolo = """
            INSERT INTO veicolo (id_veicolo, tipo_veicolo, km_totali)
            VALUES (?, ?, ?)
            """;

        String associaPezzo = """
            INSERT INTO veicolo_pezzo (id_veicolo, id_pezzo)
            VALUES (?, ?)
            """;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (
                    PreparedStatement psVeicolo = conn.prepareStatement(inserisciVeicolo);
                    PreparedStatement psAssociazione = conn.prepareStatement(associaPezzo)
            ) {
                psVeicolo.setString(1, legends.getIdVeicolo());
                psVeicolo.setString(2, legends.getTipoVeicolo().name());
                psVeicolo.setDouble(3, legends.getKmTotali());
                psVeicolo.executeUpdate();

                for (Pezzo pezzo : legends.getTuttiPezzi()) {
                    if (pezzo.getIdPezzo() == null || pezzo.getIdPezzo().isBlank()) {
                        throw new DAOException("Impossibile associare un pezzo senza id al veicolo");
                    }

                    psAssociazione.setString(1, legends.getIdVeicolo());
                    psAssociazione.setString(2, pezzo.getIdPezzo());
                    psAssociazione.executeUpdate();
                }

                conn.commit();

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }

        } catch (Exception e) {
            throw new DAOException("Errore durante il salvataggio della Legends", e);
        }
    }

    @Override
    public Optional<Legends> trovaLegendsPerId(String idVeicolo) {
        String sqlVeicolo = """
            SELECT id_veicolo, tipo_veicolo, km_totali
            FROM veicolo
            WHERE id_veicolo = ?
            AND tipo_veicolo = ?
            """;

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sqlVeicolo)
        ) {
            ps.setString(1, idVeicolo);
            ps.setString(2, TipoVeicolo.LEGENDS.name());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                Legends legends = new Legends(rs.getString("id_veicolo"));

                double kmTotali = rs.getDouble("km_totali");
                legends.percorriKm(kmTotali);

                caricaPezziDelVeicolo(conn, legends);

                return Optional.of(legends);
            }

        } catch (Exception e) {
            throw new DAOException("Errore durante la ricerca della Legends", e);
        }
    }

    @Override
    public List<Legends> trovaTutteLegends() {
        String sql = """
            SELECT id_veicolo
            FROM veicolo
            WHERE tipo_veicolo = ?
            ORDER BY id_veicolo
            """;

        List<Legends> risultato = new ArrayList<>();

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, TipoVeicolo.LEGENDS.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trovaLegendsPerId(rs.getString("id_veicolo"))
                            .ifPresent(risultato::add);
                }
            }

            return risultato;

        } catch (Exception e) {
            throw new DAOException("Errore durante il caricamento delle Legends", e);
        }
    }

    private void caricaPezziDelVeicolo(Connection conn, Legends legends) throws Exception {
        String sql = """
            SELECT p.id_pezzo, p.tipo_pezzo, p.km_max, p.tempo_max, p.km_attuali, p.tempo_attuale
            FROM pezzo p
            JOIN veicolo_pezzo vp ON p.id_pezzo = vp.id_pezzo
            WHERE vp.id_veicolo = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, legends.getIdVeicolo());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    legends.aggiungiPezzo(creaPezzoDaResultSet(rs));
                }
            }
        }
    }

    private Pezzo creaPezzoDaResultSet(ResultSet rs) throws Exception {
        Pezzo pezzo = new Pezzo(
                rs.getString("id_pezzo"),
                TipoPezzo.valueOf(rs.getString("tipo_pezzo")),
                rs.getDouble("km_max"),
                rs.getInt("tempo_max")
        );

        pezzo.aggiornaUtilizzo(
                rs.getDouble("km_attuali"),
                rs.getInt("tempo_attuale")
        );

        return pezzo;
    }
}