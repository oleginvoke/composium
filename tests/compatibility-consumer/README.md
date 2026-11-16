# Published-artifact compatibility checks

See [dependency compatibility](../../docs/dependency-compatibility.md) for the
supported baseline and profile versions.

```shell
python tests/compatibility-consumer/run.py
python tests/compatibility-consumer/run.py --profiles legacy modern --connected --lint-probe
```

The runner builds and stages this checkout's release publications in an isolated
file repository, then creates independent projects under `.gradle/compatibility`.
Each uses its own pinned Gradle wrapper. It does not modify the user's Maven
local cache or publish to a remote repository.

`--skip-stage` reuses already staged artifacts. Omit it after changing library
or processor code. `--connected` additionally requires a connected Android device
or emulator (API 23+ for the modern profile). Without a device, all build, lint,
dependency, KSP, R8, and old-Android Robolectric checks still run.
`--lint-probe` additionally verifies that the embedded custom lint rule detects
an intentionally invalid scene, then restores and rechecks the valid source.

`profiles.json` is the source of truth for consumer versions. The fixture tests
the real POM/module metadata and AAR/JAR, avoiding the repository's plugin
classpath and sample dependency graph. Logs and machine-readable results stay
in `.gradle/compatibility`.
