# Releasing

How to publish a version of the SDK. Android artifacts go to Maven Central and, optionally, a private Maven
repository. iOS ships as a Git tag that Swift Package Manager resolves.

A release is started by pushing a version tag. The [release workflow](../.github/workflows/release.yml) checks the
versions, builds and signs the Android artifacts, uploads them, checks that the iOS package resolves from the tag,
and drafts a GitHub release. Nothing becomes public on Maven Central until someone presses **Publish** in the
Central Portal.

## One-time setup

### 1. Maven Central namespace

1. Create an account at [central.sonatype.com](https://central.sonatype.com).
2. Register the namespace `io.github.winayaid` (**Namespaces › Add Namespace**). The portal verifies it by asking
   you to create a temporary public GitHub repository with the name it shows, under the `winayaid` account.
   The namespace is the Maven group (API_SPEC.md §2); changing it is a spec change (§17).
3. Generate a user token (**Account › Generate User Token**). It has a username and a password; you use these, not
   your login.

### 2. Signing key

Maven Central requires every file to be signed with a PGP key whose public half is on a public key server.

```bash
gpg --quick-gen-key "Remaply Maps SDK <releases@your-domain>" rsa4096 sign 3y
gpg --list-secret-keys --keyid-format short          # note the key ID
gpg --keyserver keys.openpgp.org --send-keys <KEY_ID>
gpg --armor --export-secret-keys <KEY_ID>            # the SIGNING_KEY secret, including the BEGIN/END lines
```

keys.openpgp.org emails a confirmation link; the key isn't searchable until you click it. Keep a backup of the key
and its passphrase outside GitHub.

### 3. GitHub settings

In the repository's **Settings › Environments**, create an environment named `release` and add yourself (or the
release owners) as **required reviewers**. The publish job waits for that approval.

Add these to the `release` environment:

| Kind | Name | Value |
|---|---|---|
| Secret | `MAVEN_CENTRAL_USERNAME` | User-token username from step 1 |
| Secret | `MAVEN_CENTRAL_PASSWORD` | User-token password from step 1 |
| Secret | `SIGNING_KEY` | ASCII-armored secret key from step 2 |
| Secret | `SIGNING_KEY_PASSWORD` | The key's passphrase |
| Variable | `PUBLISH_TO_MAVEN_CENTRAL` | `true`. Until it is set, the workflow builds and verifies but skips the upload. |

### 4. Private Maven repository (optional)

To also publish to a private repository (for example GitHub Packages, Artifactory, or Nexus), add:

| Kind | Name | Value |
|---|---|---|
| Variable | `REMAPLY_PRIVATE_REPO_URL` | Repository URL, for example `https://maven.pkg.github.com/winayaid/remaply-maplibre-sdk` |
| Secret | `REMAPLY_PRIVATE_USERNAME` | Repository user. Leave unset for a repository that needs no login. |
| Secret | `REMAPLY_PRIVATE_PASSWORD` | Password or token |

Apps then add the repository next to `mavenCentral()`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://your-private-repo/maven") {
            credentials {
                username = providers.gradleProperty("remaplyRepoUser").get()
                password = providers.gradleProperty("remaplyRepoToken").get()
            }
        }
    }
}
```

## Releasing a version

The examples use `0.1.0`.

### 1. Prepare

Open a pull request to `main` that:

- sets the version in `android/gradle.properties` (`VERSION_NAME`) and `ios/Sources/RemaplyMaps/RemaplyMaps.swift`
  (`RemaplyMaps.version`). `scripts/check-versions.sh` fails if they differ.
- renames `## [Unreleased]` in `CHANGELOG.md` to `## [0.1.0] - YYYY-MM-DD` and adds a new empty `## [Unreleased]`
  above it. The release workflow requires the dated heading and uses that section as the release notes.
- updates `Pre-release` notes in the quick starts and `README.md` if this is the first release.

Merge it when CI is green.

### 2. Dry run (optional)

CI already runs these on every pull request. To repeat them locally on the release commit:

```bash
(cd android && ./gradlew publishToMavenLocal) && scripts/check-publication.py
(cd samples/android && ./gradlew -PremaplyFromMavenLocal=true assembleDebug)
scripts/check-spm-consumer.sh
```

### 3. Tag

Tag the merge commit on `main`. No `v` prefix: SPM only resolves bare SemVer tags.

```bash
git checkout main && git pull
git tag -a 0.1.0 -m "0.1.0"
git push origin 0.1.0
```

### 4. Approve and publish

1. In **Actions › Release**, approve the `release` environment. The workflow checks, signs, and uploads.
2. In the [Central Portal](https://central.sonatype.com/publishing/deployments), open the new deployment for
   `io.github.winayaid`. Check that it lists both artifacts with `.aar`, `.pom`, `.module`, sources, javadoc, and `.asc`
   files, then press **Publish**. To abandon it instead, press **Drop**; nothing was made public.
3. Artifacts appear on Maven Central within about 30 minutes (search can take a few hours).
4. In **Releases**, review the draft the workflow created and publish it. Versions `0.x` and `-` suffixes are marked
   as pre-releases.

### 5. Verify

```bash
# iOS: an app-style package resolves the public tag and compiles the quick start
scripts/check-spm-consumer.sh https://github.com/winayaid/remaply-maplibre-sdk 0.1.0
```

For Android, follow [option A of the quick start](quickstart-android.md#option-a-maven-central-from-010) in an empty
project.

## If something goes wrong

- **Before pressing Publish:** drop the deployment in the portal, delete the tag
  (`git push --delete origin 0.1.0 && git tag -d 0.1.0`), fix, and start again.
- **After pressing Publish:** Maven Central versions can't be changed or removed. Don't move or reuse the tag
  either, because SPM and Gradle caches keep the first copy they saw. Release a patch version (`0.1.1`) instead.
- **The iOS job failed but Android was published:** the tag is still usable if the failure was in the check itself
  (for example a runner problem). Re-run the job. If the package really doesn't resolve, release a patch.
