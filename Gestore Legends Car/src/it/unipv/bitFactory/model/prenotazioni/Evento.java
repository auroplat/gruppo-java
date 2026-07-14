package it.unipv.bitFactory.model.prenotazioni;

public class Evento {

    private String nomeEvento;
    private String dataEvento;
    private int postiDisponibili;

    public Evento(String nomeEvento, String dataEvento, int postiDisponibili) {
    	
        if (nomeEvento == null || nomeEvento.isBlank()) {
            throw new IllegalArgumentException("Il nome dell'evento non può essere vuoto");
        }
        if (dataEvento == null || dataEvento.isBlank()) {
                throw new IllegalArgumentException("La data dell'evento non può essere vuota");
        }
        if (postiDisponibili < 0) {
                throw new IllegalArgumentException("I posti disponibili non possono essere negativi");        
        }       
        this.nomeEvento = nomeEvento;
        this.dataEvento = dataEvento;
        this.postiDisponibili = postiDisponibili;
    }

    public String getNomeEvento() {return nomeEvento;}
    public String getDataEvento() {return dataEvento;}
    public int getPostiDisponibili() {return postiDisponibili;}
    public void aumentaPosti() {postiDisponibili++;}

    public void diminuisciPosti() {
    	if (postiDisponibili > 0) {postiDisponibili--;}
    }

    @Override
    public String toString() {

        return "Evento: " + nomeEvento +
                " | Data: " + dataEvento +
                " | Posti disponibili: " + postiDisponibili;
    }
}