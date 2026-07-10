package it.unipv.bitFactory.model.persona;

import it.unipv.bitFactory.model.persona.Persona;

public class Cliente extends Persona {

    private boolean patenteValida;
    private int eta;

    public Cliente(String nome,
                   String cognome,
                   boolean patenteValida,
                   int eta,
                   String telefono,
                   String email) {

        super(nome, cognome, telefono, email);

        this.patenteValida = patenteValida;
        this.eta = eta;
    }

    public boolean isPatenteValida() {
        return patenteValida;
    }

    public int getEta() {
        return eta;
    }

    @Override
    public String toString() {

        return super.toString() +
                " | Patente: " + patenteValida +
                " | Età: " + eta;
    }
}
