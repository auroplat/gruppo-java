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
            creaTabellaSeNecessaria();

        } catch (ClassNotFoundException e) {
            throw new DAOException(
                    "Driver SQLite JDBC non trovato",
                    e
            );
        }
    }

    @Override
    public void salva(String idMacchina, Sessione sessione) {

        String macchina = validaIdMacchina(idMacchina);

        if (sessione == null) {
            throw new IllegalArgumentException(
                    "La sessione da salvare non può essere null"
            );
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
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, macchina);
            statement.setString(
                    2,
                    sessione.getTipoSessione().name()
            );
            statement.setString(3, sessione.getLuogo());
            statement.setDouble(4, sessione.getKmPercorsi());
            statement.setInt(5, sessione.getTempo());

            /*
             * La descrizione esiste solamente nelle sessioni Test.
             */
            if (sessione instanceof Test test) {
                statement.setString(
                        6,
                        test.getDescrizione()
                );
            } else {
                statement.setNull(
                        6,
                        Types.VARCHAR
                );
            }

            /*
             * La posizione finale esiste solamente nelle sessioni Gara.
             */
            if (sessione instanceof Gara gara) {
                statement.setInt(
                        7,
                        gara.getPosizioneFinale()
                );
            } else {
                statement.setNull(
                        7,
                        Types.INTEGER
                );
            }

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante il salvataggio della sessione "
                            + "della macchina " + macchina,
                    e
            );
        }
    }

    private void creaTabellaSeNecessaria() {

        String creaTabellaSql = """
                CREATE TABLE IF NOT EXISTS sessioni (
                    id_sessione INTEGER PRIMARY KEY AUTOINCREMENT,

                    id_macchina TEXT NOT NULL,

                    tipo_sessione TEXT NOT NULL
                        CHECK (
                            tipo_sessione IN ('TEST', 'GARA')
                        ),

                    luogo TEXT NOT NULL,

                    km_percorsi REAL NOT NULL
                        CHECK (km_percorsi >= 0),

                    tempo_passato INTEGER NOT NULL
                        CHECK (tempo_passato >= 0),

                    descrizione TEXT,

                    posizione INTEGER
                        CHECK (
                            posizione IS NULL
                            OR posizione > 0
                        ),

                    data_registrazione TEXT NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,

                    FOREIGN KEY (id_macchina)
                        REFERENCES macchine(id_macchina)
                        ON UPDATE CASCADE
                        ON DELETE CASCADE
                )
                """;

        String creaIndiceSql = """
                CREATE INDEX IF NOT EXISTS idx_sessioni_macchina
                ON sessioni(id_macchina)
                """;

        try (Connection connection = apriConnessione();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(creaTabellaSql);
            statement.executeUpdate(creaIndiceSql);

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante la creazione della tabella sessioni",
                    e
            );
        }
    }

    private Connection apriConnessione() throws SQLException {

        Connection connection =
                DriverManager.getConnection(jdbcUrl);

        /*
         * In SQLite le foreign key devono essere abilitate
         * per ogni nuova connessione.
         */
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