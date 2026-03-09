# Use Cases - JUG Feedback

## Akteure

| Akteur                     | Beschreibung                                               |
|----------------------------|------------------------------------------------------------|
| Benutzer (authentifiziert) | Eingeloggter Benutzer mit ROLE_USER                        |
| Formular-Besitzer          | Benutzer, der ein Formular erstellt hat                    |
| Geteilter Benutzer         | Benutzer, dem ein Formular freigegeben wurde               |
| Anonymer Benutzer          | Nicht authentifizierter Benutzer mit Formular-Link/QR-Code |

## Use-Case-Uebersicht

| ID                                 | Use Case                    | Akteur                                |
|------------------------------------|-----------------------------|---------------------------------------|
| [UC-01](UC-01_login.md)            | Anmelden                    | Anonymer Benutzer                     |
| [UC-02](UC-02_create_form.md)      | Feedback-Formular erstellen | Benutzer                              |
| [UC-03](UC-03_edit_form.md)        | Formular bearbeiten         | Formular-Besitzer                     |
| [UC-04](UC-04_publish_form.md)     | Formular veroeffentlichen   | Formular-Besitzer                     |
| [UC-05](UC-05_submit_feedback.md)  | Feedback abgeben            | Anonymer Benutzer                     |
| [UC-06](UC-06_view_results.md)     | Ergebnisse anzeigen         | Formular-Besitzer, Geteilter Benutzer |
| [UC-07](UC-07_share_form.md)       | Formular teilen             | Formular-Besitzer                     |
| [UC-08](UC-08_generate_qr_code.md) | QR-Code generieren          | Formular-Besitzer, Geteilter Benutzer |
| [UC-09](UC-09_close_form.md)       | Formular schliessen         | Formular-Besitzer                     |
| [UC-10](UC-10_reopen_form.md)      | Formular wieder oeffnen     | Formular-Besitzer                     |
| [UC-11](UC-11_delete_form.md)      | Formular loeschen           | Formular-Besitzer                     |

## Statusuebergaenge

```
DRAFT --[Veroeffentlichen]--> PUBLIC --[Schliessen]--> CLOSED
                                  ^                       |
                                  +---[Wieder oeffnen]----+
```
