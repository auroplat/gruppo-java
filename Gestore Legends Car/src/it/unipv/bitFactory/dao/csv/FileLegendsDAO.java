package it.unipv.bitFactory.dao.csv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import it.unipv.bitFactory.dao.interfacce.DAOException;
import it.unipv.bitFactory.dao.interfacce.LegendsDAO;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.model.veicoli.Legends;

public class FileLegendsDAO implements LegendsDAO {

    private static final String SEPARATORE = ";";
    private static final String RECORD_MACCHINA = "MACCHINA";
    private static final String RECORD_PEZZO = "PEZZO";

    private final Path filePath;

    public FileLegendsDAO(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Il percorso del file non può essere vuoto");
        }

        this.filePath = Path.of(filePath);
    }

    @Override
    public void salva(Legends legends) {
        if (legends == null) {
            throw new IllegalArgumentException("La macchina da salvare non può essere null");
        }

        Map<String, Legends> archivio = leggiTutteComeMappa();

        // Se la macchina esiste già, viene aggiornata.
        archivio.put(legends.getId(), legends);
        scriviTutte(archivio.values());
    }

    @Override
    public Optional<Legends> trovaPerId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("L'id non può essere vuoto");
        }

        Map<String, Legends> archivio = leggiTutteComeMappa();
        return Optional.ofNullable(archivio.get(id));
    }

    @Override
    public List<Legends> trovaTutte() {return new ArrayList<>(leggiTutteComeMappa().values());}

    @Override
    public void elimina(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("L'id non può essere vuoto");
        }

        Map<String, Legends> archivio = leggiTutteComeMappa();
        archivio.remove(id);
        scriviTutte(archivio.values());
    }

    private Map<String, Legends> leggiTutteComeMappa() {
        Map<String, Legends> archivio = new LinkedHashMap<>();

        if (!Files.exists(filePath)) {return archivio;}

        try {
            List<String> righe = Files.readAllLines(filePath);

            for (String riga : righe) {
                if (riga == null || riga.isBlank()) {continue;}

                String[] campi = riga.split(SEPARATORE, -1);

                if (campi.length == 0) {continue;}

                switch (campi[0]) {
                    case RECORD_MACCHINA:
                        leggiRigaMacchina(campi, archivio);
                        break;

                    case RECORD_PEZZO:
                        leggiRigaPezzo(campi, archivio);
                        break;

                    default:
                        throw new DAOException("Tipo di record CSV non riconosciuto: " + campi[0]);
                }
            }

            return archivio;

        } catch (IOException e) {
            throw new DAOException("Errore durante la lettura del file CSV", e);
        } catch (RuntimeException e) {
            throw new DAOException("Formato CSV non valido", e);
        }
    }

    private void leggiRigaMacchina(String[] campi, Map<String, Legends> archivio) {
        if (campi.length != 3) {
            throw new DAOException("Riga MACCHINA non valida");
        }

        String id = campi[1];
        double kmTotali = Double.parseDouble(campi[2]);

        Legends legends = archivio.get(id);

        if (legends == null) {
            legends = new Legends(id);
            archivio.put(id, legends);
        }

        double kmDaAggiungere = kmTotali - legends.getKmTotali();

        if (kmDaAggiungere < 0) {
            throw new DAOException("Km totali incoerenti per la macchina " + id);
        }

        legends.percorriKm(kmDaAggiungere);
    }

    private void leggiRigaPezzo(String[] campi, Map<String, Legends> archivio) {
        if (campi.length != 7) {
            throw new DAOException("Riga PEZZO non valida");
        }

        String idMacchina = campi[1];
        TipoPezzo tipo = TipoPezzo.valueOf(campi[2]);
        double kmMax = Double.parseDouble(campi[3]);
        int tempoMax = Integer.parseInt(campi[4]);
        double kmAttuali = Double.parseDouble(campi[5]);
        int tempoAttuale = Integer.parseInt(campi[6]);

        Legends legends = archivio.get(idMacchina);

        if (legends == null) {
            legends = new Legends(idMacchina);
            archivio.put(idMacchina, legends);
        }

        Pezzo pezzo = new Pezzo(tipo, kmMax, tempoMax);
        pezzo.aggiornaUtilizzo(kmAttuali, tempoAttuale);

        // Qui il pezzo viene legato alla macchina.
        legends.modificaPezzo(pezzo);
    }

    private void scriviTutte(Collection<Legends> macchine) {
        List<String> righe = new ArrayList<>();

        for (Legends legends : macchine) {
            righe.add(creaRigaMacchina(legends));

            for (Pezzo pezzo : legends.getTuttiPezzi()) {
                righe.add(creaRigaPezzo(legends, pezzo));
            }
        }

        try {
            Path parent = filePath.getParent();

            if (parent != null) {Files.createDirectories(parent);}

            Files.write(filePath, righe);

        } catch (IOException e) {
            throw new DAOException("Errore durante la scrittura del file CSV", e);
        }
    }

    private String creaRigaMacchina(Legends legends) {
        return RECORD_MACCHINA + SEPARATORE +
                legends.getId() + SEPARATORE +
                legends.getKmTotali();
    }

    private String creaRigaPezzo(Legends legends, Pezzo pezzo) {
        return RECORD_PEZZO + SEPARATORE +
                legends.getId() + SEPARATORE +
                pezzo.getTipo() + SEPARATORE +
                pezzo.getKmMax() + SEPARATORE +
                pezzo.getTempoMax() + SEPARATORE +
                pezzo.getKmAttuali() + SEPARATORE +
                pezzo.getTempoAttuale();
    }
}