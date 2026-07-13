package it.unipv.bitFactory.controller;

import java.util.List;
import java.util.Map;

import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.model.veicoli.Legends;
import it.unipv.bitFactory.service.VeicoloService;

public class GestioneVeicoliController {

    private final VeicoloService veicoloService;

    public GestioneVeicoliController(VeicoloService veicoloService) {
        if (veicoloService == null) {
            throw new IllegalArgumentException("Il service veicoli non può essere null");
        }
        this.veicoloService = veicoloService;
    }

    public Legends creaLegends(String idVeicolo) {
        return veicoloService.creaLegends(idVeicolo);
    }

    public void eliminaLegends(String idVeicolo) {
        veicoloService.eliminaLegends(idVeicolo);
    }

    public Legends sostituisciPezzo(String idVeicolo, String idPezzoVecchio, String idPezzoNuovo) {
        return veicoloService.sostituisciPezzo(idVeicolo, idPezzoVecchio, idPezzoNuovo);
    }

    public boolean puoCreareLegends() {
        return veicoloService.puoCreareLegends();
    }

    public Legends cercaLegends(String idVeicolo) {
        return veicoloService.trovaLegendsPerId(idVeicolo);
    }

    public List<Legends> visualizzaLegends() {
        return veicoloService.trovaTutteLegends();
    }

    public Map<TipoPezzo, Integer> visualizzaRicettaLegends() {
        return veicoloService.getRicettaLegends();
    }
}
