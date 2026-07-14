package it.unipv.bitFactory.model.persona;

public class GestoreMagazzino extends Addetto {

    public GestoreMagazzino(String nome, String cognome, String telefono, String email, String username, String password) {

        super(nome, cognome, telefono, email, username, password, Ruolo.MAGAZZINO);
        
    }
}