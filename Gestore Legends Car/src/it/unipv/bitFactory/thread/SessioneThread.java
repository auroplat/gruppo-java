package it.unipv.bitFactory.thread;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.model.sessioni.Sessione;

public final class SessioneThread implements Runnable {

    private final String idMacchina;
    private final Sessione sessione;
    private final GestioneSessioniController controller;

    private final ReentrantLock lockDatabase;

    private final long tempoSimulazioneMs;

    public SessioneThread(String idMacchina,Sessione sessione,GestioneSessioniController controller,
            ReentrantLock lockDatabase, long tempoSimulazioneMs) {

        if (idMacchina == null || idMacchina.isBlank()) {throw new IllegalArgumentException("L'ID della macchina non può essere vuoto");}
        if (tempoSimulazioneMs < 0) { throw new IllegalArgumentException("Il tempo non può essere negativo");}
        this.idMacchina = idMacchina;
        this.sessione = Objects.requireNonNull(sessione,"La sessione non può essere null");
        this.controller = Objects.requireNonNull(controller,"Il controller non può essere null");
        this.lockDatabase = Objects.requireNonNull(lockDatabase,"Il lock del database non può essere null");
        this.tempoSimulazioneMs = tempoSimulazioneMs;
    }

    @Override
    public void run() {

        Thread corrente = Thread.currentThread();

        String nomeWorker = corrente.getName();

        boolean lockAcquisito = false;

        try {
            System.out.printf("[%s] richiede il LOCK DATABASE per %s; worker già in attesa: %d%n", nomeWorker, idMacchina, lockDatabase.getQueueLength());

            //il tread entra nella coda
            
            lockDatabase.lockInterruptibly();
            lockAcquisito = true;

            System.out.printf("[%s] LOCK DATABASE OTTENUTO per %s%n", nomeWorker, idMacchina);

            //sezione critica
            controller.registraSessione(idMacchina, sessione);

            System.out.printf("[%s] SCRITTURA DB COMPLETATA per %s%n",nomeWorker, idMacchina);

            //mantengo il lock per 5 secondi
            if (tempoSimulazioneMs > 0) {

                System.out.printf("[%s] mantengo il lock per %d ms%n",nomeWorker, tempoSimulazioneMs);

                Thread.sleep(tempoSimulazioneMs);
            }

        } catch (InterruptedException e) {

            System.out.printf("[%s] worker interrotto per %s%n", nomeWorker, idMacchina);

            corrente.interrupt();

        } catch (RuntimeException e) {

            System.err.printf("[%s] errore per %s: %s%n",nomeWorker,idMacchina, e.getMessage());

        } finally {

            if (lockAcquisito) {

                lockDatabase.unlock();

                System.out.printf("[%s] LOCK DATABASE RILASCIATO per %s%n",nomeWorker, idMacchina);
            }
        }
    }
}
