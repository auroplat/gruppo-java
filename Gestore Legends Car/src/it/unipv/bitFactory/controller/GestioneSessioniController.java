package it.unipv.bitFactory.controller;

import java.util.List;

import it.unipv.bitFactory.dao.LegendsDAO;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.model.sessioni.Sessione;
import it.unipv.bitFactory.model.veicoli.Legends;

public class GestioneSessioniController {

    private final LegendsDAO legendsDAO;

    public GestioneSessioniController(
            LegendsDAO legendsDAO) {

        if (legendsDAO == null) {
            throw new IllegalArgumentException(
                    "Il DAO non può essere null"
            );
        }

        this.legendsDAO = legendsDAO;
    }

    public void registraSessione(
            String idMacchina,
            Sessione sessione) {

        if (sessione == null) {
            throw new IllegalArgumentException(
                    "La sessione non può essere null"
            );
        }

        Legends legends =
                legendsDAO.trovaPerId(idMacchina)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Macchina non trovata: "
                                                + idMacchina
                                )
                        );

        legends.applicaSessione(sessione);
        legendsDAO.salva(legends);
    }

    public void registraSessioneSelettiva(
            String idMacchina,
            Sessione sessione,
            List<TipoPezzo> pezziDaAggiornare) {

        Legends legends =
                legendsDAO.trovaPerId(idMacchina)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Macchina non trovata: "
                                                + idMacchina
                                )
                        );

        legends.applicaSessioneSelettiva(
                sessione,
                pezziDaAggiornare
        );

        legendsDAO.salva(legends);
    }

    public List<String> elencaIdMacchine() {
        return legendsDAO.trovaTutte()
                .stream()
                .map(Legends::getId)
                .toList();
    }
}
