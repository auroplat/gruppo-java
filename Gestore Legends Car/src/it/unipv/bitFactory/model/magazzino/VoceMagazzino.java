package it.unipv.bitFactory.model.magazzino;

import it.unipv.bitFactory.model.pezzi.TipoPezzo;

/** Rappresenta un singolo pezzo fisico, identificato dal proprio ID. */
public class VoceMagazzino {

    private final String idPezzo;
    private final TipoPezzo tipoPezzo;
    private final String idVeicolo;

    public VoceMagazzino(String idPezzo, TipoPezzo tipoPezzo, String idVeicolo) {
        if (idPezzo == null || idPezzo.isBlank()) {
            throw new IllegalArgumentException("L'id del pezzo non può essere vuoto");
        }
        if (tipoPezzo == null) {
            throw new IllegalArgumentException("Il tipo pezzo non può essere null");
        }

        this.idPezzo = idPezzo.trim();
        this.tipoPezzo = tipoPezzo;
        this.idVeicolo = idVeicolo == null || idVeicolo.isBlank() ? null : idVeicolo.trim();
    }

    public String getIdPezzo() { return idPezzo; }
    public TipoPezzo getTipoPezzo() { return tipoPezzo; }
    public String getIdVeicolo() { return idVeicolo; }

    public boolean isDisponibile() {
        return idVeicolo == null;
    }

    public StatoDisponibilita getStatoDisponibilita() {
        return isDisponibile()
                ? StatoDisponibilita.DISPONIBILE
                : StatoDisponibilita.MONTATO_SU_VEICOLO;
    }

    @Override
    public String toString() {
        return "VoceMagazzino{" +
                "idPezzo='" + idPezzo + '\'' +
                ", tipoPezzo=" + tipoPezzo +
                ", idVeicolo='" + idVeicolo + '\'' +
                ", statoDisponibilita=" + getStatoDisponibilita() +
                '}';
    }
}
