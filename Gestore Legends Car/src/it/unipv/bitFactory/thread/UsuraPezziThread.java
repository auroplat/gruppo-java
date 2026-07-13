package it.unipv.bitFactory.thread;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.model.sessioni.Sessione;

/**
 * Dispatcher delle richieste di aggiornamento usura.
 *
 * La coda viene consumata da questo thread, ma ogni richiesta viene eseguita
 * da un thread-sessione dedicato. I thread delle sessioni condividono un
 * unico lock PRIVATO e INTERNO a questa classe, realizzato con il lock
 * intrinseco (monitor) dell'oggetto {@code monitorSessioni} tramite
 * synchronized, wait() e notifyAll().
 *
 * Poiché questi thread-sessione sono gli unici a scrivere sul database,
 * questo lock protegge da solo anche le scritture su SQLite: non serve
 * alcun lock esterno condiviso.
 */
public class UsuraPezziThread extends Thread {

    private static final long TEMPO_SLEEP_CON_LOCK_MS = 5_000L;

    private final GestioneSessioniController controller;

    /*
     * ============================================================
     *  Lock interno delle sessioni (monitor intrinseco)
     * ============================================================
     * Lo stato del lock è descritto da tre variabili, tutte protette
     * dal monitor dell'oggetto monitorSessioni:
     *  - lockSessioniOccupato: true se un thread-sessione detiene il lock
     *  - proprietarioLockSessioni: nome del thread proprietario (per i log)
     *  - threadSessioniInAttesa: quanti thread sono fermi in wait()
     *
     * NB: si usa un oggetto dedicato e NON "this", perché il monitor di
     * "this" è già impegnato dalla coppia inviaAggiornamento() /
     * attendiRichiesta() per la sincronizzazione della coda: tenere le due
     * condizioni su monitor separati evita risvegli incrociati inutili.
     */
    private final Object monitorSessioni = new Object();
    private boolean lockSessioniOccupato = false;
    private String proprietarioLockSessioni = null;
    private int threadSessioniInAttesa = 0;

    private final Deque<RichiestaUsura> coda = new ArrayDeque<>();
    private final Set<Thread> threadSessioniAttivi =
            ConcurrentHashMap.newKeySet();
    private final AtomicInteger progressivoSessione = new AtomicInteger();

    private volatile boolean attivo = true;

    public UsuraPezziThread(GestioneSessioniController controller) {
        super("thread-dispatcher-sessioni");

        if (controller == null) {
            throw new IllegalArgumentException(
                    "Il controller delle sessioni non può essere null"
            );
        }

        this.controller = controller;
    }

    /**
     * Chiamato dai thread HTTP: accoda soltanto la richiesta e ritorna.
     * Il pool HTTP non viene modificato.
     */
    public synchronized void inviaAggiornamento(
            String idMacchina,
            Sessione sessione) {

        if (!attivo) {
            throw new IllegalStateException(
                    "Il dispatcher delle sessioni è stato arrestato"
            );
        }

        coda.addLast(new RichiestaUsura(idMacchina, sessione));

        System.out.printf(
                "[%s] richiesta accodata per la macchina %s%n",
                Thread.currentThread().getName(),
                idMacchina
        );

        notifyAll();
    }

    private synchronized RichiestaUsura attendiRichiesta()
            throws InterruptedException {

        while (coda.isEmpty() && attivo) {
            wait();
        }

        if (!attivo) {
            return null;
        }

        return coda.removeFirst();
    }

