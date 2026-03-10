# bip-teste-integrado

Projeto fullstack com arquitetura em camadas para gestao de beneficios:
- DB (PostgreSQL + scripts SQL)
- Modulo de dominio legado `ejb-module` (regra transacional de transferencia)
- Backend Spring Boot (CRUD + integracao com o servico de transferencia)
- Frontend Angular (CRUD + transferencia)

## Arquitetura

- `db/`: `schema.sql` e `seed.sql`
- `ejb-module/`: entidade `Beneficio` + `BeneficioEjbService`
- `backend-module/`: API REST em camadas (`controller -> service -> repository -> integration`)
- `frontend/`: app Angular 17 consumindo o backend
- `docker-compose.yml`: orquestracao de `postgres`, `backend`, `frontend` e runner de testes EJB

Decisao arquitetural:
- O nome `BeneficioEjbService` foi mantido por compatibilidade com o desafio.
- A implementacao adotada e um servico de dominio puro, gerenciado pelo Spring, sem container Jakarta EE/EJB real.
- A integracao backend -> modulo de transferencia acontece por bean Spring e retry para contencao transacional.

## Requisitos

- Docker + Docker Compose
- Java 17+ e Maven (opcional)
- Node 20+ (opcional, para rodar frontend fora do Docker)

## Subir ambiente completo com Docker

```bash
docker compose up --build -d postgres backend frontend
```

Acessos:
- Frontend: `http://localhost:4200`
- Backend: `http://localhost:8080`

## Swagger / OpenAPI

Com backend em execucao:
- UI: `http://localhost:8080/swagger-ui.html`
- JSON: `http://localhost:8080/api-docs`

## Rodar testes

### Testes Java (EJB + Backend)
```bash
mvn -B test
```

Os testes de integracao com Testcontainers sao executados automaticamente quando Docker esta disponivel.

### Testes EJB via Docker Compose
```bash
docker compose --profile tests run --rm ejb-module-tests
```

## Rodar frontend local (sem Docker)

```bash
cd frontend
npm install --no-audit --no-fund
npm start
```

## Endpoints principais

Base: `/api/v1/beneficios`

- `GET /` listar
- `GET /{id}` buscar por id
- `POST /` criar
- `PUT /{id}` atualizar
- `DELETE /{id}` remover
- `POST /transferencias` transferencia entre beneficios

Exemplo de payload para transferencia:

```json
{
  "fromId": 1,
  "toId": 2,
  "amount": 100.00
}
```

## Encerrar ambiente

```bash
docker compose down -v
```
