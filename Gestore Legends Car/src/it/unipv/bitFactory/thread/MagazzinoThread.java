package it.unipv.bitFactory.thread;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Thread esplicito che esegue le operazioni di modifica del magazzino.
 *
 * Riceve operazioni dal MagazzinoHttpHandler. Il contenuto concreto
 * dell'operazione resta nel controller/handler già presente.
 */
public final class MagazzinoThread extends Thread {

    @FunctionalInterface
    public interface OperazioneMagazzino {
        void esegui() throws Exception;
    }

    private final DatabaseWriteLock databaseWriteLock;
    private final Deque<RichiestaMagazzino> coda = new ArrayDeque<>();

    private boolean accettaRichieste = true;

    public MagazzinoThread(DatabaseWriteLock databaseWriteLock) {
        super("thread-magazzino");

        this.databaseWriteLock = Objects.requireNonNull(
                databaseWriteLock,
                "Il lock del database non può essere null"
        );
    }

    /**
     * Metodo synchronized: protegge la coda condivisa.
     */
    public synchronized CompletableFuture<Void> inviaModifica(
            String descrizione,
            OperazioneMagazzino operazione) {

        if (!accettaRichieste) {
            throw new IllegalStateException(
                    "Il thread del magazzino è in arresto"
            );
        }

        if (descrizione == null || descrizione.isBlank()) {
            throw new IllegalArgumentException(
                    "La descrizione dell'operazione non può essere vuota"
            );
        }

        Objects.requireNonNull(operazione, "L'operazione non può essere null");

        CompletableFuture<Void> risultato = new CompletableFuture<>();

        coda.addLast(new RichiestaMagazzino(
                descrizione.trim(),
                operazione,
                risultato
        ));

        System.out.printf(
                "[%s] modifica magazzino accodata: %s; coda=%d%n",
                Thread.currentThread().getName(),
                descrizione,
                coda.size()
        );

        notifyAll();
        return risultato;
    }

    private synchronized RichiestaMagazzino attendiRichiesta()
            throws InterruptedException {

        while (coda.isEmpty() && accettaRichieste) {
            System.out.println("[thread-magazzino] WAITING");
            wait();
        }

        if (coda.isEmpty()) {
            return null;
        }

        return coda.removeFirst();
    }

    @Override
    public void run() {
        System.out.println("[thread-magazzino] avviato");

        try {
            while (true) {
                RichiestaMagazzino richiesta = attendiRichiesta();

                if (richiesta == null) {
                    break;
                }

                eseguiRichiesta(richiesta);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            completaRichiesteRimanentiConErrore(
                    new IllegalStateException(
                            "Thread magazzino interrotto prima del completamento",
                            e
                    )
            );

        } finally {
            System.out.println("[thread-magazzino] terminato");
        }
    }

    private void eseguiRichiesta(RichiestaMagazzino richiesta) {
        try {
            databaseWriteLock.esegui(
                    richiesta.descrizione(),
                    richiesta.operazione()::esegui
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

    private record RichiestaMagazzino(
            String descrizione,
            OperazioneMagazzino operazione,
            CompletableFuture<Void> risultato) {
    }
}
