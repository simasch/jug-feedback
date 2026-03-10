# Use Cases - Feedback

## Actors

| Actor                    | Description                                              |
|--------------------------|----------------------------------------------------------|
| User (authenticated)     | Logged-in user with ROLE_USER                            |
| Form Owner               | User who created a form                                  |
| Shared User              | User who has been granted access to a form               |
| Anonymous User           | Unauthenticated user with form link/QR code              |

## Use Case Overview

| ID                                 | Use Case              | Actor                        |
|------------------------------------|-----------------------|------------------------------|
| [UC-01](UC-01_login.md)            | Login                 | Anonymous User               |
| [UC-02](UC-02_create_form.md)      | Create Feedback Form  | User                         |
| [UC-03](UC-03_edit_form.md)        | Edit Form             | Form Owner                   |
| [UC-04](UC-04_publish_form.md)     | Publish Form          | Form Owner                   |
| [UC-05](UC-05_submit_feedback.md)  | Submit Feedback       | Anonymous User               |
| [UC-06](UC-06_view_results.md)     | View Results          | Form Owner, Shared User      |
| [UC-07](UC-07_share_form.md)       | Share Form            | Form Owner                   |
| [UC-08](UC-08_generate_qr_code.md) | Generate QR Code      | Form Owner, Shared User      |
| [UC-09](UC-09_close_form.md)       | Close Form            | Form Owner                   |
| [UC-10](UC-10_reopen_form.md)      | Reopen Form           | Form Owner                   |
| [UC-11](UC-11_delete_form.md)      | Delete Form           | Form Owner                   |

## Status Transitions

```
DRAFT --[Publish]--> PUBLIC --[Close]--> CLOSED
                         ^                  |
                         +---[Reopen]-------+
```
