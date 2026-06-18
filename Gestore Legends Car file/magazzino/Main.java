package it.unipv.bitFactory;

import it.unipv.bitFactory.magazzino.AvvisoMagazzino;
import it.unipv.bitFactory.magazzino.CsvMagazzinoRepository;
import it.unipv.bitFactory.magazzino.GestioneMagazzinoController;
import it.unipv.bitFactory.magazzino.GestoreAvvisiMagazzino;
import it.unipv.bitFactory.magazzino.Magazzino;
import it.unipv.bitFactory.pezzi.Pezzo;
import it.unipv.bitFactory.pezzi.TipoPezzo;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        String percorsoCsv = "magazzino.csv";

        Magazzino magazzino = new Magazzino();
        CsvMagazzinoRepository repository = new CsvMagazzinoRepository();
        GestoreAvvisiMagazzino gestoreAvvisi = new GestoreAvvisiMagazzino();

        GestioneMagazzinoController controller =
                new GestioneMagazzinoController(magazzino, repository, gestoreAvvisi);

        System.out.println("=== GESTIONE MAGAZZINO ===");

        controller.aggiungiPezzo(100, TipoPezzo.MOTORE, "Motore Audi", 5, 2);
        controller.aggiungiPezzo(101, TipoPezzo.FRENO, "Freni Brembo", 3, 1);
        controller.aggiungiPezzo(102, TipoPezzo.GOMMA, "Gomme Pirelli", 1, 2);
        controller.aggiungiPezzo(103, TipoPezzo.BATTERIA, "Batteria Bosch", 0, 1);

        System.out.println("\n--- Lista iniziale pezzi ---");
        stampaPezzi(controller.visualizzaTuttiIPezzi());

        System.out.println("\n--- Ricerca per codice 100 ---");
        Pezzo pezzoTrovato = controller.cercaPerCodice(100);
        System.out.println(pezzoTrovato);

        System.out.println("\n--- Ricerca per nome 'Brembo' ---");
        List<Pezzo> risultatiNome = controller.cercaPerNome("Brembo");
        stampaPezzi(risultatiNome);

        System.out.println("\n--- Aggiungo quantità al pezzo 102 ---");
        controller.aggiungiQuantita(102, 4);
        System.out.println(controller.cercaPerCodice(102));

        System.out.println("\n--- Rimuovo quantità dal pezzo 100 ---");
        controller.rimuoviQuantita(100, 3);
        System.out.println(controller.cercaPerCodice(100));

        System.out.println("\n--- Aggiorno soglia minima del pezzo 101 ---");
        controller.aggiornaSogliaMinima(101, 4);
        System.out.println(controller.cercaPerCodice(101));

        System.out.println("\n--- Avvisi magazzino ---");
        for (Pezzo pezzo : controller.visualizzaTuttiIPezzi()) {
            AvvisoMagazzino avviso = controller.generaAvviso(pezzo.getCodice());
            System.out.println(avviso.getMessaggio());
        }

        System.out.println("\n--- Rimuovo pezzo 103 ---");
        boolean rimosso = controller.rimuoviPezzo(103);

        if (rimosso) {
            System.out.println("Pezzo rimosso correttamente.");
        } else {
            System.out.println("Pezzo non trovato.");
        }

        System.out.println("\n--- Lista finale pezzi ---");
        stampaPezzi(controller.visualizzaTuttiIPezzi());

        System.out.println("\n--- Salvataggio su CSV ---");
        controller.salvaSuCsv(percorsoCsv);
        System.out.println("File CSV salvato: " + percorsoCsv);
    }

    private static void stampaPezzi(List<Pezzo> pezzi) {
        for (Pezzo pezzo : pezzi) {
            System.out.println(pezzo);
        }
    }
}
