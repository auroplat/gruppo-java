package it.unipv.bitFactory.service;

import java.util.List;
import java.util.Objects;

import it.unipv.bitFactory.adapter.SessioneEsternaAdapter;
import it.unipv.bitFactory.dao.interfacce.LegendsDAO;
import it.unipv.bitFactory.dao.interfacce.SessioneDAO;
import it.unipv.bitFactory.external.SessioneEsterna;
import it.unipv.bitFactory.model.sessioni.Sessione;
import it.unipv.bitFactory.model.veicoli.Legends;

public final class ServizioSessioni {

    private final LegendsDAO legendsDAO;
    private final SessioneDAO sessioneDAO;

    public ServizioSessioni(
            LegendsDAO legendsDAO,
            SessioneDAO sessioneDAO
    ) {
        this.legendsDAO = Objects.requireNonNull(
                legendsDAO,
                "Il DAO delle macchine non può essere null"
        );

        this.sessioneDAO = Objects.requireNonNull(
                sessioneDAO,
                "Il DAO delle sessioni non può essere null"
        );
    }

    public synchronized void registraSessione(
            String idMacchina,
            Sessione sessione
    ) {
        String id = validaIdMacchina(idMacchina);

        Objects.requireNonNull(
                sessione,
                "La sessione non può essere null"
        );

        Legends legends = legendsDAO
                .trovaPerId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Macchina non trovata: " + id
                ));

        legends.applicaSessione(sessione);

        legendsDAO.salva(legends);
        sessioneDAO.salva(id, sessione);
    }

    public List<String> elencaIdMacchine() {
        return legendsDAO
                .trovaTutte()
                .stream()
                .map(Legends::getId)
                .toList();
    }

    public void registraSessioneEsterna(
            String idMacchina,
            SessioneEsterna sessioneEsterna
    ) {
        Objects.requireNonNull(
                sessioneEsterna,
                "La sessione esterna non può essere null"
        );

        Sessione sessioneAdattata =
                new SessioneEsternaAdapter(sessioneEsterna);

        registraSessione(idMacchina, sessioneAdattata);
    }

    private String validaIdMacchina(String idMacchina) {
        if (idMacchina == null || idMacchina.isBlank()) {
            throw new IllegalArgumentException(
                    "L'id della macchina non può essere vuoto"
            );
        }

        return idMacchina.trim();
    }
}