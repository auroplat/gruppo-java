package it.unipv.bitFactory.thread;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.model.sessioni.Sessione;

/**
 * Thread esplicito che registra le sessioni e aggiorna l'usura
 * della macchina e dei suoi pezzi.
 *
 * I thread HTTP producono richieste; questo thread le consuma.
 */
public final class UsuraPezziThread extends Thread {

    private final GestioneSessioniController controller;
    private final DatabaseWriteLock databaseWriteLock;
    private final Deque<RichiestaUsura> coda = new ArrayDeque<>();

    private boolean accettaRichieste = true;

    public UsuraPezziThread(
            GestioneSessioniController controller,
            DatabaseWriteLock databaseWriteLock) {

        super("thread-usura-pezzi");

        this.controller = Objects.requireNonNull(
                controller,
                "Il controller delle sessioni non può essere null"
        );

        this.databaseWriteLock = Objects.requireNonNull(
                databaseWriteLock,
                "Il lock del database non può essere null"
        );
    }

    /**
     * Metodo synchronized chiamato dai thread HTTP.
     * Protegge la coda condivisa e risveglia il worker.
     */
    public synchronized CompletableFuture<Void> inviaAggiornamento(
            String idMacchina,
            Sessione sessione) {

        if (!accettaRichieste) {
            throw new IllegalStateException(
                    "Il thread di aggiornamento usura è in arresto"
            );
        }

        if (idMacchina == null || idMacchina.isBlank()) {
            throw new IllegalArgumentException(
                    "L'id della macchina non può essere vuoto"
            );
        }

        Objects.requireNonNull(sessione, "La sessione non può essere null");

        CompletableFuture<Void> risultato = new CompletableFuture<>();

        coda.addLast(new RichiestaUsura(
                idMacchina.trim(),
                sessione,
                risultato
        ));

        System.out.printf(
                "[%s] richiesta usura accodata per %s; coda=%d%n",
                Thread.currentThread().getName(),
                idMacchina,
                coda.size()
        );

        notifyAll();
        return risultato;
    }

    private synchronized RichiestaUsura attendiRichiesta()
            throws InterruptedException {

        while (coda.isEmpty() && accettaRichieste) {
            System.out.println("[thread-usura-pezzi] WAITING");
            wait();
        }

        if (coda.isEmpty()) {
            return null;
        }

        return coda.removeFirst();
    }

    @Override
    public void run() {
        System.out.println("[thread-usura-pezzi] avviato");

        try {
            while (true) {
                RichiestaUsura richiesta = attendiRichiesta();

                if (richiesta == null) {
                    break;
                }

                eseguiRichiesta(richiesta);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            completaRichiesteRimanentiConErrore(
                    new IllegalStateException(
                            "Thread usura interrotto prima del completamento",
                            e
                    )
            );

        } finally {
            System.out.println("[thread-usura-pezzi] terminato");
        }
    }

    private void eseguiRichiesta(RichiestaUsura richiesta) {
        try {
            databaseWriteLock.esegui(
                    "aggiornamento usura macchina " + richiesta.idMacchina(),
                    () -> controller.registraSessione(
                            richiesta.idMacchina(),
                            richiesta.sessione()
                    )
            );

            richiesta.risultato().complete(null);

        } catch (Exception e) {
            richiesta.risultato().completeExceptionally(e);
        }
    }

    /**
     * Non accetta nuove richieste, ma termina quelle già in coda.
     */
    public synchronized void arrestaThread() {
        accettaRichieste = false;
        notifyAll();
    }

    private synchronized void completaRichiesteRimanentiConErrore(
            RuntimeException errore) {

        while (!coda.isEmpty()) {
            coda.removeFirst()
                    .risultato()
                    .completeExceptionally(errore);
        }
    }

    private record RichiestaUsura(
            String idMacchina,
            Sessione sessione,
            CompletableFuture<Void> risultato) {
    }
}
