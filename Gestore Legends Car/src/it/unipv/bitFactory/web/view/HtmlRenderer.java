package it.unipv.bitFactory.web.view;

public final class HtmlRenderer {

    public String renderHome() {
        return """
            <!DOCTYPE html>
            <html lang="it">
            <head>
                <meta charset="UTF-8">
                <title>BitFactory</title>
            </head>
            <body>
                <h1>BitFactory</h1>

                <nav>
                    <a href="/sessioni">Sessioni</a><br>
                    <a href="/magazzino">Magazzino</a><br>
                    <a href="/prenotazioni">Prenotazioni</a>
                </nav>
            </body>
            </html>
            """;
    }

    public String renderErrore(String messaggio) {
        return """
            <!DOCTYPE html>
            <html lang="it">
            <head>
                <meta charset="UTF-8">
                <title>Errore</title>
            </head>
            <body>
                <h1>Errore</h1>
                <p>%s</p>
                <a href="/">Torna alla home</a>
            </body>
            </html>
            """.formatted(messaggio);
    }
}