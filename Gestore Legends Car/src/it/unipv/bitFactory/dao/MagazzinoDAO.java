package it.unipv.bitFactory.dao.magazzino;

import java.util.List;
import java.util.Optional;

import it.unipv.bitFactory.model.magazzino.VoceMagazzino;
import it.unipv.bitFactory.model.pezzi.Pezzo;

public interface MagazzinoDAO {

    Optional<VoceMagazzino> trovaPerIdPezzo(String idPezzo);

    List<VoceMagazzino> trovaTutti();

    List<VoceMagazzino> trovaDisponibili();

    void inserisciPezzo(Pezzo pezzo);

    void eliminaPezzo(String idPezzo);
}
