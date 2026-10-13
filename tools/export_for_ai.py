#!/usr/bin/env python3
"""Export Baqiyat repository source into plain-text files for AI review.

Safety: excludes local secrets, credentials, binaries, generated build trees,
node_modules, Gradle caches, APKs, and google-services.json.
The script does not change application source files.
"""
from pathlib import Path
import hashlib

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "exports" / "ai-review"
BRANCH = "ai-review-plain-text-export"
MAX_PART_BYTES = 95_000

TEXT_EXTENSIONS = {
    ".kt", ".kts", ".java", ".xml", ".js", ".mjs", ".cjs", ".html",
    ".css", ".md", ".json", ".json5", ".rules", ".yml", ".yaml",
    ".toml", ".properties", ".pro", ".gradle", ".txt", ".sh", ".bat",
    ".gitignore", ".firebaserc", ".sql", ".ts", ".tsx", ".jsx", ".gradle.kts",
}
TEXT_NAMES = {
    "gradlew", "gradlew.bat", "Makefile", "Dockerfile", "LICENSE",
    ".env.example", "firebase.json", ".firebaserc",
}
SECRET_NAME_PARTS = (
    "service-account", "service_account", "credentials", "secrets",
    "private-key", "private_key", "id_rsa",
)
EXCLUDED_DIRS = {
    ".git", ".gradle", "build", "node_modules", ".idea", ".firebase",
    "dist", "out", "target", "coverage",
}
EXCLUDED_NAMES = {
    ".env", "local.properties", "google-services.json",
    "package-lock.json", "npm-shrinkwrap.json", "yarn.lock", "pnpm-lock.yaml",
    "gradle-wrapper.jar",
}
EXCLUDED_SUFFIXES = {
    ".apk", ".aab", ".jar", ".class", ".dex", ".so", ".png", ".jpg",
    ".jpeg", ".webp", ".gif", ".ico", ".mp3", ".wav", ".ogg", ".mp4",
    ".pdf", ".zip", ".gz", ".tar", ".keystore", ".jks", ".pem", ".p12",
    ".pfx", ".db", ".sqlite", ".bin",
}

def is_text_source(p: Path) -> bool:
    rel = p.relative_to(ROOT)
    parts = set(rel.parts)
    if any(part in EXCLUDED_DIRS for part in rel.parts):
        return False
    if any(part.startswith(".") and part not in {".github"} for part in rel.parts[:-1]):
        return False
    name = p.name.lower()
    if name in EXCLUDED_NAMES or name.endswith(".lock"):
        return False
    if any(token in name for token in SECRET_NAME_PARTS):
        return False
    if p.suffix.lower() in EXCLUDED_SUFFIXES:
        return False
    if name.startswith(".env.") and name != ".env.example":
        return False
    if name in TEXT_NAMES or p.suffix.lower() in TEXT_EXTENSIONS:
        return True
    return False

def category_for(rel: str) -> str:
    if rel.startswith("admin-panel/"):
        return "ADMIN-PANEL"
    if rel.startswith("app/"):
        return "ANDROID-APP"
    if (rel.startswith("firebase/") or rel == "firestore.rules"
        or rel in {"firebase.json", ".firebaserc", ".env.example", "storage.rules"}):
        return "FIREBASE-BACKEND"
    return "ROOT-CONFIG-AND-CI"

