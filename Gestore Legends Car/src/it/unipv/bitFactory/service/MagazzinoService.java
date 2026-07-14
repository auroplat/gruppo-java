package it.unipv.bitFactory.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import it.unipv.bitFactory.dao.interfacce.MagazzinoDAO;
import it.unipv.bitFactory.model.magazzino.StatoDisponibilita;
import it.unipv.bitFactory.model.magazzino.VoceMagazzino;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;

public class MagazzinoService {

    private final MagazzinoDAO magazzinoDAO;

    public MagazzinoService(MagazzinoDAO magazzinoDAO) {
        if (magazzinoDAO == null) {
            throw new IllegalArgumentException(
                    "Il DAO magazzino non può essere null"
            );
        }

        this.magazzinoDAO = magazzinoDAO;
    }

    public StatoDisponibilita controllaDisponibilita(String idPezzo) {
        return cercaPezzo(idPezzo).getStatoDisponibilita();
    }

    public VoceMagazzino cercaPezzo(String idPezzo) {
        return magazzinoDAO.trovaPerIdPezzo(idPezzo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pezzo non trovato: " + idPezzo
                ));
    }

    public List<VoceMagazzino> visualizzaMagazzino() {
        return magazzinoDAO.trovaTutti();
    }

    public StatoDisponibilita aggiornaQuantitaPezzo(
            String idPezzo,
            int nuovaQuantita) {

        magazzinoDAO.aggiornaQuantita(idPezzo, nuovaQuantita);

        if (nuovaQuantita == 0) {
            return StatoDisponibilita.ESAURITO;
        }

        return controllaDisponibilita(idPezzo);
    }

    public List<Pezzo> trovaPezziLiberi(TipoPezzo tipoPezzo) {
        return magazzinoDAO.trovaPezziLiberi(tipoPezzo);
    }

    public void aggiungiPezzi(
            TipoPezzo tipoPezzo,
            int quantita,
            double kmMax,
            int tempoMax) {

        magazzinoDAO.aggiungiPezzi(
                tipoPezzo,
                quantita,
                kmMax,
                tempoMax
        );
    }

    public void creaMacchina(String idMacchina) {
        magazzinoDAO.creaMacchina(
                idMacchina,
                ricettaLegends()
        );
    }

    public void cambiaPezzo(
            String idMacchina,
            TipoPezzo tipoPezzo,
            String idNuovoPezzo) {

        magazzinoDAO.cambiaPezzo(
                idMacchina,
                tipoPezzo,
                idNuovoPezzo
        );
    }

    private Map<TipoPezzo, Integer> ricettaLegends() {
        Map<TipoPezzo, Integer> ricetta =
                new EnumMap<>(TipoPezzo.class);

        ricetta.put(TipoPezzo.SCOCCA, 1);
        ricetta.put(TipoPezzo.MOTORE, 1);
        ricetta.put(TipoPezzo.VOLANTE, 1);
        ricetta.put(TipoPezzo.RUOTA, 4);
        ricetta.put(TipoPezzo.FRENO, 4);

        return ricetta;
    }
}
