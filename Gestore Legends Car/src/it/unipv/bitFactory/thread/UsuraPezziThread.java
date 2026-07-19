package it.unipv.bitFactory.thread;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.model.sessioni.Sessione;

/**
 * Dispatcher delle richieste di registrazione delle sessioni.
 *
 * Responsabilità:
 * 1. Il ReentrantLock protegge la coda condivisa.
 * 2. La Condition sospende il dispatcher quando la coda è vuota.
 * 3. Il Semaphore limita il numero di worker che possono essere creati.
 *
 * Con NUMERO_MASSIMO_WORKER = 1 esiste un solo worker di scrittura
 * alla volta. Le richieste ulteriori rimangono nella coda.
 */
public final class UsuraPezziThread extends Thread {

    private static final int NUMERO_MASSIMO_WORKER = 1;
    private static final long TEMPO_SIMULAZIONE_MS = 5_000L;

    private final GestioneSessioniController controller;

    /*
     * Risorsa condivisa tra i produttori HTTP e il dispatcher.
     * Deve essere sempre letta o modificata mentre lockCoda è acquisito.
     */
    private final Deque<RichiestaSessione> coda = new ArrayDeque<>();

    /*
     * Lock esplicito per proteggere la coda.
     * La modalità fair favorisce i thread in attesa da più tempo.
     */
    private final ReentrantLock lockCoda = new ReentrantLock(true);

    /*
     * Condition associata al lock della coda.
     * Il dispatcher attende qui quando non esistono richieste.
     */
    private final Condition richiestaDisponibile =
            lockCoda.newCondition();

    /*
     * Semaforo usato esclusivamente per limitare la creazione
     * e l'esecuzione dei worker.
     *
     * Con un permesso, il dispatcher non può creare un nuovo worker
     * finché quello precedente non è terminato.
     */
    private final Semaphore permessiCreazioneWorker =
            new Semaphore(NUMERO_MASSIMO_WORKER, true);

    private final Set<Thread> workerAttivi =
            ConcurrentHashMap.newKeySet();

    private final AtomicInteger progressivoWorker =
            new AtomicInteger();

    private volatile boolean attivo = true;

    public UsuraPezziThread(GestioneSessioniController controller) {
        super("thread-dispatcher-sessioni");

        this.controller = Objects.requireNonNull(
                controller,
                "Il controller delle sessioni non può essere null"
        );
    }

    /**
     * Metodo produttore, chiamato dai thread HTTP.
     */
    public void inviaAggiornamento(
            String idMacchina,
            Sessione sessione) {

        if (idMacchina == null || idMacchina.isBlank()) {
            throw new IllegalArgumentException(
                    "L'identificativo della macchina non può essere vuoto"
            );
        }

        Objects.requireNonNull(
                sessione,
                "La sessione non può essere null"
        );

        lockCoda.lock();

        try {
            if (!attivo) {
                throw new IllegalStateException(
                        "Il dispatcher delle sessioni è stato arrestato"
                );
            }

            coda.addLast(
                    new RichiestaSessione(idMacchina, sessione)
            );

            System.out.printf(
                    "[%s] richiesta accodata per %s; "
                            + "richieste in coda: %d%n",
                    Thread.currentThread().getName(),
                    idMacchina,
                    coda.size()
            );

            /*
             * Risveglia il dispatcher se era sospeso sulla Condition.
             */
            richiestaDisponibile.signal();

        } finally {
            lockCoda.unlock();
        }
    }

    /**
     * Attende finché la coda contiene almeno una richiesta.
     *
     * Non estrae ancora l'elemento: in questo modo, se il semaforo
     * non concede la creazione di un worker, la richiesta resta
     * visibilmente nella coda.
     */
    private boolean attendiPresenzaRichiesta()
            throws InterruptedException {

        lockCoda.lockInterruptibly();

        try {
            while (coda.isEmpty() && attivo) {
                richiestaDisponibile.await();
            }

            return attivo && !coda.isEmpty();

        } finally {
            lockCoda.unlock();
        }
    }

