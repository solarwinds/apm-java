#!/usr/bin/env zsh
# This script exist to enable conveniently building and publishing the artifacts to local maven repo
./gradlew clean build
./gradlew agent:publishLocalInstallPublicationToMavenLocal
./gradlew solarwinds-otel-sdk:publishLocalInstallPublicationToMavenLocal

