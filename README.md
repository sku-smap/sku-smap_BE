# Smap API

Java 17과 Spring Boot 기반의 API 프로젝트입니다. 현재 예제 생성·조회 도메인, 공통 응답·예외 처리, 로깅, AI 호출용 서비스, PostgreSQL·Redis 개발 환경을 구성했습니다.

로그인·회원·이미지 업로드 기능은 없으며, Spring Security·JWT·OAuth2·Flyway·QueryDSL은 사용하지 않습니다.

## 기술 구성

| 구분 | 구성 |
|---|---|
| 실행 환경 | Java 17, Gradle Wrapper |
| 프레임워크 | Spring Boot 3.5.3, Spring Cloud OpenFeign |
| 데이터 접근 | Spring Data JPA, Hibernate |
| DB | PostgreSQL 17 |
| 캐시 인프라 | Redis 7, `RedisTemplate<String, String>` |
| API 문서 | Springdoc OpenAPI 2.7.0, Swagger UI |
| 로깅 | Spring Boot 기본 콘솔 로그, AOP 요청 시간 기록, P6Spy |
| 모니터링 | Actuator, Micrometer Prometheus 지표 |
| AI | OpenFeign 기반 OpenAI 호출 서비스 |
| 개발 지원 | Lombok, DevTools |
| 배포 파일 | Dockerfile, Docker Compose, GitHub Actions |

Redis는 공용 인프라로 준비되어 있으며 현재 예제 생성·조회에서는 사용하지 않습니다. Spring Retry 등의 의존성도 포함되어 있지만 별도 업무 기능은 구현되어 있지 않습니다.

## 프로젝트 구조

```text
smap/
├── .github/
│   ├── workflows/                  # CI / CD 정의
│   ├── ISSUE_TEMPLATE/
│   └── pull_request_template.md
├── gradle/wrapper/
├── src/
│   ├── main/
│   │   ├── java/com/smap/
│   │   │   ├── SmapApplication.java
│   │   │   ├── domain/
│   │   │   │   ├── graph/           # 노드·간선 조회
│   │   │   │   ├── place/           # 장소 매핑
│   │   │   │   └── route/           # 경로 탐색·안내 API
│   │   │   └── global/
│   │   │       ├── aop/             # API 호출 경로·처리 시간 기록
│   │   │       ├── common/          # 공통 엔티티·응답·예외 처리
│   │   │       ├── config/          # Web, Redis, Swagger, P6Spy
│   │   │       └── external/ai/     # AI client·config·dto·properties·service
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── application-dev.yaml
│   │       ├── application-prod.yaml
│   │       ├── application-test.yaml
│   │       └── spy.properties
│   └── test/
│       ├── java/com/smap/SmapApplicationTests.java
│       └── resources/application-test.yaml
├── .env.example
├── build.gradle
├── settings.gradle
├── Dockerfile
├── docker-compose-dev.yaml
└── docker-compose-prod.yaml
```

새 기능은 `domain/{기능명}` 아래에 컨트롤러·서비스·리포지토리·엔티티·DTO를 구성합니다. 컨트롤러는 요청·응답을 담당하고, 비즈니스 로직과 AI 프롬프트 구성은 서비스에서 처리합니다.

## 로컬 실행

Java 17과 실행 중인 Docker Desktop이 필요합니다. 아래 명령은 프로젝트 루트의 PowerShell에서 실행합니다.

### 1. 환경 변수 준비

처음 실행할 때만 복사합니다. 기존 `.env`가 있으면 유지합니다.

```powershell
Copy-Item .env.example .env
```

`.env`에서 DB·Redis 접속 정보와 비밀번호를 설정합니다.

```dotenv
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
DB_HOST=localhost
DB_PORT=5432
DB_NAME=smap
DB_USER=smap
DB_PASSWORD=1234
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=1234
OPENAI_API_KEY=
```

`1234`는 로컬 개발 예시입니다. 운영 비밀번호는 별도로 설정합니다. `.env`와 `.env.prod`는 Git 제외 대상입니다.

### 2. PostgreSQL·Redis 실행

```powershell
docker compose --env-file .env -f docker-compose-dev.yaml up -d
docker compose --env-file .env -f docker-compose-dev.yaml ps
```

