package it.unipv.bitFactory.dao.interfacce;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import it.unipv.bitFactory.model.magazzino.VoceMagazzino;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;

public interface MagazzinoDAO {

    Optional<VoceMagazzino> trovaPerIdPezzo(String idPezzo);

    List<VoceMagazzino> trovaTutti();

    /**
     * Metodo mantenuto per compatibilità con il codice precedente.
     * Nel database ogni riga rappresenta un singolo pezzo, quindi i soli
     * valori ammessi sono 0 (rimozione del pezzo libero) e 1 (pezzo presente).
     */
    void aggiornaQuantita(String idPezzo, int nuovaQuantita);

    List<Pezzo> trovaPezziLiberi(TipoPezzo tipoPezzo);

    void aggiungiPezzi(
            TipoPezzo tipoPezzo,
            int quantita,
            double kmMax,
            int tempoMax);

    void creaMacchina(
            String idMacchina,
            Map<TipoPezzo, Integer> ricetta);

    void cambiaPezzo(
            String idMacchina,
            TipoPezzo tipoPezzo,
            String idNuovoPezzo);
}
