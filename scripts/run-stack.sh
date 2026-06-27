#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  scripts/run-stack.sh <baseline|indexes|pagination|main> [--reset-db]

Options:
  baseline    Switch to missing-index-performance-baseline.
  indexes     Switch to add-product-query-indexes.
  pagination  Switch to add-products-pagination.
  main        Switch to main.

  --reset-db  Stop the stack and remove the Postgres volume before starting.

Environment:
  PRODUCT_SEED_COUNT  Defaults to 10000000.
  APP_IMAGE           Defaults to nexus-shopping:<branch-slug>.
  GRADLE_USER_HOME    Defaults to .gradle-local inside the project.
USAGE
}

if [[ $# -lt 1 ]]; then
  usage
  exit 1
fi

option="$1"
shift

reset_db=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --reset-db)
      reset_db=true
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
  shift
done

case "$option" in
  baseline)
    branch="missing-index-performance-baseline"
    ;;
  indexes|index)
    branch="add-product-query-indexes"
    ;;
  pagination|paginated)
    branch="add-products-pagination"
    ;;
  main)
    branch="main"
    ;;
  *)
    echo "Unknown stack option: $option" >&2
    usage
    exit 1
    ;;
esac

project_root="$(git rev-parse --show-toplevel)"
cd "$project_root"

if ! git diff --quiet || ! git diff --cached --quiet || [[ -n "$(git ls-files --others --exclude-standard)" ]]; then
  echo "Working tree is not clean. Commit or stash changes before switching branches." >&2
  git status --short
  exit 1
fi

echo "Switching to branch: $branch"
git switch "$branch"

branch_slug="$(echo "$branch" | tr '/_' '--' | tr -cd '[:alnum:]-')"
export APP_IMAGE="${APP_IMAGE:-nexus-shopping:${branch_slug}}"
export PRODUCT_SEED_COUNT="${PRODUCT_SEED_COUNT:-10000000}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$project_root/.gradle-local}"

echo "Building OCI image with Spring Boot buildpacks: $APP_IMAGE"
./gradlew bootBuildImage --imageName "$APP_IMAGE"

tmp_compose=""
cleanup() {
  if [[ -n "$tmp_compose" && -f "$tmp_compose" ]]; then
    rm -f "$tmp_compose"
  fi
}
trap cleanup EXIT

compose_files=(-f docker-compose.yml)
if ! docker compose config --services | grep -qx app; then
  tmp_compose="$(mktemp "${TMPDIR:-/tmp}/nexus-shopping-compose.XXXXXX.yml")"
  cat > "$tmp_compose" <<'YAML'
services:
  app:
    image: ${APP_IMAGE:-nexus-shopping:local}
    container_name: nexus-shopping-app
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/nexus_shopping
      DB_USERNAME: nexus
      DB_PASSWORD: nexus
      PRODUCT_SEED_COUNT: ${PRODUCT_SEED_COUNT:-10000000}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
YAML
  compose_files+=(-f "$tmp_compose")
fi

if [[ "$reset_db" == true ]]; then
  echo "Resetting Docker Compose stack and Postgres volume..."
  docker compose "${compose_files[@]}" down -v
fi

echo "Starting Docker Compose stack with image: $APP_IMAGE"
docker compose "${compose_files[@]}" up -d postgres app

echo
echo "Stack started."
echo "Health:   http://localhost:8080/actuator/health"
echo "Products: http://localhost:8080/products?categoryId=1&page=0&size=50"
