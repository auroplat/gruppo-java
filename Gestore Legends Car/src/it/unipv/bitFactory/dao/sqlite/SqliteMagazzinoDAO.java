package it.unipv.bitFactory.dao.magazzino;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import it.unipv.bitFactory.dao.DAOException;
import it.unipv.bitFactory.model.magazzino.SoglieMagazzino;
import it.unipv.bitFactory.model.magazzino.StatoDisponibilita;
import it.unipv.bitFactory.model.magazzino.VoceMagazzino;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;

public class SqliteMagazzinoDAO implements MagazzinoDAO {

    private final String urlDatabase;
    private final SoglieMagazzino soglieMagazzino;

    public SqliteMagazzinoDAO(String percorsoDatabase, SoglieMagazzino soglieMagazzino) {
        if (percorsoDatabase == null || percorsoDatabase.isBlank()) {
            throw new IllegalArgumentException("Il percorso del database non può essere vuoto");
        }

        if (soglieMagazzino == null) {
            throw new IllegalArgumentException("Le soglie del magazzino non possono essere null");
        }

        this.urlDatabase = "jdbc:sqlite:" + percorsoDatabase;
        this.soglieMagazzino = soglieMagazzino;
    }

    private Connection getConnection() throws Exception {
        return DriverManager.getConnection(urlDatabase);
    }

    @Override
    public Optional<VoceMagazzino> trovaPerIdPezzo(String idPezzo) {
        String sql = """
            SELECT id_pezzo, tipo_pezzo, quantita
            FROM magazzino
            WHERE id_pezzo = ?
            """;

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, idPezzo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(creaVoceDaResultSet(rs));
                }
            }

            return Optional.empty();

        } catch (Exception e) {
            throw new DAOException("Errore durante la ricerca del pezzo in magazzino", e);
        }
    }

    @Override
    public List<VoceMagazzino> trovaTutti() {
        String sql = """
            SELECT id_pezzo, tipo_pezzo, quantita
            FROM magazzino
            ORDER BY tipo_pezzo, id_pezzo
            """;

        List<VoceMagazzino> risultato = new ArrayList<>();

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                risultato.add(creaVoceDaResultSet(rs));
            }

            return risultato;

        } catch (Exception e) {
            throw new DAOException("Errore durante il caricamento del magazzino", e);
        }
    }

    @Override
    public void aggiornaQuantita(String idPezzo, int nuovaQuantita) {
        if (idPezzo == null || idPezzo.isBlank()) {
            throw new IllegalArgumentException("L'id del pezzo non può essere vuoto");
        }

        if (nuovaQuantita < 0) {
            throw new IllegalArgumentException("La nuova quantità non può essere negativa");
        }

        String sql = """
            UPDATE magazzino
            SET quantita = ?
            WHERE id_pezzo = ?
            """;

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, nuovaQuantita);
            ps.setString(2, idPezzo);

            int righeModificate = ps.executeUpdate();

            if (righeModificate == 0) {
                throw new DAOException("Nessun pezzo trovato con id: " + idPezzo);
            }

        } catch (Exception e) {
            throw new DAOException("Errore durante l'aggiornamento della quantità", e);
        }
    }

    private VoceMagazzino creaVoceDaResultSet(ResultSet rs) throws Exception {
        String idPezzo = rs.getString("id_pezzo");
        TipoPezzo tipoPezzo = TipoPezzo.valueOf(rs.getString("tipo_pezzo"));
        int quantita = rs.getInt("quantita");

        StatoDisponibilita stato =
                soglieMagazzino.calcolaStato(quantita);

        return new VoceMagazzino(
                idPezzo,
                tipoPezzo,
                quantita,
                stato
        );
    }
}