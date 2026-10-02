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
  cookie `JSESSIONID`), no JWT ni nada stateless.

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
controller/rest  -> @RestController internos (/api/expenses, /api/dashboard) -> JSON
controller/advice-> @ControllerAdvice (vistas de error) y @RestControllerAdvice (JSON de error)
service          -> UserService, CategoryService, ExpenseService, BudgetCalculationService
repository       -> Spring Data JPA (UserRepository, CategoryRepository, ExpenseRepository)
entity           -> User, Category, Expense (JPA)
dto              -> request/response de los endpoints REST y vistas de solo lectura (CategoryBudgetView)
security         -> AppUserPrincipal (UserDetails), CurrentUser (helper de sesion)
config           -> SecurityConfig, PasswordEncoderConfig
```

Stack: **Java 21, Spring Boot 3.3, Spring MVC + Thymeleaf, Spring Data JPA,
Spring Security (form login), PostgreSQL, Flyway**. Bean Validation
(`@Valid`) en el servidor en ambos lados (formularios MVC y JSON REST), mas
una validacion basica en el cliente con jQuery antes de pegarle al servidor.

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

Levanta un Postgres descartable con `docker run`, corre `mvn test` (29 tests
unitarios de servicios + 20 de integracion con `@SpringBootTest` + `MockMvc`
contra ese Postgres real, Flyway incluido) y lo apaga al final.

- `BudgetCalculationServiceTest`: cubre 0%, 50%, 100% y sobregirado (150%),
  mas los limites exactos del semaforo y el caso de categoria sin limite.
- `CategoryServiceTest`, `ExpenseServiceTest`, `UserServiceTest`: logica de
  servicio con Mockito (duplicados, ownership de gastos por usuario, etc).
- `*ControllerIT`: MockMvc contra el contexto Spring completo -- login real
  (`demo@budgettracker.com`), redirects de usuario anonimo, CRUD completo de
  `/api/expenses` (crear/editar/listar/borrar, 400 en validacion, 404 en
  ownership, 403 sin token CSRF), el flujo de categorias con recarga de
  pagina, y el registro de usuarios nuevos.

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
endpoints REST internos (`/api/expenses/*`, `/api/dashboard/summary`):
login real + manejo de CSRF (Spring Security clasico usa sesion, no Bearer
tokens), CRUD completo de un gasto, 400 en validacion y 404 en ownership.
Se puede correr con Newman:

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
