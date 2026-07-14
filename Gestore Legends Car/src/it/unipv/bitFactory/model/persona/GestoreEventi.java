package it.unipv.bitFactory.model.persona;

public class GestoreEventi extends Addetto {

    public GestoreEventi(String nome,
                         String cognome,
                         String telefono,
                         String email,
                         String username,
                         String password) {

        super(
                nome,
                cognome,
                telefono,
                email,
                username,
                password,
                Ruolo.EVENTI
        );
    }
}