#!/usr/bin/env zsh
# This script exist to enable conveniently building and publishing the artifacts to local maven repo
./gradlew clean build
./gradlew agent:publishToMavenLocal
./gradlew solarwinds-otel-sdk:publishToMavenLocal

