package it.unipv.bitFactory.thread;

import java.util.Objects;

import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.model.sessioni.Sessione;

/**
 * Worker che elabora una singola richiesta.
 *
 * Non gestisce la coda e non crea altri thread.
 * Il numero di istanze contemporaneamente attive è controllato
 * dal semaforo posseduto dal dispatcher.
 */
public final class SessioneWorker implements Runnable {

    private final String idMacchina;
    private final Sessione sessione;
    private final GestioneSessioniController controller;
    private final long tempoSimulazioneMs;

    public SessioneWorker(
            String idMacchina,
            Sessione sessione,
            GestioneSessioniController controller,
            long tempoSimulazioneMs) {

        if (idMacchina == null || idMacchina.isBlank()) {
            throw new IllegalArgumentException(
                    "L'identificativo della macchina non può essere vuoto"
            );
        }

        if (tempoSimulazioneMs < 0) {
            throw new IllegalArgumentException(
                    "Il tempo di simulazione non può essere negativo"
            );
        }

        this.idMacchina = idMacchina;

        this.sessione = Objects.requireNonNull(
                sessione,
                "La sessione non può essere null"
        );

        this.controller = Objects.requireNonNull(
                controller,
                "Il controller non può essere null"
        );

        this.tempoSimulazioneMs = tempoSimulazioneMs;
    }

    @Override
    public void run() {
        Thread corrente = Thread.currentThread();
        String nomeWorker = corrente.getName();

        try {
            System.out.printf(
                    "[%s] INIZIO elaborazione per %s%n",
                    nomeWorker,
                    idMacchina
            );

            /*
             * Con un solo permesso di creazione esiste un solo worker
             * attivo, quindi le scritture sono eseguite in sequenza.
             */
            controller.registraSessione(
                    idMacchina,
                    sessione
            );

            System.out.printf(
                    "[%s] SCRITTURA DB COMPLETATA per %s%n",
                    nomeWorker,
                    idMacchina
            );

            /*
             * Ritardo esclusivamente didattico.
             * Il worker mantiene occupato il permesso del semaforo
             * per rendere visibile l'accumulo nella coda.
             */
            if (tempoSimulazioneMs > 0) {
                System.out.printf(
                        "[%s] simulazione lavoro per %d ms%n",
                        nomeWorker,
                        tempoSimulazioneMs
                );

                Thread.sleep(tempoSimulazioneMs);
            }

        } catch (InterruptedException e) {
            System.out.printf(
                    "[%s] worker interrotto per %s%n",
                    nomeWorker,
                    idMacchina
            );

            corrente.interrupt();

        } catch (RuntimeException e) {
            System.err.printf(
                    "[%s] errore durante la scrittura per %s: %s%n",
                    nomeWorker,
                    idMacchina,
                    e.getMessage()
            );
        }
    }
}
