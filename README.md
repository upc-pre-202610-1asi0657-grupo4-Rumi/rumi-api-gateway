# rumi-api-gateway

API gateway of **Rumi**, the structural monitoring platform by Kuntur Labs.
Single entry point for the web and mobile clients; it routes every `/api/v1/**` request to the
service that owns the bounded context.

> **SKELETON: functionality planned for later sprints. The health endpoint is NOT counted as an implemented functional endpoint.**
> Routing and JWT authentication are configured and tested; CORS and rate limiting are not implemented yet.

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

## Security

The gateway validates the JWT issued by `rumi-iam-service` and never calls IAM. Tokens are signed with
HS256 and carry `sub` (user id), `role` (`ADMINISTRATOR` or `RESIDENT`) and `buildingIds` (list of
building ids, possibly empty). IAM sets a 15-minute expiration. The gateway checks the signature, the
expiration and the role, then forwards the user to the service in these headers:

| Header | Value |
|---|---|
| `X-User-Id` | `sub` |
| `X-Role` | `role` |
| `X-Building-Id` | `buildingIds`, comma-separated (empty when the user has no buildings) |

Any of these three headers sent by the client is removed on every route, so nobody can impersonate
another user.

### Access rules

Rules are evaluated in order and the first match wins. The finer rule "only my own building" belongs to
each service, not to the gateway.

| Paths (`/api/v1/...`) | Methods | Access |
|---|---|---|
| `authentication/**`, `gateway/health`, `/v3/api-docs/**`, `/swagger-ui.html`, `/swagger-ui/**` | any | public |
| `buildings/*/risk-indexes/**`, `readings/**`, `digital-twin/**`, `seismic-events/**`, `risk-indexes/**`, `reports/**`, `subscriptions/**`, `payments/**`, `thresholds/**` | any | `ADMINISTRATOR` |
| `buildings/**`, `sensors/**`, `invitations/**` | `GET` | `ADMINISTRATOR`, `RESIDENT` |
| `buildings/**`, `sensors/**`, `invitations/**` | `POST`, `PUT`, `PATCH`, `DELETE` | `ADMINISTRATOR` |
| `users/**`, `device-tokens/**`, `evacuation-plans/**`, `checklists/**`, `inspections/**`, `damage-reports/**`, `alerts/**` and any other path | any | any valid token |

The health endpoints of the services follow the rule of their prefix: only
`/api/v1/authentication/health` is public.

A missing, malformed, badly signed or expired token is answered with `401`; a valid token whose role is
not allowed with `403`. Both answers are `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "A valid bearer token is required to access this resource.",
  "instance": "/api/v1/alerts"
}
```

### How to get a token for manual testing

The IAM sign-in does not exist yet, so build a token by hand at <https://jwt.io>:

1. Choose the algorithm `HS256`.
2. Paste as the secret the same development value you export as `JWT_SECRET` when you run the gateway,
   as plain text (not base64-encoded). Never use a production secret on a website.
3. Use a payload like this one. `exp` is a Unix timestamp in seconds; get one 15 minutes ahead with
   `date -v+15M +%s` (macOS) or `date -d '+15 min' +%s` (Linux).

   ```json
   {
     "sub": "7d3c5b1e-2f4a-4c8e-9b6d-1a2b3c4d5e6f",
     "role": "ADMINISTRATOR",
     "buildingIds": ["3f2c8a10-6b7d-4e9f-a1b2-c3d4e5f60718"],
     "exp": 1798000000
   }
   ```

4. Send the encoded token in the `Authorization` header:

   ```sh
   curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/buildings
   ```

`sub` and every building id must be UUIDs and `role` must be `ADMINISTRATOR` or `RESIDENT`; otherwise
the token is rejected with `401`. The gateway does not issue tokens.

## Endpoints

Endpoints answered by the gateway itself:

| Verb | Path | Description | Request | Response | User story | Status |
|---|---|---|---|---|---|---|
| GET | `/api/v1/gateway/health` | Check that the gateway is running (public) | none | `200` `HealthResponse` | none | skeleton |

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

The gateway documents only its own endpoint, the `bearerJwt` security scheme and the shared
`401` and `403` answers. Each service publishes its own OpenAPI documentation on its own port.
To refresh the exported spec, run the gateway on port `8080` and execute
`curl -s http://localhost:8080/v3/api-docs | python3 -m json.tool > docs/openapi.json`.

## Run

Requirements: JDK 21 (the Maven wrapper is included; use `mvnw.cmd` on Windows).

```sh
export JWT_SECRET='<the HS256 secret shared with rumi-iam-service>'
./mvnw spring-boot:run
```

| Variable | Default |
|---|---|
| `JWT_SECRET` | none: required, at least 32 bytes; the gateway does not start without it |
| `SERVER_PORT` | `8080` |
| `<NAME>_SERVICE_URL` | see the routes table |

A request routed to a service that is not running is answered with HTTP 500.

## Test

```sh
./mvnw test
```

The tests run with the `test` profile (`src/test/resources/application-test.yml`), which defines its
own test-only secret, so `JWT_SECRET` is not needed. They include:

- `ApiGatewayRoutingTest`: order of the routes and target service of every path prefix.
- `GatewayAuthenticationTest`: tokens, access rules and user headers, against a stub backend.
- `GatewayAuthenticationAcceptanceTest`: runs the Gherkin scenarios of
  [`src/test/resources/features/gateway_authentication.feature`](src/test/resources/features/gateway_authentication.feature)
  (US02, QA-04) with Cucumber.

## Origin

New component. It is part of the target architecture defined when the modular monolith
[`rumi-backend`](https://github.com/upc-pre-202610-1asi0657-grupo4-Rumi/rumi-backend) was decomposed.
