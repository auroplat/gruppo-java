package it.unipv.bitFactory.model.veicoli;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.model.sessioni.Sessione;

public class Legends extends Macchina {

    private final String id;
    private final Map<TipoPezzo, Pezzo> pezzi;

    public Legends(String id) {
    
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("L'id della macchina non può essere vuoto");
        }

        this.id = id;
        this.pezzi = new EnumMap<>(TipoPezzo.class);
    }

    public void montaPezzo(TipoPezzo tipo, double kmMax, int tempoMax) {
        Pezzo pezzo = new Pezzo(tipo, kmMax, tempoMax);
        modificaPezzo(pezzo);
    }

    public void modificaPezzo(Pezzo pezzo) {
        if (pezzo == null) {
            throw new IllegalArgumentException("Il pezzo non può essere null");
        }

        pezzi.put(pezzo.getTipo(), pezzo);
    }

    public Pezzo getPezzo(TipoPezzo tipo) {
        return pezzi.get(tipo);
    }

    public Collection<Pezzo> getTuttiPezzi() {
        return Collections.unmodifiableCollection(pezzi.values());

    }

    public String getId() {
        return id;
    }

    @Override
    public void applicaSessione(Sessione sessione) {
  
        if (sessione == null) {
            throw new IllegalArgumentException("La sessione non può essere null");
        }

    	double km = sessione.getKmPercorsi();
        int tempo = sessione.getTempo();
        super.percorriKm(km);

        for (Pezzo pezzo : pezzi.values()) {
            pezzo.aggiornaUtilizzo(km, tempo);
        }
    }

    @Override
    public void applicaSessioneSelettiva(Sessione sessione,List<TipoPezzo> tipiDaAggiornare) {
        
        if (sessione == null) {
            throw new IllegalArgumentException("La sessione non può essere null");
        }

        if (tipiDaAggiornare == null) {
            throw new IllegalArgumentException("La lista dei tipi da aggiornare non può essere null");
        }

    	double km = sessione.getKmPercorsi();
        int tempo = sessione.getTempo();
        super.percorriKm(km);
    	
        for (TipoPezzo tipo : tipiDaAggiornare) {
            Pezzo pezzo = pezzi.get(tipo);

            if (pezzo != null) {
                pezzo.aggiornaUtilizzo(km, tempo);
            }
        }
    }

    @Override
    public String toString() {
        return "Legends{" +
                "id='" + id + '\'' +
                ", kmTotali=" + getKmTotali() +
                ", pezzi=" + pezzi.values() +
                '}';
    }
}

