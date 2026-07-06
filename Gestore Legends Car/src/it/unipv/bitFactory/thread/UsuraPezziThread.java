package it.unipv.bitFactory.thread;

import java.util.ArrayDeque;
import java.util.Deque;

import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.model.sessioni.Sessione;

public class UsuraPezziThread extends Thread {

    private final GestioneSessioniController controller;
    private final DatabaseWriteLock lock;
    
    // Usiamo una normale coda e la proteggiamo noi con 'synchronized'
    private final Deque<RichiestaUsura> coda = new ArrayDeque<>();

    public UsuraPezziThread(GestioneSessioniController controller, DatabaseWriteLock lock) {
        super("thread-usura-pezzi");
        this.controller = controller;
        this.lock = lock;
    }

    // 1. IL PRODUTTORE (Chiamato dai thread HTTP)
    // 'synchronized' impedisce che due thread HTTP scrivano nella coda nello stesso istante
    public synchronized void inviaAggiornamento(String idMacchina, Sessione sessione) {
        // Aggiungiamo la richiesta in fondo alla coda
        coda.addLast(new RichiestaUsura(idMacchina, sessione));
        System.out.println("Richiesta accodata per la macchina: " + idMacchina);
        
        // IL GRILLETTO: Svegliamo il thread in background che stava dormendo
        notifyAll(); 
    }

    // 2. IL MECCANISMO DI ATTESA (Chiamato solo dal ciclo run)
    // Anche questo deve essere 'synchronized' per poter usare wait() in modo sicuro
    private synchronized RichiestaUsura attendiRichiesta() throws InterruptedException {
        // Finché la coda è vuota, il thread si mette a dormire
        while (coda.isEmpty()) {
            wait(); // Rilascia il blocco e aspetta che qualcuno chiami notifyAll()
        }
        // Quando si sveglia (e la coda non è vuota), preleva il primo elemento
        return coda.removeFirst();
    }

    // 3. IL CONSUMATORE (Il ciclo vitale del Thread in background)
    @Override
    public void run() {
        System.out.println("Thread usura avviato!");

        try {
            while (true) {
                // Prende la richiesta (o si addormenta automaticamente se non ce ne sono)
                RichiestaUsura richiesta = attendiRichiesta();

                // Esegue la scrittura usando il Lock del database per SQLite
                lock.esegui("Aggiornamento usura", () -> {
                    controller.registraSessione(richiesta.idMacchina(), richiesta.sessione());
                });
            }
        } catch (InterruptedException e) {
            // Se spegniamo il server, il thread si interrompe pacificamente
            System.out.println("Thread usura interrotto e terminato.");
        } catch (Exception e) {
            System.err.println("Errore durante la scrittura: " + e.getMessage());
        }
    }

    // Un record privato per impacchettare semplicemente i dati in coda
    private record RichiestaUsura(String idMacchina, Sessione sessione) {}
    
    public synchronized void arrestaThread() {
        notifyAll();
    }
}