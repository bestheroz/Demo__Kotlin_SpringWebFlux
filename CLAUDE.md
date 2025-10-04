# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Spring WebFlux demo project using reactive programming with R2DBC for database operations. It demonstrates modern Spring Boot development patterns with JWT authentication, role-based access control, and RESTful API design.

## Development Commands

### Build and Run
- `./gradlew build` - Build the project
- `./gradlew bootRun` - Run the application (default port: 8000)
- `./gradlew clean` - Clean build artifacts

### Code Quality
- `./gradlew spotlessCheck` - Check code formatting
- `./gradlew spotlessApply` - Apply code formatting fixes
- `./gradlew check` - Run all checks including spotless

### Testing
- `./gradlew test` - Run all tests
- `./gradlew testClasses` - Compile test classes

## Architecture Overview

### Core Technologies
- **Kotlin** with coroutines for reactive programming
- **Spring WebFlux** for reactive web layer
- **Spring Data R2DBC** for reactive database operations
- **MySQL** with R2DBC driver
- **JWT** for authentication
- **Spring Security** for authorization
- **Spotless** for code formatting (ktfmt + ktlint)

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

- `local` - Development with external MySQL
- `sandbox` - Sandbox environment
- `qa` - QA environment
- `prod` - Production (API docs disabled)

### Important Implementation Notes

1. **Coroutines with R2DBC**: All repository and service methods use `suspend` functions for reactive operations
2. **Async/Await Pattern**: Service layer uses `coroutineScope` with `async`/`await` for parallel operations (see `AdminService.updateAdmin`)
3. **Password Security**: Uses BCrypt with `PasswordUtil` helper for password hashing and validation
4. **Token Renewal**: Implements grace period (3 seconds) for refresh token renewal to handle concurrent requests
5. **Custom R2DBC Converters**: Required for Enum, Boolean (MySQL byte), and Map (JSON) type conversions
6. **Operator Pattern**: `@CurrentUser` annotation + `OperatorHelper` for handling created/updated user information in reactive context