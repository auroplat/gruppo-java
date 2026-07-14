package it.unipv.bitFactory.service;

import java.util.Objects;
import java.util.Optional;

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

        this.clienteDAO = Objects.requireNonNull( clienteDAO,"Il DAO dei clienti non può essere null" );

        this.eventoDAO = Objects.requireNonNull( eventoDAO, "Il DAO degli eventi non può essere null" );

        this.prenotazioneDAO = Objects.requireNonNull( prenotazioneDAO, "Il DAO delle prenotazioni non può essere null" ); 
        }

    public String effettuaPrenotazione(
            Cliente cliente,
            String nomeEvento) {

        if (cliente == null) { return "Cliente non valido."; }

        String eventoRichiesto =
                normalizzaNomeEvento(nomeEvento);

        if (cliente.getEta() < 18) { return "Il cliente deve essere maggiorenne."; }

        Evento evento = eventoDAO.cercaEvento( eventoRichiesto );

        if (evento == null) { return "Evento non trovato."; }

        if (evento.getPostiDisponibili() <= 0) {

            return "Prenotazione non possibile: nessun posto disponibile.";  }

        String email = normalizzaEmail( cliente.getEmail());

        if (prenotazioneDAO
                .cerca(
                        evento.getNomeEvento(),
                        email
                )
                .isPresent()) {

            return "Il cliente è già prenotato per questo evento."; }

        if (clienteDAO
                .cercaPerEmail(email)
                .isEmpty()) {

            boolean clienteSalvato =
                    clienteDAO.salva(cliente);

            if (!clienteSalvato) {
                return "Impossibile salvare i dati del cliente."; }
        }

        Prenotazione prenotazione =
                new Prenotazione(
                        evento.getNomeEvento(),
                        email,
                        cliente.getTelefono()
                );

        boolean prenotazioneSalvata =
                prenotazioneDAO.aggiungi(
                        prenotazione
                );

        if (!prenotazioneSalvata) { return "Impossibile salvare la prenotazione."; }

        int nuoviPosti = evento.getPostiDisponibili() - 1;

        boolean postiAggiornati =
                eventoDAO.aggiornaPosti(
                        evento.getNomeEvento(),
                        nuoviPosti
                );

        if (!postiAggiornati) {
            prenotazioneDAO.elimina(
                    evento.getNomeEvento(),
                    email
            );

            return "La prenotazione non è stata completata: impossibile aggiornare i posti.";
        }

        return "Prenotazione completata con successo.";
    }

    public String annullaPrenotazione(
            String emailCliente,
            String nomeEvento) {

        String eventoRichiesto =
                normalizzaNomeEvento(nomeEvento);

        String email =
                normalizzaEmail(emailCliente);

        Evento evento =
                eventoDAO.cercaEvento(
                        eventoRichiesto
                );

        if (evento == null) { return "Evento non trovato."; }

        Optional<Prenotazione> prenotazione =
                prenotazioneDAO.cerca(
                        evento.getNomeEvento(),
                        email
                );

        if (prenotazione.isEmpty()) { return "Prenotazione non trovata.";  }

        boolean eliminata =
                prenotazioneDAO.elimina(
                        evento.getNomeEvento(),
                        email
                );

        if (!eliminata) { return "Impossibile annullare la prenotazione."; }

        int nuoviPosti = evento.getPostiDisponibili() + 1;

        boolean postiAggiornati =
                eventoDAO.aggiornaPosti(
                        evento.getNomeEvento(),
                        nuoviPosti
                );

        if (!postiAggiornati) { prenotazioneDAO.aggiungi( prenotazione.get());

            return "Annullamento non completato: impossibile aggiornare i posti.";
        }

        return "Prenotazione annullata con successo.";
    }

    private String normalizzaNomeEvento(
            String nomeEvento) {

        if (nomeEvento == null || nomeEvento.isBlank()) {
            throw new IllegalArgumentException( "Il nome dell'evento non può essere vuoto" ); }

        return nomeEvento.trim();
    }

    private String normalizzaEmail(
            String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException( "L'email non può essere vuota" );
        }

        String normalizzata =
                email.trim().toLowerCase();

        if (!normalizzata.contains("@")) {
            throw new IllegalArgumentException( "L'email inserita non è valida" );
        }

        return normalizzata;
    }
}