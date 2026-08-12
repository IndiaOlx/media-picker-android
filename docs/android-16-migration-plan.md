# Media Picker Android 16 / API 36 Migration Plan

## 1. Purpose

This is the standalone Android 16 migration plan for the Media Picker repository.
It is intentionally limited to Media Picker source, sample app, publication and
consumer integration. It covers the complete Media Picker lane, not only safer
intents.

Program tickets:

- `A36-MP-01`: source/artifact provenance;
- `A36-MP-02`: JDK 21, Gradle 9, AGP 9 and compile SDK 36;
- `A36-MP-03`: media URI, permissions, camera, component security, edge-to-edge,
  predictive back and minimum large-screen safety;
- `A36-MP-04`: tests, immutable publication and Panamera adoption.

The plan was prepared from a static source audit on 12 August 2026. No Gradle
build, compilation or test was executed while preparing it.

## 2. Source References

The Media Picker tasks are derived from these Panamera program documents:

- `docs/android-16-api-36-implementation-plan.md`, especially “Media Picker
  (`A36-MP-01`–`04`)”, “Permissions and media”, and “Intents and exported
  components”;
- `docs/android-16-api-36-migration-plan.md`, especially “OLX Media Picker
  sibling assessment and migration plan”, edge-to-edge, predictive back,
  large-screen, media/storage, 16 KB and test sections;
- `docs/android-16-safer-intent-task.md`, only for the Media Picker component,
  camera, result and URI-grant security requirements;
- `docs/android-16-safer-intent-evidence.md`, only for the current Panamera
  integration status and manifest override.

This document is self-contained so Media Picker development and review do not
depend on a Panamera checkout.

## 3. Current Repository Baseline

Inspected repository:

```text
/Users/rohan.choudhary/Documents/olx-media-picker/media-picker-android
```

Current branch and important provenance:

| Item | Current evidence | Migration consequence |
|---|---|---|
| Branch | `feature/OLXIN_26952-safer-intent-android-16` | Use as a review branch, not artifact provenance by itself |
| Export fix | commit `7d6c431` makes `MediaGalleryActivity` non-exported | Source fix exists; publication/adoption still required |
| Branch base | branch follows tag `2.0.30` | Contains separately owned permission/Permify work |
| Panamera dependency | `com.github.IndiaOlx:media-picker-android:2.0.28` | Exact 2.0.28 source/AAR must be reconciled before release planning |
| Maven publication | `mediapicker/build.gradle` says `2.0.26` | Local publishing currently uses stale/misleading coordinates |
| Modules | `mediapicker` library and `app` sample | Both must migrate and be certified |
| Gradle | 8.14.3 | Planned final baseline is 9.4.1 |
| AGP | 8.11.0 | Planned final baseline is 9.2.1 |
| Kotlin | 2.2.0, legacy Kotlin Android plugin | AGP 9/built-in Kotlin migration required |
| SDK | compile 35, target 35, min 23 | Move compile first, target separately after remediation |
| JVM output | Java/Kotlin 17 | Retain bytecode target 17 initially |
| Build runtime | IDE currently supports JDK 17/21; repository does not pin CI | Pin an approved JDK 21 vendor/patch for local, CI and publication |
| CI | no repository CI file found | Add build/test/publication evidence lane or integrate with owner CI |
| Native source | no JNI/CMake/NDK or checked-in `.so` found | Still scan resolved AAR and consuming APK for 16 KB compatibility |
| Consumer rules | `mediapicker/consumer-rules.pro` is empty | Prove no rules are needed, or add minimal evidence-based rules |

Important sequencing rule: do not combine JDK, Gradle/AGP, compile SDK,
dependency upgrades, runtime remediation and target SDK 36 in one change.

## 4. Applicability Matrix

