package it.unipv.bitFactory.model.sessioni;

public class Gara extends Sessione {

	private final int posizione;	
	
	public Gara(String luogo, double kmPercorsi, int tempoPassato, int posizione) {
		
		super(luogo, kmPercorsi, tempoPassato);

		if (posizione <= 0) {
			throw new IllegalArgumentException("La posizione deve essere maggiore di zero");	
		}
	
		this.posizione = posizione;
	
	}

	@Override
	public TipoSessione getTipoSessione() {return TipoSessione.GARA;}
	public int getPosizioneFinale() {return posizione;}

}
