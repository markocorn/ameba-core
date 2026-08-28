# AMEBA Java v1 archive

This is a frozen source snapshot of the Java implementation at the beginning of
the Python migration. It includes its Maven build, resources, visualization,
example dataset, and all uncommitted operator additions that were present in the
working tree.

## Historical build

The project targeted Java 8 and used Maven:

```shell
mvn test
mvn package
```

The snapshot is preserved for reference and parity testing. It is not part of
the active build.

## Known issues at archival time

- No automated Java tests were present.
- The shaded-JAR main class did not match the actual default-package `Main`.
- Initial-population loading repeatedly selected the first serialized cell.
- Some numerical and parameter-indexing behavior requires verification before
  being treated as a compatibility requirement.