| Android 16 area | Applies? | Current assessment | Owner task |
|---|---:|---|---|
| JDK/Gradle/AGP | Yes | Not on program baseline | `A36-MP-02` |
| Compile/target SDK 36 | Yes | Both currently 35 | `A36-MP-02`, final target gate |
| Dependency compatibility | Yes | Several old/deprecated dependencies; graph not archived | `A36-MP-02` |
| Media/storage/content URIs | Yes, critical | Photo pipeline still depends on `MediaStore.DATA` and filesystem paths | `A36-MP-03` |
| Selected Photos access | Yes, critical | Partial-access support exists but permission decision is too broad | `A36-MP-03` |
| Camera/video intents and grants | Yes, critical | `file://` fallback and incomplete grant contract remain | `A36-MP-03` |
| Exported components/safer intents | Yes | Media activities now non-exported; sample and result contracts need tests | `A36-MP-03` |
| Edge-to-edge | Yes | Three activities consume root insets and apply full-root margins | `A36-MP-03` |
| Predictive back | Yes | Dispatcher APIs exist; ownership/process-state behavior is not fully tested | `A36-MP-03` |
| Large screens/orientation | Yes | Three library activities and sample launcher are portrait-only | `A36-MP-03` |
| Text/font rendering | Yes, regression scope | `includeFontPadding=false` and fixed controls require API 36/font-scale checks | `A36-MP-03` |
| Accessibility | Yes, regression scope | No focused automated/accessibility evidence found | `A36-MP-03` |
| 16 KB page size | Artifact gate | No native source found; transitive/final APK proof absent | `A36-MP-04` |
| R8/minification | Artifact gate | Empty consumer rules and no minified-host evidence | `A36-MP-04` |
| Background work/FGS | No direct code | No Worker, Service or foreground service found | Static no-impact evidence |
| Notifications | Sample cleanup only | Sample declares `POST_NOTIFICATIONS`; no notification code found | Remove if unused |
| WebView/payment | No | No WebView/payment code found | Static no-impact evidence |
| Local network | No | No socket/local-network feature found | Static no-impact evidence |
| Multi-process/broadcasts | No | No receiver, service or secondary process found | Static no-impact evidence |
| Reflection/hidden APIs | No finding | No relevant use found | Dependency/runtime scan |

## 5. Confirmed Source Findings

### 5.1 Source and publication mismatch

Panamera uses version 2.0.28, while the checked-out branch follows 2.0.30 and
the manual `MavenPublication` still emits 2.0.26. A local snapshot published
without correcting this metadata does not prove the production 2.0.28 upgrade.
The branch also contains permission changes owned by a separate workstream.

### 5.2 Filesystem-path media contract

The photo pipeline is not content-URI-first:

- `GalleryService` and `LoadPhotoViewModel` read
  `MediaStore.Images.Media.DATA`;
- `LoadVideoViewModel` filters custom folders using
  `MediaStore.Video.VideoColumns.DATA`;
- `PhotoFile` stores a mutable filesystem `path`, checks it with `File`, and
  uses it for equality when the media ID is zero;
- adapters use `Uri.fromFile(File(path))`;
- preview code calls `BitmapFactory.decodeFile`;
- validation rules consume paths rather than a resolver-backed media handle;
- camera code creates a public Pictures file and stores its absolute path.

This does not reliably support scoped storage, Selected Photos access, cloud
media providers, expired grants or process recreation.

Additional correctness finding: photo loaders read MIME type from the cursor,
but `PhotoFile.Builder.mimeType()` does not accept or assign that value.

### 5.3 Permission contract is broader than the selected operation

`PermissionsUtil.getRequiredPermissions()` requests camera, images and video as
one set. On Android 14+ it also requests
`READ_MEDIA_VISUAL_USER_SELECTED`. This creates several problems:

- camera permission is requested even when camera is disabled or the user only
  wants to browse;
- video permission is requested for photo-only configurations and vice versa;
- partial visual access is treated as overall success even if an independently
  required permission was denied;
- Android 13 “full access” UI is hidden when either image or video access is
  granted, even if the active configuration needs the other type;
- permission state is not rechecked immediately before every protected browse,
  camera or video operation;
- manual revoke and unused-app auto-reset recovery are not evidenced.

The sample manifest declares the expected storage/media permissions, but the
library manifest does not publish a documented host permission contract.

### 5.4 Camera output and grant contract

`PhotoGridFragment.startTakingPicture()` currently:

- falls back to `Uri.fromFile(...)` when the client authority is absent;
- adds only `FLAG_GRANT_READ_URI_PERMISSION` even though the camera must write;
- adds the same read flag twice;
- supplies `EXTRA_OUTPUT` without matching `ClipData`;
- does not show a controlled failure path for invalid provider authority or no
  available camera handler;
- creates a filesystem file in public Pictures before capture;
- later inserts a separate MediaStore row and, on API 29+, sets
  `IS_PENDING=1` without a visible finalization update to zero.

