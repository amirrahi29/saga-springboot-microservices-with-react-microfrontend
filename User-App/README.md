# User-App request flow

Frontend (`http://localhost:3000`) calls this app on `http://localhost:8081`.
Sessions are stateless. The only custom security filter is `JwtAuthorizationFilter`.
It extracts and validates the access token and stores the user in `SecurityContext`.
Spring Security's `AuthorizationFilter` then reads that object. This app does not assign roles.
Access is `permitAll` or `authenticated`.

## Protected request (`GET /api/home`)

```mermaid
flowchart TD
  FE["Frontend authFetch<br/>Authorization: Bearer access token"]
  DFP["DelegatingFilterProxy<br/>bean springSecurityFilterChain"]
  FCP["FilterChainProxy<br/>selects SecurityConfig SecurityFilterChain"]
  JWT["JwtAuthorizationFilter<br/>extract Bearer, validate access token,<br/>store Authentication in SecurityContext"]
  AUTHZ["AuthorizationFilter<br/>read SecurityContext<br/>/api/home must be authenticated"]
  DS["DispatcherServlet"]
  C["UserController.home"]
  S["UserService.findByEmail"]
  R["UserJpaRepository.findByEmail"]
  BACK["Response { name, imageUrl }<br/>back through Controller, servlet, filters, to the frontend"]

  FE --> DFP --> FCP --> JWT --> AUTHZ --> DS --> C --> S --> R
  R --> S --> C --> BACK
```

1. The frontend sends `GET /api/home` with `Authorization: Bearer <access token>`.
2. The servlet container hits `DelegatingFilterProxy`, which delegates to the `springSecurityFilterChain` bean.
3. `FilterChainProxy` selects the `SecurityFilterChain` from `SecurityConfig` (stateless, CSRF off, CORS for `http://localhost:3000`).
4. `JwtAuthorizationFilter` runs before `UsernamePasswordAuthenticationFilter`:
   - Reads the `Authorization` header.
   - Strips the `Bearer ` prefix and calls `JwtUtil.email`, which accepts only a non-expired token whose `type` claim is `access`.
   - On success, stores `UsernamePasswordAuthenticationToken(email, null, empty authorities)` in `SecurityContextHolder`.
   - On a bad or expired token, logs the failure and leaves the context empty. The chain still continues.
5. `AuthorizationFilter` reads that `Authentication`. Rules in `SecurityConfig`:
   - `/api/auth/**` and `/h2-console/**` are `permitAll`.
   - `/api/home` is `authenticated`.
   - Every other request, including `GET /uploads/**`, is `permitAll`.
   - There is no role check. The authority list is empty. A missing principal on `/api/home` is a 401 from `HttpStatusEntryPoint`.
6. `DispatcherServlet` routes to `UserController.home`.
7. `UserService.findByEmail` calls `UserJpaRepository.findByEmail`.
8. The controller returns `{ name, imageUrl }`. The response goes back through the servlet and the filter chain to the frontend. The dashboard shows the name and, when `imageUrl` is set, `<img src="http://localhost:8081{imageUrl}">`.

## Register with image upload

`POST /api/auth/register` is `permitAll`. No JWT is required. The body is `multipart/form-data`.

```mermaid
flowchart TD
  FE["Register tab<br/>FormData: name, email, password, optional image file"]
  CHAIN["DelegatingFilterProxy → FilterChainProxy<br/>JwtAuthorizationFilter sees no Bearer<br/>AuthorizationFilter permitAll"]
  C["UserController.register"]
  LOOKUP["UserService.register<br/>findByEmail"]
  FILE["If image is present:<br/>must be image/*, save uploads/uuid.ext,<br/>imagePath = /uploads/uuid.ext"]
  SAVE["BCrypt password<br/>UserJpaRepository.save"]
  RES["201 { name, imageUrl }<br/>409 email exists · 400 not an image"]

  FE --> CHAIN --> C --> LOOKUP --> FILE --> SAVE --> RES
```

`WebConfig` maps `/uploads/**` to `file:uploads/`, so the saved file is served as a static resource on the path returned as `imageUrl`.

## Login

`POST /api/auth/login` is JSON and `permitAll`.

```mermaid
flowchart LR
  FE["Frontend login<br/>{ email, password }"] --> C["UserController.login"]
  C --> S["UserService.authenticate<br/>findByEmail + BCrypt match"]
  S --> T["JwtUtil<br/>access token type=access<br/>refresh token type=refresh"]
  T --> RES["200 { token, refreshToken, expiresIn, imageUrl }"]
  RES --> STORE["localStorage token + refreshToken<br/>then GET /api/home"]
```

The frontend stores both tokens, then loads `/api/home` and shows the name and profile photo.

## Refresh token and 401 retry

Access tokens expire (`jwt.access-expiration`). The refresh token is a separate JWT with `type=refresh` and is not accepted as a Bearer access token.

```mermaid
flowchart TD
  CALL["authFetch GET /api/home<br/>Bearer access token"]
  OK{"status"}
  USE["Use the response"]
  REF["POST /api/auth/refresh<br/>{ refreshToken } permitAll"]
  CHECK{"refresh ok?"}
  NEW["Save new token and refreshToken<br/>retry the original request once"]
  CLEAR["Remove both tokens<br/>route guard sends the user to /login"]

  CALL --> OK
  OK -->|not 401| USE
  OK -->|401| REF --> CHECK
  CHECK -->|yes| NEW --> USE
  CHECK -->|no| CLEAR
```

`UserController.refresh` calls `JwtUtil.emailFromRefresh`. That accepts only a non-expired `type=refresh` token, then `tokens()` issues a new access token and a new refresh token. One shared refresh call is reused if several requests get 401 together. Logout only clears `localStorage`; the server does not revoke tokens.