def read_utf8(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("*.txt"):
        old.unlink()

    files = []
    for p in sorted(ROOT.rglob("*")):
        if not p.is_file():
            continue
        rel = p.relative_to(ROOT).as_posix()
        # Never export this exporter, this workflow, previous exports, or secrets.
        if rel.startswith("exports/") or rel == "tools/export_for_ai.py":
            continue
        if rel == ".github/workflows/export-ai-snapshot.yml":
            continue
        if is_text_source(p):
            try:
                text = read_utf8(p)
            except OSError:
                continue
            files.append((rel, text))

    groups = {"ADMIN-PANEL": [], "ANDROID-APP": [], "FIREBASE-BACKEND": [], "ROOT-CONFIG-AND-CI": []}
    for rel, body in files:
        groups[category_for(rel)].append((rel, body))

    outputs = []
    included_manifest = []
    for group, entries in groups.items():
        if not entries:
            continue
        part_no = 1
        chunks = []
        current_bytes = 0
        for rel, body in entries:
            entry = (
                f"\n{'=' * 88}\nSOURCE FILE: {rel}\n"
                f"{'=' * 88}\n{body.rstrip()}\n"
                f"END SOURCE FILE: {rel}\n"
            )
            size = len(entry.encode("utf-8"))
            if chunks and current_bytes + size > MAX_PART_BYTES:
                filename = f"{group}-PART-{part_no:02d}.txt"
                payload = (
                    "BAQIYAT — SOURCE EXPORT FOR AI REVIEW\n"
                    f"Component: {group}\nPart: {part_no:02d}\n"
                    "Source branch: main (export generated on a separate branch)\n"
                    "Encoding: UTF-8. Original relative paths are marked per file.\n"
                    "This is a review/export format, not a compilable project folder.\n"
                    + "".join(chunks)
                )
                dst = OUT / filename
                dst.write_text(payload, encoding="utf-8", newline="\n")
                digest = hashlib.sha256(payload.encode("utf-8")).hexdigest()
                outputs.append((filename, len(payload.encode("utf-8")), digest))
                included_manifest.extend((filename, rel0) for rel0, _ in [])
                chunks, current_bytes = [], 0
                part_no += 1
            # If an unusually large file exceeds the cap, still keep that entire file together.
            chunks.append(entry)
            current_bytes += size
        if chunks:
            filename = f"{group}-PART-{part_no:02d}.txt"
            payload = (
                "BAQIYAT — SOURCE EXPORT FOR AI REVIEW\n"
                f"Component: {group}\nPart: {part_no:02d}\n"
                "Source branch: main (export generated on a separate branch)\n"
                "Encoding: UTF-8. Original relative paths are marked per file.\n"
                "This is a review/export format, not a compilable project folder.\n"
                + "".join(chunks)
            )
            dst = OUT / filename
            dst.write_text(payload, encoding="utf-8", newline="\n")
            outputs.append((filename, len(payload.encode("utf-8")), hashlib.sha256(payload.encode("utf-8")).hexdigest()))

    source_list = "\n".join(f"- {rel}" for rel, _ in files)
    export_list = "\n".join(f"- {name} — {size:,} bytes — SHA-256 {digest}" for name, size, digest in outputs)
    index = (
        "مشروع الباقيات — فهرس ملفات النصوص للمراجعة بواسطة أدوات ذكاء اصطناعي\n"
        "====================================================================\n\n"
        f"Repository: https://github.com/athar2026library-art/-..1\n"
        f"Original branch: main\nExport branch: {BRANCH}\n"
        "صيغة التصدير: TXT UTF-8 غير مضغوط. كل جزء يحتوي مسار الملف الأصلي ومحتواه.\n\n"
        "طريقة الاستخدام:\n"
        "1) ارفع ملفات ADMIN-PANEL لمراجعة لوحة المالك.\n"
        "2) ارفع كل ملفات ANDROID-APP-PART بالتتابع لمراجعة تطبيق أندرويد.\n"
        "3) أرفق FIREBASE-BACKEND وROOT-CONFIG-AND-CI عند مراجعة التكامل أو البناء.\n"
        "4) احتفظ بعناوين SOURCE FILE وEND SOURCE FILE عند اقتباس أي تعديل؛ فهي تحدد مسار الملف الحقيقي.\n\n"
        "ملاحظات الاستبعاد:\n"
        "- لم تُضمّن ملفات البناء الثنائية أو APK/AAB أو مجلدات build/node_modules/.gradle.\n"
        "- لم تُضمّن package-lock/yarn/pnpm lockfiles لتقليل الحجم؛ لم تُحذف ملفات مصدر التطبيق بسبب ذلك.\n"
        "- استُبعد app/google-services.json وlocal.properties وملفات المفاتيح/الأسرار. أعد تهيئة إعدادات Firebase محليًا عند البناء.\n"
        "- لا يثبت هذا التصدير أن التطبيق اختُبر على جهاز فعلي؛ راجع تقرير حالة المشروع ضمن قسم ROOT-CONFIG-AND-CI إن كان مضمّنًا.\n\n"
        f"عدد الملفات المصدرية المضمّنة: {len(files)}\n\n"
        "ملفات التصدير:\n" + export_list + "\n\n"
        "قائمة ملفات المصدر المضمّنة:\n" + source_list + "\n"
    )
    (OUT / "INDEX-README.txt").write_text(index, encoding="utf-8", newline="\n")
    print(f"Source files included: {len(files)}")
    print(f"Export text files: {len(outputs) + 1}")
    for name, size, _ in outputs:
        print(f"{name}\t{size}")
    print("INDEX-README.txt\t" + str((OUT / "INDEX-README.txt").stat().st_size))

if __name__ == "__main__":
    main()
