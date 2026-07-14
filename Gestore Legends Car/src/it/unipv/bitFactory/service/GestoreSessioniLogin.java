package it.unipv.bitFactory.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import it.unipv.bitFactory.model.persona.Addetto;
import it.unipv.bitFactory.model.persona.Ruolo;

public class GestoreSessioniLogin {

    public static final String NOME_COOKIE = "BITFACTORY_SESSION";

    private final Map<String, Addetto> sessioni;

    public GestoreSessioniLogin() {
        this.sessioni = new ConcurrentHashMap<>();
    }

    public String creaSessione(Addetto addetto) {

        if (addetto == null) {
            throw new IllegalArgumentException(
                    "L'addetto non può essere nullo"
            );
        }

        String sessionId = UUID.randomUUID().toString();
        sessioni.put(sessionId, addetto);

        return sessionId;
    }

    public Addetto trovaAddetto(String sessionId) {

        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }

        return sessioni.get(sessionId);
    }

    public boolean haRuolo(
            String sessionId,
            Ruolo ruoloRichiesto) {

        if (ruoloRichiesto == null) {
            return false;
        }

        Addetto addetto = trovaAddetto(sessionId);

        return addetto != null
                && ruoloRichiesto.equals(addetto.getRuolo());
    }

    public void eliminaSessione(String sessionId) {

        if (sessionId != null && !sessionId.isBlank()) {
            sessioni.remove(sessionId);
        }
    }
}
