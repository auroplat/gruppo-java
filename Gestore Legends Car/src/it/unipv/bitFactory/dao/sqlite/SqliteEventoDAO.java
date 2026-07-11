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

import it.unipv.bitFactory.dao.DAOException;
import it.unipv.bitFactory.dao.EventoDAO;
import it.unipv.bitFactory.model.prenotazioni.Evento;

public final class SqliteEventoDAO implements EventoDAO {

    private final String jdbcUrl;

    public SqliteEventoDAO(String databasePath) {
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
    public List<Evento> caricaEventi() {
        String sql = """
                SELECT nome_evento,
                       data_evento,
                       posti_disponibili
                FROM eventi
                ORDER BY data_evento, nome_evento
                """;

        List<Evento> eventi = new ArrayList<>();

        try (Connection connection = apriConnessione();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                eventi.add(creaEvento(result));
            }

            return eventi;

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante il caricamento degli eventi",
                    e
            );
        }
    }

    @Override
    public Evento cercaEvento(String nomeEvento) {
        String nome = validaNomeEvento(nomeEvento);

        String sql = """
                SELECT nome_evento,
                       data_evento,
                       posti_disponibili
                FROM eventi
                WHERE nome_evento = ?
                """;

        try (Connection connection = apriConnessione();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, nome);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return creaEvento(result);
                }

                return null;
            }

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante la ricerca dell'evento " + nome,
                    e
            );
        }
    }

    @Override
    public boolean aggiungiEvento(Evento evento) {
        if (evento == null) {
            throw new IllegalArgumentException(
                    "L'evento non può essere null"
            );
        }

        if (evento.getPostiDisponibili() < 0) {
            throw new IllegalArgumentException(
                    "I posti disponibili non possono essere negativi"
            );
        }

        String sql = """
                INSERT INTO eventi (
                    nome_evento,
                    data_evento,
                    posti_disponibili
                )
                VALUES (?, ?, ?)
                """;

        try (Connection connection = apriConnessione();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    validaNomeEvento(evento.getNomeEvento())
            );

            statement.setString(
                    2,
                    evento.getDataEvento()
            );

            statement.setInt(
                    3,
                    evento.getPostiDisponibili()
            );

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante l'aggiunta dell'evento "
                            + evento.getNomeEvento(),
                    e
            );
        }
    }

    @Override
    public boolean aggiornaPosti(
            String nomeEvento,
            int nuoviPosti) {

        String nome = validaNomeEvento(nomeEvento);

        if (nuoviPosti < 0) {
            throw new IllegalArgumentException(
                    "I posti disponibili non possono essere negativi"
            );
        }

        String sql = """
                UPDATE eventi
                SET posti_disponibili = ?
                WHERE nome_evento = ?
                """;

        try (Connection connection = apriConnessione();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, nuoviPosti);
            statement.setString(2, nome);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante l'aggiornamento dei posti dell'evento "
                            + nome,
                    e
            );
        }
    }

    @Override
    public boolean eliminaEvento(String nomeEvento) {
        String nome = validaNomeEvento(nomeEvento);

        String sql = """
                DELETE FROM eventi
                WHERE nome_evento = ?
                """;

        try (Connection connection = apriConnessione();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, nome);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante l'eliminazione dell'evento "
                            + nome,
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

    private Evento creaEvento(ResultSet result)
            throws SQLException {

        return new Evento(
                result.getString("nome_evento"),
                result.getString("data_evento"),
                result.getInt("posti_disponibili")
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
}