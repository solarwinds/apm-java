#!/usr/bin/env zsh
# This script exist to enable conveniently building and publishing the artifacts to local maven repo
date
./gradlew spotlessApply clean build
./gradlew agent:publishToMavenLocal
./gradlew solarwinds-otel-sdk:publishToMavenLocal

