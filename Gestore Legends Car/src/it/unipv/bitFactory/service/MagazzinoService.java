package it.unipv.bitFactory.service;

import java.util.List;

import it.unipv.bitFactory.dao.MagazzinoDAO;
import it.unipv.bitFactory.model.magazzino.StatoDisponibilita;
import it.unipv.bitFactory.model.magazzino.VoceMagazzino;
import it.unipv.bitFactory.model.pezzi.Pezzo;

public class MagazzinoService {

    private final MagazzinoDAO magazzinoDAO;

    public MagazzinoService(MagazzinoDAO magazzinoDAO) {
        if (magazzinoDAO == null) {
            throw new IllegalArgumentException("Il DAO magazzino non può essere null");
        }
        this.magazzinoDAO = magazzinoDAO;
    }

    public StatoDisponibilita controllaDisponibilita(String idPezzo) {
        return cercaPezzo(idPezzo).getStatoDisponibilita();
    }

    public VoceMagazzino cercaPezzo(String idPezzo) {
        return magazzinoDAO.trovaPerIdPezzo(idPezzo)
                .orElseThrow(() -> new IllegalArgumentException("Pezzo non trovato: " + idPezzo));
    }

    public List<VoceMagazzino> visualizzaMagazzino() {
        return magazzinoDAO.trovaTutti();
    }

    public List<VoceMagazzino> visualizzaPezziDisponibili() {
        return magazzinoDAO.trovaDisponibili();
    }

    public void aggiungiPezzo(Pezzo pezzo) {
        magazzinoDAO.inserisciPezzo(pezzo);
    }

    public void rimuoviPezzo(String idPezzo) {
        VoceMagazzino voce = cercaPezzo(idPezzo);
        if (!voce.isDisponibile()) {
            throw new IllegalStateException(
                    "Non è possibile eliminare il pezzo perché è montato sul veicolo: " + voce.getIdVeicolo());
        }
        magazzinoDAO.eliminaPezzo(idPezzo);
    }
}