The provider XML also contains `<external-path path="."/>`, which exposes the
entire shared external-storage tree to any URI the host chooses to grant. The
grant is temporary, but the path scope is broader than Media Picker needs.

### 5.5 Component and input contracts

The library manifest now marks `GalleryActivity`, `FolderViewActivity`,
`MediaGalleryActivity` and the `FileProvider` non-exported. This is the correct
host-only contract.

Remaining work:

- `MediaGalleryActivity` uses `!!` for optional index/source extras and does not
  bound the incoming list or index;
- selected photos/videos are transported as Parcelable lists and need explicit
  size limits to avoid transaction-size and malformed-state failures;
- `VideoFile.uri` is `@IgnoredOnParcel`, so a selected video loses its URI after
  parcel/process restoration;
- activities depend on process-global `Gallery.galleryConfig`, which is lost
  after process death;
- `GalleryActivity` installs its initial fragment without checking
  `savedInstanceState`, risking duplicate fragments after recreation;
- `BaseFragmentActivity` uses `commitAllowingStateLoss` and catches all
  exceptions, hiding restoration failures;
- the sample launcher has an unnecessary `ACTION_VIEW` action with no data
  contract. It only needs its launcher contract unless a demo deep link is
  intentionally designed.

### 5.6 Edge-to-edge and insets

All three activities install almost identical root inset listeners. They apply
system-bar values as margins on all sides and return
`WindowInsetsCompat.CONSUMED`. This creates duplicated code and prevents child
views from seeing insets. It also makes toolbar, bottom action, gesture
navigation, cutout and IME ownership unclear.

`oss_fragment_carousal.xml` additionally uses `fitsSystemWindows="true"`, which
can double-apply or conflict with root handling.

### 5.7 Back, resize and state

Positive current state:

- `FolderViewActivity` and `MediaGalleryActivity` use lifecycle-aware
  `OnBackPressedCallback`;
- fragments route toolbar back through `OnBackPressedDispatcher`.

Required proof/remediation:

- gesture-back commit and cancel, three-button back and nested folder stack;
- avoid double-calling host close callbacks when the activity also finishes;
- preserve current selection, page, folder, preview index and pending camera
  request across rotation, resize and process recreation;
- ensure callbacks are enabled only while their UI state owns back.

All three library activities and the sample launcher are portrait-only. On
target 36, orientation restrictions can be ignored on displays with
`smallestWidth >= 600dp`; these screens must remain reachable and state-safe.
The preview layout also contains a fixed `300dp x 630dp` pager, which needs a
bounded/responsive layout for landscape, split-screen and expanded windows.

### 5.8 Lifecycle, threading and diagnostics

`BaseLoadMediaViewModel.onLoadFinished()` creates a new single-thread executor
for each result and does not shut it down. Media queries often catch broad
exceptions and return empty results without a diagnostic distinction between
permission loss, invalid projection/provider behavior and a genuinely empty
gallery. This complicates Android 16 permission/process testing.

## 6. Required Work — `A36-MP-01` Provenance

1. Identify and archive the exact Git commit/tag used for published 2.0.28.
2. Download/archive the consumed 2.0.28 AAR, POM, module metadata, manifest and
   consumer rules.
3. Record SHA-256 hashes and prove source-to-artifact correspondence.
4. Diff 2.0.28 against 2.0.29, 2.0.30 and this migration branch.
5. Decide whether the Android 16 release is:
   - a minimal successor based on 2.0.28; or
   - a later release containing the separately approved permission migration.
6. Keep permission and Android 16 changes independently reviewable even if they
   ship in one final version.
7. Replace the hard-coded 2.0.26 publication value with one authoritative
   version source shared by local publication and release CI.
8. Never overwrite a released coordinate. Local testing must use a unique
   snapshot version.

Acceptance evidence:

- source commit, tag and artifact hashes;
- dependency/POM/manifest/rules archive;
- signed owner decision on the release baseline and included feature branches.

## 7. Required Work — `A36-MP-02` Build and SDK Migration

Implement as ordered checkpoints:

### Checkpoint 1 — JDK 21 runtime, existing build

1. Select and pin one JDK 21 vendor/patch for developer, CI and publication.
2. Run the current Gradle 8.14.3/AGP 8.11.0 build on JDK 21.
3. Keep compile/target SDK 35 and Java/Kotlin bytecode target 17.
4. Compare library/sample tests, lint, AAR, POM and source/module metadata with
   the API 35 baseline.

