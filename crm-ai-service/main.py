from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel, Field
from typing import Optional
import ollama

app = FastAPI(title="AI Email Generator Microservice")

@app.middleware("http")
async def log_requests(request: Request, call_next):
    body = await request.body()
    print(f"--- Incoming Request ---")
    print(f"Method: {request.method}")
    print(f"URL: {request.url}")
    print(f"Headers: {dict(request.headers)}")
    try:
        print(f"Body: {body.decode()}")
    except:
        print(f"Body: (binary)")
    response = await call_next(request)
    print(f"Response Status: {response.status_code}")
    return response

class AiEmailRequest(BaseModel):
    client_name: str = Field(..., alias="client_name")
    company_name: str = Field(..., alias="company_name")
    end_date: str = Field(..., alias="end_date")
    tone: Optional[str] = "PROFESSIONAL"
    additional_note: Optional[str] = ""

    class Config:
        populate_by_name = True

class AiEmailResponse(BaseModel):
    content: str
    model_used: str

@app.post("/api/ai/generate", response_model=AiEmailResponse)
async def generate_email(request: AiEmailRequest):
    try:
        system_prompt = f"Anda adalah asisten sales B2B. Tugas Anda adalah menulis email perpanjangan kontrak (renewal). Gunakan nada bicara: {request.tone}. Bahasa: Indonesia formal dan profesional. Tulis langsung isi emailnya saja."
        
        user_prompt = f"Nama PIC: {request.client_name}\nPerusahaan: {request.company_name}\nKontrak Berakhir: {request.end_date}\nInstruksi Tambahan: {request.additional_note}"

        response = ollama.chat(
            model='gpt-oss:120b-cloud',
            messages=[
                {'role': 'system', 'content': system_prompt},
                {'role': 'user', 'content': user_prompt}
            ]
        )

        ai_content = response['message']['content']

        return AiEmailResponse(
            content=ai_content,
            model_used="gpt-oss:120b-cloud"
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))