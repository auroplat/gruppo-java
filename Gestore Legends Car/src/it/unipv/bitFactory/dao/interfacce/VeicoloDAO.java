package it.unipv.bitFactory.dao.veicoli;

import java.util.List;
import java.util.Optional;

import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.model.veicoli.Legends;

public interface VeicoloDAO {

    void inizializzaDatabase();

    boolean esisteVeicolo(String idVeicolo);

    int contaPezziLiberi(TipoPezzo tipoPezzo);

    List<Pezzo> trovaPezziLiberi(TipoPezzo tipoPezzo, int quantitaRichiesta);

    void salvaLegends(Legends legends);

    Optional<Legends> trovaLegendsPerId(String idVeicolo);

    List<Legends> trovaTutteLegends();
}