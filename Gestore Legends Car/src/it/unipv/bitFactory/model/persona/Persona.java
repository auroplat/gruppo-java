package it.unipv.bitFactory.model.persona;

public abstract class Persona {

    private String nome;
    private String cognome;
    private String telefono;
    private String email;

    public Persona(String nome, String cognome, String telefono, String email) {

        this.nome = nome;
        this.cognome = cognome;
        this.telefono = telefono;
        this.email = email;
    }

    public String getNome() {return nome;}

    public String getCognome() {return cognome;}

    public String getTelefono() {return telefono;}

    public String getEmail() {return email;}

    @Override
    public String toString() {
        return nome + " " + cognome + " | Telefono: " + telefono + " | Email: " + email;
    }
}