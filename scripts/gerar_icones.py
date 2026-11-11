"""Gera os recursos nativos do launcher SkipPeças (requer Pillow)."""
from pathlib import Path
from PIL import Image, ImageDraw

RES = Path(__file__).resolve().parents[1] / "front/projeto_que_vou_apagar_dando_der/app/src/main/res"
RED = "#E51923"

# O desenho fica dentro da área segura do ícone adaptativo (66/108).
FOREGROUND = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#00000000" android:strokeColor="#FFFFFF"
        android:strokeWidth="6"
        android:pathData="M54,28 A26,26 0,1 1,54,80 A26,26 0,1 1,54,28 Z" />
    <path android:fillColor="#00000000" android:strokeColor="#FFFFFF"
        android:strokeWidth="6" android:strokeLineCap="round"
        android:pathData="M30,47 L47,54 M78,47 L61,54 M54,61 L54,78" />
    <path android:fillColor="#FFFFFF"
        android:pathData="M54,44 A10,10 0,1 1,54,64 A10,10 0,1 1,54,44 Z" />
</vector>
'''


def main():
    (RES / "drawable/ic_launcher_foreground.xml").write_text(FOREGROUND, encoding="utf-8")
    (RES / "drawable/ic_launcher_background.xml").write_text('''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="@color/red_primary" android:pathData="M0,0h108v108h-108z" />
</vector>
''', encoding="utf-8")

    for density, size in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        # Supersampling evita bordas serrilhadas nos recursos de Android 7.
        scale = size * 4
        for rounded in (False, True):
            image = Image.new("RGBA", (scale, scale), (0, 0, 0, 0))
            draw = ImageDraw.Draw(image)
            if rounded:
                draw.ellipse((0, 0, scale - 1, scale - 1), fill=RED)
            else:
                draw.rounded_rectangle((0, 0, scale - 1, scale - 1), radius=scale * 0.22, fill=RED)

            def point(x, y):
                return ((x - 54) * scale / 84 + scale / 2, (y - 54) * scale / 84 + scale / 2)

            width = round(6 * scale / 84)
            # Pillow traça a borda para dentro; ampliar em metade do traço.
            draw.ellipse((*point(25, 25), *point(83, 83)), outline="white", width=width)
            for start, end in [((30, 47), (47, 54)), ((78, 47), (61, 54)), ((54, 61), (54, 78))]:
                draw.line((point(*start), point(*end)), fill="white", width=width)
                for x, y in (start, end):
                    draw.ellipse((*point(x - 3, y - 3), *point(x + 3, y + 3)), fill="white")
            draw.ellipse((*point(44, 44), *point(64, 64)), fill="white")
            filename = "ic_launcher_round.webp" if rounded else "ic_launcher.webp"
            image.resize((size, size), Image.Resampling.LANCZOS).save(RES / f"mipmap-{density}" / filename, lossless=True)


if __name__ == "__main__":
    main()
