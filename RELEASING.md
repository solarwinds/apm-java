# Versioning and releasing

Solarwinds' OpenTelemetry Auto-Instrumentation Distro uses [SemVer standard](https://semver.org) for versioning of its artifacts.


## Snapshot builds

Every successful push to any branch builds and publishes the agent artifact to
[Stage latest](https://agent-binaries.global.st-ssp.solarwinds.com/apm/java/latest/solarwinds-apm-agent.jar) and
[Stage latest(lambda)](https://agent-binaries.global.st-ssp.solarwinds.com/apm/java/latest/solarwinds-apm-agent-lambda.jar).

## Release cadence

We plan to release all unreleased changes in the `main` branch on the 3rd week of the month.

## Making the release
> ⓘ Take a look at [generate-release-notes.sh](.github/scripts/generate-release-notes.sh) to know how release note is generated.

- Update the version in project's `build.gradle` file to the appropriate version
- Make PR with version change to `main` branch
- Run the release workflow
