package it.unipv.bitFactory.model.veicoli;

import java.util.List;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.model.sessioni.Sessione;


public abstract class Macchina extends Veicolo {
	
	public abstract void applicaSessione(Sessione sessione);

    public abstract void applicaSessioneSelettiva(Sessione sessione, List<TipoPezzo> tipiDaAggiornare);

}
