package it.unipv.bitFactory.adapter;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import it.unipv.bitFactory.external.SessioneEsterna;
import it.unipv.bitFactory.model.sessioni.Sessione;
import it.unipv.bitFactory.model.sessioni.TipoSessione;

public final class SessioneEsternaAdapter extends Sessione {

    private final SessioneEsterna sessioneEsterna;
    private final TipoSessione tipoSessione;

    public SessioneEsternaAdapter(
            SessioneEsterna sessioneEsterna
    ) {
        super(
                valida(sessioneEsterna).getCircuito(),
                sessioneEsterna.getDistanzaKm(),
                sessioneEsterna.getDurataMinuti()
        );

        this.sessioneEsterna = sessioneEsterna;

        this.tipoSessione = TipoSessione.daStringa(
                sessioneEsterna.getCategoria()
        );

        validaDatiSpecifici();
    }

    private static SessioneEsterna valida(
            SessioneEsterna sessioneEsterna
    ) {
        return Objects.requireNonNull(
                sessioneEsterna,
                "La sessione esterna non può essere null"
        );
    }

    private void validaDatiSpecifici() {

        switch (tipoSessione) {

            case TEST -> {
                String descrizione =
                        sessioneEsterna.getDescrizione();

                if (descrizione == null
                        || descrizione.isBlank()) {

                    throw new IllegalArgumentException(
                            "Un test esterno deve avere una descrizione"
                    );
                }
            }

            case GARA -> {
                Integer posizione =
                        sessioneEsterna.getPosizioneFinale();

                if (posizione == null || posizione <= 0) {
                    throw new IllegalArgumentException(
                            "Una gara esterna deve avere una posizione positiva"
                    );
                }
            }
        }
    }

    @Override
    public TipoSessione getTipoSessione() {
        return tipoSessione;
    }

    @Override
    public Optional<String> getDescrizioneOpzionale() {

        if (tipoSessione == TipoSessione.TEST) {
            return Optional.of(
                    sessioneEsterna.getDescrizione()
            );
        }

        return Optional.empty();
    }

    @Override
    public OptionalInt getPosizioneFinaleOpzionale() {

        if (tipoSessione == TipoSessione.GARA) {
            return OptionalInt.of(
                    sessioneEsterna.getPosizioneFinale()
            );
        }

        return OptionalInt.empty();
    }

    public SessioneEsterna getSessioneEsterna() {
        return sessioneEsterna;
    }
}