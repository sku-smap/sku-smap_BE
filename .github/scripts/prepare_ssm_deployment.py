"""SQL은 이미지로 전달하고 SSM에는 크기가 제한된 실행 명령만 생성한다."""

import json
import os
from pathlib import Path
import shlex


def build_parameters(environment):
    keys = ("AWS_REGION", "ECR_REGISTRY", "ECR_REPOSITORY", "IMAGE_TAG")
    if any(not environment.get(key) for key in keys):
        raise ValueError("배포에 필요한 이미지·리전 설정이 없습니다.")

    commands = [
        "set -eu",
        "mkdir -p /home/ubuntu/smap",
        "cd /home/ubuntu/smap",
        "test -f .env.prod",
    ]
    commands += [f"export {key}={shlex.quote(environment[key])}" for key in keys]
    commands += [
        'aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR_REGISTRY"',
        'APP_IMAGE="$ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG"',
        'docker pull "$APP_IMAGE"',
        # 앱을 실행하지 않고 배포 파일만 꺼낸다. 실패 시에도 임시 컨테이너를 제거한다.
        'STAGING_DIR=$(mktemp -d /home/ubuntu/smap/.deploy.XXXXXX)',
        'ARTIFACT_CONTAINER=$(docker create "$APP_IMAGE")',
        'trap \'docker rm "$ARTIFACT_CONTAINER" >/dev/null 2>&1 || true\' EXIT',
        'docker cp "$ARTIFACT_CONTAINER:/opt/smap/deploy/." "$STAGING_DIR/"',
        'test -f "$STAGING_DIR/docker-compose-prod.yaml"',
        'test -f "$STAGING_DIR/db/init/02-campus-schema.sql"',
        'test -n "$(find "$STAGING_DIR/db/init" -maxdepth 1 -name \'*.sql\' -print -quit)"',
        # 이전 SQL은 삭제하지 않고 보관한다. 서버의 .env.prod는 이미지로 덮어쓰지 않는다.
        'mkdir -p db',
        'if [ -d db/init ]; then mv db/init "$STAGING_DIR/previous-init"; fi',
        'mv "$STAGING_DIR/db/init" db/init',
        'mv "$STAGING_DIR/docker-compose-prod.yaml" docker-compose-prod.yaml',
        'docker compose --env-file .env.prod -f docker-compose-prod.yaml up -d --wait --wait-timeout 180 postgres',
        # Flyway 도입 전 기존 DB에만 초기 SQL을 적용한다. 이후 갱신은 버전 마이그레이션이 담당한다.
        'MIGRATION_HISTORY=$(docker compose --env-file .env.prod -f docker-compose-prod.yaml exec -T postgres '
        'sh -c \'psql -v ON_ERROR_STOP=1 -At -U "$POSTGRES_USER" -d "$POSTGRES_DB" '
        '-c "SELECT to_regclass(\\$\\$public.flyway_schema_history\\$\\$);"\')',
        'for f in db/init/*.sql; do [ -z "$MIGRATION_HISTORY" ] || break; [ -e "$f" ] || continue; '
        'docker compose --env-file .env.prod -f docker-compose-prod.yaml exec -T postgres '
        'sh -c \'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"\' < "$f"; done',
        'docker compose --env-file .env.prod -f docker-compose-prod.yaml up -d --wait --wait-timeout 180',
        'docker compose --env-file .env.prod -f docker-compose-prod.yaml ps',
    ]
    parameters = {"commands": commands, "executionTimeout": ["600"]}
    if len(json.dumps(parameters).encode("utf-8")) > 48 * 1024:
        raise ValueError("SSM 파라미터가 너무 큽니다. SQL 등 파일 내용을 직접 포함하면 안 됩니다.")
    return parameters


if __name__ == "__main__":
    Path("ssm-parameters.json").write_text(json.dumps(build_parameters(os.environ)), encoding="utf-8")
