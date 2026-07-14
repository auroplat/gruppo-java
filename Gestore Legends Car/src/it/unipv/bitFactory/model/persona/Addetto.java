package it.unipv.bitFactory.model.persona;

public abstract class Addetto extends Persona {

    private String username;
    private String password;
    private Ruolo ruolo;

    public Addetto(String nome,
                   String cognome,
                   String telefono,
                   String email,
                   String username,
                   String password,
                   Ruolo ruolo) {

        super(nome, cognome, telefono, email);

        this.username = username;
        this.password = password;
        this.ruolo = ruolo;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Ruolo getRuolo() {
        return ruolo;
    }

    @Override
    public String toString() {
        return super.toString() +
                " | Username: " + username +
                " | Ruolo: " + ruolo;
    }
}
