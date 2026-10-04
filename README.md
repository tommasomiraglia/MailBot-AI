# MailBot-AI: Email Auto-Reply Support Bot

## Overview

A Java application built with Apache Camel and Spring Boot that works as a **support bot** for automatic email handling. It monitors a mailbox and processes only unread emails. It filters them against a whitelist of authorized senders and, for valid ones, generates a reply using an AI model (OpenAI). The system can either send a standard reply or escalate complex requests to a human expert while notifying the customer.

Built during my internship at Halnet S.r.l.

## Architecture and Flow

1. **Email intake (IMAP):** The application monitors a mailbox over the IMAP protocol.
2. **Sender filter:** Every incoming email is checked against a `whitelist.txt` of authorized addresses. Emails from unauthorized senders are ignored.
3. **AI processing:** For authorized senders, the email content is sent to the OpenAI API to generate an automatic reply.
4. **AI response handling:**
   - If the AI response contains the token `[HLN_ESCALATION]`, the request is flagged for escalation.
   - Otherwise, a standard reply is prepared for the customer.
5. **Escalation:**
   - An escalation email, containing the original message and the AI response, is sent to a configured "expert" address.
   - At the same time, the original sender is notified that their request has been forwarded.
6. **Standard reply (SMTP):** The AI-generated reply is sent by email (SMTP) to the original sender.

## Tech Stack

- **Apache Camel:** routing and integration of the email flow
- **Spring Boot:** application framework and configuration
- **OpenAI API:** reply generation and escalation decision
- **IMAP/SMTP:** receiving and sending emails
- **Maven:** build and dependency management

## Configuration

The application requires a `.env` file in the project root directory with the following environment variables:

- `IMAP_HOST`: IMAP server host (e.g. `imap.example.com`)
- `SMTP_HOST`: SMTP server host (e.g. `smtp.example.com`)
- `EMAIL_USERNAME`: Address of the monitored mailbox
- `EMAIL_PASSWORD`: Password of the mailbox
- `EXPERT_EMAIL`: Address that receives escalations
- `OPENAI_API_KEY`: API key for the OpenAI service
- `OPENAI_MODEL`: OpenAI model to use (e.g. `gpt-3.5-turbo`)

A `.env.exemple` file is included as a template.

### `whitelist.txt`

This file contains a list of email addresses or domains, one per line, that the bot is allowed to reply to. Senders not on this list are ignored.

---

## Getting Started

**1. Set up the environment variables (`.env`)**

Copy `.env.exemple` to `.env` and fill in your values.

**2. Run the application**

Use the commands below depending on your operating system:

- **Linux/macOS:**

  ```bash
  export $(cat .env | xargs)
  mvn spring-boot:run
  ```

- **Windows (PowerShell):**

  ```powershell
  Get-Content .env | ForEach-Object {
      if ($_ -match "(.*)=(.*)") {
          setx $($matches[1]) $($matches[2])
      }
  }
  mvn spring-boot:run
  ```

  Use `echo $env:EMAIL_USERNAME` to check that the variables were set correctly.

Once started, the application begins monitoring the configured mailbox.
