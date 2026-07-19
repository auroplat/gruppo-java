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

public final class Dispatcher extends Thread {

    private static final int NUMERO_MASSIMO_WORKER = 3;
    private static final long TEMPO_SIMULAZIONE_MS = 5_000L;

    private final GestioneSessioniController controller;


    //Coda FIFO delle richieste.

    private final Deque<RichiestaSessione> coda = new ArrayDeque<>();
    
    private final ReentrantLock lockCoda = new ReentrantLock(true);

    private final Condition richiestaDisponibile = lockCoda.newCondition();

    private final Semaphore semaforoWorker = new Semaphore(NUMERO_MASSIMO_WORKER, true);


     //Il parametro true rende il lock fair:
    private final ReentrantLock lockDatabase = new ReentrantLock(true);

    private final Set<Thread> workerAttivi = ConcurrentHashMap.newKeySet();

    private final AtomicInteger progressivoWorker =  new AtomicInteger();

    private volatile boolean attivo = true;

    public Dispatcher(GestioneSessioniController controller) {

        super("thread-dispatcher-sessioni");

        this.controller = Objects.requireNonNull(controller,"Il controller non può essere null");
    }

    public void inviaAggiornamento(String idMacchina, Sessione sessione) {

        if (idMacchina == null || idMacchina.isBlank()) {
            throw new IllegalArgumentException("L'ID della macchina non può essere vuoto");
        }

        Objects.requireNonNull(sessione,"La sessione non può essere null");

        lockCoda.lock();

        try {
            if (!attivo) {
                throw new IllegalStateException("Il dispatcher è stato arrestato");
            }

            coda.addLast(new RichiestaSessione(idMacchina, sessione)
            );

            System.out.printf("[%s] richiesta accodata per %s; richieste in coda: %d%n", Thread.currentThread().getName(), idMacchina, coda.size());

            richiestaDisponibile.signal();

        } finally {
            lockCoda.unlock();
        }
    }

    
     //Attende che sia presente almeno una richiesta.
     
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

    //Estrae la richiesta più vecchia dalla coda.
    private RichiestaSessione estraiRichiesta() {

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

            	//controllo chi siano richiesta
                if (!attendiPresenzaRichiesta()) {
                    break;
                }
                
                //provo a prendere il semaforo
                semaforoWorker.acquire();

                boolean permessoTrasferito = false;

                try {
                    if (!attivo) {break;}

                    RichiestaSessione richiesta = estraiRichiesta();

                    if (richiesta == null) {continue;}

                    avviaWorker(richiesta);
                    permessoTrasferito = true;

                } finally {
                	
                    if (!permessoTrasferito) {
                        semaforoWorker.release();
                    }
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

        } finally {
            System.out.printf("[%s] dispatcher terminato%n", getName());
        }
    }

    private void avviaWorker(RichiestaSessione richiesta) {

        int numero = progressivoWorker.incrementAndGet();

        String nomeWorker = "thread-sessione-" + numero;

        SessioneWorker worker = new SessioneWorker(richiesta.idMacchina(), richiesta.sessione(), controller, lockDatabase, TEMPO_SIMULAZIONE_MS);

        Thread threadWorker = new Thread(() -> {
        	
                    try {
                        worker.run();

                    } finally {
                        workerAttivi.remove(Thread.currentThread());

                        //quando il thread termina lascia semaforo
                        semaforoWorker.release();

                        System.out.printf("[%s] permesso worker rilasciato; permessi disponibili: %d%n", Thread.currentThread().getName(),semaforoWorker.availablePermits());
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

    public int getNumeroPermessiWorkerDisponibili() {
        return semaforoWorker.availablePermits();
    }

    public int getNumeroDispatcherInAttesaDelSemaforo() {
        return semaforoWorker.getQueueLength();
    }

    public int getNumeroWorkerInAttesaDelLockDatabase() {
        return lockDatabase.getQueueLength();
    }

    private record RichiestaSessione(String idMacchina, Sessione sessione) {}
}
