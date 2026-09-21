---
paths:
  - "gradle/libs.versions.toml"
  - "build.gradle.kts"
  - "settings.gradle.kts"
  - "gradle.properties"
---

# 버전 카탈로그 운용 기준

<!-- 루트 CLAUDE.md 의 "pre-release 허용" 정책과 "좌표는 카탈로그에만" 규칙을 전제로 한다. 여기서는 그 뒤의 세부만 다룬다. -->

## 버전 표기
- 원래 버전이 없던 좌표는 versionless(`{ module = "g:a" }`)로 둔다. Spring Boot BOM 을 따라가므로 Boot 플러그인 버전만 올리면 같이 움직인다
- BOM 이 관리하는데도 명시 버전을 갖고 있던 좌표(`r2dbc-mysql`)는 의도적으로 명시 버전을 유지한다. BOM 보다 앞서 달리기 위한 선택이다
- 명시 버전은 BOM 을 이긴다. 따라서 BOM 관리 좌표에 버전을 새로 붙이는 것은 "BOM 보다 앞서 가겠다"는 결정이다. 그럴 이유가 없으면 versionless 로 둔다
- Kotlin JVM 플러그인과 Kotlin Spring 플러그인은 `[versions] kotlin` 을 공유한다

## versionCatalogUpdate(VCU)
- VCU 는 버전이 적힌 항목만 갱신하고 versionless 항목은 건너뛴다
- VCU 가 카탈로그를 다시 쓸 때 항목 옆 주석이 지워질 수 있다. 설명은 `build.gradle.kts` 에 쓰고 카탈로그에는 `@pin` / `@keep` 만 남긴다
- VCU 는 configuration cache 와 호환되지 않아 "Configuration cache entry discarded" 를 출력한다. 빌드 자체는 성공하므로 무시해도 된다

## 최신 버전이 깨질 때
- 해당 좌표 하나만 동작하는 최신 버전으로 내리고 `# @pin` 을 단 뒤, 이유를 `build.gradle.kts` 에 적는다
- 다른 좌표를 함께 되돌리지 않는다

## 저장소
- `repo.spring.io/milestone` 은 `settings.gradle.kts` 와 `build.gradle.kts` 양쪽에 유지한다
- snapshot 저장소는 추가하지 않는다
