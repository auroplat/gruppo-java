package it.unipv.bitFactory.dao.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import it.unipv.bitFactory.dao.interfacce.ClienteDAO;
import it.unipv.bitFactory.dao.interfacce.DAOException;
import it.unipv.bitFactory.model.persona.Cliente;

public final class SqliteClienteDAO implements ClienteDAO {

    private final String jdbcUrl;

    public SqliteClienteDAO(String databasePath) {
        if (databasePath == null || databasePath.isBlank()) {
            throw new IllegalArgumentException(
                    "Il percorso del database non può essere vuoto"
            );
        }

        Path path = Path.of(databasePath)
                .toAbsolutePath()
                .normalize();

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(
                    "Database SQLite non trovato: " + path
            );
        }

        if (!Files.isReadable(path) || !Files.isWritable(path)) {
            throw new IllegalArgumentException(
                    "Il database deve essere leggibile e modificabile: " + path
            );
        }

        this.jdbcUrl = "jdbc:sqlite:" + path;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new DAOException(
                    "Driver SQLite JDBC non trovato",
                    e
            );
        }
    }

    @Override
    public List<Cliente> caricaClienti() {
        String sql = """
                SELECT nome,
                       cognome,
                       data_nascita,
                       email_cliente,
                       telefono
                FROM clienti
                ORDER BY cognome, nome
                """;

        List<Cliente> clienti = new ArrayList<>();

        try (Connection connection = apriConnessione();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                clienti.add(creaCliente(result));
            }

            return clienti;

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante il caricamento dei clienti",
                    e
            );
        }
    }

    @Override
    public Optional<Cliente> cercaPerEmail(String email) {
        String emailValida = validaEmail(email);

        String sql = """
                SELECT nome,
                       cognome,
                       data_nascita,
                       email_cliente,
                       telefono
                FROM clienti
                WHERE email_cliente = ? COLLATE NOCASE
                """;

        try (Connection connection = apriConnessione();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, emailValida);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                return Optional.of(creaCliente(result));
            }

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante la ricerca del cliente " + emailValida,
                    e
            );
        }
    }

    @Override
    public boolean salva(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException(
                    "Il cliente non può essere null"
            );
        }

        String sql = """
                INSERT INTO clienti (
                    email_cliente,
                    nome,
                    cognome,
                    data_nascita,
                    telefono
                )
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(email_cliente) DO UPDATE SET
                    nome = excluded.nome,
                    cognome = excluded.cognome,
                    data_nascita = excluded.data_nascita,
                    telefono = excluded.telefono
                """;

        try (Connection connection = apriConnessione();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, cliente.getEmail());
            statement.setString(2, cliente.getNome());
            statement.setString(3, cliente.getCognome());
            statement.setString(
                    4,
                    cliente.getDataNascita().toString()
            );
            statement.setString(5, cliente.getTelefono());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante il salvataggio del cliente "
                            + cliente.getEmail(),
                    e
            );
        }
    }

    @Override
    public boolean elimina(String email) {
        String emailValida = validaEmail(email);

        String sql = """
                DELETE FROM clienti
                WHERE email_cliente = ? COLLATE NOCASE
                """;

        try (Connection connection = apriConnessione();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, emailValida);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante l'eliminazione del cliente "
                            + emailValida,
                    e
            );
        }
    }

    private Connection apriConnessione()
            throws SQLException {

        return DriverManager.getConnection(jdbcUrl);
    }

    private Cliente creaCliente(ResultSet result)
            throws SQLException {

        return new Cliente(
                result.getString("nome"),
                result.getString("cognome"),
                LocalDate.parse(
                        result.getString("data_nascita")
                ),
                result.getString("email_cliente"),
                result.getString("telefono")
        );
    }

    private String validaEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "L'email non può essere vuota"
            );
        }

        return email.trim().toLowerCase();
    }
}