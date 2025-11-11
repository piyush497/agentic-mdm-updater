import os
from fastapi import FastAPI, Header
from pydantic import BaseModel
import httpx
from . import llm
from .agent import run_agent

JAVA_API_URL = os.getenv("JAVA_API_URL", "http://localhost:8080")

app = FastAPI(title="Agentic MDM Python Agent")

class ChatRequest(BaseModel):
    message: str

class ChatResponse(BaseModel):
    reply: str
    cr_id: str | None = None

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest, authorization: str | None = Header(default=None)):
    headers = {"Authorization": authorization} if authorization else {}
    try:
        # Use LangChain agent with tools; the agent will call Java API endpoints
        reply = run_agent(req.message, headers=headers)
        return ChatResponse(reply=reply)
    except Exception:
        # Fallback to simple stub behavior if agent/LLM not configured
        llm_reply = llm.generate(
            prompt=f"User request: {req.message}. Summarize intended MDM change and next step.",
            system="You are an MDM assistant that prepares change requests and does dry-runs first."
        )
        # Create a stub CR to keep UX similar
        async with httpx.AsyncClient(timeout=15) as client:
            cr = await client.post(f"{JAVA_API_URL}/cr", params={"dryRun": True}, json={
                "domain": "supplier",
                "table": "supplier_address",
                "operation": "UPDATE",
                "filter": {"supplier_id": 1},
                "proposed_changes": {"city": "New City"}
            }, headers=headers)
            cr_id = None
            if cr.status_code == 200:
                try:
                    cr_id = cr.json().get("id")
                except Exception:
                    cr_id = None
        final_reply = f"{llm_reply}\nDraft change request created (stub)."
        return ChatResponse(reply=final_reply, cr_id=cr_id)
