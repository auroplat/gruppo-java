package it.unipv.bitFactory.model.persona;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

public final class Cliente {

    private final String nome;
    private final String cognome;
    private final LocalDate dataNascita;
    private final String email;
    private final String telefono;

    public Cliente(
            String nome,
            String cognome,
            LocalDate dataNascita,
            String email,
            String telefono) {

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "Il nome non può essere vuoto"
            );
        }

        if (cognome == null || cognome.isBlank()) {
            throw new IllegalArgumentException(
                    "Il cognome non può essere vuoto"
            );
        }

        if (dataNascita == null) {
            throw new IllegalArgumentException(
                    "La data di nascita non può essere null"
            );
        }

        if (dataNascita.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La data di nascita non può essere futura"
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "L'email non può essere vuota"
            );
        }

        if (!email.contains("@")) {
            throw new IllegalArgumentException(
                    "L'email non è valida"
            );
        }

        if (telefono == null || telefono.isBlank()) {
            throw new IllegalArgumentException(
                    "Il telefono non può essere vuoto"
            );
        }

        this.nome = nome.trim();
        this.cognome = cognome.trim();
        this.dataNascita = dataNascita;
        this.email = email.trim().toLowerCase();
        this.telefono = telefono.trim();
    }

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }

    public LocalDate getDataNascita() {
        return dataNascita;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public int getEta() {
        return Period.between(
                dataNascita,
                LocalDate.now()
        ).getYears();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Cliente altroCliente)) {
            return false;
        }

        return email.equals(altroCliente.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return "Cliente{" +
                "nome='" + nome + '\'' +
                ", cognome='" + cognome + '\'' +
                ", dataNascita=" + dataNascita +
                ", email='" + email + '\'' +
                ", telefono='" + telefono + '\'' +
                '}';
    }
}