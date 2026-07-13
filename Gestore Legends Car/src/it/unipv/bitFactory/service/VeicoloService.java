package it.unipv.bitFactory.service.veicoli;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import it.unipv.bitFactory.dao.veicoli.VeicoloDAO;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.model.veicoli.Legends;
import it.unipv.bitFactory.model.veicoli.TipoVeicolo;

public class VeicoloService {

    private final VeicoloDAO veicoloDAO;

    public VeicoloService(VeicoloDAO veicoloDAO) {
        if (veicoloDAO == null) {
            throw new IllegalArgumentException("Il DAO veicoli non può essere null");
        }

        this.veicoloDAO = veicoloDAO;
        this.veicoloDAO.inizializzaDatabase();
    }

    public Legends creaLegends(String idVeicolo) {
        if (idVeicolo == null || idVeicolo.isBlank()) {
            throw new IllegalArgumentException("L'id del veicolo non può essere vuoto");
        }

        if (veicoloDAO.esisteVeicolo(idVeicolo)) {
            throw new IllegalArgumentException("Esiste già un veicolo con id: " + idVeicolo);
        }

        Map<TipoPezzo, Integer> ricetta = getRicettaLegends();

        controllaDisponibilitaPezzi(ricetta);

        Legends legends = new Legends(idVeicolo);

        for (Map.Entry<TipoPezzo, Integer> richiesta : ricetta.entrySet()) {
            TipoPezzo tipoPezzo = richiesta.getKey();
            int quantita = richiesta.getValue();

            List<Pezzo> pezziLiberi = veicoloDAO.trovaPezziLiberi(tipoPezzo, quantita);

            if (pezziLiberi.size() < quantita) {
                throw new IllegalStateException("Pezzi non più disponibili per il tipo: " + tipoPezzo);
            }

            for (Pezzo pezzo : pezziLiberi) {
                legends.aggiungiPezzo(pezzo);
            }
        }

        veicoloDAO.salvaLegends(legends);

        return legends;
    }

    public Map<TipoPezzo, Integer> getRicettaLegends() {
        Map<TipoPezzo, Integer> ricetta = new EnumMap<>(TipoPezzo.class);

        ricetta.put(TipoPezzo.SCOCCA, 1);
        ricetta.put(TipoPezzo.MOTORE, 1);
        ricetta.put(TipoPezzo.CAMBIO, 1);
        ricetta.put(TipoPezzo.VOLANTE, 1);
        ricetta.put(TipoPezzo.RUOTA, 4);
        ricetta.put(TipoPezzo.FRENO, 4);

        return ricetta;
    }

    public boolean puoCreareLegends() {
        try {
            controllaDisponibilitaPezzi(getRicettaLegends());
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public Legends trovaLegendsPerId(String idVeicolo) {
        return veicoloDAO.trovaLegendsPerId(idVeicolo)
                .orElseThrow(() -> new IllegalArgumentException("Legends non trovata: " + idVeicolo));
    }

    public List<Legends> trovaTutteLegends() {
        return veicoloDAO.trovaTutteLegends();
    }

    public TipoVeicolo getTipoVeicoloGestito() {
        return TipoVeicolo.LEGENDS;
    }

    private void controllaDisponibilitaPezzi(Map<TipoPezzo, Integer> ricetta) {
        for (Map.Entry<TipoPezzo, Integer> richiesta : ricetta.entrySet()) {
            TipoPezzo tipoPezzo = richiesta.getKey();
            int quantitaRichiesta = richiesta.getValue();

            int quantitaDisponibile = veicoloDAO.contaPezziLiberi(tipoPezzo);

            if (quantitaDisponibile < quantitaRichiesta) {
                throw new IllegalStateException(
                        "Pezzi insufficienti per creare una Legends. Tipo pezzo: " +
                                tipoPezzo +
                                ", richiesti: " +
                                quantitaRichiesta +
                                ", disponibili: " +
                                quantitaDisponibile
                );
            }
        }
    }
}