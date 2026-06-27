SHELL := /bin/bash

.DEFAULT_GOAL := help

GRADLE_USER_HOME ?= $(CURDIR)/.gradle-local
APP_IMAGE ?= nexus-shopping:latest
LATEST_BRANCH ?= main
LATEST_IMAGE ?= nexus-shopping:latest
PRODUCT_SEED_COUNT ?= 10000000

DOCKER_HUB_REPO ?= fabianofsc/nexus-shopping
HUB_BASELINE_IMAGE ?= $(DOCKER_HUB_REPO):baseline
HUB_INDEXES_IMAGE  ?= $(DOCKER_HUB_REPO):indexes
HUB_PAGINATION_IMAGE ?= $(DOCKER_HUB_REPO):pagination

HOST ?= localhost
PORT ?= 8080
THREADS ?= 50
RAMP_UP ?= 20
DURATION ?= 120
CATEGORY_ID ?= 1
NAME ?= Product 9999999
PAGE ?= 0
SIZE ?= 50
SCENARIO ?= local
RUN_ID ?= $(shell date +%Y%m%d-%H%M%S)

JMETER_RESULTS_DIR := build/jmeter-results
JMETER_REPORT_DIR := build/jmeter-report
JMETER_HEAP ?= -Xms512m -Xmx2g -Djava.awt.headless=true -Dlog4j2.status=OFF

.PHONY: help
help:
	@printf '%s\n' 'Targets:'
	@printf '%s\n' '  gradle-build       Run ./gradlew build'
	@printf '%s\n' '  gradle-test        Run ./gradlew test'
	@printf '%s\n' '  boot-run           Run the app locally'
	@printf '%s\n' '  image              Build OCI image with Spring Boot buildpacks'
	@printf '%s\n' '  image-latest       Build nexus-shopping:latest from the pagination branch'
	@printf '%s\n' '  compose-up         Start postgres and app with APP_IMAGE'
	@printf '%s\n' '  compose-down       Stop containers'
	@printf '%s\n' '  compose-reset      Stop containers and remove volumes'
	@printf '%s\n' '  compose-ps         Show compose status'
	@printf '%s\n' '  compose-logs       Tail app logs'
	@printf '%s\n' '  health             Check /actuator/health'
	@printf '%s\n' '  stack-baseline     Switch/build/run baseline branch'
	@printf '%s\n' '  stack-indexes      Switch/build/run indexes branch'
	@printf '%s\n' '  stack-pagination   Switch/build/run pagination branch'
	@printf '%s\n' '  stack-latest       Switch/build/run latest branch as nexus-shopping:latest'
	@printf '%s\n' '  load-baseline      Reset DB, run baseline stack, run both JMeter tests'
	@printf '%s\n' '  load-indexes       Reset DB, run indexes stack, run both JMeter tests'
	@printf '%s\n' '  load-pagination    Reset DB, run pagination stack, run both JMeter tests'
	@printf '%s\n' '  load-latest        Reset DB, run latest stack, run both JMeter tests'
	@printf '%s\n' '  jmeter-category    Run category JMeter test against current app'
	@printf '%s\n' '  jmeter-name        Run name JMeter test against current app'
	@printf '%s\n' '  jmeter-all         Run category and name JMeter tests'
	@printf '%s\n' ''
	@printf '%s\n' 'Docker Hub (imagens publicas, sem build local):'
	@printf '%s\n' '  push-baseline      Build e push fabianofsc/nexus-shopping:baseline'
	@printf '%s\n' '  push-indexes       Build e push fabianofsc/nexus-shopping:indexes'
	@printf '%s\n' '  push-pagination    Build e push fabianofsc/nexus-shopping:pagination'
	@printf '%s\n' '  push-all           Build e push as tres imagens'
	@printf '%s\n' '  start-baseline     Reseta banco, sobe baseline do Hub e aguarda health'
	@printf '%s\n' '  start-indexes      Troca para indexes, aguarda health (banco preservado)'
	@printf '%s\n' '  start-pagination   Troca para pagination, aguarda health (banco preservado)'
	@printf '%s\n' '  jmeter-all         Executa os dois planos JMeter contra o app no ar'
	@printf '%s\n' '  jmeter-category    Executa apenas o plano de categoria'
	@printf '%s\n' '  jmeter-name        Executa apenas o plano de nome'
	@printf '%s\n' '  load-hub-baseline  start-baseline + jmeter-all'
	@printf '%s\n' '  load-hub-indexes   start-indexes + jmeter-all'
	@printf '%s\n' '  load-hub-pagination start-pagination + jmeter-all'
	@printf '%s\n' '  hub-reset-baseline Reseta banco sem aguardar health (uso avancado)'
	@printf '%s\n' '  hub-reset-indexes  Reseta banco sem aguardar health (uso avancado)'
	@printf '%s\n' '  hub-reset-pagination Reseta banco sem aguardar health (uso avancado)'

