# Budget Tracker

![CI](https://github.com/mbeltran93/budget-tracker-spring/actions/workflows/ci.yml/badge.svg)

Control de gastos y presupuesto personal. Permite registrar categorias con un
limite mensual (ej. "Comida" $500), cargar gastos del dia a dia y ver en el
dashboard cuanto se lleva gastado del mes, por categoria, con un semaforo
verde/amarillo/rojo segun que tan cerca (o tan pasado) esta cada categoria de
su limite.

## Por que esta app es distinta al resto del portafolio

El resto de mis repos de portafolio son APIs REST "puras" (Spring Boot,
Express, FastAPI, Laravel, .NET, Go...) consumidas por una SPA separada
(React/Angular/Vanilla JS). Esta app muestra deliberadamente el **otro**
estilo de arquitectura que tambien aparece en mi CV junto a Spring MVC y Java
EE: una aplicacion web **server-rendered clasica**.

- Las paginas (dashboard, login, categorias) las renderiza el servidor con
  **Thymeleaf**, no hay build de frontend ni SPA.
- La interactividad del lado del cliente es **jQuery + Bootstrap 5**: el modal
  de alta/edicion de gastos, la validacion antes de enviar el formulario, y el
  refresco del grafico/barras de progreso.
- Conviven **a proposito** dos estilos de interaccion en el mismo proyecto
  para mostrar ambos:
  - El CRUD de **gastos** es 100% AJAX (`$.ajax` contra unos pocos endpoints
    `@RestController` bajo `/api/expenses/**` y `/api/dashboard/**` que
    devuelven JSON) sin recargar la pagina.
  - El CRUD de **categorias** es el flujo clasico Spring MVC: formulario con
    `th:action`, POST, redirect y recarga de pagina completa.
- Login con **Spring Security tradicional** (`formLogin`, sesion de servidor +
  cookie `JSESSIONID`) para la app web.
- Ademas, y en paralelo, una **API JWT stateless** (`/api/auth/login`,
  `/api/auth/oauth/google`) pensada para un futuro cliente movil, mas **login
  social** (Google y Facebook) para la app web via Spring Security OAuth2
  Client/Login. Los tres metodos de autenticacion conviven sin pisarse (ver
  "Autenticacion" mas abajo).

## Dominio

| Entidad    | Campos |
|------------|--------|
| `User`     | id, name, email, password (BCrypt) |
| `Category` | id, name, monthlyLimit |
| `Expense`  | id, userId, categoryId, amount, description, date |

El semaforo de presupuesto (`BudgetCalculationService`) es la unica regla de
negocio no trivial del dominio: `OK` (verde) si se lleva gastado menos del
50% del limite, `WARNING` (amarillo) entre 50% y 99%, `OVER` (rojo) al llegar
o pasarse del 100%. Esta exhaustivamente cubierta por tests unitarios.

## Arquitectura

```
controller/mvc   -> paginas Thymeleaf (dashboard, categorias, login, registro)
controller/rest  -> @RestController internos (/api/expenses, /api/dashboard,
                     /api/categories, /api/auth) -> JSON
controller/advice-> @ControllerAdvice (vistas de error) y @RestControllerAdvice (JSON de error)
service          -> UserService, CategoryService, ExpenseService, BudgetCalculationService,
                     JwtService, UserProvisioningService, CustomOidcUserService,
                     CustomOAuth2UserService, GoogleIdTokenVerificationService
repository       -> Spring Data JPA (UserRepository, CategoryRepository, ExpenseRepository)
entity           -> User, Category, Expense (JPA)
dto              -> request/response de los endpoints REST y vistas de solo lectura (CategoryBudgetView)
security         -> AppUserPrincipal (UserDetails), CurrentUser (helper de usuario actual),
                     JwtAuthFilter, AppOidcUser/AppOAuth2User (principales de login social)
config           -> SecurityConfig (dos SecurityFilterChain), PasswordEncoderConfig
```

Stack: **Java 21, Spring Boot 3.3, Spring MVC + Thymeleaf, Spring Data JPA,
Spring Security (form login + OAuth2 Login + JWT), PostgreSQL, Flyway**. Bean
Validation (`@Valid`) en el servidor en ambos lados (formularios MVC y JSON
REST), mas una validacion basica en el cliente con jQuery antes de pegarle al
servidor.

## Autenticacion

Hay tres formas de autenticarse, resueltas por **dos `SecurityFilterChain`**
(`SecurityConfig`), elegidas por path (`@Order` + `securityMatcher`):

| Chain | Rutas | Sesion/CSRF | Como se autentica |
|-------|-------|-------------|--------------------|
| 1 (`@Order(1)`) | `/api/**` | Sin CSRF. `SessionCreationPolicy.IF_REQUIRED` (lee la sesion si existe, no la fuerza) | Cookie de sesion (cliente jQuery, ya logueado via `/login`) **o** `Authorization: Bearer <jwt>` (cliente movil). `/api/auth/**` es publico. |
| 2 (`@Order(2)`) | todo lo demas (`/dashboard`, `/categories`, `/login`, `/register`, estaticos) | Sesion + CSRF, como siempre | `formLogin` (email/password) **o** OAuth2 Login (Google/Facebook), si hay credenciales configuradas |

Importante: `/api/**` usa `IF_REQUIRED` y no `STATELESS` a proposito. Con
`STATELESS`, Spring Security le pone un `NullSecurityContextRepository` a esa
chain y deja de leer la sesion existente -lo que rompria las llamadas
`$.ajax` del dashboard, que dependen de la cookie `JSESSIONID`-. Con
`IF_REQUIRED`, si ya hay sesion (cliente web) se usa esa; si no hay sesion
(cliente movil), el `JwtAuthFilter` autentica por Bearer token. Ambos casos
estan cubiertos por tests de integracion reales (`AuthRestControllerIT`,
`SessionRoutesUnaffectedByJwtIT`).

Por el mismo motivo, CSRF quedo deshabilitado para toda la chain `/api/**`
(el cliente movil nunca va a mandar un token CSRF). Esto es un cambio de
comportamiento intencional respecto de versiones previas de este repo, donde
`/api/expenses` sin token CSRF devolvia 403; ese test se actualizo.

### API JWT (pensada para un cliente movil)

```bash
# 1) Login: devuelve un JWT
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@budgettracker.com","password":"demo1234"}'
# -> {"token":"eyJ..."}

# 2) Usarlo como Bearer en cualquier endpoint de /api/**
curl http://localhost:8080/api/expenses \
  -H "Authorization: Bearer eyJ..."

curl http://localhost:8080/api/dashboard/summary \
  -H "Authorization: Bearer eyJ..."

curl http://localhost:8080/api/categories \
  -H "Authorization: Bearer eyJ..."
```

El JWT lleva el email como `subject` y el id de usuario como claim
(`userId`), firmado HS256 con `app.jwt.secret` (variable de entorno
`JWT_SECRET`) y expira segun `app.jwt.expiration-ms` (`JWT_EXPIRATION_MS`,
default 1 hora). El secreto por defecto en `application.yml` es solo para
que el repo arranque out-of-the-box como portfolio demo: **en produccion hay
que sobreescribirlo con la variable de entorno `JWT_SECRET`**.

### Login social (Google / Facebook)

En la pagina de `/login` hay botones "Iniciar sesion con Google" / "...con
Facebook" que llevan a los endpoints estandar de Spring Security
(`/oauth2/authorization/google` y `/oauth2/authorization/facebook`). El
primer login con cada proveedor crea un `User` local (usando el email del
proveedor); si ese email ya existia (por ejemplo, registrado antes con
password, o logueado antes con el otro proveedor), se **vincula** a esa misma
cuenta en vez de duplicarla (`UserProvisioningService`, keyed por
`User.email`, que es `UNIQUE`). Las cuentas creadas asi tienen un password
placeholder aleatorio (nunca usable para login por formulario).

Para un cliente **movil** (Flutter + `google_sign_in`), hay un endpoint
separado que no pasa por el flujo de redirect:

```bash
curl -X POST http://localhost:8080/api/auth/oauth/google \
  -H "Content-Type: application/json" \
  -d '{"idToken":"<id_token que entrega el SDK google_sign_in>"}'
# -> {"token":"eyJ..."}  (un JWT igual al de /api/auth/login)
```

El id_token se verifica server-side (firma, issuer y audience) contra el
`GOOGLE_MOBILE_CLIENT_ID` -el client id de la app Android/iOS, que es
**distinto** del client id web-, usando `GoogleIdTokenVerifier` de la
libreria oficial de Google. Si falla la verificacion, responde 401 JSON
(mismo formato `ApiError` que el resto de la API).

### Como conseguir tus propias credenciales OAuth

Sin estas variables de entorno seteadas, el login social queda deshabilitado
de forma prolija: los botones de Google/Facebook en `/login` simplemente no
van a funcionar (o van a dar un error claro al clickearlos), pero **la app
arranca igual** y el resto de los tests/flujos no se ven afectados. Eso es
esperado, no un bug.

**Google** (web + movil), en [Google Cloud Console](https://console.cloud.google.com/)
-> APIs & Services -> Credentials -> Create Credentials -> OAuth client ID:

1. Un client ID tipo **Web application**, con *Authorized redirect URI*
   `http://localhost:8080/login/oauth2/code/google` -> este es tu
   `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` (login social de la web).
2. Un segundo client ID tipo **Android** o **iOS** (segun la app movil) ->
   este es tu `GOOGLE_MOBILE_CLIENT_ID` (solo el client id, sin secret: se
   usa unicamente como *audience* esperado al verificar el id_token que
   manda el SDK `google_sign_in` del cliente movil).

**Facebook**, en [Facebook for Developers](https://developers.facebook.com/)
-> My Apps -> Create App -> agregar el producto **Facebook Login** ->
Settings -> *Valid OAuth Redirect URIs*:
`http://localhost:8080/login/oauth2/code/facebook` -> el client id/secret de
la app son tu `FACEBOOK_CLIENT_ID` / `FACEBOOK_CLIENT_SECRET`.

Variables de entorno a setear (las cinco son opcionales, cada una habilita
su propia pieza por separado):

```bash
export GOOGLE_CLIENT_ID=...
export GOOGLE_CLIENT_SECRET=...
export FACEBOOK_CLIENT_ID=...
export FACEBOOK_CLIENT_SECRET=...
export GOOGLE_MOBILE_CLIENT_ID=...
```

## Como correrlo

### Con Docker (recomendado, app + Postgres)

```bash
docker compose up --build
```

Abrir `http://localhost:8080/login`. Usuario de demo (sembrado por Flyway):

- **Email:** `demo@budgettracker.com`
- **Password:** `demo1234`

### Local, sin Docker

Necesita un Postgres accesible via las variables `DB_URL` / `DB_USERNAME` /
`DB_PASSWORD` (por defecto apunta a `jdbc:postgresql://localhost:5432/budget_tracker`
con usuario `budget_user` / `budget_pass`, igual que `docker-compose.yml`).

```bash
mvn spring-boot:run
```

## Tests

### Unitarios + integracion (JUnit 5, Mockito, MockMvc)

```bash
./scripts/run-integration-tests.sh    # o scripts\run-integration-tests.ps1 en PowerShell
```

Levanta un Postgres descartable con `docker run`, corre `mvn test` (71 tests:
33 unitarios de servicios + 38 de integracion con `@SpringBootTest` +
`MockMvc` contra ese Postgres real, Flyway incluido) y lo apaga al final.

- `BudgetCalculationServiceTest`: cubre 0%, 50%, 100% y sobregirado (150%),
  mas los limites exactos del semaforo y el caso de categoria sin limite.
- `CategoryServiceTest`, `ExpenseServiceTest`, `UserServiceTest`,
  `UserProvisioningServiceTest`: logica de servicio con Mockito (duplicados,
  ownership de gastos por usuario, alta/vinculacion de usuarios OAuth, etc).
- `*ControllerIT`: MockMvc contra el contexto Spring completo -- login real
  (`demo@budgettracker.com`), redirects de usuario anonimo, CRUD completo de
  `/api/expenses` (crear/editar/listar/borrar, 400 en validacion, 404 en
  ownership), el flujo de categorias con recarga de pagina, y el registro de
  usuarios nuevos.
- `AuthRestControllerIT`: login JWT exitoso/fallido, `/api/expenses` y
  `/api/categories` con Bearer token, sin token y con token invalido, mas el
  intercambio `POST /api/auth/oauth/google` (con `GoogleIdTokenVerificationService`
  mockeado, ya que verificar un id_token real necesita credenciales e
  infraestructura de Google que no estan disponibles en CI).
- `SessionRoutesUnaffectedByJwtIT`: confirma que `/dashboard` y `/categories`
  siguen exigiendo sesion y CSRF exactamente igual que antes de introducir
  JWT, y que un header `Authorization: Bearer ...` no los afecta para nada.
- `OAuth2LoginProvisioningIT`: alta de usuario nuevo por login social (una
  sola fila), login repetido con el mismo email (no duplica), vinculacion de
  un email que ya existia por registro con password, y el cableado de
  principal (`AppOidcUser`/`AppOAuth2User` -> `CurrentUser.id()`) probado
  contra `/dashboard` y `/categories` reales.

> Nota: los tests de integracion no usan Testcontainers porque el Docker
> Desktop de la maquina donde se armo este repo bloquea el acceso directo a
> la API del engine por npipe para clientes que no son el propio CLI de
> `docker` (asi que `docker run`/`docker compose` funcionan perfecto, pero la
> libreria `docker-java` que usa Testcontainers no). El script de arriba logra
> lo mismo (Postgres real, descartable, Flyway corriendo) con un `docker run`
> plano. En GitHub Actions (Linux) esto no aplica: el workflow de CI usa un
> contenedor de servicio de Postgres normal.

### End-to-end (Playwright, navegador real)

```bash
./scripts/run-e2e.sh    # o scripts\run-e2e.ps1 en PowerShell
```

Un solo comando: levanta app + Postgres con `docker compose up --build`,
espera a que responda, corre la suite de Playwright (`e2e/`) contra la app
real corriendo en Docker, y al final apaga y borra los contenedores + el
volumen. La suite simula un usuario real:

login -> ver dashboard con los datos sembrados -> agregar un gasto por el
modal AJAX -> confirmar que aparece en la tabla sin recargar y que el total,
la barra de progreso y el grafico de Chart.js se actualizan solos -> editar
ese gasto -> borrarlo -> crear una categoria nueva (flujo con recarga de
pagina) -> borrarla -> logout. Mas un test de que una ruta protegida redirige
a `/login` si no hay sesion.

### Coleccion de Postman

`postman/budget-tracker-rest-internos.postman_collection.json` cubre los
endpoints REST internos (`/api/expenses/*`, `/api/dashboard/summary`): login
real con sesion, CRUD completo de un gasto, 400 en validacion y 404 en
ownership. Nota: `/api/**` ya no exige token CSRF (ver "Autenticacion" mas
arriba), asi que la coleccion solo necesita la cookie de sesion tras el
login. Se puede correr con Newman:

```bash
docker compose up -d
npx newman run postman/budget-tracker-rest-internos.postman_collection.json
```

## CI

`.github/workflows/ci.yml` corre en cada push/PR a `main`: `mvn test` (con un
Postgres de servicio), build de la imagen Docker, y un analisis estatico con
CodeQL (Java).

## Limitaciones conocidas

- No hay recuperacion de contrasena ni verificacion de email: el registro
  crea la cuenta directo.
- El limite mensual de una categoria aplica igual todos los meses (no hay
  historico de limites distintos por mes).
- La suite de Playwright corre un solo navegador (Chromium) y un solo
  worker, pensada para validar el flujo funcional, no para cross-browser.
- Los tests de integracion no son Testcontainers (ver nota mas arriba); en un
  entorno donde la API de Docker sea accesible directamente, migrar a
  Testcontainers es un cambio chico y contenido en `AbstractIntegrationTest`.
- El login social (Google/Facebook) y el intercambio movil de id_token no se
  pueden probar de punta a punta contra la infraestructura real de esos
  proveedores sin credenciales y sin una app movil real: los tests cubren la
  logica de alta/vinculacion de usuarios y el cableado del principal
  directamente (`UserProvisioningServiceTest`, `OAuth2LoginProvisioningIT`),
  y el endpoint `/api/auth/oauth/google` con el verificador de Google
  mockeado (`AuthRestControllerIT`). Sin las variables de entorno
  correspondientes seteadas, esas piezas quedan deshabilitadas de forma
  prolija (ver "Como conseguir tus propias credenciales OAuth").
