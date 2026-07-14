package it.unipv.bitFactory.adapter;

import java.util.Objects;

import it.unipv.bitFactory.external.SessioneEsterna;
import it.unipv.bitFactory.model.sessioni.Sessione;
import it.unipv.bitFactory.model.sessioni.TipoSessione;

public class SessioneEsternaAdapter extends Sessione {

    private final SessioneEsterna sessioneEsterna;

    public SessioneEsternaAdapter(SessioneEsterna sessioneEsterna) {

        super(Objects.requireNonNull(sessioneEsterna, "La sessione esterna non può essere null")
        		.getCircuito(),
                sessioneEsterna.getDistanzaKm(),
                sessioneEsterna.getDurataMinuti()
        );

        this.sessioneEsterna = sessioneEsterna;
    }

    @Override
    public TipoSessione getTipoSessione() {
        return TipoSessione.daStringa(sessioneEsterna.getCategoria());
    }

    public SessioneEsterna getSessioneEsterna() {return sessioneEsterna;}
}