### Checkpoint 2 — Gradle 9.4.1 and AGP 9.2.1

1. Upgrade wrapper and AGP while SDK remains 35.
2. Migrate the legacy Kotlin Android plugin/buildscript configuration to the
   approved AGP 9 built-in Kotlin model.
3. Preserve public Kotlin/Java API and bytecode target 17.
4. Update publication configuration for AGP 9 components and verify sources,
   POM scopes, module metadata and Maven Local/JitPack/owner repository behavior.
5. Enable configuration cache only after incompatible tasks are corrected; do
   not hide configuration errors.

### Checkpoint 3 — compile SDK 36, target SDK 35

1. Raise library and sample compile SDK to 36.
2. Keep target SDK 35 until runtime remediation and API 36 tests pass.
3. Upgrade dependencies in compatible families, freezing exact versions.
4. Specifically review/remove deprecated `lifecycle-extensions:2.2.0`, align
   AppCompat/Core/Activity/Fragment/Loader/Material/ConstraintLayout/Coil and
   permission dependencies, and archive the resolved graph.

### Checkpoint 4 — target SDK 36

Raise target only in a dedicated change after `A36-MP-03` and the API 36 test
matrix are complete. Do not retain orientation, edge-to-edge or intent-security
opt-outs as the production solution.

## 8. Required Work — `A36-MP-03` Runtime Remediation

### 8.1 Replace path-first media models with content URIs

1. Introduce a stable selected-media contract containing at minimum:
   - content URI;
   - media ID when available;
   - MIME type;
   - display name/size/duration where relevant;
   - remote/backend identity separately from local media identity.
2. Make `content://` the primary local identity. Deprecate filesystem `path`
   without silently changing public consumers in one release.
3. Query explicit projections; never use a null projection and then assume
   `_data` exists.
4. Build photo URIs with `ContentUris.withAppendedId`, as the video loader
   already does.
5. Replace `DATA` folder filters with bucket/relative-path/provider-supported
   contracts. Define behavior when a cloud provider has no filesystem path.
6. Load previews through Coil/ContentResolver URI APIs, not
   `Uri.fromFile`/`BitmapFactory.decodeFile`.
7. Validate MIME using `ContentResolver.getType()` plus an allowlist; correct
   the no-op `PhotoFile.Builder.mimeType()` implementation.
8. Preserve URI through Parcelable/saved state. Do not mark the only usable
   `VideoFile.uri` as ignored on parcel without a deterministic reconstruction
   contract.
9. Define persisted-grant behavior for system-picker/document URIs when the
   provider supports it; otherwise surface recoverable reselection.
10. Handle deleted media, expired grants, provider unavailability and cloud
    download failure without treating them as an empty local file path.

### 8.2 Split permissions by capability

1. Derive the required permission from active media type and the user action.
2. Request image browse permission only for image browsing, video only for
   video browsing, and camera only when the camera action is selected.
3. Support three visual-media states independently:
   - full access;
   - selected/partial access;
   - denied.
4. Treat partial access as a valid, limited gallery—not as full permission for
   unrelated camera/video operations.
5. Recheck permission immediately before each protected query/camera action.
6. Handle temporary denial, permanent denial, manual Settings revoke and
   unused-app auto-reset without losing the current selection/draft.
7. Provide a deliberate “manage/reselect photos” action for partial access.
8. Do not request permissions at app startup or request capabilities disabled
   in `GalleryConfig`.
9. Document exactly which permissions a host must declare for each feature and
   API range; align sample manifest and README.
10. Remove sample `POST_NOTIFICATIONS` unless a real notification journey is
    added and tested.

### 8.3 Harden camera and video capture

1. Remove all `file://` output fallbacks.
2. Choose one output ownership model:
   - create a pending MediaStore item and pass its content URI; or
   - create an app-private/cache file exposed through a narrowly scoped
     FileProvider and publish it after success.
3. Do not pre-create an unmanaged public filesystem path and then insert a
   separate MediaStore row.
4. Put the same output content URI in `EXTRA_OUTPUT` and `ClipData`.
5. Grant exactly read and write URI permission for the camera transaction.
6. Narrow `provider_paths.xml`; remove the root `<external-path path="."/>`
   unless an approved compatibility contract proves it is required.
