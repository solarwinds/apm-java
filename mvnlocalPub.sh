#!/usr/bin/env zsh
./gradlew agent:publishLocalInstallPublicationToMavenLocal
./gradlew solarwinds-otel-sdk:publishLocalInstallPublicationToMavenLocal

