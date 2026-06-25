import argparse
import json
from datetime import datetime
from pathlib import Path


def signal(value: int) -> int:
    return max(0, min(100, value))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Create a local Constellation Pulse seal.")
    parser.add_argument("--message", default="oggi tremo ma resto lucido.")
    parser.add_argument("--valence", type=int, default=65)
    parser.add_argument("--arousal", type=int, default=50)
    parser.add_argument("--energy", type=int, default=70)
    parser.add_argument("--focus", type=int, default=40)
    parser.add_argument("--social", type=int, default=55)
    parser.add_argument("--data-dir", default="data")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    data_dir = Path(args.data_dir)
    data_dir.mkdir(parents=True, exist_ok=True)
    today = datetime.now().strftime("%Y%m%d")

    mood = {
        "date": today,
        "createdAtMillis": int(datetime.now().timestamp() * 1000),
        "message": args.message[:140],
        "valence": signal(args.valence),
        "arousal": signal(args.arousal),
        "energy": signal(args.energy),
        "focus": signal(args.focus),
        "social": signal(args.social),
    }

    output_path = data_dir / f"{today}.json"
    output_path.write_text(
        json.dumps(mood, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    print(f"Sigillo salvato -> {output_path}")


if __name__ == "__main__":
    main()
