# Agentic MDM Updater - Runbook

This runbook explains how to run the system locally (no Azure required) and how to prepare/run with Azure infrastructure (Azure OpenAI, Azure AD, Service Bus) when you are ready.

Components:
- pythen-agent-client: FastAPI + LangChain agent (Stub/Ollama/Azure LLM)
- java-mdm-api: Spring Boot API (Gradle) with Flyway migrations and minimal Approver UI
- Postgres: via docker-compose

---

## 1) Prerequisites

Install the following on your machine:
- Docker Desktop (for Postgres)
- Git (optional)
- Java 17 (AZUL/Temurin) + Gradle (wrapper will be generated)
- Python 3.10+ (recommended 3.11)
- PowerShell (Windows) or a shell of your choice
- Optional: Ollama (for local LLM)
  - https://ollama.com/download

Ports used by default:
- Postgres: 5432
- Java API: 8080
- Python agent: 8000

---

## 2) Project Structure

- docker-compose.yml: Postgres service
- RUNBOOK.md: This guide
- pythen-agent-client/
  - app/main.py: FastAPI app with /chat endpoint
  - app/llm.py: LLM dispatcher (Stub, Ollama, Azure)
  - requirements.txt
- java-mdm-api/
  - build.gradle, settings.gradle (Gradle build)
  - src/main/resources/db/migration/V1__init.sql (Flyway)
  - src/main/resources/application.yml (defaults)
  - src/main/resources/application-local.yml (local profile)
  - src/main/java/.../MdmApiApplication.java
  - src/main/java/.../controller/CrController.java (CR endpoints - stubbed)
  - src/main/java/.../controller/ValidateController.java (validation/catalog - stubbed)
  - src/main/java/.../controller/ApproverUiController.java (@Profile("local"))
  - src/main/java/.../events/LocalEventLogger.java (@Profile("local"))
  - src/main/resources/templates/cr.html (Approver UI page, local only)

---

## 3) Running Locally (NO Azure)

### 3.1 Start Postgres
From repo root:

```
docker compose up -d postgres
```

This boots a Postgres 16 container with DB/user/pass = mdm/mdm/mdm, port 5432.

### 3.2 Run Java API (local profile)
From `java-mdm-api` directory:

```
# First time only (generates Gradle wrapper)
gradle wrapper

# Run Spring Boot with local profile
./gradlew.bat bootRun --args='--spring.profiles.active=local'
```

What happens:
- Connects to Postgres at `localhost:5432`
- Runs Flyway migrations to create schemas/tables and seed sample data
- Starts server at `http://localhost:8080`
- Local-only:
  - Approver UI: http://localhost:8080/ui/cr/{CR_ID}
  - In-memory event logger prints CR lifecycle events to logs

### 3.3 Run Python agent (Stub mode by default)
From `pythen-agent-client` directory:

```
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt

# Ensure agent knows where Java API is
$env:JAVA_API_URL = "http://localhost:8080"

# Start agent
uvicorn app.main:app --reload --port 8000
```

Health check:
- GET http://localhost:8000/health → {"status":"ok"}

