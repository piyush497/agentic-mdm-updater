import os
from typing import Optional

USE_AZURE = os.getenv("USE_AZURE_OPENAI", "false").lower() == "true"
USE_OLLAMA = os.getenv("USE_OLLAMA", "false").lower() == "true"

# Lazy imports so that local dev without extras still works
_ollama_llm = None
_azure_llm = None


def _get_ollama_llm():
    global _ollama_llm
    if _ollama_llm is None:
        try:
            from langchain_community.llms import Ollama
        except Exception as e:
            raise RuntimeError("langchain-community is required for Ollama. pip install langchain-community") from e
        model = os.getenv("OLLAMA_MODEL", "llama3.1")
        base_url = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
        _ollama_llm = Ollama(model=model, base_url=base_url)
    return _ollama_llm


def _get_azure_llm():
    global _azure_llm
    if _azure_llm is None:
        try:
            from langchain_openai import AzureChatOpenAI
        except Exception as e:
            raise RuntimeError("langchain-openai is required for Azure OpenAI. pip install langchain-openai") from e
        deployment = os.getenv("AZURE_OPENAI_DEPLOYMENT")
        api_version = os.getenv("AZURE_OPENAI_API_VERSION", "2024-08-01-preview")
        if not deployment:
            raise RuntimeError("AZURE_OPENAI_DEPLOYMENT is required when USE_AZURE_OPENAI=true")
        _azure_llm = AzureChatOpenAI(
            azure_deployment=deployment,
            api_version=api_version,
            temperature=0.2,
        )
    return _azure_llm


def generate(prompt: str, system: Optional[str] = None) -> str:
    """
    Minimal LLM dispatcher.
    Order of precedence:
    1) USE_OLLAMA=true -> Ollama
    2) USE_AZURE_OPENAI=true -> Azure OpenAI
    3) Fallback -> stubbed response
    """
    if USE_OLLAMA:
        llm = _get_ollama_llm()
        return llm.invoke(prompt)

    if USE_AZURE:
        llm = _get_azure_llm()
        msgs = []
        if system:
            msgs.append(("system", system))
        msgs.append(("user", prompt))
        # AzureChatOpenAI expects messages
        return llm.invoke(msgs).content

    # Stub response for local testing without models
    return "[stub] I parsed your intent. Creating a draft change request with a dry run."
