# API Gateway - Centralized Security

This is the centralized API Gateway for the Hsurveys microservices architecture. It handles JWT token validation and user context propagation to downstream services.

## Architecture

### Centralized Security Flow

1. **Client Request** → Gateway (Port 8080)
2. **Gateway** validates JWT token (from Authorization header or cookies)
3. **Gateway** extracts user context and adds to request headers
4. **Gateway** routes to appropriate microservice with user context
5. **Microservice** reads user context from headers (no JWT validation needed)

### Request Headers Added by Gateway

- `X-User-Id`: User's unique identifier
- `X-Username`: User's email/username
- `X-Organization-Id`: User's organization context
- `X-Department-Id`: User's department context (optional)
- `X-Team-Id`: User's team context (optional)
- `X-Authorities`: Comma-separated list of user permissions
- `X-Authenticated`: "true" to indicate successful authentication

## Routes Configuration

### User Management Service (Port 8081)
- `/api/users/**` - User management endpoints
- `/api/auth/**` - Authentication endpoints
- `/api/roles/**` - Role management endpoints
- `/api/permissions/**` - Permission management endpoints

### Survey Management Service (Port 8082)
- `/api/surveys/**` - Survey management endpoints
- `/api/questions/**` - Question management endpoints
- `/api/options/**` - Option management endpoints

### Organization Management Service (Port 8083)
- `/api/organizations/**` - Organization management endpoints
- `/api/departments/**` - Department management endpoints
- `/api/teams/**` - Team management endpoints

## Authentication Endpoints (Bypassed)

The following endpoints bypass JWT validation:
- `/api/auth/login`
- `/api/auth/register`
- `/api/auth/refresh`
- `/api/users/*/exists`
- `/api/users/bulk`

## Running the Gateway

```bash
mvn spring-boot:run
```

The gateway will start on port 8080.

## Testing

### Login Flow
1. POST `/api/auth/login` (bypasses gateway validation)
2. Gateway receives response with cookies
3. Subsequent requests include cookies
4. Gateway validates JWT and adds user context headers

### Protected Endpoint Flow
1. Client sends request with Authorization header or cookies
2. Gateway validates JWT token
3. Gateway adds user context headers
4. Request routed to appropriate microservice
5. Microservice reads user context from headers

## Benefits

1. **Single Point of Authentication**: All JWT validation happens in gateway
2. **Cookie Domain Issues Resolved**: Gateway handles cookies, microservices use headers
3. **Simplified Microservices**: No JWT validation logic needed in individual services
4. **Centralized Security**: Security policies managed in one place
5. **Better Performance**: Microservices don't need to validate tokens 