    /**
     * Estrae la prima richiesta in ordine FIFO.
     */
    private RichiestaSessione estraiPrimaRichiesta() {
        lockCoda.lock();

        try {
            return coda.pollFirst();

        } finally {
            lockCoda.unlock();
        }
    }

    @Override
    public void run() {
        try {
            while (attivo) {

                /*
                 * Il dispatcher aspetta prima che esista lavoro.
                 * In questa fase non occupa alcun permesso.
                 */
                if (!attendiPresenzaRichiesta()) {
                    break;
                }

                /*
                 * Se il numero massimo di worker è già attivo,
                 * il dispatcher si blocca qui.
                 *
                 * La richiesta non è stata ancora estratta e rimane
                 * quindi nella coda protetta dal lock.
                 */
                permessiCreazioneWorker.acquire();

                boolean permessoTrasferitoAlWorker = false;

                try {
                    if (!attivo) {
                        break;
                    }

                    RichiestaSessione richiesta =
                            estraiPrimaRichiesta();

                    if (richiesta == null) {
                        continue;
                    }

                    avviaWorker(richiesta);

                    /*
                     * Da questo momento sarà il worker a restituire
                     * il permesso nel proprio finally.
                     */
                    permessoTrasferitoAlWorker = true;

                } finally {
                    /*
                     * Se il worker non è stato avviato, il dispatcher
                     * deve restituire immediatamente il permesso.
                     */
                    if (!permessoTrasferitoAlWorker) {
                        permessiCreazioneWorker.release();
                    }
                }
            }

        } catch (InterruptedException e) {
            if (attivo) {
                System.err.printf(
                        "[%s] dispatcher interrotto in modo inatteso%n",
                        getName()
                );
            }

            Thread.currentThread().interrupt();

        } finally {
            System.out.printf(
                    "[%s] dispatcher terminato%n",
                    getName()
            );
        }
    }

    private void avviaWorker(RichiestaSessione richiesta) {
        int numero = progressivoWorker.incrementAndGet();
        String nomeWorker = "thread-sessione-" + numero;

        SessioneWorker worker = new SessioneWorker(
                richiesta.idMacchina(),
                richiesta.sessione(),
                controller,
                TEMPO_SIMULAZIONE_MS
        );

        Thread threadWorker = new Thread(
                () -> {
                    try {
                        worker.run();

                    } finally {
                        workerAttivi.remove(
                                Thread.currentThread()
                        );

                        /*
                         * Il worker è terminato: consente al dispatcher
                         * di creare il worker successivo.
                         */
                        permessiCreazioneWorker.release();

                        System.out.printf(
                                "[%s] permesso di creazione rilasciato; "
                                        + "permessi disponibili: %d%n",
                                Thread.currentThread().getName(),
                                permessiCreazioneWorker.availablePermits()
                        );
                    }
                },
                nomeWorker
        );

        workerAttivi.add(threadWorker);

        try {
            threadWorker.start();

        } catch (RuntimeException e) {
            workerAttivi.remove(threadWorker);
            throw e;
        }
    }

    public void arrestaThread() {
        attivo = false;

        lockCoda.lock();

        try {
            richiestaDisponibile.signalAll();

        } finally {
            lockCoda.unlock();
        }

        /*
         * Interrompe il dispatcher anche se è fermo su:
         * - Condition.await();
         * - Semaphore.acquire().
         */
        interrupt();

        for (Thread worker : workerAttivi) {
            worker.interrupt();
        }
    }

    public int getNumeroRichiesteInCoda() {
        lockCoda.lock();

        try {
            return coda.size();

        } finally {
            lockCoda.unlock();
        }
    }

    public int getNumeroWorkerAttivi() {
        return workerAttivi.size();
    }

    public int getNumeroPermessiCreazioneDisponibili() {
        return permessiCreazioneWorker.availablePermits();
    }

    public int getNumeroThreadInAttesaDelSemaforo() {
        return permessiCreazioneWorker.getQueueLength();
    }

    private record RichiestaSessione(
            String idMacchina,
            Sessione sessione) {
    }
}