    @Override
    public void run() {
        System.out.printf("[%s] dispatcher avviato%n", getName());

        try {
            while (attivo) {
                RichiestaUsura richiesta = attendiRichiesta();

                if (richiesta == null) {
                    break;
                }

                avviaThreadSessione(richiesta);
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
            System.out.printf("[%s] dispatcher terminato%n", getName());
        }
    }

    private void avviaThreadSessione(RichiestaUsura richiesta) {
        int numero = progressivoSessione.incrementAndGet();
        String nomeThread = "thread-sessione-" + numero;

        Thread threadSessione = new Thread(
                () -> eseguiRichiestaSessione(richiesta),
                nomeThread
        );

        threadSessioniAttivi.add(threadSessione);
        threadSessione.start();
    }

    private void eseguiRichiestaSessione(RichiestaUsura richiesta) {
        Thread corrente = Thread.currentThread();
        String nome = corrente.getName();
        boolean lockSessioniAcquisito = false;

        try {
            acquisisciLockSessioni(richiesta.idMacchina());
            lockSessioniAcquisito = true;

            // Sezione critica: siamo gli unici a poter scrivere sul DB.
            controller.registraSessione(
                    richiesta.idMacchina(),
                    richiesta.sessione()
            );

            System.out.printf(
                    "[%s] SCRITTURA DB COMPLETATA per %s; "
                            + "mantengo il lock sessioni per %d ms%n",
                    nome,
                    richiesta.idMacchina(),
                    TEMPO_SLEEP_CON_LOCK_MS
            );

            // Lo sleep è dentro la sezione critica delle sessioni:
            // il secondo thread-sessione deve attendere.
            Thread.sleep(TEMPO_SLEEP_CON_LOCK_MS);

        } catch (InterruptedException e) {
            System.out.printf(
                    "[%s] thread sessione interrotto per la macchina %s%n",
                    nome,
                    richiesta.idMacchina()
            );
            corrente.interrupt();

        } catch (Exception e) {
            System.err.printf(
                    "[%s] errore durante la scrittura per %s: %s%n",
                    nome,
                    richiesta.idMacchina(),
                    e.getMessage()
            );

        } finally {
            if (lockSessioniAcquisito) {
                rilasciaLockSessioni(richiesta.idMacchina());
            }

            threadSessioniAttivi.remove(corrente);
        }
    }

    /**
     * Richiede l'accesso esclusivo al lock delle sessioni.
     * Se il lock è occupato da un altro thread-sessione, il chiamante
     * stampa il messaggio di WAIT una sola volta e resta sospeso in
     * wait() finché il lock non viene rilasciato.
     */
    private void acquisisciLockSessioni(String idMacchina)
            throws InterruptedException {

        String nome = Thread.currentThread().getName();

        synchronized (monitorSessioni) {
            System.out.printf(
                    "[%s] PROVA ad ottenere il lock sessioni per la macchina %s%n",
                    nome,
                    idMacchina
            );

            boolean waitGiaStampato = false;

            /*
             * SEMPRE while, mai if: dopo notifyAll() tutti i thread in attesa
             * si risvegliano insieme, ma solo uno troverà il lock libero;
             * gli altri devono ricontrollare la condizione e tornare in
             * wait(). Il while protegge anche dai risvegli spuri della JVM.
             */
            while (lockSessioniOccupato) {
                if (!waitGiaStampato) {
                    System.out.printf(
                            "[%s] WAIT: lock sessioni occupato da %s; "
                                    + "attendo per la macchina %s%n",
                            nome,
                            proprietarioLockSessioni,
                            idMacchina
                    );
                    waitGiaStampato = true;
                }

                threadSessioniInAttesa++;
                try {
                    // wait() rilascia il monitor e sospende il thread:
                    // gli altri thread possono quindi entrare nei blocchi
                    // synchronized(monitorSessioni), incluso rilascia().
                    monitorSessioni.wait();
                } finally {
                    threadSessioniInAttesa--;
                }
            }

            lockSessioniOccupato = true;
            proprietarioLockSessioni = nome;

            System.out.printf(
                    "[%s] LOCK SESSIONI OTTENUTO per la macchina %s%n",
                    nome,
                    idMacchina
            );
        }
    }

    /**
     * Rilascia il lock delle sessioni e sveglia tutti i thread in attesa:
     * saranno loro, uno alla volta, a ricontrollare la condizione nel while.
     */
    private void rilasciaLockSessioni(String idMacchina) {
        String nome = Thread.currentThread().getName();

        synchronized (monitorSessioni) {
            if (!nome.equals(proprietarioLockSessioni)) {
                throw new IllegalMonitorStateException(
                        "[" + nome + "] tenta di rilasciare il lock sessioni"
                                + " senza possederlo (proprietario: "
                                + proprietarioLockSessioni + ")"
                );
            }

            lockSessioniOccupato = false;
            proprietarioLockSessioni = null;

            System.out.printf(
                    "[%s] LOCK SESSIONI RILASCIATO per la macchina %s%n",
                    nome,
                    idMacchina
            );

            monitorSessioni.notifyAll();
        }
    }

    public void arrestaThread() {
        attivo = false;

        synchronized (this) {
            notifyAll();
        }

        interrupt();

        for (Thread threadSessione : threadSessioniAttivi) {
            threadSessione.interrupt();
        }
    }

    public boolean isLockSessioniOccupato() {
        synchronized (monitorSessioni) {
            return lockSessioniOccupato;
        }
    }

    public int getNumeroThreadSessioneInAttesa() {
        synchronized (monitorSessioni) {
            return threadSessioniInAttesa;
        }
    }

    private record RichiestaUsura(
            String idMacchina,
            Sessione sessione) {
    }
}