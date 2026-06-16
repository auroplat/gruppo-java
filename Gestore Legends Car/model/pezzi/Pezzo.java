package it.unipv.bitFactory.pezzi;

public class Pezzo {
	//vedere se pubblico o primavato 
	private final TipoPezzo tipo;
	private final double kmMax;
    private int kmAttuali;
    private final double tempoMax;
    private int tempoAttuale;
    
    public Pezzo(TipoPezzo tipo, int kmMax, int tempoMax) {
        this.tipo = tipo;
        this.kmMax = kmMax;
        this.tempoMax = tempoMax;
        this.kmAttuali = 0;
        this.tempoAttuale = 0;
    }
    
    public void aggiornaKm(int km) {
		if (km < 0) {
            throw new IllegalArgumentException("I km non possono essere negativi");
        }
        kmAttuali += km;
    }
    
    public void aggiornaTempo(int tempo) {
		if (tempo < 0) {
            throw new IllegalArgumentException("Il tempo non può essere negativo");
        }
        tempoAttuale += tempo;
    }
    
    public boolean daSostituire() {
        return kmAttuali >= kmMax || tempoAttuale >= tempoMax;
    }

	    
    public void aggiornaUtilizzo(double km, double tempo) {
        aggiornaKm(km);
        aggiornaTempo(tempo);
    }
	
    public TipoPezzo getTipo() {return tipo;}
    public double getKmAttuali() {return kmAttuali;}
    public double getTempoAttuale() {return tempoAttuale;}
	public double getKmMax() {return kmMax;}
    public double getTempoMax() {return tempoMax;}

    @Override
    public String toString() {
        return "Pezzo{" +
                "tipo=" + tipo +
                ", kmAttuali=" + kmAttuali +
                ", kmMax=" + kmMax +
                ", tempoAttuale=" + tempoAttuale +
                ", tempoMax=" + tempoMax +
                '}';
    }
}
