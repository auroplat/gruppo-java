package it.unipv.bitFactory.dao.interfacce;

import it.unipv.bitFactory.model.persona.Addetto;

public interface AddettoDAO {

     //Cerca un addetto tramite il suo username.             
    Addetto trovaPerUsername(String username);
}