### 3.4 Optional: Use Ollama for local LLM
- Install and start Ollama (see https://ollama.com)
- Pull a model (example):
  - `ollama pull llama3.1`
- In the same PowerShell session before running the agent:

```
$env:USE_OLLAMA = "true"
$env:OLLAMA_MODEL = "llama3.1"
# If Ollama runs on a different host/port, set OLLAMA_BASE_URL, default is http://localhost:11434
```

### 3.5 Test End-to-End (local)
- Create a draft CR via the agent:

```
POST http://localhost:8000/chat
Content-Type: application/json

{ "message": "Update supplier 1 city to New City" }
```

- Expected response:
  - `reply`: text from Stub or Ollama, plus a note about draft CR creation
  - `cr_id`: a UUID returned by the Java API

- Approve (from Java API):
  - POST `http://localhost:8080/cr/{cr_id}/approve` with body `{}`
- Apply (from Java API):
  - POST `http://localhost:8080/cr/{cr_id}/apply` with header `Idempotency-Key: <any-uuid>`
- UI option (local only):
  - Open `http://localhost:8080/ui/cr/{cr_id}` and click Approve/Apply

Notes:
- Controller logic is currently stubbed: the APIs return simulated statuses and events while the DB schema and seed data are real.

---

## 4) Environment Variables (local)

Java API:
- POSTGRES_HOST (default: localhost)
- POSTGRES_PORT (default: 5432)
- POSTGRES_DB (default: mdm)
- POSTGRES_USER (default: mdm)
- POSTGRES_PASSWORD (default: mdm)

Python Agent:
- JAVA_API_URL (default: http://localhost:8080)
- USE_OLLAMA (true|false, default: false)
- OLLAMA_MODEL (default: llama3.1)
- OLLAMA_BASE_URL (default: http://localhost:11434)
- USE_AZURE_OPENAI (true|false, default: false)
- AZURE_OPENAI_DEPLOYMENT (required when USE_AZURE_OPENAI=true)
- AZURE_OPENAI_API_VERSION (default: 2024-08-01-preview)

---

## 5) Preparing for Azure (Overview)

When ready to integrate Azure services, plan for these:

### 5.1 Azure AD (Entra ID) for Identity
- Create an App Registration for the Java API (Resource Server)
  - Expose an API with scopes (e.g., `cr.read`, `cr.write`)
- Create an App Registration for the Chat/Web (client application)
  - Configure redirect URIs
  - Grant consent to the API scopes
- Spring Boot (production profile, e.g., `dev` or `prod`):
  - Enable Spring Security OAuth2 Resource Server with Azure issuer and audience
  - Require bearer JWT tokens on APIs
- Python Agent:
  - Accept bearer tokens from the chat UI and forward them to the Java API

### 5.2 Azure OpenAI
- Provision Azure OpenAI resource and a model deployment (e.g., `gpt-4o`)
- Configure Python agent env:
  - `USE_AZURE_OPENAI=true`
  - `AZURE_OPENAI_DEPLOYMENT=<your-deployment-name>`
  - Also export Azure OpenAI endpoint and API key as required by your environment (see Azure SDK/portal)

### 5.3 Azure Service Bus (CR lifecycle events)
- Create a Service Bus namespace and a Topic (e.g., `cr-events`)
- In Spring Boot (non-local profile), define a bean to publish CR events to the Topic (instead of in-memory)
- Keep using `@Profile("local")` for local event loggers; use `@Profile("!local")` for Service Bus publishers

---

## 6) Running With Azure (Planned Profiles)

We provide a `prod` profile for Azure-backed runs (you can also add `dev` similarly):

- application-dev.yml (to be added):
  - Configure Spring Security OAuth2 Resource Server (Azure AD issuer)
  - Configure Azure Service Bus publisher (connection string/topic)
  - Keep DB connection to a managed Postgres or your local Postgres as needed

Run command (example):

```
./gradlew.bat bootRun --args='--spring.profiles.active=prod'
```

Python agent with Azure OpenAI (example):

```
$env:JAVA_API_URL = "https://<your-api-host>"
$env:USE_AZURE_OPENAI = "true"
$env:AZURE_OPENAI_DEPLOYMENT = "<your-deployment>"
# Also set any required Azure OpenAI endpoint/key environment variables per your setup
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Identity:
- Front-end chat client must sign in via MSAL and attach bearer tokens when calling the agent; the agent forwards the token to the API.

---

## 7) Troubleshooting

- Java API fails to start, Flyway errors:
  - Ensure Postgres is running and reachable at 5432
  - Check DB credentials in application.yml or environment vars

- Python agent cannot reach Java API:
  - Verify `$env:JAVA_API_URL` and that Java is listening on 8080

- Ollama issues:
  - Ensure Ollama service is running and you pulled the model
  - Confirm `OLLAMA_BASE_URL`

- Ports already in use:
  - Stop conflicting services or adjust ports in application.yml and agent run command

- Authorization errors (when Azure enabled):
  - Verify token audience (aud), tenant, and scopes

---

## 8) Roadmap / What’s stubbed

- CR persistence and idempotent apply flow (currently stubbed in controller)
- Event transport abstraction (local vs Azure Service Bus impl)
- OAuth2 integration (Azure AD) & RBAC
- RAG over schema/catalog and richer agent toolset
- Web chat UI with MSAL login

These can be added incrementally without breaking local runs.

---

## 9) Quick Commands Reference (Windows PowerShell)

- Start Postgres:
```
docker compose up -d postgres
```

- Run Java API (local):
```
cd java-mdm-api
gradle wrapper
./gradlew.bat bootRun --args='--spring.profiles.active=local'
```

- Run Python agent (Stub):
```
cd pythen-agent-client
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
$env:JAVA_API_URL = "http://localhost:8080"
uvicorn app.main:app --reload --port 8000
```

- Run Python agent (Ollama):
```
$env:USE_OLLAMA = "true"
$env:OLLAMA_MODEL = "llama3.1"
uvicorn app.main:app --reload --port 8000
```

- Test Agent:
```
Invoke-RestMethod -Method Post -Uri http://localhost:8000/chat -ContentType 'application/json' -Body '{"message":"Update supplier 1 city to New City"}'
```

- Approve via API:
```
Invoke-RestMethod -Method Post -Uri http://localhost:8080/cr/<CR_ID>/approve -ContentType 'application/json' -Body '{}'
```

- Apply via API:
```
$k=[guid]::NewGuid()
Invoke-RestMethod -Method Post -Uri http://localhost:8080/cr/<CR_ID>/apply -Headers @{"Idempotency-Key"=$k}
```

---

This runbook aims to minimize gaps so you can run locally today and transition to Azure smoothly later. If you want, we can add the `application-dev.yml` with sample Azure configuration next.
