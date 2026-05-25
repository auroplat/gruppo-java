package it.unipv.bitFactory.model.sessioni;

public class Test extends Sessione {

    private final String descrizione;

    public Test(String luogo, double kmPercorsi, int tempoPassato, String descrizione) {
        super(luogo, kmPercorsi, tempoPassato);
        this.descrizione = descrizione;
    }

    @Override
    public TipoSessione getTipoSessione() {
        return TipoSessione.TEST;
    }

    public String getDescrizione() {
        return descrizione;
    }
}