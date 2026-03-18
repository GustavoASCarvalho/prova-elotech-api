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

3. Rodar testes:

```bash
mvn test
```

## Decisoes tecnicas e tradeoffs

1. Arquitetura em camadas (controller -> service -> repository).
   Tradeoff: separa bem responsabilidades e facilita manutencao/testes, não escala tão bem quanto clean arquiteture em projetos grandes.

2. Cache no endpoint de resumo por projeto.
   Decisao: leitura de resumo e uma operacao recorrente e agregada, entao foi aplicada estrategia de cache para reduzir consultas repetidas.
   Tradeoff: melhora performance em leitura, mas exige invalidacao correta a cada escrita para evitar dado desatualizado.

3. Historico de alteracoes com Hibernate Envers.
   Decisao: manter trilha automatica de versoes das entidades auditadas sem implementar tabela de auditoria manual.
   Tradeoff: entrega rapida de rastreabilidade (quem/quando/o que mudou), porem aumenta volume de dados no banco e custo de manutencao/consulta historica.

## O que eu faria diferente com mais tempo

1. Reestruturar o historico de alteracoes para um modelo mais aderente aos requisitos de negocio.
2. Evoluir o registro de historico para um fluxo assíncrono (fila + banco NoSQL) para nao impactar o tempo de resposta da API transacional.
3. Melhorar a observabilidade com logs estruturados, correlacao de requisicoes, metricas e dashboards.
4. Criar mais rotas orientadas ao consumo do front para reduzir transformacoes no cliente.
5. Ampliar os testes de fluxos criticos com cenarios de ponta a ponta (ex.: autenticacao, autorizacao, transicoes de status e regras de WIP).
