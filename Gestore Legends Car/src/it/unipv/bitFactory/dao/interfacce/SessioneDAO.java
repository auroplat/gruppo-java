package it.unipv.bitFactory.dao.interfacce;

import it.unipv.bitFactory.model.sessioni.Sessione;

public interface SessioneDAO {

    void salva(String idMacchina, Sessione sessione);
}