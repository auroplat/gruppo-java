package it.unipv.bitFactory.model.sessioni;

import java.util.Arrays;
import java.util.Locale;

public enum TipoSessione {

    GARA(),
    TEST();

    public static TipoSessione daStringa(String valore) {
        if (valore == null || valore.isBlank()) {
            throw new IllegalArgumentException("Il tipo di sessione non può essere vuoto");
        }

        String normalizzato = valore.trim().toUpperCase(Locale.ROOT);

        return Arrays.stream(values()).filter(tipo ->tipo.name().equals(normalizzato))
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException("Tipo di sessione non valido: " + valore));
    }
}