# JDiagram

## Overview

JDiagram was extracted from the WRIMS GUI repository into a standalone repository to support independent development, maintenance, and future integration.

This README documents the initial migration and provides historical references for future developers.

## UI Test Harness

`jdiagram-swing` has a `harness` source set (not included in the published jar) that runs the Swing UI against
fake data: an in-memory `SchematicCore`/`SchematicDataSource`, a scripted `PlatformHarness`, and a small built-in
sample schematic with synthetic monthly series for four studies.

**Launch it manually** (IntelliJ: use the shared *Harness* run configurations):

```
gradlew :jdiagram-swing:runHarness                              # SchematicView (read-only)
gradlew :jdiagram-swing:runHarness -PharnessArgs="--editor"     # SchematicEditor
gradlew :jdiagram-swing:runHarness -PharnessArgs="--dir C:/temp/harness"
```

The side panel has controls for units, Base/Diff mode, selected studies and debug flags, plus Overview,
Properties and an event log of every host/presenter callback. Add `-PmindfusionLicenseKey=...` to run licensed.

**Run the automated UI tests** (open real windows, so they need a desktop session; they are tagged `ui` and
excluded from `gradlew test` and CI):

```
gradlew :jdiagram-swing:uiTest
```

`SchematicViewUiTest` covers the toolbar, date selection and value labels, diff mode, decimals, long-term
averages, zoom/magnifier, search, selection/properties and open/save. `SchematicEditorUiTest` re-runs all of those
against `SchematicEditor` and adds editor checks (toolbar, edit setup, copy, align, delete/undo).

## Migration History

### October 29, 2026 — Initial Repository Extraction

**What was done**

- Migrated the complete `jdiagram` module from the `wrims-gui` repository into the standalone `jdiagram` repository.
- Preserved the original source code, directory structure, configuration files, and resources.
- Performed a file-level comparison between the original module and the standalone repository to verify migration integrity.

**Source Repository**

[WRIMS GUI — jdiagram module](https://github.com/CentralValleyModeling/wrims-gui/tree/53a4af02499d44371c7d70e272458eda77c5d5b0/jdiagram)

**Destination Repository**

[Standalone JDiagram — Initial Commit](https://github.com/CentralValleyModeling/jdiagram/tree/a569f1490494b47d9ba6ca3e4d8f3145024376f1)

**Verification**

- Compared the Git Trees and Blob SHAs of both locations.
- Verified all 66 files across the module.
- Confirmed that file contents, relative paths, and directory structures were identical.
- No missing, additional, or modified files were identified.

**Result**

The JDiagram module was successfully migrated to the standalone repository with no file-level differences detected at the verified commits.

## Historical References

The following references provide traceability to the original implementation and the extracted version:

| Reference | Commit |
|---|---|
| Original WRIMS GUI module | `53a4af02` |
| Standalone JDiagram repository | `a569f149` |
| Migration verification date | October 29, 2026 |

Future developers can use these references to review the original implementation, understand the initial extraction, and trace subsequent changes.

## Maintenance Notes

The standalone repository provides a separate location for future JDiagram development.

The migration and verification do not establish an automatic synchronization mechanism between the two repositories. Future changes and integration responsibilities should be documented and coordinated to avoid version divergence.
