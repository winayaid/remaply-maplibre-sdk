#!/usr/bin/env python3
"""Checks the Android artifacts from `./gradlew publishToMavenLocal` before a release.

For each artifact: all files Maven Central requires are present, the POM has the required
metadata, and dependencies match API_SPEC.md §2. Pass --signed to also require .asc signatures.

Usage: scripts/check-publication.py [--signed] [--repo ~/.m2/repository]
"""
import argparse
import pathlib
import sys
import xml.etree.ElementTree as ET
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
GROUP = "io.github.winayaid"

# artifact -> dependencies that must be in compile scope (exposed to apps)
ARTIFACTS = {
    "maps-android": ["org.maplibre.gl:android-sdk-opengl"],
    "maps-android-compose": [f"{GROUP}:maps-android"],
}
REQUIRED_POM_FIELDS = [
    "m:name", "m:description", "m:url",
    "m:licenses/m:license/m:name", "m:licenses/m:license/m:url",
    "m:developers/m:developer/m:name",
    "m:scm/m:url", "m:scm/m:connection",
]


def sdk_version():
    for line in (ROOT / "android/gradle.properties").read_text().splitlines():
        if line.startswith("VERSION_NAME="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("VERSION_NAME not found in android/gradle.properties")


def check_artifact(repo, artifact, version, signed, compile_deps):
    errors = []
    base = repo / GROUP.replace(".", "/") / artifact / version
    stem = f"{artifact}-{version}"
    files = [f"{stem}.aar", f"{stem}.pom", f"{stem}.module", f"{stem}-sources.jar", f"{stem}-javadoc.jar"]
    for name in files:
        path = base / name
        if not path.is_file():
            errors.append(f"missing {name}")
        elif signed and not (base / f"{name}.asc").is_file():
            errors.append(f"missing signature {name}.asc")
    if errors:
        return errors

    pom = ET.parse(base / f"{stem}.pom").getroot()
    for field in REQUIRED_POM_FIELDS:
        el = pom.find(field, NS)
        if el is None or not (el.text or "").strip():
            errors.append(f"POM is missing <{field.replace('m:', '')}>")
    if pom.findtext("m:version", namespaces=NS) != version:
        errors.append(f"POM version is not {version}")

    deps = {}
    for dep in pom.findall("m:dependencies/m:dependency", NS):
        key = f"{dep.findtext('m:groupId', namespaces=NS)}:{dep.findtext('m:artifactId', namespaces=NS)}"
        deps[key] = (dep.findtext("m:version", namespaces=NS), dep.findtext("m:scope", namespaces=NS))
    for key in compile_deps:
        if key not in deps:
            errors.append(f"POM does not depend on {key}")
        elif deps[key][1] != "compile":
            errors.append(f"{key} has scope {deps[key][1]}, expected compile (API_SPEC.md §2)")
    for key, (dep_version, _) in deps.items():
        if dep_version and "SNAPSHOT" in dep_version:
            errors.append(f"depends on snapshot {key}:{dep_version}")

    for jar, suffix in [("sources", ".kt"), ("javadoc", ".html")]:
        with zipfile.ZipFile(base / f"{stem}-{jar}.jar") as z:
            if not any(n.endswith(suffix) for n in z.namelist()):
                errors.append(f"{jar} jar has no {suffix} files")
    return errors


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--signed", action="store_true")
    parser.add_argument("--repo", type=pathlib.Path, default=pathlib.Path.home() / ".m2/repository")
    args = parser.parse_args()

    version = sdk_version()
    failed = False
    for artifact, compile_deps in ARTIFACTS.items():
        errors = check_artifact(args.repo.expanduser(), artifact, version, args.signed, compile_deps)
        coordinate = f"{GROUP}:{artifact}:{version}"
        if errors:
            failed = True
            for e in errors:
                print(f"FAIL {coordinate}: {e}")
        else:
            print(f"ok   {coordinate}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
