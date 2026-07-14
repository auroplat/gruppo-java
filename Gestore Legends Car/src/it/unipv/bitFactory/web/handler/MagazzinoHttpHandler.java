package it.unipv.bitFactory.web.handler;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.sun.net.httpserver.HttpExchange;

import it.unipv.bitFactory.controller.GestioneMagazzinoController;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class MagazzinoHttpHandler extends BaseHttpHandler {

    private final GestioneMagazzinoController controller;

    public MagazzinoHttpHandler(
            GestioneMagazzinoController controller,
            HtmlRenderer renderer) {

        this.controller = Objects.requireNonNull(controller);
        Objects.requireNonNull(renderer);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String percorso = exchange.getRequestURI().getPath();

        if (!"/magazzino".equals(percorso)
                && !"/magazzino.html".equals(percorso)) {
            sendJson(exchange, 404, erroreJson("Risorsa non trovata"));
            return;
        }

        if (isGet(exchange)) {
            sendResource(
                    exchange,
                    "/web/magazzino.html",
                    "text/html; charset=UTF-8"
            );
            return;
        }

        if (isPost(exchange) && "/magazzino".equals(percorso)) {
            gestisciOperazione(exchange);
            return;
        }

        exchange.getResponseHeaders().set("Allow", "GET, POST");
        sendJson(
                exchange,
                405,
                erroreJson("Metodo HTTP non supportato")
        );
    }

    private void gestisciOperazione(HttpExchange exchange)
            throws IOException {

        try {
            Map<String, String> parametri = leggiParametriForm(exchange);
            String operazione = parametroObbligatorio(
                    parametri,
                    "operazione"
            );

            String messaggio = switch (operazione) {
                case "aggiungiPezzi" -> aggiungiPezzi(parametri);
                case "creaMacchina" -> creaMacchina(parametri);
                case "cambiaPezzo" -> cambiaPezzo(parametri);
                default -> throw new IllegalArgumentException(
                        "Operazione di magazzino non riconosciuta: "
                                + operazione
                );
            };

            sendJson(
                    exchange,
                    200,
                    "{\"messaggio\":\"" + escape(messaggio) + "\"}"
            );

        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, erroreJson(e.getMessage()));

        } catch (IllegalStateException e) {
            sendJson(exchange, 409, erroreJson(e.getMessage()));

        } catch (RuntimeException e) {
            e.printStackTrace();
            sendJson(
                    exchange,
                    500,
                    erroreJson("Errore interno durante l'operazione di magazzino")
            );
        }
    }

    private String aggiungiPezzi(Map<String, String> parametri) {
        TipoPezzo tipo = leggiTipo(parametri, "tipoPezzo");
        int quantita = leggiInteroPositivo(parametri, "quantita");
        double kmMax = leggiDoubleNonNegativo(parametri, "kmMax");
        int tempoMax = leggiInteroNonNegativo(parametri, "tempoMax");

        controller.aggiungiPezzi(
                tipo,
                quantita,
                kmMax,
                tempoMax
        );

        return "Aggiunti " + quantita + " pezzi di tipo " + tipo;
    }

    private String creaMacchina(Map<String, String> parametri) {
        String idMacchina = parametroObbligatorio(
                parametri,
                "idMacchina"
        );

        controller.creaMacchina(idMacchina);
        return "Macchina " + idMacchina + " creata correttamente";
    }

    private String cambiaPezzo(Map<String, String> parametri) {
        String idMacchina = parametroObbligatorio(
                parametri,
                "idMacchina"
        );
        TipoPezzo tipo = leggiTipo(parametri, "tipoPezzo");
        String idPezzo = parametroObbligatorio(
                parametri,
                "idPezzo"
        );

        controller.cambiaPezzo(
                idMacchina,
                tipo,
                idPezzo
        );

        return "Pezzo " + idPezzo
                + " montato sulla macchina " + idMacchina;
    }

    private TipoPezzo leggiTipo(
            Map<String, String> parametri,
            String nome) {

        String valore = parametroObbligatorio(parametri, nome);

        try {
            return TipoPezzo.valueOf(
                    valore.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Tipo pezzo non valido: " + valore
            );
        }
    }

    private int leggiInteroPositivo(
            Map<String, String> parametri,
            String nome) {

        int valore = leggiIntero(parametri, nome);

        if (valore <= 0) {
            throw new IllegalArgumentException(
                    "Il parametro " + nome + " deve essere maggiore di zero"
            );
        }

        return valore;
    }

    private int leggiInteroNonNegativo(
            Map<String, String> parametri,
            String nome) {

        int valore = leggiIntero(parametri, nome);

        if (valore < 0) {
            throw new IllegalArgumentException(
                    "Il parametro " + nome + " non può essere negativo"
            );
        }

        return valore;
    }

    private int leggiIntero(
            Map<String, String> parametri,
            String nome) {

        String valore = parametroObbligatorio(parametri, nome);

        try {
            return Integer.parseInt(valore);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Il parametro " + nome + " deve essere un numero intero"
            );
        }
    }

    private double leggiDoubleNonNegativo(
            Map<String, String> parametri,
            String nome) {

        String valore = parametroObbligatorio(parametri, nome);
        double numero;

        try {
            numero = Double.parseDouble(valore);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Il parametro " + nome + " deve essere numerico"
            );
        }

        if (!Double.isFinite(numero) || numero < 0) {
            throw new IllegalArgumentException(
                    "Il parametro " + nome + " non può essere negativo"
            );
        }

        return numero;
    }

    private String erroreJson(String messaggio) {
        String testo = messaggio == null || messaggio.isBlank()
                ? "Operazione non riuscita"
                : messaggio;

        return "{\"errore\":\"" + escape(testo) + "\"}";
    }

    private String escape(String valore) {
        return valore == null ? "" : valore
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