.PHONY: gradle-build gradle-test boot-run boot-jar image
gradle-build:
	rtk env GRADLE_USER_HOME=$(GRADLE_USER_HOME) ./gradlew build

gradle-test:
	rtk env GRADLE_USER_HOME=$(GRADLE_USER_HOME) ./gradlew test

boot-run:
	rtk env GRADLE_USER_HOME=$(GRADLE_USER_HOME) ./gradlew bootRun

boot-jar:
	rtk env GRADLE_USER_HOME=$(GRADLE_USER_HOME) ./gradlew bootJar

image:
	rtk env GRADLE_USER_HOME=$(GRADLE_USER_HOME) ./gradlew bootBuildImage --imageName $(APP_IMAGE)

image-latest:
	rtk bash -lc 'if [[ -n "$$(git status --porcelain)" ]]; then git status --short; exit 1; fi'
	rtk git switch $(LATEST_BRANCH)
	rtk env GRADLE_USER_HOME=$(GRADLE_USER_HOME) ./gradlew bootBuildImage --imageName $(LATEST_IMAGE)

.PHONY: compose-up compose-down compose-reset compose-ps compose-logs health
compose-up:
	rtk env APP_IMAGE=$(APP_IMAGE) PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) docker compose up -d postgres app

compose-down:
	rtk docker compose down

compose-reset:
	rtk docker compose down -v

compose-ps:
	rtk docker compose ps

compose-logs:
	rtk docker compose logs -f app

health:
	rtk curl -s -o /dev/null -w "%{http_code} %{time_total}\n" http://$(HOST):$(PORT)/actuator/health

.PHONY: stack-baseline stack-indexes stack-pagination stack-latest stack-main
stack-baseline:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh baseline

stack-indexes:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh indexes

stack-pagination:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh pagination

stack-latest:
	rtk env APP_IMAGE=$(LATEST_IMAGE) PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh pagination

stack-main:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh main

.PHONY: stack-reset-baseline stack-reset-indexes stack-reset-pagination stack-reset-latest stack-reset-main
stack-reset-baseline:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh baseline --reset-db

stack-reset-indexes:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh indexes --reset-db

stack-reset-pagination:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh pagination --reset-db

stack-reset-latest:
	rtk env APP_IMAGE=$(LATEST_IMAGE) PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh pagination --reset-db

stack-reset-main:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh main --reset-db

.PHONY: wait-health
wait-health:
	rtk bash -lc 'for attempt in {1..120}; do status=$$(curl -s -o /dev/null -w "%{http_code}" http://$(HOST):$(PORT)/actuator/health || true); if [[ "$$status" == "200" ]]; then echo "health OK"; exit 0; fi; sleep 2; done; echo "health check failed"; exit 1'

.PHONY: jmeter-dirs jmeter-category jmeter-name jmeter-all
jmeter-dirs:
	rtk mkdir -p $(JMETER_RESULTS_DIR) $(JMETER_REPORT_DIR)

jmeter-category: jmeter-dirs
	rtk env JVM_ARGS="$(JMETER_HEAP)" scripts/jmeter.sh -n \
	  -t load-tests/jmeter/products-by-category.jmx \
	  -l $(JMETER_RESULTS_DIR)/products-by-category-$(SCENARIO)-$(RUN_ID).jtl \
	  -e -o $(JMETER_REPORT_DIR)/products-by-category-$(SCENARIO)-$(RUN_ID) \
	  -Jthreads=$(THREADS) \
	  -JrampUp=$(RAMP_UP) \
	  -Jduration=$(DURATION) \
	  -Jhost=$(HOST) \
	  -Jport=$(PORT) \
	  -JcategoryId=$(CATEGORY_ID) \
	  -Jpage=$(PAGE) \
	  -Jsize=$(SIZE) \
	  -Jsummariser.interval=10

jmeter-name: jmeter-dirs
	rtk env JVM_ARGS="$(JMETER_HEAP)" scripts/jmeter.sh -n \
	  -t load-tests/jmeter/products-by-name.jmx \
	  -l $(JMETER_RESULTS_DIR)/products-by-name-$(SCENARIO)-$(RUN_ID).jtl \
	  -e -o $(JMETER_REPORT_DIR)/products-by-name-$(SCENARIO)-$(RUN_ID) \
	  -Jthreads=$(THREADS) \
	  -JrampUp=$(RAMP_UP) \
	  -Jduration=$(DURATION) \
	  -Jhost=$(HOST) \
	  -Jport=$(PORT) \
	  '-Jname=$(NAME)' \
	  -Jpage=$(PAGE) \
	  -Jsize=$(SIZE) \
	  -Jsummariser.interval=10

