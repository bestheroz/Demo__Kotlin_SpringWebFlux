# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Spring WebFlux demo project using reactive programming with R2DBC for database operations. It demonstrates modern Spring Boot development patterns with JWT authentication, role-based access control, and RESTful API design.

## Development Commands

### Build and Run
- `./gradlew build` - Build the project
- `./gradlew bootRun` - Run the application (default port: 8000)
- `./gradlew clean` - Clean build artifacts
- `./gradlew assemble` - Build the jar (configuration cache, build cache and parallel execution come from `gradle.properties`)

### Code Quality
- `./gradlew spotlessCheck` - Check code formatting
- `./gradlew spotlessApply` - Apply code formatting fixes
- `./gradlew check` - Run all checks including spotless

### Testing
- `./gradlew test` - Run all tests
- `./gradlew test --tests "ClassName"` - Run single test class
- `./gradlew test --tests "ClassName.methodName"` - Run single test method

### Dependency Updates
- `./gradlew dependencyUpdates` - Update report including BOM-managed dependencies (pre-releases included)
- `./gradlew versionCatalogUpdate --interactive` - Write update candidates to `gradle/libs.versions.updates.toml`
- `./gradlew versionCatalogApplyUpdates` - Apply only the entries left in that file to the catalog

## Dependency Management

- **Demo policy**: this repo exists to try new versions early and spot changes, so pre-releases (M/RC/Beta/Alpha/Preview) are allowed and preferred. `versionCatalogUpdate` uses the `LATEST` selector and `dependencyUpdates` sets `rejectPreReleases = false`. The `repo.spring.io/milestone` repository stays in `settings.gradle.kts` and `build.gradle.kts`; no snapshot repository is added. The Spring Boot plugin, Kotlin and the Gradle wrapper use the values shared across the Demo repos.
- Every plugin and library coordinate lives in `gradle/libs.versions.toml`. `build.gradle.kts` references only `libs.xxx` / `alias(libs.plugins.xxx)`.
- Coordinates that never had a version stay versionless (`{ module = "g:a" }`) and follow the Spring Boot BOM, so they move with the BOM of whatever Boot plugin version is applied.
- Coordinates that originally carried an explicit version even though the BOM manages them (`r2dbc-mysql`) keep an explicit version on purpose, to run ahead of the BOM, and `versionCatalogUpdate` raises them to the latest version including pre-releases. An explicit version beats the BOM, so adding one to a BOM-managed coordinate is a decision to run ahead of it; otherwise leave it versionless.
- `versionCatalogUpdate` (VCU) only updates entries that carry a version and skips versionless ones. When it rewrites the catalog, comments next to entries may be removed, so keep explanations in `build.gradle.kts` and use only `@pin` / `@keep` in the catalog.
- If the latest version of a coordinate breaks the build and cannot be fixed, lower only that coordinate to the newest working version, mark it `# @pin`, and write the reason in `build.gradle.kts`.
- The Kotlin JVM / Spring plugins share `[versions] kotlin`.
- `gradle.properties` turns on the configuration cache, the build cache and parallel execution, so CI does not pass those flags. `versionCatalogUpdate` is not configuration-cache compatible and prints "Configuration cache entry discarded"; the build still succeeds.

## Architecture Overview

### Core Technologies
- **Java 25** / **Kotlin 2.4.20** with coroutines for reactive programming
- **Spring Boot 4.2.0-M1** / **Spring WebFlux** for reactive web layer
- **Spring Data R2DBC** for reactive database operations
- **MySQL** with R2DBC driver (io.asyncer:r2dbc-mysql)
- **JWT** (com.auth0:java-jwt) for authentication
- **Spring Security** for authorization
- **Spotless** for code formatting (ktlint)

### Project Structure

#### Domain Layer (`demo/`)
- **Controllers**: `AdminController`, `UserController`, `NoticeController`
- **Domain Models**: `Admin`, `User`, `Notice`
- **DTOs**: Separate packages for each domain with create/update/login DTOs
- **Services**: Business logic layer
- **Repositories**: Data access layer with custom implementations

#### Standard Framework (`standard/`)
- **Authentication**: JWT-based auth with `JwtTokenProvider` and `JwtAuthenticationFilter`
- **Security**: Role-based access control with `AuthorityEnum` and `UserTypeEnum`
- **Exception Handling**: Global exception handler with custom exception types
- **Database**: R2DBC configuration with custom converters for enums and complex types
- **Logging**: Custom logging utilities with trace support

### Database Configuration

Uses R2DBC with MySQL. The configuration includes custom converters for:
- Enum to/from String conversion
- Boolean to/from Byte conversion (MySQL compatibility)
- JSON Map serialization/deserialization
- Enum List handling

### Security Model

- JWT-based authentication with access/refresh token pattern
- Role-based authorization with `ADMIN` and `USER` roles
- Public endpoints configured in `SecurityConfig`
- CORS enabled for localhost:3000

### Key Configuration Files

