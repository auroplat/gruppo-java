package it.unipv.bitFactory.model.prenotazioni;

import java.util.Objects;

public final class Prenotazione {

    private final String nomeEvento;
    private final String emailCliente;
    private final String telefonoCliente;

    public Prenotazione(
            String nomeEvento,
            String emailCliente,
            String telefonoCliente) {

        if (nomeEvento == null || nomeEvento.isBlank()) {
            throw new IllegalArgumentException(
                    "Il nome dell'evento non può essere vuoto"
            );
        }

        if (emailCliente == null || emailCliente.isBlank()) {
            throw new IllegalArgumentException(
                    "L'email del cliente non può essere vuota"
            );
        }

        if (!emailCliente.contains("@")) {
            throw new IllegalArgumentException(
                    "L'email del cliente non è valida"
            );
        }

        if (telefonoCliente == null || telefonoCliente.isBlank()) {
            throw new IllegalArgumentException(
                    "Il telefono del cliente non può essere vuoto"
            );
        }

        this.nomeEvento = nomeEvento.trim();
        this.emailCliente = emailCliente.trim().toLowerCase();
        this.telefonoCliente = telefonoCliente.trim();
    }

    public String getNomeEvento() {
        return nomeEvento;
    }

    public String getEmailCliente() {
        return emailCliente;
    }

    public String getTelefonoCliente() {
        return telefonoCliente;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Prenotazione altra)) {
            return false;
        }

        return nomeEvento.equals(altra.nomeEvento)
                && emailCliente.equals(altra.emailCliente);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nomeEvento, emailCliente);
    }

    @Override
    public String toString() {
        return "Prenotazione{" +
                "nomeEvento='" + nomeEvento + '\'' +
                ", emailCliente='" + emailCliente + '\'' +
                ", telefonoCliente='" + telefonoCliente + '\'' +
                '}';
    }
}