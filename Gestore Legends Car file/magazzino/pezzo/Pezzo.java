package it.unipv.bitFactory.pezzo;

public class Pezzo {

    private final String codice;
    private String nome;
    private TipoPezzo tipo;
    private int quantita;
    private int quantitaMinima;

    public Pezzo(String codice, String nome, TipoPezzo tipo, int quantita, int quantitaMinima) {
        if (codice == null || codice.isBlank()) {
            throw new IllegalArgumentException("Il codice non può essere vuoto");
        }

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Il nome non può essere vuoto");
        }

        if (tipo == null) {
            throw new IllegalArgumentException("Il tipo del pezzo non può essere vuoto");
        }

        if (quantita < 0) {
            throw new IllegalArgumentException("La quantità non può essere negativa");
        }

        if (quantitaMinima < 0) {
            throw new IllegalArgumentException("La quantità minima non può essere negativa");
        }

        this.codice = codice;
        this.nome = nome;
        this.tipo = tipo;
        this.quantita = quantita;
        this.quantitaMinima = quantitaMinima;
    }

    public void aggiungiQuantita(int quantitaDaAggiungere) {
        if (quantitaDaAggiungere < 0) {
            throw new IllegalArgumentException("La quantità da aggiungere non può essere negativa");
        }

        quantita += quantitaDaAggiungere;
    }

    public void rimuoviQuantita(int quantitaDaRimuovere) {
        if (quantitaDaRimuovere < 0) {
            throw new IllegalArgumentException("La quantità da rimuovere non può essere negativa");
        }

        if (quantitaDaRimuovere > quantita) {
            throw new IllegalArgumentException("Non puoi rimuovere più pezzi di quelli presenti");
        }

        quantita -= quantitaDaRimuovere;
    }

    public void aggiornaQuantitaMinima(int nuovaQuantitaMinima) {
        if (nuovaQuantitaMinima < 0) {
            throw new IllegalArgumentException("La quantità minima non può essere negativa");
        }

        quantitaMinima = nuovaQuantitaMinima;
    }

    public boolean sottoScorta() {
        return quantita <= quantitaMinima;
    }

    public String getCodice() {
        return codice;
    }

    public String getNome() {
        return nome;
    }

    public TipoPezzo getTipo() {
        return tipo;
    }

    public int getQuantita() {
        return quantita;
    }

    public int getQuantitaMinima() {
        return quantitaMinima;
    }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Il nome non può essere vuoto");
        }

        this.nome = nome;
    }

    public void setTipo(TipoPezzo tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("Il tipo del pezzo non può essere vuoto");
        }

        this.tipo = tipo;
    }

    @Override
    public String toString() {
        return "Pezzo{" +
                "codice='" + codice + '\'' +
                ", nome='" + nome + '\'' +
                ", tipo=" + tipo +
                ", quantita=" + quantita +
                ", quantitaMinima=" + quantitaMinima +
                ", sottoScorta=" + sottoScorta() +
                '}';
    }
}
