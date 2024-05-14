# Versioning and releasing

Solarwinds' OpenTelemetry Auto-Instrumentation Distro uses [SemVer standard](https://semver.org) for versioning of its artifacts.


## Snapshot builds

Every successful push to any branch builds and publishes the agent artifact to
[Stage latest](https://agent-binaries.global.st-ssp.solarwinds.com/apm/java/latest/solarwinds-apm-agent.jar) and
[Stage latest(lambda)](https://agent-binaries.global.st-ssp.solarwinds.com/apm/java/latest/solarwinds-apm-agent-lambda.jar).

## Release cadence

We plan to release all unreleased changes in the `main` branch on the 3rd week of the month.

## Making the release

- Update the version in project's `build.gradle` file to the appropriate version
- Make PR with version change to `main` branch
- After the pull request is merged, approve the CI's release steps
- Release the SDK by closing and releasing the snapshot in sonatype repo
