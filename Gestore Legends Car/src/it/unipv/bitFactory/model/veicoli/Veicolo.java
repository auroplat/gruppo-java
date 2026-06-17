package it.unipv.bitFactory.model.veicoli;
	
public abstract class Veicolo {
	private double kmTotali;
	
	  public Veicolo() {
	        this.kmTotali = 0;
	    }
	  
	  public void percorriKm(double km) {
	        if (km < 0) {
	            throw new IllegalArgumentException("I km non possono essere negativi");
	        }

	        kmTotali += km;
	    }

	    public double getKmTotali() {
	        return kmTotali;
	    }
	  
}
