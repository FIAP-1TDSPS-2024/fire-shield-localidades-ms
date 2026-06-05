# Fire Shield Localidades MS

Microsservico responsavel por receber ocorrencias brutas de incendio identificadas por satelite, enriquecer os dados com informacoes de endereco no Brasil e publicar a ocorrencia enriquecida para o microsservico de SMS.

Este modulo faz parte do projeto academico **Fire Shield**, composto por dois microsservicos Java:

- `fire-shield-localidades-ms`: consome ocorrencias brutas, consulta dados de localidade por coordenadas e publica ocorrencias enriquecidas.
- `fire-shield-sms-ms`: consome ocorrencias enriquecidas, persiste os dados, consulta contatos por estado e envia SMS ao orgao responsavel.

## Fluxo Geral

1. Uma mensagem com coordenadas de uma ocorrencia chega na fila configurada em `RABBITMQ_QUEUE_ORIGIN`.
2. O `fire-shield-localidades-ms` consome a mensagem via RabbitMQ.
3. O servico consulta uma API externa de localidade usando OpenFeign.
4. A ocorrencia e persistida no banco.
5. Um evento de outbox e criado e publicado para `RABBITMQ_QUEUE_DESTINY`.
6. O `fire-shield-sms-ms` consome a ocorrencia enriquecida e executa o fluxo de notificacao por SMS.

## Arquitetura e Tecnologias

Tecnologias principais:

- Java 21
- Spring Boot 3.5.x
- Spring AMQP / RabbitMQ
- Spring Data JPA
- Flyway
- Oracle Database
- H2 para testes
- Spring Cloud OpenFeign
- Lombok
- Maven Wrapper

O servico segue uma organizacao simples em camadas, com separacao entre:

- `external_interface`: adaptadores externos, como RabbitMQ e Feign.
- `service`: regras de aplicacao e orquestracao do fluxo de ocorrencias.
- `entity`: entidades persistidas.
- `repository`: acesso a dados via Spring Data JPA.
- `jobs`: tarefas de outbox e enriquecimento.

## Variaveis de Ambiente

Configure as variaveis abaixo antes de executar localmente:

| Variavel | Descricao | Exemplo |
| --- | --- | --- |
| `DB_URL` | JDBC URL do banco Oracle | `jdbc:oracle:thin:@localhost:1521/FREEPDB1` |
| `DB_USER` | Usuario do banco | `fire_shield` |
| `DB_PASSWORD` | Senha do banco | `fire_shield` |
| `API_URL` | URL base da API externa de geocodificacao reversa | `https://api.example.com` |
| `RABBITMQ_HOST` | Host do RabbitMQ | `localhost` |
| `RABBITMQ_PORT` | Porta do RabbitMQ | `5671` ou `5672` |
| `RABBITMQ_USER` | Usuario do RabbitMQ | `guest` |
| `RABBITMQ_PASSWORD` | Senha do RabbitMQ | `guest` |
| `RABBITMQ_VHOST` | Virtual host do RabbitMQ | `/` |
| `RABBITMQ_QUEUE_ORIGIN` | Fila de entrada com ocorrencias brutas | `fs.ocorrencia.queue` |
| `RABBITMQ_QUEUE_DESTINY` | Fila de saida com ocorrencias enriquecidas | `loc.ocorrencia.queue` |

Observacao: o `application.properties` atual usa SSL para RabbitMQ (`spring.rabbitmq.ssl.enabled=true`), pensando em CloudAMQP. Para RabbitMQ local sem SSL, ajuste a propriedade para `false` em um perfil local ou sobrescreva via configuracao.

## Executando Localmente

Entre no diretorio do microsservico:

```powershell
cd fire-shield-localidades-ms
```

Configure as variaveis no PowerShell:

```powershell
$env:DB_URL="jdbc:oracle:thin:@localhost:1521/FREEPDB1"
$env:DB_USER="fire_shield"
$env:DB_PASSWORD="fire_shield"
$env:API_URL="https://api.example.com"
$env:RABBITMQ_HOST="localhost"
$env:RABBITMQ_PORT="5672"
$env:RABBITMQ_USER="guest"
$env:RABBITMQ_PASSWORD="guest"
$env:RABBITMQ_VHOST="/"
$env:RABBITMQ_QUEUE_ORIGIN="fs.ocorrencia.queue"
$env:RABBITMQ_QUEUE_DESTINY="loc.ocorrencia.queue"
```

Execute:

```powershell
.\mvnw.cmd spring-boot:run
```

Por padrao, o servico sobe em:

```text
http://localhost:8081
```

## Testes

Para rodar os testes:

```powershell
.\mvnw.cmd test
```

Os testes usam H2 em memoria e desabilitam o consumo automatico do RabbitMQ.

## Integracao com o SMS MS

O `fire-shield-localidades-ms` publica a ocorrencia enriquecida na fila definida por `RABBITMQ_QUEUE_DESTINY`.

O `fire-shield-sms-ms` deve estar configurado para consumir essa mesma fila como `RABBITMQ_QUEUE_ORIGIN`.

Exemplo:

```text
fire-shield-localidades-ms:
RABBITMQ_QUEUE_DESTINY=loc.ocorrencia.queue

fire-shield-sms-ms:
RABBITMQ_QUEUE_ORIGIN=loc.ocorrencia.queue
```

Esse alinhamento de filas e o contrato principal entre os dois microsservicos.

