# ADR 0001: Estrategia de lock na transferencia

## Contexto

A transferencia entre beneficios precisa garantir consistencia sob concorrencia.
Os riscos principais sao:
- race condition (saldo negativo ou lost update)
- deadlock entre duas transacoes concorrentes
- contencao elevada sob pico de requisicoes

## Decisao

Adotar lock pessimista (`PESSIMISTIC_WRITE`) na leitura das duas entidades e ordenar a aquisicao de locks por ID.

Complementos:
- timeout curto de lock e retry limitado para contencao transacional
- validacao de saldo antes do debito

## Justificativa

- O dominio envolve debito/credito, onde a perda de consistencia e critica.
- Lock pessimista evita races sem depender do retry do client.
- Lock ordenado reduz risco de deadlock em transferencias cruzadas.

## Consequencias

- Em cargas altas, pode ocorrer maior espera e aumento de latencia.
- Para evolucao futura, um modelo otimista com reprocessamento pode ser considerado se o throughput for prioridade.