jmeter-all: jmeter-category jmeter-name

.PHONY: load-baseline load-indexes load-pagination load-latest
load-baseline:
	rtk make stack-reset-baseline
	rtk make wait-health
	rtk make jmeter-all SCENARIO=baseline

load-indexes:
	rtk make stack-reset-indexes
	rtk make wait-health
	rtk make jmeter-all SCENARIO=indexes

load-pagination:
	rtk make stack-reset-pagination
	rtk make wait-health
	rtk make jmeter-all SCENARIO=pagination

load-latest:
	rtk make stack-reset-latest
	rtk make wait-health
	rtk make jmeter-all SCENARIO=latest

# --- Docker Hub: build e push ---

.PHONY: push-baseline push-indexes push-pagination push-all
push-baseline:
	rtk bash -lc 'if [[ -n "$$(git status --porcelain)" ]]; then git status --short; exit 1; fi'
	rtk git switch missing-index-performance-baseline
	rtk env GRADLE_USER_HOME=$(GRADLE_USER_HOME) ./gradlew bootBuildImage --imageName $(HUB_BASELINE_IMAGE)
	rtk docker push $(HUB_BASELINE_IMAGE)
	rtk git switch main

push-indexes:
	rtk bash -lc 'if [[ -n "$$(git status --porcelain)" ]]; then git status --short; exit 1; fi'
	rtk git switch add-product-query-indexes
	rtk env GRADLE_USER_HOME=$(GRADLE_USER_HOME) ./gradlew bootBuildImage --imageName $(HUB_INDEXES_IMAGE)
	rtk docker push $(HUB_INDEXES_IMAGE)
	rtk git switch main

push-pagination:
	rtk bash -lc 'if [[ -n "$$(git status --porcelain)" ]]; then git status --short; exit 1; fi'
	rtk env GRADLE_USER_HOME=$(GRADLE_USER_HOME) ./gradlew bootBuildImage --imageName $(HUB_PAGINATION_IMAGE)
	rtk docker push $(HUB_PAGINATION_IMAGE)

push-all: push-baseline push-indexes push-pagination

# --- Docker Hub: iniciar cenarios ---
# start-* = baixa imagem + sobe stack + aguarda health. Pronto para rodar jmeter-all.
# O banco e criado apenas no baseline. Indexes e pagination aproveitam o volume existente.

.PHONY: start-baseline start-indexes start-pagination
start-baseline:
	rtk env APP_IMAGE=$(HUB_BASELINE_IMAGE) PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) docker compose down -v
	rtk env APP_IMAGE=$(HUB_BASELINE_IMAGE) PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) docker compose up -d postgres app
	rtk make wait-health HOST=$(HOST) PORT=$(PORT)

start-indexes:
	rtk docker compose stop app
	rtk env APP_IMAGE=$(HUB_INDEXES_IMAGE) PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) docker compose up -d app
	rtk make wait-health HOST=$(HOST) PORT=$(PORT)

start-pagination:
	rtk docker compose stop app
	rtk env APP_IMAGE=$(HUB_PAGINATION_IMAGE) PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) docker compose up -d app
	rtk make wait-health HOST=$(HOST) PORT=$(PORT)

# hub-reset-* = reseta banco sem aguardar health (uso avancado)
.PHONY: hub-reset-baseline hub-reset-indexes hub-reset-pagination
hub-reset-baseline:
	rtk env APP_IMAGE=$(HUB_BASELINE_IMAGE) PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) docker compose down -v
	rtk env APP_IMAGE=$(HUB_BASELINE_IMAGE) PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) docker compose up -d postgres app

hub-reset-indexes:
	rtk env APP_IMAGE=$(HUB_INDEXES_IMAGE) PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) docker compose down -v
	rtk env APP_IMAGE=$(HUB_INDEXES_IMAGE) PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) docker compose up -d postgres app

hub-reset-pagination:
	rtk env APP_IMAGE=$(HUB_PAGINATION_IMAGE) PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) docker compose down -v
	rtk env APP_IMAGE=$(HUB_PAGINATION_IMAGE) PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) docker compose up -d postgres app

# load-hub-* = start-* + jmeter-all (atalho para rodar tudo em sequencia)
.PHONY: load-hub-baseline load-hub-indexes load-hub-pagination
load-hub-baseline:
	rtk make start-baseline HOST=$(HOST) PORT=$(PORT)
	rtk make jmeter-all SCENARIO=baseline

load-hub-indexes:
	rtk make start-indexes HOST=$(HOST) PORT=$(PORT)
	rtk make jmeter-all SCENARIO=indexes

load-hub-pagination:
	rtk make start-pagination HOST=$(HOST) PORT=$(PORT)
	rtk make jmeter-all SCENARIO=pagination
