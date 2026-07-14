package it.unipv.bitFactory.model.persona;

public class GestoreSessioni extends Addetto {

    public GestoreSessioni(String nome, String cognome, String telefono, String email, String username, String password) {
    	
        super(nome, cognome, telefono, email, username, password, Ruolo.SESSIONI);
        
    }
}