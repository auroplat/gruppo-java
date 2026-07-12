package it.unipv.bitFactory.model.veicoli;

import it.unipv.bitFactory.model.sessioni.Sessione;


public abstract class Macchina extends Veicolo {
	
	public abstract void applicaSessione(Sessione sessione);

}
