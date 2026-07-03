package it.unipv.bitFactory.thread;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Lock condiviso dai thread che modificano lo stesso database SQLite.
 * Deve esistere una sola istanza, passata a entrambi i worker.
 */
public final class DatabaseWriteLock {

    private final ReentrantLock lock = new ReentrantLock(true);

    @FunctionalInterface
    public interface OperazioneCritica {
        void esegui() throws Exception;
    }

    public void esegui(
            String descrizione,
            OperazioneCritica operazione) throws Exception {

        Objects.requireNonNull(operazione, "L'operazione non può essere null");

        lock.lock();
        try {
            System.out.printf(
                    "[%s] lock database acquisito: %s%n",
                    Thread.currentThread().getName(),
                    descrizione
            );

            operazione.esegui();

        } finally {
            System.out.printf(
                    "[%s] lock database rilasciato: %s%n",
                    Thread.currentThread().getName(),
                    descrizione
            );

            lock.unlock();
        }
    }

    public boolean isBloccato() {
        return lock.isLocked();
    }

    public int getNumeroThreadInAttesa() {
        return lock.getQueueLength();
    }
}
