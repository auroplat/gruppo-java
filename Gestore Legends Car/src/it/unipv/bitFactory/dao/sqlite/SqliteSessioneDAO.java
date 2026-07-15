package it.unipv.bitFactory.dao.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import it.unipv.bitFactory.dao.interfacce.DAOException;
import it.unipv.bitFactory.dao.interfacce.SessioneDAO;
import it.unipv.bitFactory.model.sessioni.Gara;
import it.unipv.bitFactory.model.sessioni.Sessione;
import it.unipv.bitFactory.model.sessioni.Test;

public final class SqliteSessioneDAO implements SessioneDAO {

    private final String jdbcUrl;

    public SqliteSessioneDAO(String databasePath) {

        if (databasePath == null || databasePath.isBlank()) {
            throw new IllegalArgumentException("Il percorso del database non può essere vuoto");
        }

        Path path = Path.of(databasePath).toAbsolutePath().normalize();

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Database SQLite non trovato: " + path);
        }

        if (!Files.isReadable(path) || !Files.isWritable(path)) {
            throw new IllegalArgumentException("Il database deve essere leggibile e modificabile: " + path);
        }

        this.jdbcUrl = "jdbc:sqlite:" + path;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new DAOException("Driver SQLite JDBC non trovato", e);
        }
    }

    @Override
    public void salva(String idMacchina, Sessione sessione) {

        String macchina = validaIdMacchina(idMacchina);

        if (sessione == null) {
            throw new IllegalArgumentException("La sessione da salvare non può essere null");
        }

        String sql = """
                INSERT INTO sessioni (
                    id_macchina,
                    tipo_sessione,
                    luogo,
                    km_percorsi,
                    tempo_passato,
                    descrizione,
                    posizione
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = apriConnessione();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, macchina);
            statement.setString(2,sessione.getTipoSessione().name());
            statement.setString(3, sessione.getLuogo());
            statement.setDouble(4, sessione.getKmPercorsi());
            statement.setInt(5, sessione.getTempo());

            if (sessione instanceof Test test) {
                statement.setString(6,test.getDescrizione());
            } else {
                statement.setNull(6,Types.VARCHAR);
            }

            if (sessione instanceof Gara gara) {
                statement.setInt(7,gara.getPosizioneFinale());
            } else {
                statement.setNull(7,Types.INTEGER);
            }

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DAOException("Errore durante il salvataggio della sessione della macchina " + macchina, e);
        }
    }

    private Connection apriConnessione() throws SQLException {

        Connection connection = DriverManager.getConnection(jdbcUrl);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }

        return connection;
    }

    private String validaIdMacchina(String idMacchina) {

        if (idMacchina == null || idMacchina.isBlank()) {
            throw new IllegalArgumentException(
                    "L'id della macchina non può essere vuoto"
            );
        }

        return idMacchina.trim();
    }
}