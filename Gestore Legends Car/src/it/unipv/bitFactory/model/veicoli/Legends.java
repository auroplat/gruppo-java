package it.unipv.bitFactory.model.veicoli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.model.sessioni.Sessione;

public class Legends extends Macchina {

    private final String id;
    private final Map<String, Pezzo> pezzi;

    public Legends(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "L'id della macchina non può essere vuoto"
            );
        }

        this.id = id.trim();
        this.pezzi = new LinkedHashMap<>();
    }

    public void montaPezzo(
            TipoPezzo tipo,
            double kmMax,
            int tempoMax
    ) {
        aggiungiPezzo(new Pezzo(tipo, kmMax, tempoMax));
    }

    public void aggiungiPezzo(Pezzo pezzo) {
        if (pezzo == null) {
            throw new IllegalArgumentException(
                    "Il pezzo non può essere null"
            );
        }

        if (pezzi.containsKey(pezzo.getIdPezzo())) {
            throw new IllegalArgumentException(
                    "Esiste già un pezzo con id: " + pezzo.getIdPezzo()
            );
        }

        pezzi.put(pezzo.getIdPezzo(), pezzo);
    }

    public void modificaPezzo(Pezzo pezzo) {
        if (pezzo == null) {
            throw new IllegalArgumentException(
                    "Il pezzo non può essere null"
            );
        }

        pezzi.put(pezzo.getIdPezzo(), pezzo);
    }


    public Pezzo sostituisciPezzo(
            String idVecchioPezzo,
            Pezzo nuovoPezzo
    ) {
        if (idVecchioPezzo == null || idVecchioPezzo.isBlank()) {
            throw new IllegalArgumentException(
                    "L'id del pezzo da sostituire non può essere vuoto"
            );
        }

        if (nuovoPezzo == null) {
            throw new IllegalArgumentException(
                    "Il nuovo pezzo non può essere null"
            );
        }

        String idVecchio = idVecchioPezzo.trim();
        Pezzo vecchioPezzo = pezzi.get(idVecchio);

        if (vecchioPezzo == null) {
            throw new IllegalArgumentException(
                    "Il pezzo " + idVecchio
                            + " non è montato sulla macchina " + id
            );
        }

        if (vecchioPezzo.getTipo() != nuovoPezzo.getTipo()) {
            throw new IllegalArgumentException(
                    "Il nuovo pezzo deve essere dello stesso tipo "
                            + "del pezzo sostituito"
            );
        }

        if (pezzi.containsKey(nuovoPezzo.getIdPezzo())) {
            throw new IllegalArgumentException(
                    "Il nuovo pezzo è già montato sulla macchina: "
                            + nuovoPezzo.getIdPezzo()
            );
        }

        pezzi.remove(idVecchio);
        pezzi.put(nuovoPezzo.getIdPezzo(), nuovoPezzo);

        return vecchioPezzo;
    }

    public Pezzo getPezzoPerId(String idPezzo) {
        if (idPezzo == null || idPezzo.isBlank()) {
            return null;
        }

        return pezzi.get(idPezzo.trim());
    }

    public Pezzo getPezzo(TipoPezzo tipo) {
        if (tipo == null) {
            return null;
        }

        return pezzi.values()
                .stream()
                .filter(pezzo -> pezzo.getTipo() == tipo)
                .findFirst()
                .orElse(null);
    }

    public List<Pezzo> getPezzi(TipoPezzo tipo) {
        if (tipo == null) {
            return List.of();
        }

        List<Pezzo> risultato = new ArrayList<>();

        for (Pezzo pezzo : pezzi.values()) {
            if (pezzo.getTipo() == tipo) {
                risultato.add(pezzo);
            }
        }

        return Collections.unmodifiableList(risultato);
    }

    public Collection<Pezzo> getTuttiPezzi() {
        return Collections.unmodifiableCollection(pezzi.values());
    }

    public String getId() {
        return id;
    }

    public String getIdVeicolo() {
        return id;
    }

    public TipoVeicolo getTipoVeicolo() {
        return TipoVeicolo.LEGENDS;
    }

    @Override
    public void applicaSessione(Sessione sessione) {
        validaSessione(sessione);

        double km = sessione.getKmPercorsi();
        int tempo = sessione.getTempo();

        percorriKm(km);

        for (Pezzo pezzo : pezzi.values()) {
            pezzo.aggiornaUtilizzo(km, tempo);
        }
    }

    private void validaSessione(Sessione sessione) {
        if (sessione == null) {
            throw new IllegalArgumentException(
                    "La sessione non può essere null"
            );
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
