package it.unipv.bitFactory.dao.interfacce;

import java.util.List;
import java.util.Optional;

import it.unipv.bitFactory.model.magazzino.VoceMagazzino;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;

/**
 * DAO dedicato esclusivamente alla persistenza dei pezzi di magazzino.
 * Non crea macchine e non coordina la sostituzione dei pezzi sulle Legends.
 */
public interface MagazzinoDAO {

    Optional<VoceMagazzino> trovaPerIdPezzo(String idPezzo);

    List<VoceMagazzino> trovaTutti();

    /**
     * Mantiene la compatibilità con la precedente API:
     * 0 elimina un pezzo libero, 1 verifica che il pezzo sia libero.
     */
    void aggiornaQuantita(String idPezzo, int nuovaQuantita);

    List<Pezzo> trovaPezziLiberi(TipoPezzo tipoPezzo);

    void aggiungiPezzi(
            TipoPezzo tipoPezzo,
            int quantita,
            double kmMax,
            int tempoMax
    );

    /**
     * Elimina definitivamente un pezzo, ad esempio quello usurato
     * rimosso durante una sostituzione.
     */
    void scartaPezzo(String idPezzo);
}