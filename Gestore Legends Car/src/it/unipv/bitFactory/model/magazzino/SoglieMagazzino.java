package it.unipv.bitFactory.model.magazzino;

public class SoglieMagazzino {

    private final int sogliaPocaDisponibilita;

    public SoglieMagazzino(int sogliaPocaDisponibilita) {
        if (sogliaPocaDisponibilita < 0) {
            throw new IllegalArgumentException("La soglia non può essere negativa");
        }

        this.sogliaPocaDisponibilita = sogliaPocaDisponibilita;
    }

    public StatoDisponibilita calcolaStato(int quantita) {
        if (quantita <= 0) {return StatoDisponibilita.ESAURITO;}

        if (quantita <= sogliaPocaDisponibilita) {return StatoDisponibilita.POCA_DISPONIBILITA;}

        return StatoDisponibilita.DISPONIBILE;
    }

    public int getSogliaPocaDisponibilita() {return sogliaPocaDisponibilita;}
}