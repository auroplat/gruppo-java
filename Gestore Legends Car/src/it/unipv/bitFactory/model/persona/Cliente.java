package it.unipv.bitFactory.model.persona;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

public final class Cliente extends Persona{

    private final LocalDate dataNascita;

    public Cliente(String nome, String cognome, String telefono, String email, LocalDate dataNascita) {
    	
    	super(nome, cognome, telefono, email);

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Il nome non può essere vuoto");
        }

        if (cognome == null || cognome.isBlank()) {
            throw new IllegalArgumentException("Il cognome non può essere vuoto");
        }

        if (dataNascita == null) {
            throw new IllegalArgumentException("La data di nascita non può essere null");
        }

        if (dataNascita.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La data di nascita non può essere futura");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L'email non può essere vuota");
        }

        if (!email.contains("@")) {
            throw new IllegalArgumentException("L'email non è valida");
        }

        if (telefono == null || telefono.isBlank()) {
            throw new IllegalArgumentException("Il telefono non può essere vuoto");
        }

        this.dataNascita = dataNascita;

    }

    public LocalDate getDataNascita() {return dataNascita;}
    public int getEta() {return Period.between(dataNascita, LocalDate.now()).getYears();}

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Cliente altroCliente)) {
            return false;
        }

        return Objects.equals(getEmail(), altroCliente.getEmail());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEmail());
    }

    @Override
    public String toString() {
        return "Cliente{" +
                "nome='" + getNome() + '\'' +
                ", cognome='" + getCognome() + '\'' +
                ", dataNascita=" + dataNascita +
                ", email='" + getEmail() + '\'' +
                ", telefono='" + getTelefono() + '\'' +
                '}';
    }
}