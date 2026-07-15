package it.unipv.bitFactory.dao.interfacce;

import java.util.List;
import java.util.Optional;

import it.unipv.bitFactory.model.magazzino.VoceMagazzino;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;

public interface MagazzinoDAO {

    Optional<VoceMagazzino> trovaPerIdPezzo(String idPezzo);

    List<VoceMagazzino> trovaTutti();

    void aggiornaQuantita(String idPezzo, int nuovaQuantita);

    List<Pezzo> trovaPezziLiberi(TipoPezzo tipoPezzo);

    void aggiungiPezzi(
            TipoPezzo tipoPezzo,
            int quantita,
            double kmMax,
            int tempoMax
    );

    void scartaPezzo(String idPezzo);
}