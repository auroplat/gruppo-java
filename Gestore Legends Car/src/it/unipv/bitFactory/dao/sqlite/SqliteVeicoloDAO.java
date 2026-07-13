package it.unipv.bitFactory.dao.veicoli;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import it.unipv.bitFactory.dao.DAOException;
import it.unipv.bitFactory.dao.LegendsDAO;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.model.veicoli.Legends;

public class SqliteVeicoloDAO implements VeicoloDAO, LegendsDAO {

    private final String urlDatabase;

    public SqliteVeicoloDAO(String percorsoDatabase) {
        if (percorsoDatabase == null || percorsoDatabase.isBlank()) {
            throw new IllegalArgumentException("Il percorso del database non può essere vuoto");
        }
        this.urlDatabase = "jdbc:sqlite:" + percorsoDatabase;
    }

    private Connection getConnection() throws Exception {
        Connection conn = DriverManager.getConnection(urlDatabase);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    @Override
    public void inizializzaDatabase() {
        String creaMacchine = """
            CREATE TABLE IF NOT EXISTS macchine (
                id_macchina TEXT PRIMARY KEY,
                km_macchina DOUBLE NOT NULL DEFAULT 0
            )
            """;
        String creaPezzo = """
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

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.execute(creaMacchine);
            st.execute(creaPezzo);
        } catch (Exception e) {
            throw new DAOException("Errore durante l'inizializzazione del database veicoli", e);
        }
    }

    @Override
    public boolean esisteVeicolo(String idVeicolo) {
        validaId(idVeicolo);
        String sql = "SELECT 1 FROM macchine WHERE id_macchina = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idVeicolo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new DAOException("Errore durante il controllo dell'esistenza del veicolo", e);
        }
    }

    @Override
    public int contaPezziLiberi(TipoPezzo tipoPezzo) {
        validaTipo(tipoPezzo);
        String sql = """
            SELECT COUNT(*) AS totale
            FROM pezzo
            WHERE LOWER(TRIM(tipo)) = ? AND id_macchina IS NULL
            """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tipoDatabase(tipoPezzo));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("totale") : 0;
            }
        } catch (Exception e) {
            throw new DAOException("Errore durante il conteggio dei pezzi liberi", e);
        }
    }

    @Override
    public List<Pezzo> trovaPezziLiberi(TipoPezzo tipoPezzo, int quantitaRichiesta) {
        validaTipo(tipoPezzo);
        if (quantitaRichiesta <= 0) {
            throw new IllegalArgumentException("La quantità richiesta deve essere maggiore di zero");
        }

        String sql = """
            SELECT id_pezzo, tipo,
                   COALESCE(km_max, 0) AS km_max,
                   COALESCE(tempo_max, 0) AS tempo_max,
                   COALESCE(km, 0) AS km,
                   COALESCE(tempo_utilizzo, 0) AS tempo_utilizzo
            FROM pezzo
            WHERE LOWER(TRIM(tipo)) = ? AND id_macchina IS NULL
            ORDER BY id_pezzo
            LIMIT ?
            """;

        List<Pezzo> pezzi = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tipoDatabase(tipoPezzo));
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

        String inserisciMacchina = """
            INSERT INTO macchine (id_macchina, km_macchina)
            VALUES (?, ?)
            ON CONFLICT(id_macchina) DO UPDATE SET km_macchina = excluded.km_macchina
            """;
        String salvaPezzo = """
            INSERT INTO pezzo (
                id_pezzo, tipo, id_macchina, km, km_max, tempo_utilizzo, tempo_max
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id_pezzo) DO UPDATE SET
                tipo = excluded.tipo,
                id_macchina = excluded.id_macchina,
                km = excluded.km,
                km_max = excluded.km_max,
                tempo_utilizzo = excluded.tempo_utilizzo,
                tempo_max = excluded.tempo_max
            WHERE pezzo.id_macchina IS NULL
               OR pezzo.id_macchina = excluded.id_macchina
            """;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psMacchina = conn.prepareStatement(inserisciMacchina);
                 PreparedStatement psPezzo = conn.prepareStatement(salvaPezzo)) {

                psMacchina.setString(1, legends.getIdVeicolo());
                psMacchina.setDouble(2, legends.getKmTotali());
                psMacchina.executeUpdate();

                for (Pezzo pezzo : legends.getTuttiPezzi()) {
                    psPezzo.setString(1, pezzo.getIdPezzo());
                    psPezzo.setString(2, tipoDatabase(pezzo.getTipo()));
                    psPezzo.setString(3, legends.getIdVeicolo());
                    psPezzo.setDouble(4, pezzo.getKmAttuali());
                    psPezzo.setDouble(5, pezzo.getKmMax());
                    psPezzo.setInt(6, pezzo.getTempoAttuale());
                    psPezzo.setInt(7, pezzo.getTempoMax());
                    if (psPezzo.executeUpdate() != 1) {
                        throw new DAOException("Pezzo già assegnato a un altro veicolo: " + pezzo.getIdPezzo());
                    }
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
    public Optional<Pezzo> trovaPezzoLiberoPerId(String idPezzo) {
        if (idPezzo == null || idPezzo.isBlank()) {
            throw new IllegalArgumentException("L'id del pezzo non può essere vuoto");
        }

        String sql = """
            SELECT id_pezzo, tipo,
                   COALESCE(km_max, 0) AS km_max,
                   COALESCE(tempo_max, 0) AS tempo_max,
                   COALESCE(km, 0) AS km,
                   COALESCE(tempo_utilizzo, 0) AS tempo_utilizzo
            FROM pezzo
            WHERE id_pezzo = ? AND id_macchina IS NULL
            """;

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPezzo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(creaPezzoDaResultSet(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DAOException("Errore durante la ricerca del pezzo libero", e);
        }
    }

    @Override
    public void sostituisciPezzo(String idVeicolo, String idPezzoVecchio, String idPezzoNuovo) {
        validaId(idVeicolo);
        if (idPezzoVecchio == null || idPezzoVecchio.isBlank()
                || idPezzoNuovo == null || idPezzoNuovo.isBlank()) {
            throw new IllegalArgumentException("Gli ID dei pezzi non possono essere vuoti");
        }

        String liberaVecchio = "UPDATE pezzo SET id_macchina = NULL WHERE id_pezzo = ? AND id_macchina = ?";
        String montaNuovo = "UPDATE pezzo SET id_macchina = ? WHERE id_pezzo = ? AND id_macchina IS NULL";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psLibera = conn.prepareStatement(liberaVecchio);
                 PreparedStatement psMonta = conn.prepareStatement(montaNuovo)) {

                psLibera.setString(1, idPezzoVecchio.trim());
                psLibera.setString(2, idVeicolo.trim());
                if (psLibera.executeUpdate() != 1) {
                    throw new DAOException("Il pezzo da sostituire non è montato sul veicolo");
                }

                psMonta.setString(1, idVeicolo.trim());
                psMonta.setString(2, idPezzoNuovo.trim());
                if (psMonta.executeUpdate() != 1) {
                    throw new DAOException("Il nuovo pezzo non è disponibile");
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new DAOException("Errore durante la sostituzione del pezzo", e);
        }
    }

    @Override
    public void eliminaVeicolo(String idVeicolo) {
        validaId(idVeicolo);
        String liberaPezzi = "UPDATE pezzo SET id_macchina = NULL WHERE id_macchina = ?";
        String eliminaMacchina = "DELETE FROM macchine WHERE id_macchina = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psLibera = conn.prepareStatement(liberaPezzi);
                 PreparedStatement psElimina = conn.prepareStatement(eliminaMacchina)) {
                psLibera.setString(1, idVeicolo.trim());
                psLibera.executeUpdate();

                psElimina.setString(1, idVeicolo.trim());
                if (psElimina.executeUpdate() != 1) {
                    throw new DAOException("Veicolo non trovato: " + idVeicolo);
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new DAOException("Errore durante l'eliminazione del veicolo", e);
        }
    }

    @Override
    public Optional<Legends> trovaLegendsPerId(String idVeicolo) {
        validaId(idVeicolo);
        String sql = "SELECT id_macchina, COALESCE(km_macchina, 0) AS km_macchina FROM macchine WHERE id_macchina = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idVeicolo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Legends legends = new Legends(rs.getString("id_macchina"));
                legends.percorriKm(rs.getDouble("km_macchina"));
                caricaPezziDelVeicolo(conn, legends);
                return Optional.of(legends);
            }
        } catch (Exception e) {
            throw new DAOException("Errore durante la ricerca della Legends", e);
        }
    }

    @Override
    public List<Legends> trovaTutteLegends() {
        String sql = "SELECT id_macchina FROM macchine ORDER BY id_macchina";
        List<Legends> risultato = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id_macchina");
                trovaLegendsPerId(id).ifPresent(risultato::add);
            }
            return risultato;
        } catch (Exception e) {
            throw new DAOException("Errore durante il caricamento delle Legends", e);
        }
    }

    private void caricaPezziDelVeicolo(Connection conn, Legends legends) throws Exception {
        String sql = """
            SELECT id_pezzo, tipo,
                   COALESCE(km_max, 0) AS km_max,
                   COALESCE(tempo_max, 0) AS tempo_max,
                   COALESCE(km, 0) AS km,
                   COALESCE(tempo_utilizzo, 0) AS tempo_utilizzo
            FROM pezzo
            WHERE id_macchina = ?
            ORDER BY tipo, id_pezzo
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, legends.getIdVeicolo());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TipoPezzo tipo = tipoJava(rs.getString("tipo"));
                    if (tipo != null) legends.montaPezzo(creaPezzoDaResultSet(rs));
                }
            }
        }
    }

    private Pezzo creaPezzoDaResultSet(ResultSet rs) throws Exception {
        TipoPezzo tipo = tipoJava(rs.getString("tipo"));
        if (tipo == null) throw new DAOException("Tipo pezzo non gestito: " + rs.getString("tipo"));
        Pezzo pezzo = new Pezzo(rs.getString("id_pezzo"), tipo, rs.getDouble("km_max"), rs.getInt("tempo_max"));
        pezzo.aggiornaUtilizzo(rs.getDouble("km"), rs.getInt("tempo_utilizzo"));
        return pezzo;
    }

    private String tipoDatabase(TipoPezzo tipo) {
        return switch (tipo) {
            case RUOTA -> "pneumatico";
            case FRENO -> "freno";
            default -> tipo.name().toLowerCase(Locale.ROOT);
        };
    }

    private TipoPezzo tipoJava(String valore) {
        if (valore == null) return null;
        return switch (valore.trim().toLowerCase(Locale.ROOT)) {
            case "pneumatico", "pneumatici", "ruota", "gomme" -> TipoPezzo.RUOTA;
            case "freno", "freni" -> TipoPezzo.FRENO;
            case "scocca" -> TipoPezzo.SCOCCA;
            case "motore" -> TipoPezzo.MOTORE;
            case "cambio" -> TipoPezzo.CAMBIO;
            case "volante" -> TipoPezzo.VOLANTE;
            default -> null;
        };
    }

    private void validaId(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("L'id del veicolo non può essere vuoto");
    }

    private void validaTipo(TipoPezzo tipo) {
        if (tipo == null) throw new IllegalArgumentException("Il tipo del pezzo non può essere null");
    }
    // Metodi dell'interfaccia LegendsDAO mantenuti per rendere compatibile
    // il controller delle sessioni con la persistenza SQLite.
    @Override
    public void salva(Legends legends) {
        salvaLegends(legends);
    }

    @Override
    public Optional<Legends> trovaPerId(String id) {
        return trovaLegendsPerId(id);
    }

    @Override
    public List<Legends> trovaTutte() {
        return trovaTutteLegends();
    }

    @Override
    public void elimina(String id) {
        eliminaVeicolo(id);
    }

}
