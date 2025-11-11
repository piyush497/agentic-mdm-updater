import os
from typing import Any, Dict, Optional
import httpx
from pydantic import BaseModel, Field

from langchain.agents import AgentExecutor, initialize_agent
from langchain.agents.agent_types import AgentType
from langchain.tools import BaseTool
from langchain_core.language_models import BaseChatModel

# LLMs
from langchain_openai import AzureChatOpenAI
from langchain_community.chat_models import ChatOllama

JAVA_API_URL = os.getenv("JAVA_API_URL", "http://localhost:8080")

# ---------- LLM selection ----------

def get_chat_model() -> BaseChatModel:
    use_ollama = os.getenv("USE_OLLAMA", "false").lower() == "true"
    use_azure = os.getenv("USE_AZURE_OPENAI", "false").lower() == "true"

    if use_ollama:
        model = os.getenv("OLLAMA_MODEL", "llama3.1")
        base_url = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
        return ChatOllama(model=model, base_url=base_url, temperature=0.2)

    if use_azure:
        deployment = os.getenv("AZURE_OPENAI_DEPLOYMENT")
        api_version = os.getenv("AZURE_OPENAI_API_VERSION", "2024-08-01-preview")
        if not deployment:
            raise RuntimeError("AZURE_OPENAI_DEPLOYMENT is required when USE_AZURE_OPENAI=true")
        return AzureChatOpenAI(azure_deployment=deployment, api_version=api_version, temperature=0.2)

    # Default to a very simple stub-like behavior via ChatOllama mock: raise to indicate stub path
    # The /chat route can catch and fallback to stub
    raise RuntimeError("No LLM selected. Set USE_OLLAMA=true or USE_AZURE_OPENAI=true, or use stub path in main.")

# ---------- Tools ----------

class ValidateArgs(BaseModel):
    payload: Dict[str, Any] = Field(..., description="Validation input JSON to send to /validate endpoint")

class CreateCrArgs(BaseModel):
    domain: str
    table: str
    operation: str = Field(..., description="One of UPDATE|INSERT|DELETE|UPSERT")
    filter: Dict[str, Any] = Field(default_factory=dict)
    proposed_changes: Dict[str, Any] = Field(default_factory=dict)
    dryRun: bool = True

class StatusArgs(BaseModel):
    id: str = Field(..., description="Change Request ID")

class ValidateTool(BaseTool):
    name = "validate"
    description = "Validate a proposed change request against schema and constraints using the Java API /validate endpoint."
    args_schema = ValidateArgs

    def __init__(self, headers: Optional[Dict[str, str]] = None):
        super().__init__()
        self.headers = headers or {}

    def _run(self, payload: Dict[str, Any], run_manager=None) -> str:
        try:
            with httpx.Client(timeout=20) as client:
                r = client.post(f"{JAVA_API_URL}/validate", json=payload, headers=self.headers)
                r.raise_for_status()
                return r.text
        except Exception as e:
            return f"validate_error: {e}"

    async def _arun(self, payload: Dict[str, Any], run_manager=None) -> str:
        try:
            async with httpx.AsyncClient(timeout=20) as client:
                r = await client.post(f"{JAVA_API_URL}/validate", json=payload, headers=self.headers)
                r.raise_for_status()
                return r.text
        except Exception as e:
            return f"validate_error: {e}"

class CreateCrTool(BaseTool):
    name = "create_cr"
    description = "Create a draft Change Request with dryRun via Java API /cr?dryRun=true. Returns CR id and diff_preview."
    args_schema = CreateCrArgs

    def __init__(self, headers: Optional[Dict[str, str]] = None):
        super().__init__()
        self.headers = headers or {}

    def _run(self, domain: str, table: str, operation: str, filter: Dict[str, Any], proposed_changes: Dict[str, Any], dryRun: bool = True, run_manager=None) -> str:
        try:
            params = {"dryRun": str(dryRun).lower()}
            body = {
                "domain": domain,
                "table": table,
                "operation": operation,
                "filter": filter,
                "proposed_changes": proposed_changes,
            }
            with httpx.Client(timeout=20) as client:
                r = client.post(f"{JAVA_API_URL}/cr", params=params, json=body, headers=self.headers)
                r.raise_for_status()
                return r.text
        except Exception as e:
            return f"create_cr_error: {e}"

    async def _arun(self, domain: str, table: str, operation: str, filter: Dict[str, Any], proposed_changes: Dict[str, Any], dryRun: bool = True, run_manager=None) -> str:
        try:
            params = {"dryRun": str(dryRun).lower()}
            body = {
                "domain": domain,
                "table": table,
                "operation": operation,
                "filter": filter,
                "proposed_changes": proposed_changes,
            }
            async with httpx.AsyncClient(timeout=20) as client:
                r = await client.post(f"{JAVA_API_URL}/cr", params=params, json=body, headers=self.headers)
                r.raise_for_status()
                return r.text
        except Exception as e:
            return f"create_cr_error: {e}"

class StatusTool(BaseTool):
    name = "status"
    description = "Get the status of a Change Request by id using the Java API /cr/{id}."
    args_schema = StatusArgs

    def __init__(self, headers: Optional[Dict[str, str]] = None):
        super().__init__()
        self.headers = headers or {}

    def _run(self, id: str, run_manager=None) -> str:
        try:
            with httpx.Client(timeout=15) as client:
                r = client.get(f"{JAVA_API_URL}/cr/{id}", headers=self.headers)
                r.raise_for_status()
                return r.text
        except Exception as e:
            return f"status_error: {e}"

    async def _arun(self, id: str, run_manager=None) -> str:
        try:
            async with httpx.AsyncClient(timeout=15) as client:
                r = await client.get(f"{JAVA_API_URL}/cr/{id}", headers=self.headers)
                r.raise_for_status()
                return r.text
        except Exception as e:
            return f"status_error: {e}"

# ---------- Agent runner ----------

SYSTEM_PROMPT = (
    "You are an MDM assistant. Parse user intent to domain/table/operation. "
    "Use tools to validate and create a draft Change Request with dryRun before applying. "
    "Always return a concise summary and include the CR id if created."
)

def run_agent(message: str, headers: Optional[Dict[str, str]] = None) -> str:
    headers = headers or {}

    try:
        llm = get_chat_model()
    except Exception as e:
        # Fallback: if no LLM configured, return a stub reply
        return "[stub] No LLM configured. Set USE_OLLAMA or USE_AZURE_OPENAI."

    tools = [
        ValidateTool(headers=headers),
        CreateCrTool(headers=headers),
        StatusTool(headers=headers),
    ]

    agent = initialize_agent(
        tools=tools,
        llm=llm,
        agent=AgentType.OPENAI_FUNCTIONS,
        verbose=False,
        handle_parsing_errors=True,
        agent_kwargs={"system_message": SYSTEM_PROMPT},
    )

    result = agent.run(message)
    return result
