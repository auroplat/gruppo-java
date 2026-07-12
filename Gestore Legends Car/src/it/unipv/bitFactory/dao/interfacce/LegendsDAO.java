package it.unipv.bitFactory.dao.interfacce;

import java.util.List;
import java.util.Optional;

import it.unipv.bitFactory.model.veicoli.Legends;

public interface LegendsDAO {

    void salva(Legends legends);

    Optional<Legends> trovaPerId(String id);

    List<Legends> trovaTutte();

    void elimina(String id);
    
}