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
- **Keycloak 26** para o login — via Docker ou pelo script que baixa e roda sem Docker (veja abaixo)

## Como executar (desenvolvimento)

Abra três terminais.

**0. Keycloak** (porta 8081)

Sem Docker (Windows, precisa de Java 21+; na primeira vez baixa ~200 MB para `keycloak/servidor/`):
```powershell
.\keycloak\iniciar-keycloak.ps1
```

Com Docker:
```bash
docker compose up keycloak
```

O realm `redacao-enem` é importado automaticamente de `keycloak/realm-redacao-enem.json`, com:

| O quê                 | Valor                                                       |
|-----------------------|-------------------------------------------------------------|
| Usuário de teste      | `estudante` / `estudante123`                                |
| Novo estudante        | link **"Cadastre-se"** na tela de login                     |
| Console administrativo | http://localhost:8081 — `admin` / `admin` (só para desenvolvimento) |

Para demonstrar sem Keycloak, inicie o backend com `AUTH_HABILITADA=false`: o front percebe pelo `/api/status` e não pede login.

**1. Backend** (porta 8080)

A chave da OpenAI pode ficar num arquivo local, para não precisar defini-la a cada terminal:

```bash
cd backend
cp application-local.properties.example application-local.properties   # Windows: copy
# edite application-local.properties e preencha OPENAI_API_KEY
./gradlew bootRun
```

O `application-local.properties` está no `.gitignore` e fica fora de `src/main/resources`, então nunca vai para o Git nem para o `.jar`. **Nunca coloque a chave em `application.properties`.**

Ou, se preferir, defina a variável de ambiente (ela tem prioridade sobre o arquivo):

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
| `AUTH_HABILITADA` | `true`                       | `false` libera a API sem login              |
| `KEYCLOAK_URL`    | `http://localhost:8081`      | Endereço do Keycloak (o mesmo que o navegador usa) |
| `KEYCLOAK_REALM`  | `redacao-enem`               | Realm                                       |
| `KEYCLOAK_CLIENT_ID` | `corretor-frontend`       | Cliente público usado pelo front            |

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
Keycloak (localhost:8081) ◄── login OIDC + PKCE ── keycloak-js
     │ chaves públicas (JWKS)                          │
     ▼                                                 ▼
Angular (localhost:4200)                     token no cabeçalho Authorization
  FormularioRedacao ─(enviar)─► App ─► AvaliacaoService (HttpClient + interceptor)
  ResultadoAvaliacao ◄──────────┘            │  POST /api/avaliacoes  (Bearer)
    ├─ CartaoCompetencia (×5)                 ▼
    └─ TextoMarcado                 Spring Boot (localhost:8080) — valida o JWT
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
- **`LocalizadorDeTrechos`** — quando o modelo cita um trecho com pequenas diferenças (aspas tipográficas, espaços, maiúsculas, acento perdido, aspas ou ponto final em volta), encontra o fragmento correspondente e devolve-o como está no texto original, para que o destaque funcione.
- **`OpenAiAvaliador`** — repete a chamada uma vez quando a OpenAI responde 5xx, diferencia "sem créditos" de "limite de requisições" no erro 429 e avisa quando a resposta vem truncada.
- **`TratadorDeErros`** — toda falha, inclusive inesperada, chega ao front no formato `{ "mensagem": ... }`.
- **`SegurancaConfig`** — a API é um *resource server* OAuth2 sem sessão: valida assinatura, emissor e validade do JWT emitido pelo Keycloak. `POST /api/avaliacoes` exige login (é o que consome créditos da OpenAI); `GET /api/status` é público. CORS é aplicado antes da autenticação (`CorsConfig`).
- **`AvaliadorIA`** — interface; trocar de provedor exige só uma nova implementação.

### Frontend (`frontend/src/app`)

- `components/formulario-redacao` — formulário reativo, contadores de palavras e de linhas estimadas (alerta abaixo de 8 ou acima de 30 linhas), validação do tamanho mínimo/máximo antes de enviar e rascunho salvo automaticamente no navegador.
- `components/resultado-avaliacao` — nota total, barras por competência, comentário geral, próximos passos e botão para imprimir/salvar em PDF (a impressão mostra só o resultado).
- `components/cartao-competencia` — justificativa, pontos fortes e pontos de melhoria de uma competência.
- `components/texto-marcado` — a redação com os trechos destacados e numerados.
- `components/variacao-nota` — selo "+40 / −40 / =" que compara a reescrita com a avaliação anterior do mesmo tema.
- `auth/autenticacao.service.ts` — inicia o `keycloak-js` antes de a página aparecer (login obrigatório, PKCE), renova o token e faz logout. A configuração vem do `/api/status`, então não fica duplicada no front.
- `auth/autenticacao.interceptor.ts` — anexa `Authorization: Bearer ...` às chamadas `/api` e leva ao login se o backend responder 401.
- `services/avaliacao.service.ts` — chamadas HTTP e tradução das mensagens de erro.
- `utils/redacao.ts` — contagens, limites, numeração dos pontos, segmentação do texto e variação de notas (testadas em `redacao.spec.ts`).
- `utils/armazenamento.ts` — guarda no `localStorage` o rascunho e a última avaliação, separados por usuário (para quem divide o computador do laboratório); ao recarregar a página o resultado continua na tela. Se o navegador bloquear o armazenamento, tudo funciona, só que sem lembrar.

## API

`POST /api/avaliacoes` — exige `Authorization: Bearer <token do Keycloak>` (exceto com `AUTH_HABILITADA=false`)

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

`GET /api/status` (público) — informa se a chave está configurada, qual modelo está em uso e, em `autenticacao`, se o login está ligado e em qual Keycloak/realm/cliente.

Erros: `400` para entrada inválida (tema vazio, texto curto ou longo demais), `401` sem token ou com token inválido/expirado e `502` quando a OpenAI falha (chave inválida, sem créditos, timeout). A mensagem vem no campo `mensagem`.

## Limitações conhecidas (para a validação com o público-alvo)

- A nota é uma estimativa; o sistema avisa que não substitui o professor.
- A contagem de linhas é aproximada (cerca de 65 caracteres por linha manuscrita).
- Pequenas diferenças na citação dos trechos são toleradas, mas se o modelo parafrasear o trecho, o ponto de melhoria aparece sem destaque no texto.
- Os limites de tamanho (300 a 6000 caracteres) estão duplicados no front (`utils/redacao.ts`) e no backend (`application.properties`); ao alterar um, altere o outro.
- Não há histórico completo no servidor: o navegador guarda só a última avaliação de cada estudante (e a anterior, para comparação).
- O Keycloak roda em modo de desenvolvimento (`start-dev`, HTTP, admin `admin`/`admin`). Para publicar: use `kc start` com HTTPS e banco de dados próprio, troque a senha do admin, ajuste `redirectUris`/`webOrigins` do cliente para o domínio real e configure SMTP se quiser "esqueci minha senha".
