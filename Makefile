SHELL := /bin/bash

.DEFAULT_GOAL := help

GRADLE_USER_HOME ?= $(CURDIR)/.gradle-local
APP_IMAGE ?= nexus-shopping:local
PRODUCT_SEED_COUNT ?= 10000000

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

.PHONY: help
help:
	@printf '%s\n' 'Targets:'
	@printf '%s\n' '  gradle-build       Run ./gradlew build'
	@printf '%s\n' '  gradle-test        Run ./gradlew test'
	@printf '%s\n' '  boot-run           Run the app locally'
	@printf '%s\n' '  image              Build OCI image with Spring Boot buildpacks'
	@printf '%s\n' '  compose-up         Start postgres and app with APP_IMAGE'
	@printf '%s\n' '  compose-down       Stop containers'
	@printf '%s\n' '  compose-reset      Stop containers and remove volumes'
	@printf '%s\n' '  compose-ps         Show compose status'
	@printf '%s\n' '  compose-logs       Tail app logs'
	@printf '%s\n' '  health             Check /actuator/health'
	@printf '%s\n' '  stack-baseline     Switch/build/run baseline branch'
	@printf '%s\n' '  stack-indexes      Switch/build/run indexes branch'
	@printf '%s\n' '  stack-pagination   Switch/build/run pagination branch'
	@printf '%s\n' '  load-baseline      Reset DB, run baseline stack, run both JMeter tests'
	@printf '%s\n' '  load-indexes       Reset DB, run indexes stack, run both JMeter tests'
	@printf '%s\n' '  load-pagination    Reset DB, run pagination stack, run both JMeter tests'
	@printf '%s\n' '  jmeter-category    Run category JMeter test against current app'
	@printf '%s\n' '  jmeter-name        Run name JMeter test against current app'
	@printf '%s\n' '  jmeter-all         Run category and name JMeter tests'

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

.PHONY: stack-baseline stack-indexes stack-pagination stack-main
stack-baseline:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh baseline

stack-indexes:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh indexes

stack-pagination:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh pagination

stack-main:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh main

.PHONY: stack-reset-baseline stack-reset-indexes stack-reset-pagination stack-reset-main
stack-reset-baseline:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh baseline --reset-db

stack-reset-indexes:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh indexes --reset-db

stack-reset-pagination:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh pagination --reset-db

stack-reset-main:
	rtk env PRODUCT_SEED_COUNT=$(PRODUCT_SEED_COUNT) scripts/run-stack.sh main --reset-db

.PHONY: wait-health
wait-health:
	rtk bash -lc 'for attempt in {1..120}; do status=$$(curl -s -o /dev/null -w "%{http_code}" http://$(HOST):$(PORT)/actuator/health || true); if [[ "$$status" == "200" ]]; then echo "health OK"; exit 0; fi; sleep 2; done; echo "health check failed"; exit 1'

.PHONY: jmeter-dirs jmeter-category jmeter-name jmeter-all
jmeter-dirs:
	rtk mkdir -p $(JMETER_RESULTS_DIR) $(JMETER_REPORT_DIR)

jmeter-category: jmeter-dirs
	rtk jmeter -n \
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
	  -Jsize=$(SIZE)

jmeter-name: jmeter-dirs
	rtk jmeter -n \
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
	  -Jsize=$(SIZE)

jmeter-all: jmeter-category jmeter-name

.PHONY: load-baseline load-indexes load-pagination
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