PostgreSQL은 `localhost:5432`, Redis는 `localhost:6379`에 연결됩니다. 초기 PostgreSQL 사용자·DB·비밀번호는 `.env`에서 읽습니다. 기존 PostgreSQL 볼륨이 있으면 `.env`의 비밀번호 변경만으로 기존 DB 비밀번호가 바뀌지는 않습니다.

### 3. 앱 실행

```powershell
.\gradlew.bat bootRun
```

macOS/Linux에서는 `./gradlew bootRun`을 사용합니다. `spring-dotenv`가 실행 작업 디렉터리의 `.env`를 읽으므로 IDE의 작업 디렉터리도 프로젝트 루트로 설정합니다. YAML의 파일 import는 사용하지 않습니다. 설정에서는 `${DB_HOST}`처럼 값을 참조합니다.

현재 사용하는 `spring-dotenv:4.0.0`에 맞춰 `SPRING_PROFILES_ACTIVE`도 공통 YAML에서 명시적으로 연결합니다. OS·Docker에서 주입한 환경 변수는 `.env` 값보다 우선하며, `.env`가 없는 환경에서는 주입된 환경 변수를 사용합니다. [라이브러리 소스](https://github.com/paulschwarz/spring-dotenv/tree/v4.0.0)

### 4. API 문서 확인

- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

`/`에는 화면이나 컨트롤러가 없습니다. API 확인은 Swagger를 사용합니다.

인프라 종료:

```powershell
docker compose --env-file .env -f docker-compose-dev.yaml down
```

데이터는 PostgreSQL·Redis의 Docker 명명 볼륨에 보관됩니다. 일반 `down`은 볼륨을 삭제하지 않습니다.

## 프로필과 DB 관리

`application.yaml`의 공통 설정에 활성 프로필 설정을 덮어씁니다.

| 설정 파일 | 적용 범위 | DB 테이블 관리 | SQL 로그 | Swagger |
|---|---|---|---|---|
| `src/main/resources/application.yaml` | 공통 | 기본값 `validate` | P6Spy 활성 | 활성 |
| `src/main/resources/application-dev.yaml` | `dev` | `update` | Hibernate·P6Spy 활성 | 활성 |
| `src/main/resources/application-prod.yaml` | `prod` | `validate` | Hibernate·P6Spy 비활성 | 비활성 |
| `src/main/resources/application-test.yaml` | `test` 기본 설정 | `create-drop` | 비활성 | 공통 설정 상속 |
| `src/test/resources/application-test.yaml` | 자동 테스트 | H2 메모리 DB, `create-drop` | 비활성 | 활성 |

- 개발: `.env`의 `SPRING_PROFILES_ACTIVE=dev`를 사용합니다.
- 운영: 운영 Compose가 `SPRING_PROFILES_ACTIVE=prod`를 지정합니다.
- 테스트: 테스트 클래스의 `@ActiveProfiles("test")`로 선택합니다. 테스트 리소스에는 H2와 테스트용 접속 설정이 들어 있습니다.
- 별도 프로필 없이 실행하면 공통 설정만 적용되며 개발용 `update`는 적용되지 않습니다.

SQL 마이그레이션 도구와 마이그레이션 파일은 없습니다. 개발에서는 Hibernate가 엔티티에 따라 테이블을 생성·변경합니다. `update`가 사용하지 않는 테이블을 자동 삭제하는 것은 아닙니다.

운영은 구조 검증만 하므로 첫 배포 전에 테이블을 생성해야 합니다. 이후 엔티티 구조가 바뀌면 운영 DB에 직접 SQL을 적용한 뒤 배포합니다.

`BaseTimeEntity`는 생성·수정 시간을 관리하고, `BaseEntity`는 소프트 삭제 필드를 추가합니다.

## 현재 API

모든 API는 애플리케이션 인증 없이 호출할 수 있습니다.

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/v1/routes` | 노드 ID와 이동 조건으로 경로·장소·사진 URL 조회 |
| GET | `/actuator/health` | 상태 확인. 상세 정보 숨김 |
| GET | `/actuator/prometheus` | Prometheus 수집용 지표 |

API는 다음 공통 응답 형식을 사용합니다. 상세 경로 응답은 `docs/ROUTING.md`를 참고합니다.

```json
{
  "success": true,
  "code": "200",
  "message": "요청이 성공했습니다.",
  "data": { "nodes": [], "segments": [], "totalSteps": 0 }
}
```

`GlobalExceptionHandler`는 Spring MVC의 `ResponseEntityExceptionHandler`를 기반으로 HTTP 상태와 표준 헤더를 유지하면서 공통 JSON 오류 응답을 반환합니다. 도메인 예외는 해당 도메인의 코드·상태를 사용합니다.

| 상태 | 처리 대상 |
|---|---|
| 400 | 필수 요청 값 누락, 파라미터 타입 오류, JSON 본문 오류, 입력 검증 실패 |
| 404 | 없는 경로·정적 리소스, 도메인에서 찾지 못한 데이터 |
| 405 | 지원하지 않는 HTTP 메서드. `Allow` 헤더 유지 |
| 406 | 제공할 수 없는 응답 형식 (`Accept`) |
| 409 | DB 무결성 제약 또는 낙관적 잠금 충돌 |
| 413 | 업로드 크기 초과 |
| 415 | 지원하지 않는 요청 본문 형식 (`Content-Type`) |
| 429 | 명시적으로 발생시킨 요청 제한 오류. `Retry-After` 등 지정 헤더 유지 |
| 502 | Feign 외부 API의 인증·처리 실패 등 |
| 503 | DB 일시 장애, 비동기 요청 시간 초과, 외부 API 연결 실패·429·503 응답 |
| 504 | 외부 API의 HTTP 408·504 응답 |
| 500 | 예상하지 못한 내부 오류, 응답 직렬화·서버 측 검증 등의 서버 오류 |

명시적인 `ResponseStatusException`, `ErrorResponse`, 예외 클래스의 `@ResponseStatus`도 지정된 상태를 유지합니다. 요청 오류는 WARN, 서버·외부 연동 오류는 ERROR와 스택 트레이스로 기록합니다. DB·외부 API 오류의 내부 메시지는 응답에 노출하지 않습니다. 없는 `/` 경로도 Whitelabel 화면 대신 404 JSON으로 응답합니다.

CORS 허용 출처는 `WebConfig`의 `http://localhost:3000`, `http://localhost:5173`입니다. CORS는 서버 API 접근 인증 기능이 아닙니다.

## AI 호출

현재 코드에는 AI 호출용 `AiService`만 있으며, 외부에서 호출할 AI 컨트롤러는 없습니다.

`.env`의 `OPENAI_API_KEY`를 설정하고 앱을 재시작한 뒤, 도메인 서비스에 `AiService`를 주입해 사용합니다.

```java
public String analyze(String input) {
    return aiService.chat("한국어로 간결하게 답변해 주세요.", input);
}
```

현재 `application.yaml`에 설정된 값은 모델 `gpt-4o-mini`, 응답 토큰 설정 `2048`입니다. `AiClient`는 `/v1/chat/completions`로 요청하며 실제 키를 사용한 외부 호출은 별도로 검증해야 합니다.

## 로깅과 모니터링

- Spring Boot 기본 콘솔 로그 형식을 사용합니다. 커스텀 Logback·파일 롤링 설정은 없습니다.
- `LoggingAspect`가 컨트롤러 호출의 HTTP 메서드·경로·처리 시간을 기록합니다.
- P6Spy SQL 로그는 개발에서 활성화하고 운영에서는 비활성화합니다.
- Prometheus 지표 제공만 구성되어 있습니다. 수집용 Prometheus 서버와 Grafana는 Compose에 포함되어 있지 않습니다.
- 운영 공개 시 프록시나 네트워크에서 `/actuator/**` 접근을 제한해야 합니다. 현재 애플리케이션 자체의 인증 제한은 없습니다.

## 빌드와 테스트

```powershell
.\gradlew.bat clean build
```

테스트만 실행:

```powershell
.\gradlew.bat test
```

현재 테스트는 앱 컨텍스트, 인증 없는 예제 생성, Swagger 문서와 인증 설정 부재, 프런트엔드 CORS, 없는 경로의 404 응답을 확인합니다. 예외 처리 전용 테스트는 요청·DB·외부 API 오류의 상태 코드와 공통 응답, 표준 헤더 보존, 내부 오류 메시지 비노출을 검증합니다. 테스트 DB는 H2이며 로컬 PostgreSQL 데이터를 사용하지 않습니다. 실제 PostgreSQL·Redis·AI 연동을 검증하는 테스트는 아닙니다.

실행 가능한 JAR는 `build/libs/smap-0.0.1-SNAPSHOT.jar`, 테스트 보고서는 `build/reports/tests/test/index.html`에 생성됩니다.

앱 설정은 로컬에서 spring-dotenv가 읽는 `.env`, 운영에서 Docker가 주입하는 `.env.prod`로 관리합니다. 앱은 AWS Parameter Store·Secrets Manager를 직접 호출하지 않으며 AWS SDK 의존성도 포함하지 않습니다. 배포의 SSM은 EC2에 실행 명령을 전달하는 별도 용도입니다.

## CodeRabbit 리뷰

루트의 `.coderabbit.yaml`에 한국어·`chill` 리뷰 설정을 정의합니다. `develop`·`main` 대상의 일반 PR은 자동 리뷰하고, Draft PR과 제목에 `[WIP]`가 있는 PR은 건너뜁니다. 기본 브랜치도 CodeRabbit의 자동 리뷰 대상입니다.

리뷰는 버그·데이터 정합성·예외 처리·배포 오류를 중심으로 하며, 변경 요약은 접힌 리뷰 댓글에 표시합니다. 사소한 스타일 지적을 줄이고 시·운세·시퀀스 다이어그램은 비활성화했습니다. GitHub 저장소에 CodeRabbit 앱을 설치하고 접근 권한을 부여해야 실제 리뷰가 실행됩니다. 설정 파일만으로 GitHub 필수 리뷰나 병합 보호 규칙이 생기지는 않습니다.

## 운영 배포 (Amazon ECR → EC2)

앱 이미지는 AWS 프라이빗 ECR에 저장합니다. GitHub Actions는 OIDC 역할로 ECR에 게시하고, EC2는 인스턴스 역할로 이미지를 내려받습니다. 실제 AWS 리소스 생성과 배포 검증은 별도로 필요합니다.

| 파일 | 역할 |
|---|---|
| `Dockerfile` | Java 17 기반 다단계 빌드와 JAR 실행 |
| `docker-compose-dev.yaml` | PostgreSQL·Redis만 실행 |
| `docker-compose-prod.yaml` | ECR의 앱 이미지·PostgreSQL·Redis 실행 |
| `.github/workflows/ci.yaml` | `develop` 대상 PR에서 `clean test` 실행 |
| `.github/workflows/cd.yaml` | `main` push 시 ECR 게시 후 SSM으로 EC2 배포 |

CI에는 PostgreSQL·Redis 서비스도 정의되어 있지만 현재 자동 테스트의 DB는 H2입니다. CD는 Dockerfile에서 JAR를 빌드하며 테스트는 CI에서 별도로 실행합니다.

### 최초 준비

1. 배포할 리전에 프라이빗 ECR 저장소를 만듭니다. 기본 이름은 `smap-api`입니다. 현재 워크플로우는 `latest`를 갱신하므로 해당 태그를 덮어쓸 수 있어야 합니다.
2. EC2에 Docker Compose v2(`docker compose`), AWS CLI, SSM Agent를 준비하고 `/home/ubuntu/smap/.env.prod`를 작성합니다.
3. GitHub OIDC 배포 역할과 EC2 인스턴스 역할에 아래 권한을 부여합니다.
4. 운영 DB에 엔티티와 일치하는 테이블을 준비합니다. `prod`는 테이블을 생성하지 않습니다.
5. 같은 서버의 리버스 프록시에 도메인·HTTPS와 `/actuator/**` 접근 제한을 구성합니다.

| 역할 | 필요한 권한 |
|---|---|
| GitHub OIDC 역할 | `ecr:GetAuthorizationToken`, 대상 저장소의 `ecr:DescribeRepositories`, `ecr:BatchCheckLayerAvailability`, `ecr:GetDownloadUrlForLayer`, `ecr:BatchGetImage`, `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`, `ecr:PutImage`; 대상 EC2에 대한 `ssm:SendCommand`, `ssm:GetCommandInvocation` |
| EC2 인스턴스 역할 | SSM 관리 권한, `ecr:GetAuthorizationToken`, 대상 저장소의 `ecr:BatchGetImage`, `ecr:GetDownloadUrlForLayer`, `ecr:BatchCheckLayerAvailability` |

ECR 인증은 `get-login-password`와 Docker 로그인을 사용합니다. 배포할 때마다 EC2에서도 다시 로그인합니다. [AWS ECR 인증 문서](https://docs.aws.amazon.com/AmazonECR/latest/userguide/registry_auth.html)

### GitHub Actions 설정

| 종류 | 이름 | 값 |
|---|---|---|
| Secret | `AWS_DEPLOY_ROLE_ARN` | GitHub OIDC 배포 역할 ARN |
| Secret | `EC2_INSTANCE_ID` | SSM 배포 대상 EC2 ID |
| Variable | `AWS_REGION` | 기본값 `ap-northeast-2` |
| Variable | `ECR_REPOSITORY` | 기본값 `smap-api` |

ECR 레지스트리 주소는 OIDC로 인증한 AWS 계정에서 자동으로 가져옵니다. ECR 저장소는 워크플로우와 같은 리전에 미리 생성해야 합니다. GitHub 역할의 임시 자격 증명은 EC2로 전달하지 않으며 EC2는 자신의 인스턴스 역할을 사용합니다.

### 운영 환경 변수

서버의 `.env.prod`에는 DB·Redis 설정과 사용할 API 키, 다음 이미지 설정을 작성합니다.

```dotenv
AWS_REGION=ap-northeast-2
ECR_REGISTRY=123456789012.dkr.ecr.ap-northeast-2.amazonaws.com
ECR_REPOSITORY=smap-api
IMAGE_TAG=latest
```

계정 번호는 실제 AWS 계정으로 변경합니다. 자동 배포는 레지스트리·저장소·리전과 `IMAGE_TAG`를 실행 환경에 주입하며, 태그는 해당 커밋 SHA를 사용합니다. `.env.prod`의 값은 수동 실행 시 사용됩니다.

### 배포 흐름

1. `main` push → AWS OIDC 인증 → ECR 저장소 존재 확인 → ECR 로그인.
2. Dockerfile로 이미지 빌드 → `latest`와 커밋 SHA 태그 게시.
3. SSM으로 운영 Compose 파일을 `/home/ubuntu/smap`에 동기화.
4. EC2의 IAM 역할로 ECR 로그인 → 해당 SHA 이미지 pull → `docker compose up -d --wait`로 상태 확인.

여러 배포가 겹치지 않도록 워크플로우를 순서대로 실행합니다. 앱뿐 아니라 DB·Redis도 상태 확인 대상이며, 앱 health가 실패하면 배포도 실패합니다.

### 수동 실행

EC2에서 `.env.prod`의 리전·레지스트리에 맞춰 로그인한 뒤 실행합니다.

```bash
cd /home/ubuntu/smap
aws ecr get-login-password --region ap-northeast-2 \
  | docker login --username AWS --password-stdin 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com
docker compose --env-file .env.prod -f docker-compose-prod.yaml pull app
docker compose --env-file .env.prod -f docker-compose-prod.yaml up -d --wait --wait-timeout 180
```

앱 이미지는 `${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}`를 사용합니다. 앱 컨테이너의 DB·Redis 호스트는 Compose에서 `postgres`, `redis`로 지정합니다. 앱 포트는 호스트의 `127.0.0.1:8080`에만 연결하고 DB·Redis 포트는 호스트에 공개하지 않습니다.

Docker 없이 JAR로 실행하는 경우:

```bash
java -jar build/libs/smap-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

이때 운영 환경 변수는 실행 환경에서 별도로 주입해야 합니다. `spring-dotenv`가 기본으로 읽는 파일은 `.env`이며 `.env.prod`는 자동으로 읽지 않습니다. 운영 Compose의 `env_file`이 `.env.prod`를 환경 변수로 앱에 전달합니다.
