"""Elimina el fondo de las cuatro poses originales con rembg, sin sobrescribirlas.

Uso: python scripts/prepare_qhawa_mascot.py /ruta/a/originales
Salida predeterminada: drawable_output/ (PNG RGBA para revisar antes de integrar).
"""

import argparse
from pathlib import Path


FILES = {
    "guiño.jpg": "qhawa_mascot_greeting.png",
    "feliz.jpg": "qhawa_mascot_happy.png",
    "idle.jpg": "qhawa_mascot_idle.png",
    "alert_triste.jpg": "qhawa_mascot_alert.png",
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input_dir", type=Path, help="Carpeta de originales con fondo sólido")
    parser.add_argument("--output-dir", type=Path, default=Path("drawable_output"))
    args = parser.parse_args()

    missing = [name for name in FILES if not (args.input_dir / name).is_file()]
    if missing:
        parser.error("Faltan imágenes originales: " + ", ".join(missing))
    existing = [name for name in FILES.values() if (args.output_dir / name).exists()]
    if existing:
        parser.error("La salida ya contiene archivos; usa otra carpeta: " + ", ".join(existing))

    # Se importan después de validar las entradas para informar archivos faltantes
    # incluso antes de instalar las dependencias o descargar el modelo.
    from PIL import Image, ImageOps
    from rembg import new_session, remove

    args.output_dir.mkdir(parents=True, exist_ok=True)
    session = new_session("u2net", providers=["CPUExecutionProvider"])
    for source, target in FILES.items():
        with Image.open(args.input_dir / source) as original:
            image = ImageOps.exif_transpose(original).convert("RGB")
            output = remove(image, session=session).convert("RGBA")
        low, high = output.getchannel("A").getextrema()
        if low == high:
            raise RuntimeError(f"{source}: máscara uniforme; revisar la segmentación antes de integrar")
        destination = args.output_dir / target
        output.save(destination, "PNG")
        print(f"Generado: {destination} (RGBA, alfa {low}–{high})")


if __name__ == "__main__":
    main()
