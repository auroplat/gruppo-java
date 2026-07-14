package it.unipv.bitFactory.controller;

import java.util.List;

import it.unipv.bitFactory.dao.interfacce.LegendsDAO;
import it.unipv.bitFactory.dao.interfacce.SessioneDAO;
import it.unipv.bitFactory.model.sessioni.Sessione;
import it.unipv.bitFactory.model.veicoli.Legends;

public class GestioneSessioniController {

    private final LegendsDAO legendsDAO;
    private final SessioneDAO sessioneDAO;

    public GestioneSessioniController(LegendsDAO legendsDAO, SessioneDAO sessioneDAO) {

        if (legendsDAO == null) {
            throw new IllegalArgumentException("Il DAO delle macchine non può essere null");
        }

        if (sessioneDAO == null) {
            throw new IllegalArgumentException("Il DAO delle sessioni non può essere null");
        }

        this.legendsDAO = legendsDAO;
        this.sessioneDAO = sessioneDAO;
    }

    public void registraSessione(String idMacchina, Sessione sessione) {

        if (idMacchina == null || idMacchina.isBlank()) {
            throw new IllegalArgumentException("L'id della macchina non può essere vuoto");
        }

        if (sessione == null) {
            throw new IllegalArgumentException("La sessione non può essere null");
        }

        Legends legends = legendsDAO.trovaPerId(idMacchina)
                .orElseThrow(() -> new IllegalArgumentException("Macchina non trovata: " + idMacchina));

        legends.applicaSessione(sessione);
        legendsDAO.salva(legends);
        sessioneDAO.salva(idMacchina, sessione);
    }

    public List<String> elencaIdMacchine() {
        return legendsDAO.trovaTutte().stream().map(Legends::getId).toList();
    }
}