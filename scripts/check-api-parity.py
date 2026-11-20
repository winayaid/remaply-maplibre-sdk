#!/usr/bin/env python3
"""
Checks that the public API names of the Android and iOS SDKs are identical (API_SPEC.md §3.1):
types, members, enum cases, and parameter names.

Reads the public declarations from source:
  - Kotlin: `public` declarations (the Android SDK uses explicit API mode).
  - Swift:  `public` declarations, protocol requirements, and enum cases.

Names are normalized with the spec's mechanical rules (UPPER_SNAKE enum entries become lowerCamel;
companion-object members become static members of the outer type). Documented platform-only items
are listed in PLATFORM_ONLY; anything else that differs fails the check.

Usage: scripts/check-api-parity.py [--dump]
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
KOTLIN_FILES = sorted((ROOT / "android/maps/src/main/kotlin/io/github/winayaid/mapstrial").glob("*.kt")) + sorted(
    (ROOT / "android/maps-compose/src/main/kotlin/io/github/winayaid/mapstrial/compose").glob("*.kt"))
SWIFT_FILES = sorted((ROOT / "ios/Sources/RemaplyMaps").glob("*.swift"))

# Documented differences (API_SPEC.md §3.1, §5.4, §7, §19 Q4).
PLATFORM_ONLY = {
    "android": {
        "RemaplyMapView.onSaveInstanceState",             # §9.3, Q4
        "RemaplyMapView.onSaveInstanceState(outState)",
        "RemaplyMapView.onCreate(savedInstanceState)",    # Q4: Bundle parameter
        "RemaplyMapView.init(context)",                   # Android Context vs iOS frame
        "RemaplyMapView.init(attrs)",                     # XML inflation constructors
        "RemaplyMapView.init(defStyleAttr)",
        "RemaplyMap(modifier)",                           # §7.4: Compose modifier
        "rememberMapController",                          # API-032: Compose only
        "rememberMapController(key)",
        "ExperimentalRemaplyApi",                         # Kotlin opt-in annotation for §13.2
        "MapStyle.Url", "MapStyle.Url.url", "MapStyle.Url.init(url)",               # §5.4: sealed subclasses
        "MapStyle.Bundled", "MapStyle.Bundled.path", "MapStyle.Bundled.init(path)",  # = Swift enum cases
        "MapStyle.url(url)", "MapStyle.bundled(path)",    # Swift cases have unlabeled associated values
    },
    "ios": {
        "RemaplyMapView.init(frame)",                     # iOS frame vs Android Context
        "RemaplyMapView.init(coder)",                     # storyboard initializer
        "MapError.description",                           # CustomStringConvertible
        "RemaplyMap.body",                                # SwiftUI View requirement
        "MapController.init",                             # Kotlin primary constructor has no name
    },
}

KOTLIN_TYPE = re.compile(
    r"\bpublic\s+(?:(?:data|sealed|enum|annotation|abstract|open|fun)\s+)*(class|object|interface)\s+(\w+)")
SWIFT_TYPE = re.compile(r"\bpublic\s+(?:final\s+)?(class|struct|enum|protocol)\s+(\w+)")


def snake_to_camel(name):
    if not re.fullmatch(r"[A-Z][A-Z0-9_]*", name):
        return name
    first, *rest = name.lower().split("_")
    return first + "".join(p.capitalize() for p in rest)


def strip_comments(text):
    """Blanks out comments, keeping line breaks. Block comments nest (as in Kotlin and Swift)."""
    out, i, depth, n = [], 0, 0, len(text)
    while i < n:
        two = text[i:i + 2]
        if two == "/*":
            depth += 1
            out.append("  ")
            i += 2
        elif two == "*/" and depth:
            depth -= 1
            out.append("  ")
            i += 2
        elif depth:
            out.append("\n" if text[i] == "\n" else " ")
            i += 1
        elif two == "//":
            j = text.find("\n", i)
            j = n if j == -1 else j
            out.append(" " * (j - i))
            i = j
        else:
            out.append(text[i])
            i += 1
    return "".join(out)


def strip_strings(text):
    return re.sub(r'"(?:\\.|[^"\\\n])*"', lambda m: '"' + " " * (len(m.group(0)) - 2) + '"', text)


def match_close(text, i):
    depth = 0
    for j in range(i, len(text)):
        if text[j] in "([{":
            depth += 1
        elif text[j] in ")]}":
            depth -= 1
            if depth == 0:
                return j
    raise ValueError("unbalanced")


def split_top(s):
    out, depth, cur = [], 0, ""
    for ch in s:
        depth += ch in "([{<"
        depth -= ch in ")]}>"
        if ch == "," and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += ch
    out.append(cur)
    return [p.strip() for p in out if p.strip()]


def frames(text, type_re, is_swift):
    """Returns [(start, end, owner)] for each {...} block that belongs to a public type,
    a companion object (owner = outer type), or a Swift extension."""
    result = []
    stack = []  # (owner or None)
    last_boundary = 0
    i = 0
    while i < len(text):
        ch = text[i]
        if ch == "{":
            header = text[last_boundary:i]
            owner = None
            parent = next((o for o in reversed(stack) if o), None)
            m = None
            for m in type_re.finditer(header):
                pass
            companion = None
            for companion in re.finditer(r"\bcompanion\s+object\b", header):
                pass
            # The declaration closest to the brace owns the block.
            if companion and (m is None or companion.start() > m.start()):
                owner = parent
            elif m:
                owner = m.group(2)
            elif is_swift:
                e = re.search(r"\bextension\s+(\w+)", header)
                if e and header.strip().startswith(("public extension", "extension")):
                    owner = e.group(1)
            stack.append(owner)
            result.append([i, None, owner])
            last_boundary = i + 1
        elif ch == "}":
            owner = stack.pop()
            for f in reversed(result):
                if f[1] is None:
                    f[1] = i
                    break
            last_boundary = i + 1
        elif ch in ";\n" and text[last_boundary:i].strip() == "":
            last_boundary = i + 1
        i += 1
    return result


def owner_at(fr, pos):
    best = None
    for start, end, owner in fr:
        if start < pos < end and owner and (best is None or start > best[0]):
            best = (start, owner)
    return best[1] if best else None


def kotlin_params(s):
    names = []
    for p in split_top(s):
        p = re.sub(r"@[\w.]+(\([^)]*\))?\s*", "", p)
        m = re.match(r"(?:(?:public|private|internal|override|val|var|vararg)\s+)*(\w+)\s*:", p)
        if m:
            names.append(m.group(1))
    return names


def swift_params(s):
    names = []
    for p in split_top(s):
        m = re.match(r"(?:(\w+)\s+)?(\w+)\s*:", p)
        if m:
            names.append(m.group(2) if m.group(1) in (None, "_") else m.group(1))
    return names


def add(items, owner, name, params=None, prefix=""):
    base = f"{owner}.{prefix}{name}" if owner else f"{prefix}{name}"
    items.add(base)
    for p in params or []:
        items.add(f"{base}({p})")


def parse_kotlin():
    items = set()
    for f in KOTLIN_FILES:
        text = strip_strings(strip_comments(f.read_text()))
        fr = frames(text, KOTLIN_TYPE, False)
        for m in KOTLIN_TYPE.finditer(text):
            outer = owner_at(fr, m.start())
            name = f"{outer}.{m.group(2)}" if outer else m.group(2)
            items.add(name)
            rest = text[m.end():]
            ctor = re.match(r"\s*(?:(?:private|public)\s+constructor\s*)?\(", rest)
            if ctor:
                o = m.end() + ctor.end() - 1
                params = text[o + 1:match_close(text, o)]
                for p in split_top(params):
                    pm = re.match(r"(?:(?:public|override)\s+)+(?:val|var)\s+(\w+)", p)
                    if pm:
                        items.add(f"{name}.{pm.group(1)}")
                if "private constructor" not in ctor.group(0):
                    for p in kotlin_params(params):
                        items.add(f"{name}.init({p})")
            if re.search(r"\benum\s+class\b", m.group(0)):
                brace = text.index("{", m.end())
                body = text[brace + 1:match_close(text, brace)].split(";")[0]
                for e in re.findall(r"^\s*([A-Z][A-Z0-9_]*)\s*,?\s*$", body, flags=re.M):
                    items.add(f"{name}.{snake_to_camel(e)}")
        for mm in re.finditer(r"\bpublic\s+(?:(?:abstract|open|inline|operator)\s+)*(fun|val|var|constructor)\b"
                              r"\s*(?:<[^>]*>\s*)?(\w+)?", text):
            kind, name = mm.group(1), mm.group(2)
            if name == "interface":
                continue
            owner = owner_at(fr, mm.start())
            # Skip primary-constructor properties (handled above).
            if owner is None and kind in ("val", "var"):
                line = text[text.rfind("\n", 0, mm.start()):mm.start()]
                if line.strip() == "" and not text[:mm.start()].rstrip().endswith(("}", ")")):
                    pass
            if kind == "constructor":
                o = text.index("(", mm.start())
                for p in kotlin_params(text[o + 1:match_close(text, o)]):
                    items.add(f"{owner}.init({p})")
                continue
            if kind == "fun":
                o = text.index("(", mm.end())
                add(items, owner, name, kotlin_params(text[o + 1:match_close(text, o)]))
            elif in_parens(text, mm.start()):
                continue  # primary-constructor property, already recorded
            else:
                add(items, owner, name)
        # `override val message/cause` in MapError's primary constructor are public API.
        for mm in re.finditer(r"\boverride\s+val\s+(message|cause)\b", text):
            items.add(f"MapError.{mm.group(1)}")
    return items


def in_parens(text, pos):
    depth = 0
    for ch in reversed(text[:pos]):
        if ch in ")]":
            depth += 1
        elif ch in "([":
            if depth == 0:
                return True
            depth -= 1
        elif ch in "{}":
            return False
    return False


def parse_swift():
    items = set()
    for f in SWIFT_FILES:
        text = strip_strings(strip_comments(f.read_text()))
        fr = frames(text, SWIFT_TYPE, True)
        for m in SWIFT_TYPE.finditer(text):
            kind, name = m.group(1), m.group(2)
            items.add(name)
            brace = text.index("{", m.end())
            body = text[brace + 1:match_close(text, brace)]
            if kind == "enum":
                for c in re.findall(r"\bcase\s+`?(\w+)`?", body):
                    items.add(f"{name}.{c}")
            if kind == "protocol":
                for fm in re.finditer(r"\bfunc\s+(\w+)\s*\(", body):
                    o = fm.end() - 1
                    add(items, name, fm.group(1), swift_params(body[o + 1:match_close(body, o)]))
        for mm in re.finditer(r"(?<!override )\bpublic\s+(?:(?:static|final|required|nonisolated|weak|"
                              r"private\(set\))\s+)*(func|var|let|init)\b\s*`?(\w+)?`?", text):
            kind, name = mm.group(1), mm.group(2)
            owner = owner_at(fr, mm.start())
            if kind == "init":
                o = text.index("(", mm.start())
                params = swift_params(text[o + 1:match_close(text, o)])
                if owner == "RemaplyMap":
                    for p in params:
                        items.add(f"RemaplyMap({p})")
                elif params:
                    for p in params:
                        items.add(f"{owner}.init({p})")
                else:
                    items.add(f"{owner}.init")
            elif kind == "func":
                if name is None:
                    continue  # operator (==, <): Equatable / Comparable conformance
                o = text.index("(", mm.end())
                add(items, owner, name, swift_params(text[o + 1:match_close(text, o)]))
            elif name:
                add(items, owner, name)
    return items


def main():
    kotlin, swift = parse_kotlin(), parse_swift()
    if "--dump" in sys.argv:
        print("\n".join(sorted(kotlin)) + "\n----\n" + "\n".join(sorted(swift)))
        return 0
    only_kotlin = sorted(kotlin - swift - PLATFORM_ONLY["android"])
    only_swift = sorted(swift - kotlin - PLATFORM_ONLY["ios"])
    stale = sorted((PLATFORM_ONLY["android"] - kotlin) | (PLATFORM_ONLY["ios"] - swift))
    print(f"Public API items: {len(kotlin & swift)} identical on both platforms; "
          f"{len(PLATFORM_ONLY['android'])} Android-only and {len(PLATFORM_ONLY['ios'])} iOS-only (documented).")
    ok = not (only_kotlin or only_swift or stale)
    for label, rows in (("Only in Kotlin (Android)", only_kotlin), ("Only in Swift (iOS)", only_swift),
                        ("Allowlisted but not found (update PLATFORM_ONLY)", stale)):
        if rows:
            print(f"\n{label}:")
            print("\n".join(f"  - {r}" for r in rows))
    print("\nAPI name parity: OK" if ok else "\nAPI name parity: FAILED")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
