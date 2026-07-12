package it.unipv.bitFactory.model.magazzino;

import it.unipv.bitFactory.model.pezzi.TipoPezzo;

public class VoceMagazzino {

    private final String idPezzo;
    private final TipoPezzo tipoPezzo;
    private final int quantita;
    private final StatoDisponibilita statoDisponibilita;

    public VoceMagazzino(
            String idPezzo,
            TipoPezzo tipoPezzo,
            int quantita,
            StatoDisponibilita statoDisponibilita) {

        if (idPezzo == null || idPezzo.isBlank()) {
            throw new IllegalArgumentException("L'id del pezzo non può essere vuoto");
        }

        if (tipoPezzo == null) {
            throw new IllegalArgumentException("Il tipo pezzo non può essere null");
        }

        if (quantita < 0) {
            throw new IllegalArgumentException("La quantità non può essere negativa");
        }

        if (statoDisponibilita == null) {
            throw new IllegalArgumentException("Lo stato disponibilità non può essere null");
        }

        this.idPezzo = idPezzo;
        this.tipoPezzo = tipoPezzo;
        this.quantita = quantita;
        this.statoDisponibilita = statoDisponibilita;
    }

    public String getIdPezzo() {
        return idPezzo;
    }

    public TipoPezzo getTipoPezzo() {
        return tipoPezzo;
    }

    public int getQuantita() {
        return quantita;
    }

    public StatoDisponibilita getStatoDisponibilita() {
        return statoDisponibilita;
    }

    @Override
    public String toString() {
        return "VoceMagazzino{" +
                "idPezzo='" + idPezzo + '\'' +
                ", tipoPezzo=" + tipoPezzo +
                ", quantita=" + quantita +
                ", statoDisponibilita=" + statoDisponibilita +
                '}';
    }
}
