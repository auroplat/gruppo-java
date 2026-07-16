package it.unipv.bitFactory.model.sessioni;

import java.util.Optional;
import java.util.OptionalInt;

public abstract class Sessione {

    private final String luogo;
    private final double kmPercorsi;
    private final int tempoPassato;

    public Sessione(String luogo, double kmPercorsi, int tempoPassato) {

        if (luogo == null || luogo.isBlank()) {
            throw new IllegalArgumentException("Il luogo non può essere vuoto");
        }

        if (kmPercorsi < 0 || tempoPassato < 0) {
            throw new IllegalArgumentException("Km e tempo non possono essere negativi");
        }

        this.luogo = luogo;
        this.kmPercorsi = kmPercorsi;
        this.tempoPassato = tempoPassato;
    }

    public double getKmPercorsi() {
        return kmPercorsi;
    }

    public int getTempo() {
        return tempoPassato;
    }

    public String getLuogo() {
        return luogo;
    }

    public abstract TipoSessione getTipoSessione();

    public Optional<String> getDescrizioneOpzionale() {
        return Optional.empty();
    }

    public OptionalInt getPosizioneFinaleOpzionale() {
        return OptionalInt.empty();
    }
}
