# NÉBULA

**Una entidad digital experimental: curiosidad, memoria y pensamiento crítico.**

NÉBULA comienza como un laboratorio local-first. Registra exploraciones, relaciona ideas, distingue hechos de hipótesis y propone preguntas nuevas. Su comportamiento inicial es transparente y ampliable.

## Requisitos
- Python 3.10 o superior
- Navegador moderno
- Sin dependencias externas

## Ejecutar
```bash
python main.py
```
Abre http://127.0.0.1:8765

En Android puedes ejecutarlo desde Termux con Python instalado.

## Primera versión
- Memoria persistente en `data/memory.json`
- Etiquetas: hecho, hipótesis, imaginación, pregunta y descubrimiento
- Pregunta de exploración
- Conversación inicial basada en reglas locales
- Interfaz web adaptable a móvil

## Límites honestos
Esta versión no es un modelo de IA autónomo, no tiene conciencia y no navega por Internet. Usa reglas locales y memoria explícita. Está preparada para integrar posteriormente un modelo de lenguaje y herramientas externas con permisos claros.

## Privacidad
Los datos se guardan localmente en `data/memory.json`. No se envían a servidores externos.
