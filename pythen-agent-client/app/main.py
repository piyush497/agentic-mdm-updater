import os
from fastapi import FastAPI, Header
from pydantic import BaseModel
import httpx
from . import llm

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
    # Use LLM dispatcher (Stub/Ollama/Azure) to simulate intent parsing
    llm_reply = llm.generate(
        prompt=f"User request: {req.message}. Summarize intended MDM change and next step.",
        system="You are an MDM assistant that prepares change requests and does dry-runs first."
    )
    headers = {}
    if authorization:
        headers["Authorization"] = authorization
    async with httpx.AsyncClient(timeout=15) as client:
        # Call validate endpoint as a stub
        try:
            v = await client.post(f"{JAVA_API_URL}/validate", json={"sample": True}, headers=headers)
            v.raise_for_status()
        except Exception:
            pass
        # Create a stub CR
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
