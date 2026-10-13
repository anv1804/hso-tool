"""Package the clean game compilation without replacing a running client's JAR."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED


def main():
    root = Path(__file__).resolve().parent.parent
    classes = root / "patch_bin" / "remote_sell_game"
    compiled = {p.relative_to(classes).as_posix(): p for p in classes.rglob("*.class")}
    required = "com/silverknight/RemoteSellController.class"
    if required not in compiled:
        raise SystemExit("Run tools/test_remote_sell.ps1 successfully before staging the JAR")
    pending = root / "KnightOnline_v403.jar.pending"
    temporary = pending.with_suffix(".pending.tmp")
    with ZipFile(root / "KnightOnline_v403.jar") as original, ZipFile(temporary, "w", ZIP_DEFLATED) as output:
        for entry in original.infolist():
            # All game classes are rebuilt; retain resources and the original manifest.
            if not entry.filename.endswith(".class"):
                output.writestr(entry, original.read(entry.filename))
        for name, path in sorted(compiled.items()):
            output.write(path, name)
    with ZipFile(temporary) as check:
        if check.testzip() is not None or required not in check.namelist():
            raise SystemExit("Staged JAR verification failed")
    temporary.replace(pending)
    print(f"Staged {len(compiled)} game classes: {pending}")


if __name__ == "__main__":
    main()
