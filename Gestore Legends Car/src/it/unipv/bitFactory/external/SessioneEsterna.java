package it.unipv.bitFactory.external;

public final class SessioneEsterna {

    private final String circuito;
    private final double distanzaKm;
    private final int durataMinuti;
    private final String categoria;

    private final String descrizione;
    private final Integer posizioneFinale;

    private SessioneEsterna(String circuito, double distanzaKm, int durataMinuti, String categoria, String descrizione, Integer posizioneFinale) {
    	
        this.circuito = circuito;
        this.distanzaKm = distanzaKm;
        this.durataMinuti = durataMinuti;
        this.categoria = categoria;
        this.descrizione = descrizione;
        this.posizioneFinale = posizioneFinale;
    }

    public static SessioneEsterna test(String circuito, double distanzaKm, int durataMinuti, String descrizione    ) {
    	
        return new SessioneEsterna(circuito,distanzaKm,durataMinuti,"TEST", descrizione, null);

    }

    public static SessioneEsterna gara(String circuito, double distanzaKm, int durataMinuti,int posizioneFinale) {
    	
        return new SessioneEsterna(circuito,distanzaKm,durataMinuti,"GARA", null, posizioneFinale);

    }

    public String getCircuito() {
        return circuito;
    }

    public double getDistanzaKm() {
        return distanzaKm;
    }

    public int getDurataMinuti() {
        return durataMinuti;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public Integer getPosizioneFinale() {
        return posizioneFinale;
    }
}