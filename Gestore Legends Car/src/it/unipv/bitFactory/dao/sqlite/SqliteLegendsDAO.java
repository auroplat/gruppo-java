package it.unipv.bitFactory.dao.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import it.unipv.bitFactory.dao.DAOException;
import it.unipv.bitFactory.dao.LegendsDAO;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.model.veicoli.Legends;

/**
 * Implementazione SQLite dell'interfaccia stabile LegendsDAO.
 * Controller e modello non conoscono JDBC né la struttura del database.
 */
public final class SqliteLegendsDAO implements LegendsDAO {

    private final String jdbcUrl;

    public SqliteLegendsDAO(String databasePath) {
        if (databasePath == null || databasePath.isBlank()) {
            throw new IllegalArgumentException(
                    "Il percorso del database non può essere vuoto"
            );
        }

        this.jdbcUrl = "jdbc:sqlite:" + databasePath;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new DAOException(
                    "Driver SQLite JDBC non trovato nel classpath/module-path",
                    e
            );
        }
    }

    @Override
    public void salva(Legends legends) {
        if (legends == null) {
            throw new IllegalArgumentException(
                    "La macchina da salvare non può essere null"
            );
        }

        try (Connection connection = apriConnessione()) {
            connection.setAutoCommit(false);

            try {
                salvaMacchina(connection, legends);
                sostituisciPezzi(connection, legends);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                rollbackSilenzioso(connection);
                throw e;
            }

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante il salvataggio della macchina "
                            + legends.getId(),
                    e
            );
        }
    }

    @Override
    public Optional<Legends> trovaPerId(String id) {
        int numeroMacchina = convertiId(id);

        try (Connection connection = apriConnessione()) {
            return Optional.ofNullable(
                    caricaMacchina(connection, numeroMacchina)
            );
        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante la lettura della macchina " + id,
                    e
            );
        }
    }

    @Override
    public List<Legends> trovaTutte() {
        String sql = "SELECT Id FROM Macchine ORDER BY Id";
        List<Legends> macchine = new ArrayList<>();

        try (Connection connection = apriConnessione();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                Legends macchina = caricaMacchina(
                        connection,
                        result.getInt("Id")
                );

                if (macchina != null) {
                    macchine.add(macchina);
                }
            }

            return macchine;

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante la lettura delle macchine",
                    e
            );
        }
    }

    @Override
    public void elimina(String id) {
        int numeroMacchina = convertiId(id);

        try (Connection connection = apriConnessione()) {
            connection.setAutoCommit(false);

            try (PreparedStatement eliminaPezzi = connection.prepareStatement(
                    "DELETE FROM Pezzi WHERE Id_pezzo = ?"
            ); PreparedStatement eliminaMacchina = connection.prepareStatement(
                    "DELETE FROM Macchine WHERE Id = ?"
            )) {

                eliminaPezzi.setInt(1, numeroMacchina);
                eliminaPezzi.executeUpdate();

                eliminaMacchina.setInt(1, numeroMacchina);
                eliminaMacchina.executeUpdate();

                connection.commit();

            } catch (SQLException e) {
                rollbackSilenzioso(connection);
                throw e;
            }

        } catch (SQLException e) {
            throw new DAOException(
                    "Errore durante l'eliminazione della macchina " + id,
                    e
            );
        }
    }

    private Connection apriConnessione() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);

        try (PreparedStatement statement = connection.prepareStatement(
                "PRAGMA foreign_keys = ON"
        )) {
            statement.execute();
        }

        return connection;
    }

    private Legends caricaMacchina(
            Connection connection,
            int numeroMacchina) throws SQLException {

        String sqlMacchina = "SELECT Id, Km FROM Macchine WHERE Id = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sqlMacchina)) {

            statement.setInt(1, numeroMacchina);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }

                Legends legends = new Legends(
                        String.valueOf(result.getInt("Id"))
                );

                legends.percorriKm(result.getDouble("Km"));
                caricaPezzi(connection, legends, numeroMacchina);
                return legends;
            }
        }
    }

    private void caricaPezzi(
            Connection connection,
            Legends legends,
            int numeroMacchina) throws SQLException {

        String sql = """
                SELECT tipo, Km, Tempo, KmMax, TempoMax
                FROM Pezzi
                WHERE Id_pezzo = ?
                ORDER BY tipo
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, numeroMacchina);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    TipoPezzo tipo = TipoPezzo.valueOf(
                            result.getString("tipo")
                    );

                    Pezzo pezzo = new Pezzo(
                            tipo,
                            result.getDouble("KmMax"),
                            result.getInt("TempoMax")
                    );

                    pezzo.aggiornaUtilizzo(
                            result.getDouble("Km"),
                            result.getInt("Tempo")
                    );

                    legends.modificaPezzo(pezzo);
                }
            }
        }
    }

    private void salvaMacchina(
            Connection connection,
            Legends legends) throws SQLException {

        String sql = """
                INSERT INTO Macchine (Id, Km)
                VALUES (?, ?)
                ON CONFLICT(Id) DO UPDATE SET Km = excluded.Km
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, convertiId(legends.getId()));
            statement.setDouble(2, legends.getKmTotali());
            statement.executeUpdate();
        }
    }

    private void sostituisciPezzi(
            Connection connection,
            Legends legends) throws SQLException {

        int numeroMacchina = convertiId(legends.getId());

        try (PreparedStatement elimina = connection.prepareStatement(
                "DELETE FROM Pezzi WHERE Id_pezzo = ?"
        )) {
            elimina.setInt(1, numeroMacchina);
            elimina.executeUpdate();
        }

        String sqlInserimento = """
                INSERT INTO Pezzi
                    (Id_pezzo, Km, Tempo, KmMax, TempoMax, tipo)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement inserisci =
                     connection.prepareStatement(sqlInserimento)) {

            for (Pezzo pezzo : legends.getTuttiPezzi()) {
                inserisci.setInt(1, numeroMacchina);
                inserisci.setDouble(2, pezzo.getKmAttuali());
                inserisci.setInt(3, pezzo.getTempoAttuale());
                inserisci.setDouble(4, pezzo.getKmMax());
                inserisci.setInt(5, pezzo.getTempoMax());
                inserisci.setString(6, pezzo.getTipo().name());
                inserisci.addBatch();
            }

            inserisci.executeBatch();
        }
    }

    private int convertiId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Il numero della macchina non può essere vuoto"
            );
        }

        try {
            return Integer.parseInt(id.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Il numero della macchina deve essere numerico: " + id,
                    e
            );
        }
    }

    private void rollbackSilenzioso(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Mantiene l'eccezione originale.
        }
    }
}
