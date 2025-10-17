# Documentazione Applicazione Email-Reader

### Panoramica
Questa applicazione Java, basata su Apache Camel e Spring Boot, funge da **SupportBot** per la gestione automatica delle email. Intercetta le email in arrivo analizzando solo quelle non lette, le filtra in base a una whitelist di mittenti autorizzati e, per quelle valide, genera una risposta utilizzando un modello di intelligenza artificiale (OpenAI). Il sistema è in grado di inviare risposte standard o di scalare le richieste complesse a un esperto, notificando il cliente.

### Architettura e Flusso

1.  **Ingresso Email (IMAP):** L'applicazione monitora una casella di posta tramite il protocollo IMAP.
2.  **Filtro Mittenti:** Ogni email in arrivo viene controllata rispetto a una `whitelist.txt` di indirizzi email autorizzati. Le email da mittenti non autorizzati vengono ignorate.
3.  **Elaborazione AI:** Per i mittenti autorizzati, il contenuto dell'email viene inviato a un servizio OpenAI per generare una risposta automatica.
4.  **Gestione Risposta AI:**
    *   Se la risposta AI contiene il token `[HLN_ESCALATION]`, la richiesta viene marcata per l'escalation.
    *   Altrimenti, viene preparata una risposta standard per il cliente.
5.  **Escalation:**
    *   Un'email di escalation, contenente i dettagli originali e la risposta AI, viene inviata a un indirizzo email di "esperto" configurato.
    *   Contemporaneamente, viene inviata una notifica al cliente originale informandolo che la sua richiesta è stata inoltrata.
6.  **Risposta Standard (SMTP):** La risposta generata dall'AI viene inviata via email (SMTP) al mittente originale.

### Strumenti e Framework Utilizzati
*   **Apache Camel:**
*   **Spring Boot:** 
*   **OpenAI API:** 
*   **IMAP/SMTP:** 
*   **Maven:** 

### Configurazione

L'applicazione richiede un file `.env` nella directory radice del progetto per caricare le seguenti variabili d'ambiente:

*   `IMAP_HOST`: Host del server IMAP (es. `imap.example.com`)
*   `SMTP_HOST`: Host del server SMTP (es. `smtp.example.com`)
*   `EMAIL_USERNAME`: Indirizzo email della casella monitorata.
*   `EMAIL_PASSWORD`: Password della casella email.
*   `EXPERT_EMAIL`: Indirizzo email a cui inviare le escalation.
*   `OPENAI_API_KEY`: Chiave API per il servizio OpenAI.
*   `OPENAI_MODEL`: Nome del modello OpenAI da utilizzare (es. `gpt-3.5-turbo`).

Ho lasciato .env.exemple come template 

### File `whitelist.txt`

Questo file deve contenere un elenco di indirizzi email o domini, uno per riga, che sono autorizzati a ricevere risposte dal bot. I mittenti non presenti in questa lista verranno ignorati. 

---

### Istruzioni per l'Avvio

**1. Configurazione delle Variabili d'Ambiente (.env)**

**2. Avvio dell'Applicazione**

utilizza i seguenti comandi a seconda del tuo sistema operativo:

*   **Linux/macOS:**

    ```bash
    export $(cat .env | xargs)
    mvn spring-boot:run
    ```

*   **Windows (PowerShell):**

    ```powershell
    Get-Content .env | ForEach-Object {
        if ($_ -match "(.*)=(.*)") {
            setx $($matches[1]) $($matches[2])
        }
    }
    mvn spring-boot:run
    ```
Usa echo `$env:EMAIL_USERNAME` per verificare che le variabili siano state impostate correttamente 
Una volta avviata, l'applicazione inizierà a monitorare la casella di posta configurata
