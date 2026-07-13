package it.unipv.bitFactory.dao.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import it.unipv.bitFactory.dao.interfacce.DAOException;
import it.unipv.bitFactory.model.magazzino.VoceMagazzino;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;

public class SqliteMagazzinoDAO implements MagazzinoDAO {

    private final String urlDatabase;

    public SqliteMagazzinoDAO(String percorsoDatabase) {
        if (percorsoDatabase == null || percorsoDatabase.isBlank()) {
            throw new IllegalArgumentException("Il percorso del database non può essere vuoto");
        }
        this.urlDatabase = "jdbc:sqlite:" + percorsoDatabase;
    }

    private Connection getConnection() throws Exception {
        return DriverManager.getConnection(urlDatabase);
    }

    @Override
    public Optional<VoceMagazzino> trovaPerIdPezzo(String idPezzo) {
        validaId(idPezzo);
        String sql = "SELECT id_pezzo, tipo, id_macchina FROM pezzo WHERE id_pezzo = ?";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPezzo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(creaVoceDaResultSet(rs)) : Optional.empty();
            }
        } catch (Exception e) {
            throw new DAOException("Errore durante la ricerca del pezzo in magazzino", e);
        }
    }

    @Override
    public List<VoceMagazzino> trovaTutti() {
        return eseguiRicerca("SELECT id_pezzo, tipo, id_macchina FROM pezzo ORDER BY tipo, id_pezzo");
    }

    @Override
    public List<VoceMagazzino> trovaDisponibili() {
        return eseguiRicerca("SELECT id_pezzo, tipo, id_macchina FROM pezzo WHERE id_macchina IS NULL ORDER BY tipo, id_pezzo");
    }

    @Override
    public void inserisciPezzo(Pezzo pezzo) {
        if (pezzo == null) {
            throw new IllegalArgumentException("Il pezzo non può essere null");
        }

        String sql = """
            INSERT INTO pezzo (id_pezzo, tipo, id_macchina, km, km_max, tempo_utilizzo, tempo_max)
            VALUES (?, ?, NULL, ?, ?, ?, ?)
            """;

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pezzo.getIdPezzo());
            ps.setString(2, tipoDatabase(pezzo.getTipo()));
            ps.setDouble(3, pezzo.getKmAttuali());
            ps.setDouble(4, pezzo.getKmMax());
            ps.setInt(5, pezzo.getTempoAttuale());
            ps.setInt(6, pezzo.getTempoMax());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DAOException("Errore durante l'inserimento del pezzo", e);
        }
    }

    @Override
    public void eliminaPezzo(String idPezzo) {
        validaId(idPezzo);
        String sql = "DELETE FROM pezzo WHERE id_pezzo = ? AND id_macchina IS NULL";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPezzo.trim());
            if (ps.executeUpdate() != 1) {
                throw new DAOException("Pezzo inesistente oppure montato su un veicolo: " + idPezzo);
            }
        } catch (DAOException e) {
            throw e;
        } catch (Exception e) {
            throw new DAOException("Errore durante l'eliminazione del pezzo", e);
        }
    }

    private List<VoceMagazzino> eseguiRicerca(String sql) {
        List<VoceMagazzino> risultato = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                risultato.add(creaVoceDaResultSet(rs));
            }
            return risultato;
        } catch (Exception e) {
            throw new DAOException("Errore durante il caricamento del magazzino", e);
        }
    }

    private VoceMagazzino creaVoceDaResultSet(ResultSet rs) throws Exception {
        TipoPezzo tipo = tipoJava(rs.getString("tipo"));
        if (tipo == null) {
            throw new DAOException("Tipo pezzo non gestito: " + rs.getString("tipo"));
        }
        return new VoceMagazzino(rs.getString("id_pezzo"), tipo, rs.getString("id_macchina"));
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

    private void validaId(String idPezzo) {
        if (idPezzo == null || idPezzo.isBlank()) {
            throw new IllegalArgumentException("L'id del pezzo non può essere vuoto");
        }
    }
}
