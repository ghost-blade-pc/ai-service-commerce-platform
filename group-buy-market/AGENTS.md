# AGENTS.md

## Project snapshot
- Maven multi-module Spring Boot 2.7.x DDD scaffold for group-buy marketing.
- Root modules: `api`, `app`, `domain`, `trigger`, `infrastructure`, `types`.
- The runnable entry point is `group-buy-market-lpc-app` with `top.licodetech.market.Application`.

## Module boundaries
- `group-buy-market-lpc-api`: service interfaces, DTOs, and `Response<T>` wrappers shared across modules.
- `group-buy-market-lpc-trigger`: HTTP controllers and RabbitMQ listeners; controllers implement API interfaces such as `IMarketTradeService` and `IMarketIndexService`.
- `group-buy-market-lpc-domain`: core business services and rule-chain logic; examples include `TradeLockOrderService` and `TradeSettlementOrderService` using `BusinessLinkedList` filters.
- `group-buy-market-lpc-infrastructure`: adapters for MyBatis DAOs, Redis, RabbitMQ, HTTP callbacks, and DCC wiring (`TradeRepository`, `TradePort`, `EventPublisher`, `DCCService`).
- `group-buy-market-lpc-types`: shared enums/constants/exceptions such as `ResponseCode` and `Constants.UNDERLINE`.

## Code patterns to follow
- Keep controllers thin: validate input, log with JSON, call domain services, and return `Response.builder()` results.
- Use the repo’s response codes instead of ad hoc strings; e.g. `ResponseCode.ILLEGAL_PARAMETER`, `E0005`, `RATE_LIMITER`.
- Preserve the repository/port split: domain depends on abstractions like `ITradeRepository` / `ITradePort`, infrastructure implements them.
- Messaging is RabbitMQ topic-based; `TeamSuccessTopicListener` binds via `@RabbitListener` + `@QueueBinding`.
- Dynamic config updates go through `DCCController` -> `RTopic.publish(new AttributeVO(key, value))` and are consumed by `DCCService` fields annotated with `@DCCValue`.

## Runtime and integration points
- `group-buy-market-lpc-app/src/main/resources/application.yml` activates `dev` by default.
- `application-dev.yml` wires MySQL `127.0.0.1:13306`, Redis `16379`, RabbitMQ `5672`, Logstash, and Prometheus/Actuator.
- Logs go to `./data/log/log_info.log` and `./data/log/log_error.log`, plus Logstash TCP output.
- External callbacks are sent with OkHttp in `GroupBuyNotifyService`; MQ publishing uses `EventPublisher`.

## Build and test workflow
- Build the full reactor from the root with Maven; the app jar is produced by `group-buy-market-lpc-app`.
- The app module’s Surefire config sets `skipTests>true</skipTests>` by default, so override it explicitly when you need to run tests in that module.
- Representative local checks live under `group-buy-market-lpc-app/src/test/java/top/licodetech/market/test/**` and are SpringBoot tests that exercise controllers, services, MQ, and DCC flows.
- Docker helpers live in `group-buy-market-lpc-app/build.sh` and `docs/dev-ops/docker-compose-app.yml`; environment services are defined in `docs/dev-ops/docker-compose-environment.yml`.

## Repository cues
- `README.md` is minimal; the most useful documentation is in the POMs, `application-*.yml`, and the controller/service classes above.
- `docs/dev-ops/` contains the deployment stack definitions for local infrastructure and should be checked before changing ports or broker/database settings.

