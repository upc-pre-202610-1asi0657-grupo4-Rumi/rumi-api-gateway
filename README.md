# rumi-api-gateway

API gateway of **Rumi**, the structural monitoring platform by Kuntur Labs.
Single entry point for the web and mobile clients; it routes every `/api/v1/**` request to the
service that owns the bounded context.

> **SKELETON: functionality planned for later sprints. The health endpoint is NOT counted as an implemented functional endpoint.**
> Routing is configured and tested; cross-cutting concerns (authentication, CORS, rate limiting) are not implemented yet.

| | |
|---|---|
| Role | API Gateway (Spring Cloud Gateway, WebFlux) |
| Port | `8080` |
| Base package | `com.rumi.gateway` |

## Routes

Routes are evaluated in order and the first match wins.

| Order | Route id | Path predicates (`/api/v1/...`) | Target service | Default URI | Variable |
|---|---|---|---|---|---|
| 1 | `seismic-service-building-risk-indexes` | `buildings/{buildingId}/risk-indexes/**` | `rumi-seismic-service` | `http://localhost:8083` | `SEISMIC_SERVICE_URL` |
| 2 | `building-service` | `buildings/**`, `sensors/**`, `invitations/**` | `rumi-building-service` | `http://localhost:8081` | `BUILDING_SERVICE_URL` |
| 3 | `monitoring-service` | `readings/**`, `digital-twin/**` | `rumi-monitoring-service` | `http://localhost:8082` | `MONITORING_SERVICE_URL` |
| 4 | `seismic-service` | `seismic-events/**`, `risk-indexes/**`, `reports/**` | `rumi-seismic-service` | `http://localhost:8083` | `SEISMIC_SERVICE_URL` |
| 5 | `iam-service` | `authentication/**`, `users/**`, `device-tokens/**` | `rumi-iam-service` | `http://localhost:8084` | `IAM_SERVICE_URL` |
| 6 | `notification-service` | `alerts/**`, `thresholds/**` | `rumi-notification-service` | `http://localhost:8085` | `NOTIFICATION_SERVICE_URL` |
| 7 | `safety-service` | `evacuation-plans/**`, `checklists/**` | `rumi-safety-service` | `http://localhost:8086` | `SAFETY_SERVICE_URL` |
| 8 | `billing-service` | `subscriptions/**`, `payments/**` | `rumi-billing-service` | `http://localhost:8087` | `BILLING_SERVICE_URL` |
| 9 | `maintenance-service` | `inspections/**`, `damage-reports/**` | `rumi-maintenance-service` | `http://localhost:8088` | `MAINTENANCE_SERVICE_URL` |

Route 1 must stay before route 2: the risk indexes of a building belong to Seismic Correlation,
and `/api/v1/buildings/**` would capture them otherwise. `ApiGatewayRoutingTest` checks this order
and the target of every path prefix.

## Endpoints

Endpoints answered by the gateway itself:

| Verb | Path | Description | Request | Response | User story | Status |
|---|---|---|---|---|---|---|
| GET | `/api/v1/gateway/health` | Check that the gateway is running | none | `200` `HealthResponse` | none | skeleton |

Implemented functional endpoints: 0. Skeleton endpoints: 1.

```json
{
  "status": "UP",
  "service": "rumi-api-gateway"
}
```

## API documentation

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI spec: <http://localhost:8080/v3/api-docs>
- Exported spec: [`docs/openapi.json`](docs/openapi.json)

The gateway documents only its own endpoint. Each service publishes its own OpenAPI
documentation on its own port.

## Run

Requirements: JDK 21, Maven.

```sh
mvn spring-boot:run
```

| Variable | Default |
|---|---|
| `SERVER_PORT` | `8080` |
| `<NAME>_SERVICE_URL` | see the routes table |

A request routed to a service that is not running is answered with HTTP 500.

## Test

```sh
mvn test
```

## Origin

New component. It is part of the target architecture defined when the modular monolith
[`rumi-backend`](https://github.com/upc-pre-202610-1asi0657-grupo4-Rumi/rumi-backend) was decomposed.