7. Validate the configured authority and require a declared, non-exported
   provider before enabling the camera tile.
8. Check intent resolution and catch controlled provider/activity failures.
9. Track one active capture token/URI, consume it once and ignore duplicate or
   stale results.
10. On success validate existence, MIME, size and decodability; finalize
    `IS_PENDING=0` for MediaStore output.
11. On cancel/failure/process recovery, clean up abandoned pending output when
    ownership is proven.
12. Apply the same principles to video capture in the sample/host contract.

### 8.4 Component, intent and result security

1. Retain all three library activities as `android:exported="false"`.
2. Retain FileProvider as non-exported with temporary grants only.
3. Remove the sample launcher’s unused `ACTION_VIEW` action unless a typed demo
   deep-link contract is explicitly required.
4. Keep factory intents fresh and explicit; never copy caller component,
   selector, categories, `ClipData`, flags or arbitrary extras.
5. Bound all Parcelable collections to configured selection limits and a hard
   safety maximum.
6. Validate preview list/index/source and finish cancelled on malformed input;
   remove `!!` reads.
7. Accept result payload only from the expected active launcher and validate
   type, count, MIME and URI access before applying it.
8. Do not use `removeLaunchSecurityProtection()`.
9. Add hostile tests for forged explicit launch, nested intent, selector,
   ClipData, grant flags, malformed Parcelable and oversized list.

### 8.5 Edge-to-edge and window insets

1. Enable/accept edge-to-edge at one activity-shell boundary.
2. Create one reusable inset contract for all Media Picker activities.
3. Do not apply every system inset as a margin to the whole root.
4. Assign ownership explicitly:
   - toolbar/preview close control owns safe top/cutout inset;
   - grids/content use safe horizontal insets;
   - bottom action owns navigation/gesture inset;
   - input surfaces, if introduced, own IME inset.
5. Return unconsumed insets where descendants need them.
6. Remove conflicting `fitsSystemWindows="true"` after equivalent ownership is
   implemented.
7. Preserve initial padding/margins and prevent duplicate application.
8. Test light/dark status icons, gesture navigation, three-button navigation,
   cutout, landscape, split-screen and IME where applicable.

### 8.6 Predictive back and state restoration

1. Keep `OnBackPressedDispatcher` as the View/activity boundary.
2. Register callbacks with the correct lifecycle owner and enable them only
   while the folder/preview/selection state owns back.
3. Define expected back order: preview → folder → gallery → host.
4. Ensure gesture cancel changes no selection or navigation state.
5. Ensure gesture commit produces one close/pop callback, not both finish and a
   second host callback.
6. Initialize fragments only when `savedInstanceState == null`.
7. Replace broad `commitAllowingStateLoss`/catch-all behavior with explicit,
   state-safe navigation.
8. Restore selected media, current page/folder, preview index, scroll position
   and active camera output after rotation/process recreation.
9. Define recovery when `Gallery.galleryConfig` is absent after process death:
   rehydrate a process-safe configuration or finish with an explicit recoverable
   result; never crash through a nullable global/late callback.

### 8.7 Minimum large-screen compatibility

Android 16 can ignore orientation restrictions on `sw >= 600dp`. Full tablet
redesign is not required, but minimum safety is.

1. Remove portrait-only assumptions from all three library activities.
2. Keep the sample launcher resize-safe as the certification host.
3. Replace the fixed `300dp x 630dp` preview with constrained responsive sizing.
4. Use adaptive grid spans and bounded/centered content where phone-width UI is
   still desired.
5. Preserve selection, pager index and pending camera state across rotation,
   split-screen resize and fold/unfold.
6. Ensure toolbar, selection indicators, permission panel and bottom CTA remain
   visible and reachable at compact/medium/expanded widths.
7. Test portrait, landscape, `sw600dp`, split-screen and foldable resize.

### 8.8 Text, accessibility and lifecycle quality

1. Screenshot-test Hindi/English, long labels and font scales 1.0, 1.3 and 2.0
   on API 35 and 36.
2. Verify `includeFontPadding=false` does not clip toolbar, buttons, counters or
   duration text under API 36 font metrics.
3. Add meaningful content descriptions/state descriptions for camera, folder,
   selection checkboxes, back/close and previous/next preview controls.
4. Verify TalkBack order and announcements for permission status, selected
   count, validation errors and loading/empty states.
