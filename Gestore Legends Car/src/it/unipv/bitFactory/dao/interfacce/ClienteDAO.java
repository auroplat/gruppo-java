package it.unipv.bitFactory.dao.interfacce;

import java.util.List;
import java.util.Optional;

import it.unipv.bitFactory.model.persona.Cliente;

public interface ClienteDAO {

    List<Cliente> caricaClienti();

    Optional<Cliente> cercaPerEmail(String email);

    boolean salva(Cliente cliente);

    boolean elimina(String email);
}