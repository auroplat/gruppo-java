package it.unipv.bitFactory.model.sessioni;

import java.util.Objects;

public final class SessioneFactory {

    private SessioneFactory() {}

    public static Sessione crea(TipoSessione tipo, String luogo, double kmPercorsi, int tempoPassato,
            					String descrizione, String posizione) {

        Objects.requireNonNull(tipo,"Il tipo di sessione non può essere null");

        return switch (tipo) {
            case TEST -> new Test(luogo, kmPercorsi, tempoPassato, testoObbligatorio(descrizione,"descrizione"));
            case GARA -> new Gara(luogo, kmPercorsi, tempoPassato, interoPositivoObbligatorio(posizione,"posizione"));
        };
    }

    private static String testoObbligatorio(String valore, String nome) {

        if (valore == null || valore.isBlank()) {throw new IllegalArgumentException("Parametro obbligatorio mancante: " + nome);}
        return valore.trim();
    }

    private static int interoPositivoObbligatorio(String valore, String nome) {

        String normalizzato = testoObbligatorio(valore, nome);

        try {
            
        	int numero = Integer.parseInt(normalizzato);

            if (numero <= 0) {throw new IllegalArgumentException("Il parametro " + nome + " deve essere maggiore di zero");}

            return numero;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Il parametro " + nome + " deve essere un numero intero",e);
        }
    }
}