package it.unipv.bitFactory.external;

public class SessioneEsterna {

    private final String circuito;
    private final double distanzaKm;
    private final int durataMinuti;
    private final String categoria;

    public SessioneEsterna(String circuito, double distanzaKm, int durataMinuti, String categoria) {
        this.circuito = circuito;
        this.distanzaKm = distanzaKm;
        this.durataMinuti = durataMinuti;
        this.categoria = categoria;
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
}