- `application.yml` - Multi-profile configuration (local/sandbox/qa/prod)
- `R2dbcConfig.kt` - Database and custom type converters
- `SecurityConfig.kt` - Security rules and public endpoints
- `OpenApiConfig.kt` - Swagger/OpenAPI documentation

### Development Patterns

1. **Reactive Programming**: Uses Kotlin coroutines with Spring WebFlux
2. **Repository Pattern**: Custom repository implementations for complex queries
3. **DTO Pattern**: Separate DTOs for different operations (create, update, response)
4. **Exception Handling**: Global exception handler with standardized API responses
5. **Logging**: Structured logging with correlation IDs using kotlin-logging-jvm

### Transaction Boundaries

**Correct Patterns**:
- Controller → Service (with `@Transactional`) → Repository
- Service (with `@Transactional`) → Helper Service (without `@Transactional`)
- Service (with `@Transactional`) → Private methods (without `@Transactional`)

**Anti-Patterns to Avoid**:
- Service → Service (both with `@Transactional`) - causes nested transactions
- Helper Service with `@Transactional` - violates single responsibility
- Private methods with `@Transactional` - Spring AOP cannot intercept private methods

### Database Migration

SQL migration scripts located in `migration/` directory:
- `V1__Create_admins.sql`
- `V2__Create_users.sql` 
- `V3__Create_notices.sql`

### API Documentation

Swagger UI available at `/swagger-ui.html` when running the application.

### Environment Profiles

- `local` - Development with external MySQL (Swagger enabled, extended token expiration)
- `sandbox` - Sandbox environment (Swagger enabled)
- `qa` - QA environment (Swagger enabled)
- `prod` - Production (Swagger disabled)

### Important Implementation Notes

1. **Coroutines with R2DBC**: All repository and service methods use `suspend` functions for reactive operations
2. **Async/Await Pattern**: Service layer uses `coroutineScope` with `async`/`await` for parallel operations (see `AdminService.updateAdmin`)
3. **Password Security**: Uses BCrypt with `PasswordUtil` helper for password hashing and validation
4. **Token Renewal**: Implements grace period (3 seconds) for refresh token renewal to handle concurrent requests
5. **Custom R2DBC Converters**: Required for Enum, Boolean (MySQL byte), and Map (JSON) type conversions (see `R2dbcConfig.kt`)
6. **Operator Pattern**: `@CurrentUser` annotation + `OperatorHelper` for handling created/updated user information in reactive context

### CI/CD

GitHub Actions workflows in `.github/workflows/`:
- `test.yml` - Runs spotlessCheck and build on push (excludes sandbox/qa branches)
- `deploy.yml` - Deployment workflow
- `commit-and-push-version.yml` - Version management

## CLAUDE.md 관리 규칙
- 이 파일은 200줄 이하 유지. 매 세션 필요한 내용만 둔다: 빌드/테스트 명령, 전역 컨벤션, 도메인 간 의존 규칙, 함정과 그 이유
- 코드에서 유추 가능한 내용(디렉터리 구조, 의존성 목록, 아키텍처 개요)은 쓰지 않는다
- 지시는 검증 가능한 수준으로 구체적으로 쓴다 (X "포맷 잘 맞춰라" / O "2-space 들여쓰기")
- 특정 도메인/경로에만 해당하는 규칙은 이 파일에 넣지 않는다
  - 도메인이 단일 폴더로 분리돼 있으면 → 해당 폴더의 CLAUDE.md
  - 여러 폴더에 흩어져 있으면 → `.claude/rules/<topic>.md` + `paths` frontmatter
  - 다단계 절차는 → 스킬
- 하위 CLAUDE.md 와 rules 에는 루트 규칙을 재진술하지 않는다. 충돌/중복 발견 시 사용자에게 알린다
- 도메인 규칙을 분리하면 아래 "도메인 인덱스"에 한 줄 추가한다
- 지시 파일을 추가/수정할 때는 변경 전 사용자에게 위치와 내용을 먼저 제안한다

## 도메인 인덱스
<!-- 형식: `경로/` — 한 줄 설명, 규칙 파일 위치 -->
- `demo/` — admin·user·notice 업무 도메인. 레이어별 폴더(controllers/services/repository/dtos)에 흩어져 있어 도메인 규칙은 `.claude/rules/` + paths 로 작성한다. 규칙 파일 없음
- `standard/common/authenticate/`, `standard/common/security/` — JWT 발급·검증과 Operator 인증 컨텍스트. 규칙 파일 없음
- `standard/common/exception/` — 전역 예외 처리와 `ExceptionCode` 기반 응답 체계. 규칙 파일 없음
- `standard/config/` — R2DBC 커스텀 컨버터, 시큐리티, OpenAPI 등 부트 설정. 규칙 파일 없음
- `migration/` — MySQL 스키마 SQL(`V{n}__` 접두사). 규칙 파일 없음
- `gradle/` — 버전 카탈로그 기반 의존성 관리(pre-release 허용 정책 포함). 규칙 파일 없음
- `.claude/rules/claude-md-maintenance.md` — 지시 파일 작성/수정 상세 기준 (paths: 모든 CLAUDE.md / rules)
