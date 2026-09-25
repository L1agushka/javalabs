.PHONY: all build test lint format run-client run-server

all: build test

build:
	./gradlew build

test:
	./gradlew test

lint:
	./gradlew spotlessCheck

format:
	./gradlew spotlessApply

run-client:
	./gradlew :client:run

run-server:
	./gradlew :server:bootRun