package it.unipv.bitFactory.service;

import it.unipv.bitFactory.dao.interfacce.ClienteDAO;
import it.unipv.bitFactory.dao.interfacce.EventoDAO;
import it.unipv.bitFactory.dao.interfacce.PrenotazioneDAO;
import it.unipv.bitFactory.model.persona.Cliente;
import it.unipv.bitFactory.model.prenotazioni.Evento;
import it.unipv.bitFactory.model.prenotazioni.Prenotazione;

public final class SistemaPrenotazioni {

    private final ClienteDAO clienteDAO;
    private final EventoDAO eventoDAO;
    private final PrenotazioneDAO prenotazioneDAO;

    public SistemaPrenotazioni(
            ClienteDAO clienteDAO,
            EventoDAO eventoDAO,
            PrenotazioneDAO prenotazioneDAO) {

        if (clienteDAO == null
                || eventoDAO == null
                || prenotazioneDAO == null) {

            throw new IllegalArgumentException(
                    "Le dipendenze non possono essere null"
            );
        }

        this.clienteDAO = clienteDAO;
        this.eventoDAO = eventoDAO;
        this.prenotazioneDAO = prenotazioneDAO;
    }

    public synchronized String effettuaPrenotazione(
            Cliente cliente,
            String nomeEvento) {

        if (cliente == null) {
            return "Cliente non valido.";
        }

        if (nomeEvento == null || nomeEvento.isBlank()) {
            return "Evento non valido.";
        }

        if (cliente.getEta() < 18) {
            return "Il cliente deve essere maggiorenne.";
        }

        Evento evento = eventoDAO.cercaEvento(nomeEvento);

        if (evento == null) {
            return "Evento non trovato.";
        }

        if (evento.getPostiDisponibili() <= 0) {
            return "Prenotazione non possibile: nessun posto disponibile.";
        }

        boolean prenotazioneGiaPresente =
                prenotazioneDAO.cerca(
                        evento.getNomeEvento(),
                        cliente.getEmail()
                ).isPresent();

        if (prenotazioneGiaPresente) {
            return "Il cliente è già prenotato per questo evento.";
        }

        clienteDAO.salva(cliente);

        Prenotazione prenotazione =
                new Prenotazione(
                        evento.getNomeEvento(),
                        cliente.getEmail(),
                        cliente.getTelefono()
                );

        boolean aggiunta =
                prenotazioneDAO.aggiungi(prenotazione);

        if (!aggiunta) {
            return "Errore durante il salvataggio della prenotazione.";
        }

        boolean postiAggiornati =
                eventoDAO.aggiornaPosti(
                        evento.getNomeEvento(),
                        evento.getPostiDisponibili() - 1
                );

        if (!postiAggiornati) {
            /*
             * Se l'aggiornamento dei posti fallisce,
             * viene rimossa la prenotazione appena creata.
             */
            prenotazioneDAO.elimina(
                    prenotazione.getNomeEvento(),
                    prenotazione.getEmailCliente()
            );

            return "Errore durante l'aggiornamento dei posti disponibili.";
        }

        return "Prenotazione completata con successo.";
    }

    public synchronized String annullaPrenotazione(
            String emailCliente,
            String nomeEvento) {

        if (emailCliente == null || emailCliente.isBlank()) {
            return "Email non valida.";
        }

        if (nomeEvento == null || nomeEvento.isBlank()) {
            return "Evento non valido.";
        }

        Prenotazione prenotazione =
                prenotazioneDAO
                        .cerca(nomeEvento, emailCliente)
                        .orElse(null);

        if (prenotazione == null) {
            return "Prenotazione non trovata.";
        }

        Evento evento = eventoDAO.cercaEvento(nomeEvento);

        if (evento == null) {
            return "Evento non trovato.";
        }

        boolean eliminata =
                prenotazioneDAO.elimina(
                        nomeEvento,
                        emailCliente
                );

        if (!eliminata) {
            return "Errore durante l'annullamento della prenotazione.";
        }

        boolean postiAggiornati =
                eventoDAO.aggiornaPosti(
                        nomeEvento,
                        evento.getPostiDisponibili() + 1
                );

        if (!postiAggiornati) {
            return "Prenotazione eliminata, ma i posti non sono stati aggiornati.";
        }

        return "Prenotazione annullata con successo.";
    }
}