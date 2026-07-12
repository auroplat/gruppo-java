package it.unipv.bitFactory.dao.magazzino;

import java.util.List;
import java.util.Optional;

import it.unipv.bitFactory.model.magazzino.VoceMagazzino;

public interface MagazzinoDAO {

    Optional<VoceMagazzino> trovaPerIdPezzo(String idPezzo);

    List<VoceMagazzino> trovaTutti();

    void aggiornaQuantita(String idPezzo, int nuovaQuantita);
}