5. Meet minimum touch target and contrast expectations in compact/expanded UI.
6. Replace per-load unmanaged executors with lifecycle-owned coroutine/executor
   work that is cancelled or shut down correctly.
7. Distinguish permission/provider/query failures from an empty gallery using a
   non-PII diagnostic contract.

## 9. Required Work — `A36-MP-04` Tests, Artifact and Adoption

### 9.1 Automated test layers

Unit/Robolectric tests:

- content-URI photo/video mapping and MIME validation;
- no `DATA` requirement and no `file://` output;
- full/partial/denied permission-state matrix per media configuration;
- camera intent URI, ClipData and read/write grant contract;
- camera success/cancel/failure/replay/pending-output cleanup;
- activity factory/input/result bounds and malformed values;
- process-restored selected photo/video identities;
- configuration-missing recovery;
- back ownership and saved-state decisions.

Instrumentation/device tests:

- full, selected and denied photo access;
- permanent denial, manual revoke and unused-app auto-reset;
- system reselection/limited-library changes;
- local MediaStore and supported cloud-provider media;
- image and video selection;
- camera success, cancel, no handler, low storage and process death;
- expired/revoked URI grant and recoverable reselection;
- malicious external activity launch cannot reach Media Picker activities;
- predictive gesture cancel/commit and three-button back;
- gesture/three-button insets, cutout, rotation, `sw600dp`, split-screen and
  fold/unfold;
- Hindi/large-font/accessibility smoke;
- API 35 and API 36, including a 16 KB page-size API 36 device where available.

Host integration tests:

- Panamera posting create/edit/republish selected photos and camera;
- profile photo and profile-completion photo;
- chat attachment consumers where the artifact is used;
- cancel/back/reselect, upload retry, process death and app update;
- same journeys with full and selected photo access.

### 9.2 Artifact gates

For every release candidate archive:

- exact source commit/tag and dependency lock/graph;
- AAR, POM, Gradle module metadata and source artifact;
- merged library manifest showing no exported Media Picker component;
- public API/binary compatibility report;
- consumer rules and minified sample/host evidence;
- lint/test reports;
- AAR contents and resolved native-library inventory;
- 16 KB ELF/zip alignment evidence for any present native library and final
  consuming APK/AAB;
- SHA-256 hashes and publication repository coordinate.

No JNI/native source is currently present. That is not sufficient closure:
resolved transitive dependencies and the final consuming app still require an
automated 16 KB scan.

### 9.3 Publication and Panamera adoption

1. Publish a unique local snapshot only from the approved baseline, for example
   `2.0.28-android16-SNAPSHOT`; never overwrite 2.0.28.
2. Verify Panamera resolves the local snapshot from `mavenLocal()` and record
   the dependency insight/coordinate.
3. Run the Panamera integration matrix before publishing a final artifact.
4. Publish an immutable reviewed successor using the Media Platform owner’s
   approved version/tag.
5. Update Panamera’s `galleryVersion` in a separate adoption change while
   Panamera still targets 35.
6. Keep Panamera’s manifest override until the fixed artifact is adopted and a
   fresh merged manifest proves `MediaGalleryActivity` is non-exported. It may
   remain as defence in depth if owners approve.
7. Verify all other owned consumers, including Ragnarok/sample use, or record a
   named owner and blocker for each unverified consumer.
8. Record the tested rollback pair: Panamera commit ↔ Media Picker version.

## 10. Ordered Delivery Plan

| Packet | Contents | Must remain unchanged | Exit gate |
|---|---|---|---|
| MP-01 | 2.0.28 provenance, artifact archive, versioning decision | Runtime behavior | Source/artifact proof signed |
| MP-02A | JDK 21 runtime with current build | SDK 35, bytecode 17 | Baseline outputs equivalent |
| MP-02B | Gradle 9.4.1, AGP 9.2.1, built-in Kotlin | SDK 35, bytecode 17 | Library/sample/publication green |
| MP-02C | Compile SDK 36 and dependency waves | Target 35, bytecode 17 | Graph/manifest/API diff approved |
| MP-03A | Content URI and selected-access model | UI/product behavior | URI and permission tests green |
| MP-03B | Camera/provider/grants and component security | Selection journeys | Hostile/replay tests green |
| MP-03C | Insets, back, resize, state, text/accessibility | Full adaptive redesign | API 35/36 device matrix green |
| MP-04 | RC artifact, minified/16 KB gates and consumers | No snapshot in production | Immutable release adopted |
| MP-TGT | Target SDK 36 only | All prior remediation | Final API 36 certification green |

