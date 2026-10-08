# Corretor de Redação ENEM — Agente de IA

Protótipo do projeto de **Extensão IV (UECE, 2026.2)** — Francisco Leonardo Marcos Leitão e Vinicius Vieira Carneiro.

O estudante informa o tema e o texto da redação; o agente devolve uma **estimativa de nota pelas cinco competências do ENEM**, a justificativa de cada nota, os pontos fortes e os **trechos exatos** do texto que precisam de melhoria, com sugestão de reescrita. Os trechos apontados aparecem destacados no próprio texto; clicar num ponto de melhoria leva até ele.

| Parte      | Tecnologia                                              |
|------------|---------------------------------------------------------|
| `backend/`  | Java 25 · Spring Boot 4.1.1 · Gradle 9.8.0 (Kotlin DSL) |
| `frontend/` | Angular 22 (componentes standalone, signals, zoneless)  |
| IA         | OpenAI Chat Completions com JSON Schema estrito          |

## Requisitos

- **JDK 25** — se não estiver instalado, o Gradle baixa automaticamente (plugin foojay). Para rodar o próprio Gradle basta qualquer JDK 17+.
- **Node.js 22.22.3+ ou 24.15+** (exigência do Angular CLI 22)
- Uma chave da API da OpenAI (https://platform.openai.com/api-keys) com créditos

## Como executar (desenvolvimento)

Abra dois terminais.

**1. Backend** (porta 8080)

Linux / macOS:
```bash
cd backend
export OPENAI_API_KEY="sk-..."
./gradlew bootRun
```

Windows (PowerShell):
```powershell
cd backend
$env:OPENAI_API_KEY="sk-..."
.\gradlew.bat bootRun
```

**2. Frontend** (porta 4200)

```bash
cd frontend
npm install
npm start
```

Abra http://localhost:4200. O `ng serve` repassa as chamadas `/api` para o backend (`frontend/proxy.conf.json`). O link **"usar redação de exemplo"** preenche um tema e um texto para demonstração.

### Configuração do backend

| Variável          | Padrão                       | Descrição                                   |
|-------------------|------------------------------|---------------------------------------------|
| `OPENAI_API_KEY`  | —                            | Chave da OpenAI (obrigatória)               |
| `OPENAI_MODEL`    | `gpt-5-mini`                 | Modelo usado na correção                    |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1`  | URL base da API                             |
| `CORS_ORIGENS`    | `http://localhost:4200`      | Origens do front autorizadas (vírgula)      |

Limites de tamanho do texto e timeout ficam em `backend/src/main/resources/application.properties`.

## Testes

```bash
cd backend && ./gradlew test        # JUnit 5 — usa um avaliador falso, não gasta créditos
cd frontend && npm test -- --watch=false   # Vitest
```

## Build de produção

```bash
cd backend && ./gradlew bootJar     # gera build/libs/avaliador-redacao-enem-0.1.0.jar
cd frontend && npm run build        # gera dist/frontend/browser
```

O front gerado é estático e pode ser publicado em qualquer hospedagem; basta incluir o endereço dele em `CORS_ORIGENS` e apontar `/api` para o backend.

## Arquitetura

```
Angular (localhost:4200)
  FormularioRedacao ─(enviar)─► App ─► AvaliacaoService (HttpClient)
  ResultadoAvaliacao ◄──────────┘            │  POST /api/avaliacoes
    ├─ CartaoCompetencia (×5)                 ▼
    └─ TextoMarcado                 Spring Boot (localhost:8080)
                                      AvaliacaoController
                                        └─ AvaliacaoService ─► AvaliadorIA (interface)
                                             valida tamanho        └─ OpenAiAvaliador
                                             normaliza notas            Chat Completions +
                                             soma nota final            JSON Schema estrito
```

### Backend (`backend/src/main`)

- **`resources/ia/prompt-sistema.txt`** — instruções do agente: rubrica das cinco competências, níveis 0–200, os cinco elementos da proposta de intervenção (agente, ação, modo/meio, efeito, detalhamento), casos de nota zero e formato do retorno pedagógico. É o principal ponto para calibrar a correção após a validação com o público-alvo.
- **`resources/ia/schema-avaliacao.json`** — schema da resposta; com `strict: true`, a OpenAI só pode devolver notas 0, 40, 80, 120, 160 ou 200.
- **`AvaliacaoService`** — não confia cegamente no modelo: exige as cinco competências, ajusta notas fora da escala, zera tudo se a redação for anulada, soma a nota final e confere se cada trecho citado existe no texto.
- **`AvaliadorIA`** — interface; trocar de provedor exige só uma nova implementação.

### Frontend (`frontend/src/app`)

- `components/formulario-redacao` — formulário reativo, contadores de palavras e de linhas estimadas (alerta abaixo de 8 ou acima de 30 linhas).
- `components/resultado-avaliacao` — nota total, barras por competência, comentário geral e próximos passos.
- `components/cartao-competencia` — justificativa, pontos fortes e pontos de melhoria de uma competência.
- `components/texto-marcado` — a redação com os trechos destacados e numerados.
- `services/avaliacao.service.ts` — chamadas HTTP e tradução das mensagens de erro.
- `utils/redacao.ts` — contagens, numeração dos pontos e segmentação do texto (testadas em `redacao.spec.ts`).

## API

`POST /api/avaliacoes`

```json
{ "tema": "Desafios para o combate à desinformação no Brasil", "texto": "..." }
```

Resposta (resumida):

```json
{
  "notaTotal": 680,
  "anulada": false,
  "competencias": [
    {
      "numero": 5, "nome": "Proposta de intervenção", "nota": 80,
      "justificativa": "...",
      "pontosFortes": ["..."],
      "pontosDeMelhoria": [
        { "trecho": "O governo deve criar campanhas", "trechoEncontrado": true,
          "problema": "Falta o modo/meio e o detalhamento.",
          "sugestao": "Indique como: 'por meio de vídeos curtos veiculados nas redes sociais...'" }
      ]
    }
  ],
  "comentarioGeral": "...",
  "proximosPassos": ["..."],
  "aviso": "Nota estimada por inteligência artificial..."
}
```

`GET /api/status` — informa se a chave está configurada e qual modelo está em uso.

Erros: `400` para entrada inválida (tema vazio, texto curto ou longo demais) e `502` quando a OpenAI falha (chave inválida, sem créditos, timeout). A mensagem vem no campo `mensagem`.

## Limitações conhecidas (para a validação com o público-alvo)

- A nota é uma estimativa; o sistema avisa que não substitui o professor.
- A contagem de linhas é aproximada (cerca de 65 caracteres por linha manuscrita).
- O modelo às vezes cita trechos com pequenas diferenças do original; nesses casos o ponto de melhoria aparece, mas o trecho não é destacado no texto.
- Não há histórico de redações nem login nesta versão.
