
# gsb-oauth (Gradle Spring Boot OAuth)

Consume el servicio OAuth de keycloak, construido con Spring Boot 3 y WebFlux.

## Arquitectura Hexagonal

```
src/main/java/com/work/proxy/
├── domain/                              # Núcleo del dominio
│   ├── exception/
│   │   └── ServiceException.java
│   └── port/out/
│       └── OAuthPort.java               # Puerto de salida
│
├── application/                         # Capa de aplicación
│   ├── dto/
│   │   ├── TokenRequest.java
│   │   ├── TokenResponse.java
│   │   ├── Response.java
│   │   └── Error.java
│   ├── mapper/
│   │   └── TokenMapper.java
│   ├── port/in/
│   │   └── OAuthUseCase.java            # Puerto de entrada
│   └── service/
│       └── OAuthService.java
│
└── infrastructure/                      # Capa de infraestructura
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── OAuthController.java
    │   │   └── GlobalExceptionHandler.java
    │   └── out/oauth/
    │       └── OAuthAdapter.java
    └── config/
        ├── OAuthProperties.java
        ├── WebClientProperties.java
        ├── WebClientConfig.java
        └── OpenApiConfig.java
```

## Tecnologías

- Java 17
- Spring Boot 3.5.11
- Spring WebFlux
- Spring Boot Actuator
- Springdoc OpenAPI (Swagger)
- Lombok
- Gradle

## Configuración

```properties
# Server
server.port=9090

# OAuth Client
oauth.client.url=https://oauth-server.example.com/oauth/token
oauth.client.attempts=3
oauth.client.timeout=10

# WebClient
webclient.max-connections=500
webclient.connection-timeout=5000
webclient.read-timeout=10000
webclient.write-timeout=10000

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
```

## Ejecución

```bash
# Docker
docker run -d -p 127.0.0.1:8080:8080 -v keycloakdata:/opt/keycloak/data --name keycloak -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:26.5.4 start-dev

# Build
gradle build

# Producción
java -jar build/libs/proxy-oauth-0.0.1-SNAPSHOT.jar
```

## API

### POST /api/v1/oauth/token

Obtiene un token del servidor OAuth.

**Request:**
```http
POST /api/v1/oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials&client_id=my-client&client_secret=my-secret&scope=read
```

**Parámetros del form:**

| Parámetro | Requerido | Descripción |
|-----------|-----------|-------------|
| `grant_type` | Sí | Tipo de flujo OAuth (`client_credentials`, `password`) |
| `client_id` | No | ID del cliente OAuth |
| `client_secret` | No | Secret del cliente OAuth |
| `username` | No | Usuario (requerido para flujo `password`) |
| `password` | No | Contraseña (requerido para flujo `password`) |
| `scope` | No | Alcance del token solicitado |

**Response 200:**
```json
{
    "data": {
        "access_token": "eyJhbGciOiJSUzI1NiIs...",
        "token_type": "Bearer",
        "expires_in": 3600,
        "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2g...",
        "scope": "read write"
    }
}
```

**Response 4xx / 5xx:**
```json
{
    "error": "Service Unavailable",
    "status": 503,
    "timestamp": "2024-01-15T10:30:45.123"
}
```

## Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/v1/oauth/token` | Obtener token OAuth |
| GET | `/swagger-ui.html` | Swagger UI |
| GET | `/api-docs` | OpenAPI JSON |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/info` | Información de la aplicación |
| GET | `/actuator/metrics` | Métricas |

## Tests

```bash
gradle test
```

## Características

- **Reactivo**: WebFlux non-blocking
- **Resiliente**: Retry con backoff exponencial
- **Observable**: Actuator para health y métricas
- **Documentado**: Swagger UI

## Licencia

Apache 2.0
