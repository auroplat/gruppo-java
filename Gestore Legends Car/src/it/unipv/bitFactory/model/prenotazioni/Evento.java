package it.unipv.bitFactory.model.prenotazioni;

public class Evento {

    private String nomeEvento;
    private String dataEvento;
    private int postiDisponibili;

    public Evento(String nomeEvento,
                  String dataEvento,
                  int postiDisponibili) {

        this.nomeEvento = nomeEvento;
        this.dataEvento = dataEvento;
        this.postiDisponibili = postiDisponibili;
    }

    public String getNomeEvento() {return nomeEvento;}

    public String getDataEvento() {return dataEvento;}

    public int getPostiDisponibili() {return postiDisponibili;}

    public void diminuisciPosti() {if (postiDisponibili > 0) {postiDisponibili--;}}

    public void aumentaPosti() {postiDisponibili++;}

    @Override
    public String toString() {

        return "Evento: " + nomeEvento +
                " | Data: " + dataEvento +
                " | Posti disponibili: " + postiDisponibili;
    }
}