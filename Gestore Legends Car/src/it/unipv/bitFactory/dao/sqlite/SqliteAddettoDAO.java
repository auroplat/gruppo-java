
package it.unipv.bitFactory.dao.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import it.unipv.bitFactory.dao.interfacce.AddettoDAO;
import it.unipv.bitFactory.dao.interfacce.DAOException;
import it.unipv.bitFactory.model.persona.*;

public class SqliteAddettoDAO implements AddettoDAO {

    private final String urlDatabase;

    public SqliteAddettoDAO(String percorsoDatabase) {

        if (percorsoDatabase == null || percorsoDatabase.isBlank()) {
            throw new IllegalArgumentException("Il percorso del database non può essere vuoto");
        }

        this.urlDatabase = "jdbc:sqlite:" + percorsoDatabase;
    }

    @Override
    public Addetto trovaPerUsername(String username) {

        if (username == null || username.isBlank()) {return null;}

        String sql = """
                SELECT nome,
                       cognome,
                       telefono,
                       email,
                       username,
                       password,
                       ruolo
                FROM addetti
                WHERE username = ?
                """;

        try (Connection connection = DriverManager.getConnection(urlDatabase);

             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {return null;}

                return creaAddetto(resultSet);
            }

        } catch (SQLException e) {
            throw new DAOException("Errore durante la ricerca dell'addetto: " + username, e);
        }
    }

    private Addetto creaAddetto(ResultSet resultSet)
            throws SQLException {

        String nome = resultSet.getString("nome");
        String cognome = resultSet.getString("cognome");
        String telefono = resultSet.getString("telefono");
        String email = resultSet.getString("email");
        String username = resultSet.getString("username");
        String password = resultSet.getString("password");
        String valoreRuolo = resultSet.getString("ruolo");

        if (valoreRuolo == null || valoreRuolo.isBlank()) {
            throw new DAOException("Il ruolo dell'addetto " + username + " non è valido");
        }

        final Ruolo ruolo;

        try {
            ruolo = Ruolo.valueOf(valoreRuolo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DAOException("Ruolo sconosciuto nel database: " + valoreRuolo, e);
        }

        return switch (ruolo) {

            case MAGAZZINO -> new GestoreMagazzino(
                    nome,
                    cognome,
                    telefono,
                    email,
                    username,
                    password
            );

            case EVENTI -> new GestoreEventi(
                    nome,
                    cognome,
                    telefono,
                    email,
                    username,
                    password
            );

            case SESSIONI -> new GestoreSessioni(
                    nome,
                    cognome,
                    telefono,
                    email,
                    username,
                    password
            );
        };
    }
}
