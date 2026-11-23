#!/usr/bin/env python3
"""
Checks every relative link in the repository's Markdown files: the target file or folder must exist,
and a #anchor must match a heading in the target (GitHub's slug rules). External links (http, mailto)
are not fetched.

Usage: scripts/check-doc-links.py
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SKIP_DIRS = {"build", ".build", ".gradle", "node_modules", ".git", "DerivedData"}
LINK = re.compile(r"(?<!!)\[[^\]]*\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)")
HEADING = re.compile(r"^(#{1,6})\s+(.*?)\s*#*\s*$")


def slug(heading: str) -> str:
    text = re.sub(r"`([^`]*)`", r"\1", heading)
    text = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", text)
    text = text.strip().lower()
    text = re.sub(r"[^\w\- ]", "", text)
    return text.replace(" ", "-")


def anchors(path: Path) -> set:
    found, counts, in_code = set(), {}, False
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.strip().startswith("```"):
            in_code = not in_code
            continue
        m = None if in_code else HEADING.match(line)
        if m:
            base = slug(m.group(2))
            n = counts.get(base, 0)
            found.add(base if n == 0 else f"{base}-{n}")
            counts[base] = n + 1
    return found


def markdown_files():
    for p in ROOT.rglob("*.md"):
        if not any(part in SKIP_DIRS for part in p.relative_to(ROOT).parts):
            yield p


def main() -> int:
    errors, checked = [], 0
    for md in sorted(markdown_files()):
        in_code = False
        for lineno, line in enumerate(md.read_text(encoding="utf-8").splitlines(), 1):
            if line.strip().startswith("```"):
                in_code = not in_code
                continue
            if in_code:
                continue
            for target in LINK.findall(line):
                if re.match(r"^[a-z][a-z0-9+.-]*:", target):  # http:, https:, mailto:, ...
                    continue
                checked += 1
                path_part, _, anchor = target.partition("#")
                dest = md if not path_part else (md.parent / path_part).resolve()
                where = f"{md.relative_to(ROOT)}:{lineno}"
                if not dest.exists():
                    errors.append(f"{where}: missing target '{target}'")
                elif anchor and dest.suffix == ".md" and anchor not in anchors(dest):
                    errors.append(f"{where}: no heading for '#{anchor}' in {dest.relative_to(ROOT)}")
    for e in errors:
        print(e)
    print(f"Checked {checked} relative links: {'OK' if not errors else f'{len(errors)} broken'}")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
