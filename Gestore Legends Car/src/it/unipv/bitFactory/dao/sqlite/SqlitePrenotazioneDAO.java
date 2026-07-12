package it.unipv.bitFactory.dao.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import it.unipv.bitFactory.dao.interfacce.DAOException;
import it.unipv.bitFactory.dao.interfacce.PrenotazioneDAO;
import it.unipv.bitFactory.model.prenotazioni.Prenotazione;

public final class SqlitePrenotazioneDAO
        implements PrenotazioneDAO {

    private final String jdbcUrl;

    public SqlitePrenotazioneDAO(String databasePath) {
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
    public List<Prenotazione> caricaPrenotazioni() {
        String sql = """
                SELECT nome_evento,
                       email_cliente,
                       telefono_cliente
                FROM prenotazioni
                ORDER BY nome_evento, email_cliente
                """;

        List<Prenotazione> prenotazioni = new ArrayList<>();

        try (Connection connection = apriConnessione();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                prenotazioni.add(
                        creaPrenotazione(result)
                );
            }

            return prenotazioni;

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante il caricamento delle prenotazioni",
                    e
            );
        }
    }

    @Override
    public List<Prenotazione> cercaPerEvento(
            String nomeEvento) {

        String evento = validaNomeEvento(nomeEvento);

        String sql = """
                SELECT nome_evento,
                       email_cliente,
                       telefono_cliente
                FROM prenotazioni
                WHERE nome_evento = ?
                ORDER BY email_cliente
                """;

        List<Prenotazione> prenotazioni = new ArrayList<>();

        try (Connection connection = apriConnessione();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, evento);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    prenotazioni.add(
                            creaPrenotazione(result)
                    );
                }
            }

            return prenotazioni;

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante la ricerca delle prenotazioni "
                            + "per l'evento " + evento,
                    e
            );
        }
    }

    @Override
    public List<Prenotazione> cercaPerCliente(
            String emailCliente) {

        String email = validaEmail(emailCliente);

        String sql = """
                SELECT nome_evento,
                       email_cliente,
                       telefono_cliente
                FROM prenotazioni
                WHERE email_cliente = ? COLLATE NOCASE
                ORDER BY nome_evento
                """;

        List<Prenotazione> prenotazioni = new ArrayList<>();

        try (Connection connection = apriConnessione();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    prenotazioni.add(
                            creaPrenotazione(result)
                    );
                }
            }

            return prenotazioni;

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante la ricerca delle prenotazioni "
                            + "del cliente " + email,
                    e
            );
        }
    }

    @Override
    public Optional<Prenotazione> cerca(
            String nomeEvento,
            String emailCliente) {

        String evento = validaNomeEvento(nomeEvento);
        String email = validaEmail(emailCliente);

        String sql = """
                SELECT nome_evento,
                       email_cliente,
                       telefono_cliente
                FROM prenotazioni
                WHERE nome_evento = ?
                  AND email_cliente = ? COLLATE NOCASE
                """;

        try (Connection connection = apriConnessione();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, evento);
            statement.setString(2, email);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        creaPrenotazione(result)
                );
            }

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante la ricerca della prenotazione",
                    e
            );
        }
    }

    @Override
    public boolean aggiungi(Prenotazione prenotazione) {
        if (prenotazione == null) {
            throw new IllegalArgumentException(
                    "La prenotazione non può essere null"
            );
        }

        String sql = """
                INSERT INTO prenotazioni (
                    nome_evento,
                    email_cliente,
                    telefono_cliente
                )
                VALUES (?, ?, ?)
                """;

        try (Connection connection = apriConnessione();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    prenotazione.getNomeEvento()
            );

            statement.setString(
                    2,
                    prenotazione.getEmailCliente()
            );

            statement.setString(
                    3,
                    prenotazione.getTelefonoCliente()
            );

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante il salvataggio della prenotazione. "
                            + "Il cliente potrebbe essere già prenotato.",
                    e
            );
        }
    }

    @Override
    public boolean elimina(
            String nomeEvento,
            String emailCliente) {

        String evento = validaNomeEvento(nomeEvento);
        String email = validaEmail(emailCliente);

        String sql = """
                DELETE FROM prenotazioni
                WHERE nome_evento = ?
                  AND email_cliente = ? COLLATE NOCASE
                """;

        try (Connection connection = apriConnessione();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, evento);
            statement.setString(2, email);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante l'annullamento della prenotazione",
                    e
            );
        }
    }

    private Connection apriConnessione()
            throws SQLException {

        Connection connection =
                DriverManager.getConnection(jdbcUrl);

        try (Statement statement =
                     connection.createStatement()) {

            statement.execute(
                    "PRAGMA foreign_keys = ON"
            );
        }

        return connection;
    }

    private Prenotazione creaPrenotazione(
            ResultSet result) throws SQLException {

        return new Prenotazione(
                result.getString("nome_evento"),
                result.getString("email_cliente"),
                result.getString("telefono_cliente")
        );
    }

    private String validaNomeEvento(String nomeEvento) {
        if (nomeEvento == null || nomeEvento.isBlank()) {
            throw new IllegalArgumentException(
                    "Il nome dell'evento non può essere vuoto"
            );
        }

        return nomeEvento.trim();
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