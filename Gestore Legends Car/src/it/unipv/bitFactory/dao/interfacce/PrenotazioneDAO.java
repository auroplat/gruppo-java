package it.unipv.bitFactory.dao.interfacce;

import java.util.List;
import java.util.Optional;

import it.unipv.bitFactory.model.prenotazioni.Prenotazione;

public interface PrenotazioneDAO {

    List<Prenotazione> caricaPrenotazioni();

    List<Prenotazione> cercaPerEvento(String nomeEvento);

    List<Prenotazione> cercaPerCliente(String emailCliente);

    Optional<Prenotazione> cerca(String nomeEvento, String emailCliente);

    boolean aggiungi(Prenotazione prenotazione);

    boolean elimina(String nomeEvento, String emailCliente);
}