package it.unipv.bitFactory.model.sessioni;

import java.util.Optional;

public class Test extends Sessione {

    private final String descrizione;

    public Test(String luogo, double kmPercorsi, int tempoPassato, String descrizione) {
        super(luogo, kmPercorsi, tempoPassato);

        if (descrizione == null || descrizione.isBlank()) {
            throw new IllegalArgumentException("La descrizione non può essere vuota");
        }

        this.descrizione = descrizione;
    }

    @Override
    public TipoSessione getTipoSessione() {
        return TipoSessione.TEST;
    }

    public String getDescrizione() {
        return descrizione;
    }

    @Override
    public Optional<String> getDescrizioneOpzionale() {
        return Optional.of(descrizione);
    }
}
