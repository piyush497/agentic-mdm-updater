Project: Agentic MDM Updater

Structure:
- pythen-agent-client: Python agent using LangChain + Azure OpenAI
- java-mdm-api: Spring Boot API for CR lifecycle and validation
- docker-compose.yml: Postgres for local dev

Quick start:
1) Start Postgres: docker compose up -d postgres
2) Run Java API (Gradle): in java-mdm-api
   - First time: generate wrapper
     - Windows PowerShell: `gradle wrapper`
   - Start app:
     - Windows PowerShell: `./gradlew.bat bootRun`
3) Run Python agent: in pythen-agent-client, install requirements and start server

Config:
- Azure OpenAI: set AZURE_OPENAI_ENDPOINT, AZURE_OPENAI_API_KEY, AZURE_OPENAI_DEPLOYMENT
- Azure AD: set AZURE_AD_TENANT_ID, CLIENT_ID as needed later
- Database: POSTGRES_* envs used by Spring Boot

Notes:
- The Java module now uses Gradle (Spring Boot 3). The existing pom.xml can be ignored or removed.
