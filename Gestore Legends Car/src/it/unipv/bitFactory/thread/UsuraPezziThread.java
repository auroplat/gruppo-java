package it.unipv.bitFactory.thread;

import java.util.ArrayDeque; //implementazione interfaccia Deque
import java.util.Deque; //interfaccia coda a doppia estremità
import java.util.Set; //dove metto i thread in coda
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.model.sessioni.Sessione;

public class UsuraPezziThread extends Thread {

    private static final long TIME = 5_000L;

    private final GestioneSessioniController controller;

    private final Object lockSessioni = new Object(); //oggetto usato come lock
    private boolean lockSessioniOccupato = false;
    private String proprietarioLockSessioni = null;
    private int threadSessioniInAttesa = 0;

    private final Deque<RichiestaUsura> coda = new ArrayDeque<>(); //Crea la coda delle richieste.
    private final Set<Thread> threadSessioniAttivi = ConcurrentHashMap.newKeySet(); //Contiene i thread-sessione attualmente esistenti
    private final AtomicInteger progressivoSessione = new AtomicInteger(); //lo uso solo per i printi per avere nomi diversi

    private volatile boolean attivo = true;

    public UsuraPezziThread(GestioneSessioniController controller) {
        super("thread-dispatcher-sessioni"); //il costruttore di Thread assegna il nome

        if (controller == null) {
            throw new IllegalArgumentException("Il controller delle sessioni non può essere null");
        }

        this.controller = controller;
    }

    public synchronized void inviaAggiornamento(String idMacchina,Sessione sessione) {
        if (!attivo) {
            throw new IllegalStateException("Il dispatcher delle sessioni è stato arrestato");
        }

        coda.addLast(new RichiestaUsura(idMacchina, sessione));

        System.out.printf("[%s] richiesta accodata per la macchina %s%n",Thread.currentThread().getName(),idMacchina);

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


    //il dispatcher se ci sono richieste avvia il thread 
    
    @Override
    public void run() {

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
                System.err.printf("[%s] dispatcher interrotto in modo inatteso%n",getName());
            }

            Thread.currentThread().interrupt();
        } finally {
            System.out.printf("[%s] dispatcher terminato%n", getName());
        }
    }

    
    //creazione thread per scrittura in db
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

            System.out.printf("[%s] SCRITTURA DB COMPLETATA per %s; " + "mantengo il lock sessioni per %d ms%n",nome,richiesta.idMacchina(),TIME);

            //così non lascia il lock
            Thread.sleep(TIME);

        } catch (InterruptedException e) {
            System.out.printf("[%s] thread sessione interrotto per la macchina %s%n",nome,richiesta.idMacchina());
            corrente.interrupt();

        } catch (Exception e) {
            System.err.printf("[%s] errore durante la scrittura per %s: %s%n",nome,richiesta.idMacchina(),e.getMessage());

        } finally {
            if (lockSessioniAcquisito) {
                rilasciaLockSessioni(richiesta.idMacchina());
            }

            threadSessioniAttivi.remove(corrente);
        }
    }


    private void acquisisciLockSessioni(String idMacchina)
            throws InterruptedException {

        String nome = Thread.currentThread().getName();

        synchronized (lockSessioni) {
            System.out.printf("[%s] PROVA ad ottenere il lock sessioni per la macchina %s%n",nome,idMacchina);

            boolean waitGiaStampato = false;

            while (lockSessioniOccupato) {
                if (!waitGiaStampato) {
                    System.out.printf("[%s] WAIT: lock sessioni occupato da %s; " + "attendo per la macchina %s%n",nome,proprietarioLockSessioni,idMacchina);
                    waitGiaStampato = true;
                }

                threadSessioniInAttesa++;
                try {
                    lockSessioni.wait();
                } finally {
                    threadSessioniInAttesa--;
                }
            }

            lockSessioniOccupato = true;
            proprietarioLockSessioni = nome;

            System.out.printf("[%s] LOCK SESSIONI OTTENUTO per la macchina %s%n",nome,idMacchina);
        }
    }

    private void rilasciaLockSessioni(String idMacchina) {
        String nome = Thread.currentThread().getName();

        synchronized (lockSessioni) {
            if (!nome.equals(proprietarioLockSessioni)) {
                throw new IllegalMonitorStateException("[" + nome + "] tenta di rilasciare il lock sessioni"+ " senza possederlo (proprietario: "+ proprietarioLockSessioni + ")");
            }

            lockSessioniOccupato = false;
            proprietarioLockSessioni = null;

            System.out.printf("[%s] LOCK SESSIONI RILASCIATO per la macchina %s%n",nome,idMacchina);

            lockSessioni.notifyAll();
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

    private record RichiestaUsura(
            String idMacchina,
            Sessione sessione) {
    }
}