package it.unipv.bitFactory.adapter;

import it.unipv.bitFactory.external.SessioneEsterna;
import it.unipv.bitFactory.model.sessioni.Sessione;
import it.unipv.bitFactory.model.sessioni.TipoSessione;
import java.util.Objects;

public class SessioneEsternaAdapter extends Sessione {

    private final SessioneEsterna sessioneEsterna;

    public SessioneEsternaAdapter(SessioneEsterna sessioneEsterna) {
        super(
            Objects.requireNonNull(sessioneEsterna, "La sessione esterna non può essere null").getCircuito(),
            sessioneEsterna.getDistanzaKm(),
            sessioneEsterna.getDurataMinuti()
        );

        this.sessioneEsterna = sessioneEsterna;
    }

    @Override
    public TipoSessione getTipoSessione() {
        String categoria = sessioneEsterna.getCategoria();

        if (categoria == null || categoria.isBlank()) {
            throw new IllegalArgumentException("Categoria esterna non valida");
        }

        switch (categoria.trim().toUpperCase()) {
            case "RACE":
            case "GARA":
                return TipoSessione.GARA;

            case "TEST":
                return TipoSessione.TEST;

            default:
                throw new IllegalArgumentException("Categoria esterna non riconosciuta: " + categoria);
        }
}

    public SessioneEsterna getSessioneEsterna() {
        return sessioneEsterna;
    }
}