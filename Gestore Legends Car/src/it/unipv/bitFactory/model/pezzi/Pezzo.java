package it.unipv.bitFactory.model.pezzi;

import java.util.UUID;

public class Pezzo {
    private final String idPezzo;
    private final TipoPezzo tipo;
    private final double kmMax;
    private double kmAttuali;
    private final int tempoMax;
    private int tempoAttuale;

    public Pezzo(TipoPezzo tipo, double kmMax, int tempoMax) {
        this(generaId(), tipo, kmMax, tempoMax);
    }

    public Pezzo(String idPezzo, TipoPezzo tipo, double kmMax, int tempoMax) {
        if (idPezzo == null || idPezzo.isBlank()) {
            throw new IllegalArgumentException("L'id del pezzo non può essere vuoto");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Il tipo del pezzo non può essere null");
        }
        if (kmMax < 0 || tempoMax < 0) {
            throw new IllegalArgumentException("Km max e tempo max non possono essere negativi");
        }

        this.idPezzo = idPezzo.trim();
        this.tipo = tipo;
        this.kmMax = kmMax;
        this.tempoMax = tempoMax;
        this.kmAttuali = 0;
        this.tempoAttuale = 0;
    }

    private static String generaId() {
        return "PZ-" + UUID.randomUUID().toString().replace("-", "");
    }

    public void aggiornaKm(double km) {
        if (km < 0) {
            throw new IllegalArgumentException("I km non possono essere negativi");
        }
        kmAttuali += km;
    }

    public void aggiornaTempo(int tempo) {
        if (tempo < 0) {
            throw new IllegalArgumentException("Il tempo non può essere negativo");
        }
        tempoAttuale += tempo;
    }

    public void aggiornaUtilizzo(double km, int tempo) {
        aggiornaKm(km);
        aggiornaTempo(tempo);
    }

    public boolean daSostituire() {
        boolean limiteKmRaggiunto = kmMax > 0 && kmAttuali >= kmMax;
        boolean limiteTempoRaggiunto = tempoMax > 0 && tempoAttuale >= tempoMax;
        return limiteKmRaggiunto || limiteTempoRaggiunto;
    }

    public String getIdPezzo() { return idPezzo; }
    public TipoPezzo getTipo() { return tipo; }
    public double getKmAttuali() { return kmAttuali; }
    public int getTempoAttuale() { return tempoAttuale; }
    public double getKmMax() { return kmMax; }
    public int getTempoMax() { return tempoMax; }

    @Override
    public String toString() {
        return "Pezzo{" +
                "idPezzo='" + idPezzo + '\'' +
                ", tipo=" + tipo +
                ", kmAttuali=" + kmAttuali +
                ", kmMax=" + kmMax +
                ", tempoAttuale=" + tempoAttuale +
                ", tempoMax=" + tempoMax +
                '}';
    }
}