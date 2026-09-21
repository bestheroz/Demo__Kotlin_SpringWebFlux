# CLAUDE.md

Claude Code(claude.ai/code)가 이 저장소에서 작업할 때 따르는 지침이다.

## 개발 명령

### 빌드/실행
- `./gradlew build` — 빌드
- `./gradlew bootRun` — 실행 (기본 포트 8000)
- `./gradlew assemble` — jar 생성
- configuration cache·build cache·병렬 실행은 `gradle.properties` 에서 켜므로 CI 에서 해당 플래그를 따로 넘기지 않는다

### 코드 품질
- `./gradlew spotlessApply` — ktlint 포맷 적용. 커밋 전 필수
- `./gradlew spotlessCheck` — 포맷 검사
- `./gradlew check` — spotless 포함 전체 검사

### 테스트
- `./gradlew test` — 전체 테스트
- `./gradlew test --tests "ClassName"` / `--tests "ClassName.methodName"` — 단건 실행
- 현재 `src/test/` 에 테스트 파일이 하나도 없다. "테스트로 확인했다"고 말하려면 테스트를 먼저 작성한다

### 의존성 업데이트
- `./gradlew dependencyUpdates` — 업데이트 리포트 (pre-release 포함)
- `./gradlew versionCatalogUpdate --interactive` → `./gradlew versionCatalogApplyUpdates` 순서로 적용
- 상세 규칙은 `.claude/rules/dependency-catalog.md`

## 전역 컨벤션
- 모든 plugin/library 좌표는 `gradle/libs.versions.toml` 에만 둔다. `build.gradle.kts` 는 `libs.xxx` / `alias(libs.plugins.xxx)` 만 참조한다
- 이 저장소는 새 버전을 먼저 써보려고 만든 데모다. pre-release(M/RC/Beta/Alpha/Preview)를 허용하고 선호한다

## 트랜잭션 경계
- `@Transactional` 은 Service 진입점 한 곳에만 붙인다
- Service → Service 호출에서 양쪽 모두 `@Transactional` 금지 — 중첩 트랜잭션이 생긴다
- 헬퍼 Service 에 `@Transactional` 금지 — 트랜잭션 경계가 두 곳으로 갈라진다
- private 메서드에 `@Transactional` 금지 — Spring AOP 가 프록시할 수 없어 조용히 무시된다

## 함정
- MySQL 은 boolean 을 byte 로 돌려주고 enum·Map 도 그대로 매핑되지 않는다. 새 타입을 쓰면 `R2dbcConfig.kt` 에 커스텀 컨버터를 함께 등록해야 한다
- refresh token 갱신에는 3초 grace period 가 있다. 동시 요청이 서로의 토큰을 무효화하는 것을 막는 장치이니 임의로 줄이거나 제거하지 않는다
- 생성/수정자 정보는 `@CurrentUser` + `OperatorHelper` 로 주입한다. 리액티브 컨텍스트에서는 ThreadLocal 기반 조회가 동작하지 않는다
- 병렬 조회가 필요하면 `coroutineScope` + `async`/`await` 를 쓴다 (`AdminService.updateAdmin` 참고)
- 비밀번호는 `PasswordUtil`(BCrypt) 로만 다룬다

## DB 마이그레이션
- `migration/` 에 `V{n}__{설명}.sql` 형식으로 추가한다. 번호는 기존 최댓값 다음으로 붙인다
- 스키마 변경은 코드 배포 전에 선적용한다

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
- `gradle/`, `build.gradle.kts`, `settings.gradle.kts` — 버전 카탈로그 기반 의존성 관리. `.claude/rules/dependency-catalog.md`
- 모든 `CLAUDE.md` / `.claude/rules/**` — 지시 파일 작성·수정 기준. `.claude/rules/claude-md-maintenance.md`
