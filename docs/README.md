# Documentacao do Desafio

## Status dos itens

1. Banco de dados (schema + seed): concluido
2. Correcao EJB (validacao, saldo, locking): concluido
3. Backend CRUD + integracao EJB: concluido
4. Frontend Angular: concluido
5. Testes EJB e Backend: concluido
6. Swagger + README: concluido

## Qualidade tecnica entregue

- Arquitetura em camadas no backend
- Regra critica de transferencia centralizada no modulo legado `ejb-module`
- Tratamento global de erros HTTP (400, 404, 409, 500)
- Testes de servico e controller no backend
- Testes unitarios do EJB
- Testes de integracao com Postgres real via Testcontainers para concorrencia, retry e constraints
- Frontend Angular integrado com CRUD e transferencia
- Ambiente containerizado para banco, backend e frontend

## Decisao arquitetural

- O desafio forneceu o nome `BeneficioEjbService`, mas a execucao final nao depende de container Jakarta EE.
- A decisao adotada foi usar o modulo como servico de dominio puro, gerenciado pelo Spring.
- Isso elimina a ambiguidade de instanciar um pseudo-EJB dentro do contexto Spring e torna o runtime coerente com a stack entregue.

## Evidencias de execucao

- Build e testes Java: `mvn -B test`
- Frontend Angular build: `cd frontend && npm run build`
- API: `http://localhost:8080`
- Frontend: `http://localhost:4200`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## Fluxos de erro na transferencia

- `400`: payload invalido (`fromId == toId`, nulos, amount <= 0)
- `404`: beneficio nao encontrado
- `409`: saldo insuficiente ou contencao transacional esgotada

## Observabilidade

- Estrategia atual: mensagens padronizadas via `ApiError`
- Evolucao: logs estruturados para transferencia, metricas de latencia e retries

## ADRs

- `docs/adr/0001-locking-strategy.md`
