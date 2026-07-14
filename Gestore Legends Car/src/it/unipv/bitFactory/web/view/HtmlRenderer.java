package it.unipv.bitFactory.web.view;

public final class HtmlRenderer {

    public String renderErrore(String messaggio) {

        String messaggioSicuro = escapeHtml(messaggio);

        return """
            <!DOCTYPE html>
            <html lang="it">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport"
                      content="width=device-width, initial-scale=1.0">
                <title>Errore | BitFactory</title>
                <link rel="stylesheet" href="/styles.css">
            </head>
            <body>
                <header class="page-shell topbar">
                    <a class="brand"
                       href="/"
                       aria-label="Torna agli eventi">

                        <span class="brand-mark">B</span>
                        <span>BitFactory</span>
                    </a>
                </header>

                <main class="page-shell"
                      style="
                          display: grid;
                          place-items: center;
                          min-height: calc(100vh - 81px);
                          padding: 50px 0;
                      ">

                    <section class="booking-card"
                             style="
                                 width: min(560px, 100%%);
                                 text-align: center;
                             ">

                        <span class="eyebrow">
                            Operazione non completata
                        </span>

                        <h1 style="
                            margin-top: 12px;
                            font-size: clamp(2.2rem, 7vw, 4rem);
                        ">
                            Errore
                        </h1>

                        <p style="
                            margin: 24px 0;
                            color: var(--muted);
                            line-height: 1.7;
                        ">
                            %s
                        </p>

                        <a class="primary-button"
                           href="/">
                            Torna agli eventi
                        </a>
                    </section>
                </main>
            </body>
            </html>
            """.formatted(messaggioSicuro);
    }

    public String renderEsitoPrenotazione(
            boolean successo,
            String messaggio,
            String nomeEvento) {

        String titolo = successo
                ? "Prenotazione confermata"
                : "Prenotazione non completata";

        String etichetta = successo
                ? "Operazione completata"
                : "Operazione rifiutata";

        String simbolo = successo
                ? "✓"
                : "!";

        String messaggioSicuro = escapeHtml(messaggio);

        String eventoSicuro = escapeHtml(nomeEvento);

        return """
            <!DOCTYPE html>
            <html lang="it">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport"
                      content="width=device-width, initial-scale=1.0">

                <title>%s | BitFactory</title>
                <link rel="stylesheet" href="/styles.css">
            </head>

            <body>
                <header class="page-shell topbar">
                    <a class="brand"
                       href="/"
                       aria-label="Torna agli eventi">

                        <span class="brand-mark">B</span>
                        <span>BitFactory</span>
                    </a>
                </header>

                <main class="page-shell"
                      style="
                          display: grid;
                          place-items: center;
                          min-height: calc(100vh - 81px);
                          padding: 50px 0;
                      ">

                    <section class="booking-card"
                             style="
                                 width: min(620px, 100%%);
                                 text-align: center;
                             ">

                        <div style="
                            display: grid;
                            width: 68px;
                            height: 68px;
                            margin: 0 auto 22px;
                            place-items: center;
                            border: 1px solid
                                rgba(255, 255, 255, 0.14);
                            border-radius: 50%%;
                            background:
                                rgba(255, 255, 255, 0.06);
                            font-size: 2rem;
                            font-weight: 800;
                        ">
                            %s
                        </div>

                        <span class="eyebrow">
                            %s
                        </span>

                        <h1 style="
                            margin-top: 12px;
                            font-size: clamp(2.2rem, 7vw, 4rem);
                        ">
                            %s
                        </h1>

                        <div class="selected-event"
                             style="text-align: left;">

                            <span>Evento selezionato</span>
                            <strong>%s</strong>
                        </div>

                        <p style="
                            margin: 24px 0;
                            color: var(--muted);
                            line-height: 1.7;
                        ">
                            %s
                        </p>

                        <a class="primary-button"
                           href="/">
                            Torna agli eventi
                        </a>
                    </section>
                </main>
            </body>
            </html>
            """.formatted(
                escapeHtml(titolo),
                simbolo,
                escapeHtml(etichetta),
                escapeHtml(titolo),
                eventoSicuro,
                messaggioSicuro
        );
    }

    private String escapeHtml(String valore) {

        if (valore == null) {return "";}

        return valore
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}