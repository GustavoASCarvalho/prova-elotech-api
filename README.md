# Task Manager

API REST para gerenciamento de projetos e tarefas com autenticacao JWT.

## Como rodar

1. Subir a aplicacao:

```bash
docker compose up -d
```

2. Swagger:

```text
http://localhost:8080/swagger-ui.html
```

3. H2 Console:

```text
http://localhost:8080/h2-console
```

4. Rodar testes:

```bash
./mvnw test
```

## Fluxo rapido de uso

1. Registrar usuario:

```http
POST /api/auth/register
```

2. Login:

```http
POST /api/auth/login
```

3. Copiar o token retornado e enviar no header:

```text
Authorization: Bearer <token>
```

4. Usar endpoints de projetos e tarefas via Swagger.

## Decisoes tecnicas e tradeoffs

1. Arquitetura em camadas (`controller -> service -> repository`).
   Tradeoff: simples de manter e testar, mas pode crescer com mais classes em regras complexas.

2. JWT stateless com Spring Security.
   Tradeoff: escalavel e sem sessao no servidor, mas exige cuidado com expiracao e revogacao de token.

3. Regras de negocio centralizadas nos services (WIP limit, transicao de status, regra de CRITICAL).
   Tradeoff: regra fica explicita e testavel, mas services ficam maiores.

4. H2 em memoria para simplificar avaliacao.
   Tradeoff: setup rapido, mas nao representa todos os cenarios de producao (PostgreSQL seria mais realista).

5. Cache no resumo por projeto com invalidacao em escritas de tarefa.
   Tradeoff: melhora leitura, mas aumenta complexidade de coerencia de cache.

6. DTOs de request/response e handler global de excecoes.
   Tradeoff: contratos mais claros e respostas padronizadas, em troca de mais codigo de mapeamento.

## O que eu faria diferente com mais tempo

1. Migrar erro padronizado para ProblemDetail (RFC 7807) em todos os cenarios.
2. Adicionar testes E2E de fluxo critico (login -> criar projeto -> criar tarefa -> atualizar status).
3. Melhorar observabilidade (logs estruturados e correlacao de requisicoes).
4. Adicionar perfil de producao com PostgreSQL e migracoes versionadas.
5. Refatorar alguns services em componentes menores de dominio para reduzir acoplamento.

## Cobertura do desafio (resumo)

1. Autenticacao/autorizacao com perfis ADMIN e MEMBER.
2. CRUD de projetos e tarefas com regras de negocio exigidas.
3. Filtros, ordenacao, busca textual e relatorio resumido.
4. Swagger/OpenAPI disponivel.
5. Testes unitarios (services) e de integracao com SpringBootTest.
