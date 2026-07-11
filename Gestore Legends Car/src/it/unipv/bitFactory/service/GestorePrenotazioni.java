package it.unipv.bitFactory.service;

import java.time.LocalDate;

import it.unipv.bitFactory.dao.ClienteDAO;
import it.unipv.bitFactory.dao.EventoDAO;
import it.unipv.bitFactory.dao.PrenotazioneDAO;
import it.unipv.bitFactory.model.persona.Cliente;
import it.unipv.bitFactory.model.prenotazioni.Evento;
import it.unipv.bitFactory.model.prenotazioni.Prenotazione;

public final class GestorePrenotazioni {

    private final ClienteDAO clienteDAO;
    private final EventoDAO eventoDAO;
    private final PrenotazioneDAO prenotazioneDAO;

    public GestorePrenotazioni(
            ClienteDAO clienteDAO,
            EventoDAO eventoDAO,
            PrenotazioneDAO prenotazioneDAO) {

        if (clienteDAO == null
                || eventoDAO == null
                || prenotazioneDAO == null) {

            throw new IllegalArgumentException(
                    "I DAO non possono essere null"
            );
        }

        this.clienteDAO = clienteDAO;
        this.eventoDAO = eventoDAO;
        this.prenotazioneDAO = prenotazioneDAO;
    }

    public synchronized Prenotazione creaPrenotazione(
            String nome,
            String cognome,
            LocalDate dataNascita,
            String email,
            String telefono,
            String nomeEvento) {

        Cliente cliente = new Cliente(
                nome,
                cognome,
                dataNascita,
                email,
                telefono
        );

        Evento evento =
                eventoDAO.cercaEvento(nomeEvento);

        if (evento == null) {
            throw new IllegalArgumentException(
                    "Evento non trovato: " + nomeEvento
            );
        }

        if (evento.getPostiDisponibili() <= 0) {
            throw new IllegalStateException(
                    "Non ci sono posti disponibili"
            );
        }

        boolean giaPrenotato = prenotazioneDAO
                .cerca(nomeEvento, cliente.getEmail())
                .isPresent();

        if (giaPrenotato) {
            throw new IllegalStateException(
                    "Il cliente è già prenotato per questo evento"
            );
        }

        /*
         * Se il cliente non esiste viene inserito.
         * Se esiste, i suoi dati vengono aggiornati.
         */
        clienteDAO.salva(cliente);

        Prenotazione prenotazione =
                new Prenotazione(
                        evento.getNomeEvento(),
                        cliente.getEmail(),
                        cliente.getTelefono()
                );

        boolean salvata =
                prenotazioneDAO.aggiungi(prenotazione);

        if (!salvata) {
            throw new IllegalStateException(
                    "La prenotazione non è stata salvata"
            );
        }

        boolean postiAggiornati =
                eventoDAO.aggiornaPosti(
                        evento.getNomeEvento(),
                        evento.getPostiDisponibili() - 1
                );

        if (!postiAggiornati) {
            /*
             * Compensazione: elimina la prenotazione appena inserita.
             */
            prenotazioneDAO.elimina(
                    prenotazione.getNomeEvento(),
                    prenotazione.getEmailCliente()
            );

            throw new IllegalStateException(
                    "Non è stato possibile aggiornare i posti"
            );
        }

        return prenotazione;
    }

    public synchronized boolean annullaPrenotazione(
            String nomeEvento,
            String emailCliente) {

        Evento evento =
                eventoDAO.cercaEvento(nomeEvento);

        boolean eliminata =
                prenotazioneDAO.elimina(
                        nomeEvento,
                        emailCliente
                );

        if (!eliminata) {
            return false;
        }

        if (evento != null) {
            eventoDAO.aggiornaPosti(
                    nomeEvento,
                    evento.getPostiDisponibili() + 1
            );
        }

        return true;
    }
}