## 11. Static Audit Commands

These commands do not build the project:

```bash
git status --short --branch
git diff --check
rg -n 'compileSdk|targetSdk|minSdk|com.android.tools.build:gradle|kotlin_version' .
rg -n 'MediaStore.*DATA|VideoColumns.DATA|Uri.fromFile|decodeFile|absolutePath' mediapicker/src/main
rg -n 'READ_MEDIA|READ_EXTERNAL|WRITE_EXTERNAL|CAMERA|requestPermission' app/src/main mediapicker/src/main
rg -n 'FLAG_GRANT|ClipData|EXTRA_OUTPUT|FileProvider|provider_paths' app/src/main mediapicker/src/main
rg -n 'android:exported="true"|screenOrientation|ACTION_VIEW' app/src/main mediapicker/src/main
rg -n 'WindowInsetsCompat.CONSUMED|fitsSystemWindows|setOnApplyWindowInsetsListener' mediapicker/src/main
rg -n 'onBackPressed|OnBackPressedCallback|onBackPressedDispatcher' mediapicker/src/main
rg -n 'PendingIntent|removeLaunchSecurityProtection|Intent.parseUri' mediapicker/src/main
find . -type f -name '*.so' -o -name 'CMakeLists.txt' -o -name 'Android.mk'
xmllint --noout app/src/main/AndroidManifest.xml mediapicker/src/main/AndroidManifest.xml
```

Expected final static outcomes:

- no reachable `MediaStore.DATA`, `VideoColumns.DATA`, `Uri.fromFile` or raw
  filesystem requirement for selected media;
- no exported Media Picker library component;
- no root-consumed inset ambiguity;
- no `file://` camera output;
- camera grants include matching ClipData and only necessary flags;
- no mutable/arbitrary PendingIntent or Android 16 protection opt-out;
- every remaining portrait declaration has an approved target-36 disposition.

## 12. Definition of Done

Media Picker is Android 16 development-complete only when:

- exact 2.0.28 source/artifact provenance is signed;
- local, CI and publication builds use pinned JDK 21, Gradle 9.4.1 and AGP
  9.2.1 while bytecode remains 17;
- compile SDK 36 and dependency upgrades are complete before target SDK 36;
- selected photos/videos use content URIs end to end and work with full,
  selected and cloud-provider access;
- permission denial, revoke, auto-reset and reselection preserve state;
- camera/video output uses a scoped content URI, correct temporary grants and
  one-time result ownership;
- all Media Picker activities/providers have approved non-exported contracts;
- edge-to-edge, predictive back and minimum expanded-window behavior pass on
  API 35/36;
- process death restores or safely recovers configuration, selection and active
  capture without duplicate fragments or crashes;
- text/accessibility, minified-host, 16 KB and artifact gates pass;
- a fixed immutable artifact is published and Panamera/owned consumers adopt
  it with a documented rollback pair;
- target SDK 36 is enabled only after every developer-owned finding above is
  closed or has an explicitly approved owner/blocker.

## 13. Non-Goals

- Full tablet/two-pane/foldable product redesign; Android 16 requires minimum
  reachable and state-safe resize behavior only.
- Panamera notification, payment, deep-link or XMPP remediation.
- Unrelated permission UX/Permify behavior mixed silently into this workstream.
- Permanent edge-to-edge, orientation, back or intent-security opt-outs.
- Broad refactoring without a mapped Android 16 finding and focused test.

## 14. QA Handoff Record

Before target SDK 36, attach:

| Evidence | Value/link |
|---|---|
| Media Picker commit/tag | |
| Published coordinate and SHA-256 | |
| Panamera adoption commit | |
| JDK/Gradle/AGP/Kotlin versions | |
| compile/target/min SDK | |
| API 35 test report | |
| API 36 test report | |
| Selected Photos/cloud-provider report | |
| Camera/URI-grant report | |
| Predictive back/inset/large-screen screenshots | |
| Hostile activity/result test report | |
| Minified host and consumer-rules report | |
| Native/16 KB scan | |
| Merged manifest export inventory | |
| Known blockers, owner and due date | |
| Tested rollback pair | |
