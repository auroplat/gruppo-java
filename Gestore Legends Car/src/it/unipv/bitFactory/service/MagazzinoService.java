package it.unipv.bitFactory.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import it.unipv.bitFactory.dao.interfacce.LegendsDAO;
import it.unipv.bitFactory.dao.interfacce.MagazzinoDAO;
import it.unipv.bitFactory.model.magazzino.StatoDisponibilita;
import it.unipv.bitFactory.model.magazzino.VoceMagazzino;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.model.veicoli.Legends;

public final class MagazzinoService {

    private final MagazzinoDAO magazzinoDAO;
    private final LegendsDAO legendsDAO;

    public MagazzinoService(
            MagazzinoDAO magazzinoDAO,
            LegendsDAO legendsDAO
    ) {
        this.magazzinoDAO = Objects.requireNonNull(
                magazzinoDAO,
                "Il DAO magazzino non può essere null"
        );
        this.legendsDAO = Objects.requireNonNull(
                legendsDAO,
                "Il DAO delle Legends non può essere null"
        );
    }

    public StatoDisponibilita controllaDisponibilita(String idPezzo) {
        return cercaPezzo(idPezzo).getStatoDisponibilita();
    }

    public VoceMagazzino cercaPezzo(String idPezzo) {
        return magazzinoDAO.trovaPerIdPezzo(idPezzo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pezzo non trovato: " + idPezzo
                ));
    }

    public List<VoceMagazzino> visualizzaMagazzino() {
        return magazzinoDAO.trovaTutti();
    }

    public StatoDisponibilita aggiornaQuantitaPezzo(
            String idPezzo,
            int nuovaQuantita
    ) {
        magazzinoDAO.aggiornaQuantita(idPezzo, nuovaQuantita);

        if (nuovaQuantita == 0) {
            return StatoDisponibilita.ESAURITO;
        }

        return controllaDisponibilita(idPezzo);
    }

    public List<Pezzo> trovaPezziLiberi(TipoPezzo tipoPezzo) {
        validaTipo(tipoPezzo);
        return magazzinoDAO.trovaPezziLiberi(tipoPezzo);
    }

    public List<Pezzo> trovaPezziMontati(
            String idMacchina,
            TipoPezzo tipoPezzo
    ) {
        String id = validaIdMacchina(idMacchina);
        validaTipo(tipoPezzo);

        Legends legends = legendsDAO
                .trovaPerId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Macchina non trovata: " + id
                ));

        return legends.getPezzi(tipoPezzo);
    }

    public void aggiungiPezzi(
            TipoPezzo tipoPezzo,
            int quantita,
            double kmMax,
            int tempoMax
    ) {
        magazzinoDAO.aggiungiPezzi(
                tipoPezzo,
                quantita,
                kmMax,
                tempoMax
        );
    }

    public void creaMacchina(String idMacchina) {
        String id = validaIdMacchina(idMacchina);

        if (legendsDAO.trovaPerId(id).isPresent()) {
            throw new IllegalArgumentException(
                    "Esiste già una macchina con id: " + id
            );
        }

        Legends legends = new Legends(id);

        for (Map.Entry<TipoPezzo, Integer> voce
                : ricettaLegends().entrySet()) {

            TipoPezzo tipo = voce.getKey();
            int quantitaRichiesta = voce.getValue();
            List<Pezzo> disponibili =
                    magazzinoDAO.trovaPezziLiberi(tipo);

            if (disponibili.size() < quantitaRichiesta) {
                throw new IllegalStateException(
                        "Pezzi insufficienti per " + tipo
                                + ": richiesti " + quantitaRichiesta
                                + ", disponibili " + disponibili.size()
                );
            }

            disponibili.stream()
                    .limit(quantitaRichiesta)
                    .forEach(legends::aggiungiPezzo);
        }


        legendsDAO.salva(legends);
    }


    public synchronized void cambiaPezzo(
            String idMacchina,
            String idVecchioPezzo,
            String idNuovoPezzo
    ) {
        String idMacchinaPulito = validaIdMacchina(idMacchina);
        String idVecchioPulito = validaIdPezzo(idVecchioPezzo);
        String idNuovoPulito = validaIdPezzo(idNuovoPezzo);

        if (idVecchioPulito.equals(idNuovoPulito)) {
            throw new IllegalArgumentException(
                    "Il nuovo pezzo deve essere diverso da quello vecchio"
            );
        }

        Legends legends = legendsDAO
                .trovaPerId(idMacchinaPulito)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Macchina non trovata: " + idMacchinaPulito
                ));

        Pezzo vecchioPezzo = legends.getPezzoPerId(idVecchioPulito);

        if (vecchioPezzo == null) {
            throw new IllegalArgumentException(
                    "Il pezzo " + idVecchioPulito
                            + " non è montato sulla macchina "
                            + idMacchinaPulito
            );
        }

        TipoPezzo tipoPezzo = vecchioPezzo.getTipo();

        Pezzo nuovoPezzo = magazzinoDAO
                .trovaPezziLiberi(tipoPezzo)
                .stream()
                .filter(pezzo -> pezzo.getIdPezzo()
                        .equals(idNuovoPulito))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Il pezzo " + idNuovoPulito
                                + " non è libero oppure non è di tipo "
                                + tipoPezzo
                ));

        Pezzo pezzoRimosso = legends.sostituisciPezzo(
                idVecchioPulito,
                nuovoPezzo
        );

        legendsDAO.salva(legends);

   
        magazzinoDAO.scartaPezzo(pezzoRimosso.getIdPezzo());
    }

    private Map<TipoPezzo, Integer> ricettaLegends() {
        Map<TipoPezzo, Integer> ricetta =
                new EnumMap<>(TipoPezzo.class);

        ricetta.put(TipoPezzo.SCOCCA, 1);
        ricetta.put(TipoPezzo.MOTORE, 1);
        ricetta.put(TipoPezzo.VOLANTE, 1);
        ricetta.put(TipoPezzo.RUOTA, 4);
        ricetta.put(TipoPezzo.FRENO, 4);

        return ricetta;
    }

    private String validaIdMacchina(String idMacchina) {
        if (idMacchina == null || idMacchina.isBlank()) {
            throw new IllegalArgumentException(
                    "L'id della macchina non può essere vuoto"
            );
        }

        return idMacchina.trim();
    }

    private String validaIdPezzo(String idPezzo) {
        if (idPezzo == null || idPezzo.isBlank()) {
            throw new IllegalArgumentException(
                    "L'id del pezzo non può essere vuoto"
            );
        }

        return idPezzo.trim();
    }

    private void validaTipo(TipoPezzo tipoPezzo) {
        if (tipoPezzo == null) {
            throw new IllegalArgumentException(
                    "Il tipo del pezzo non può essere null"
            );
        }
    }
}