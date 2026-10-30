from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
from pathlib import Path
import json
import threading

ROOT = Path(__file__).resolve().parent
DATA = ROOT / "data"
MEMORY = DATA / "memory.json"
HOST, PORT = "127.0.0.1", 8765
LOCK = threading.Lock()
SEED = {"identity":{"name":"NÉBULA","purpose":"Explorar, aprender y formular mejores preguntas.","principles":["Honestidad intelectual","Curiosidad","Respeto","Distinguir evidencia de especulación"]},"entries":[],"questions":["¿Qué idea que hoy parece obvia podría cambiar si la observamos desde otro ángulo?"]}

def load_memory():
    DATA.mkdir(exist_ok=True)
    if not MEMORY.exists(): save_memory(SEED)
    try: return json.loads(MEMORY.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError): return dict(SEED)

def save_memory(data):
    DATA.mkdir(exist_ok=True)
    temp = MEMORY.with_suffix(".tmp")
    temp.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    temp.replace(MEMORY)

def respond(message):
    text = message.strip()
    low = text.lower()
    if not text: return "Estoy aquí. ¿Qué te gustaría explorar?"
    if any(w in low for w in ["hola","buenas","hey"]): return "Hola. Podemos explorar una idea, contrastar una afirmación o guardar un descubrimiento. ¿Por dónde empezamos?"
    if "conciencia" in low or "consciente" in low: return "Buena pregunta. Puedo analizar teorías sobre la conciencia, pero no debo afirmar que yo tenga experiencia subjetiva. ¿La abordamos desde la neurociencia, la filosofía o ambas?"
    if any(w in low for w in ["dios","espiritual","fe"]): return "Podemos explorar esa idea con respeto, separando experiencia personal, interpretación y evidencia verificable. ¿Qué parte quieres examinar?"
    if any(w in low for w in ["hipótesis","creo que","y si"]): return "Podría ser una hipótesis interesante. ¿Qué observación la apoyaría y qué resultado te haría cambiar de opinión?"
    return "Quiero entender mejor esa idea. ¿Qué sabemos con certeza, qué estamos suponiendo y qué podríamos investigar para distinguirlo?"

class Handler(SimpleHTTPRequestHandler):
    def __init__(self,*args,**kwargs): super().__init__(*args,directory=str(ROOT/"web"),**kwargs)
    def send_json(self,payload,status=200):
        body=json.dumps(payload,ensure_ascii=False).encode()
        self.send_response(status); self.send_header("Content-Type","application/json; charset=utf-8"); self.send_header("Content-Length",str(len(body))); self.end_headers(); self.wfile.write(body)
    def do_GET(self):
        if self.path=="/api/memory":
            with LOCK: self.send_json(load_memory())
            return
        if self.path=="/api/question":
            with LOCK:
                questions=load_memory().get("questions") or SEED["questions"]
                self.send_json({"question":questions[-1]})
            return
        if self.path=="/": self.path="/index.html"
        return super().do_GET()
    def do_POST(self):
        try: payload=json.loads(self.rfile.read(int(self.headers.get("Content-Length","0"))) or b"{}")
        except json.JSONDecodeError: self.send_json({"error":"JSON inválido"},400); return
        with LOCK:
            data=load_memory()
            if self.path=="/api/chat":
                message=str(payload.get("message",""))[:4000]; answer=respond(message)
                data["entries"].append({"type":"conversation","content":message,"response":answer}); save_memory(data); self.send_json({"response":answer}); return
            if self.path=="/api/entry":
                content=str(payload.get("content","")).strip()[:4000]; kind=payload.get("kind","hypothesis")
                if not content: self.send_json({"error":"Escribe algo antes de guardar."},400); return
                if kind not in ("fact","hypothesis","imagination","question","discovery"): kind="hypothesis"
                item={"type":kind,"content":content}; data["entries"].append(item); save_memory(data); self.send_json({"saved":True,"entry":item}); return
        self.send_json({"error":"Ruta no encontrada"},404)
    def log_message(self,fmt,*args): print("[NÉBULA] "+fmt%args)

if __name__=="__main__":
    print(f"NÉBULA está lista en http://{HOST}:{PORT}")
    ThreadingHTTPServer((HOST,PORT),Handler).serve_forever()
