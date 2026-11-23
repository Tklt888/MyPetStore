#!/usr/bin/env python3
"""Download unique Unsplash images for the 54 on-sale MyPetStore products.

The Unsplash access key is read only from UNSPLASH_ACCESS_KEY. Progress and
credits are updated after every successfully downloaded image so the command
can be safely re-run after a rate-limit response or interruption.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
import sys
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit
from urllib.request import Request, urlopen


SEARCH_URL = "https://api.unsplash.com/search/photos"
USER_AGENT = "MyPetStore-course-image-downloader/1.0"
OUTPUT_SIZE = (800, 800)
SEARCH_RESULTS_PER_PAGE = 30
MAX_SEARCH_PAGES = 10

# Keywords approved in step 2. Supply queries deliberately describe the item
# as a product-only still life and do not request an animal subject.
PRODUCTS: tuple[tuple[str, str, str], ...] = (
    ("K9-BGL-03", "比格犬", "beagle dog portrait blue background"),
    ("K9-PUP-01", "比格犬幼宠", "beagle puppy portrait light background"),
    ("K9-RTV-11", "金毛犬养护套装", "grooming brush shampoo care kit product only flat lay"),
    ("K9-LEASH-01", "犬用出行套装", "leash collar travel accessories product only flat lay"),
    ("FL-DLH-02", "成年波斯猫", "adult Persian cat portrait light background"),
    ("CT-SHORT-01", "短毛猫", "domestic shorthair cat portrait studio"),
    ("CT-SIA-05", "暹罗猫养护套装", "grooming comb nail clipper care set product only"),
    ("CT-BED-01", "猫咪舒适用品套装", "cozy pet bed blanket cushion product only interior"),
    ("AQ-KOI-04", "观赏锦鲤", "colorful koi fish pond close up"),
    ("AQ-GOLD-01", "观赏金鱼", "goldfish aquarium portrait blue water"),
    ("AQ-CRF-12", "珊瑚礁水族套装", "coral aquarium decoration kit product only"),
    ("BR-CNR-08", "金丝雀", "yellow canary bird portrait light background"),
    ("BR-FINCH-01", "文鸟", "Java sparrow bird portrait studio"),
    ("BRD-MAC-07", "金刚鹦鹉养护套装", "bird cage care accessories product only flat lay"),
    ("BR-BOWL-01", "鸟类日常用品套装", "small feeder water dispenser accessories product only"),
    ("RP-SNK-02", "玉米蛇", "corn snake"),
    ("RP-TURTLE-01", "观赏龟", "small pet turtle portrait light background"),
    ("RP-IGU-09", "绿鬣蜥养护套装", "terrarium accessories"),
    ("DOG-LEASH-26", "轻便反光牵引绳", "leash flat lay"),
    ("CAT-LITTER-26", "豆腐猫砂2.5kg", "natural pellets texture"),
    ("FISH-FILTER-26", "鱼缸静音过滤器", "aquarium equipment"),
    ("BIRD-PERCH-26", "天然木质站杆", "natural wood branch"),
    ("REP-HIDE-26", "爬宠树洞躲避屋", "hollow log"),
    ("DOG-BRUSH-26", "犬用双面梳", "grooming brush flat lay"),
    ("CAT-BED-26", "可拆洗猫窝", "cozy round pet bed"),
    ("FISH-FOOD-26", "观赏鱼颗粒饲料100g", "food pellets"),
    ("BIRD-FOOD-26", "鸟用混合谷物500g", "mixed grains flat lay"),
    ("DOG-BOWL-26", "不锈钢防滑食盆", "stainless steel bowl"),
    ("CAT-TEASER-26", "羽毛逗猫棒", "feather toy flat lay"),
    ("FISH-GRAVEL-26", "鱼缸造景底砂1kg", "colorful gravel texture"),
    ("BIRD-BATH-26", "鸟用挂式浴盆", "small ceramic bowl"),
    ("REP-THERMO-26", "爬宠数字温湿度计", "digital thermometer"),
    ("REP-DISH-26", "爬宠浅口食盆", "stone bowl"),
    ("BIRD-COVER-26", "透气鸟笼罩", "light fabric cover"),
    ("CAT-SCRATCH-26", "瓦楞纸猫抓板", "corrugated cardboard texture"),
    ("DOG-FOOD-26", "成年犬日常粮2kg", "dry food kibble"),
    ("DOG-GOLDEN-26", "金毛寻回犬", "golden retriever dog portrait outdoors"),
    ("CAT-BRITISH-26", "英国短毛猫", "British shorthair cat portrait light background"),
    ("DOG-LABRADOR-26", "拉布拉多寻回犬", "Labrador retriever dog portrait outdoors"),
    ("DOG-SHIBA-26", "柴犬", "Shiba Inu dog portrait studio"),
    ("CAT-AMERICAN-26", "美国短毛猫", "American shorthair cat portrait studio"),
    ("CAT-SIAMESE-26", "暹罗猫", "Siamese cat portrait light background"),
    ("FISH-BETTA-26", "暹罗斗鱼", "Siamese fighting betta fish aquarium close up"),
    ("FISH-NEON-26", "霓虹灯鱼", "neon tetra fish aquarium blue water"),
    ("FISH-GOLDFISH-26", "金鱼", "orange goldfish aquarium close up"),
    ("BIRD-BUDGIE-26", "虎皮鹦鹉", "budgerigar parakeet portrait light background"),
    ("BIRD-COCKATIEL-26", "玄凤鹦鹉", "cockatiel bird portrait studio"),
    ("BIRD-LOVEBIRD-26", "桃脸牡丹鹦鹉", "peach faced lovebird portrait"),
    ("BIRD-FINCH-26", "斑胸草雀", "zebra finch bird portrait natural light"),
    ("REP-LEOPARD-26", "豹纹守宫", "leopard gecko portrait light background"),
    ("REP-CRESTED-26", "睫角守宫", "crested gecko portrait natural light"),
    ("DOG-CORGI-26", "威尔士柯基犬", "Pembroke Welsh corgi dog portrait outdoors"),
    ("FISH-GUPPY-26", "孔雀鱼", "colorful guppy fish aquarium close up"),
    ("CAT-RAGDOLL-26", "布偶猫", "Ragdoll cat portrait light background"),
)

# Unsplash is a photography library rather than a product catalogue. If an
# approved query returns no unused result, retry with one broader visual term.
# Supply fallbacks intentionally contain no animal subject.
FALLBACK_KEYWORDS: dict[str, str] = {
    "DOG-LEASH-26": "rope",
    "CAT-LITTER-26": "wood pellets",
    "FISH-FILTER-26": "water filter",
    "BIRD-PERCH-26": "wood branch",
    "REP-HIDE-26": "hollow wood",
    "DOG-BRUSH-26": "brush",
    "CAT-BED-26": "round bed",
    "FISH-FOOD-26": "pellets",
    "BIRD-FOOD-26": "grain",
    "DOG-BOWL-26": "steel bowl",
    "CAT-TEASER-26": "feather",
    "FISH-GRAVEL-26": "gravel",
    "BIRD-BATH-26": "ceramic bowl",
    "REP-THERMO-26": "thermometer",
    "REP-DISH-26": "stone bowl",
    "BIRD-COVER-26": "fabric",
    "CAT-SCRATCH-26": "cardboard",
    "DOG-FOOD-26": "dry food",
    "DOG-GOLDEN-26": "golden retriever",
    "CAT-BRITISH-26": "British shorthair",
    "DOG-LABRADOR-26": "Labrador retriever",
    "DOG-SHIBA-26": "Shiba Inu",
    "CAT-AMERICAN-26": "American shorthair",
    "CAT-SIAMESE-26": "Siamese cat",
    "FISH-BETTA-26": "betta fish",
    "FISH-NEON-26": "neon tetra",
    "FISH-GOLDFISH-26": "goldfish",
    "BIRD-BUDGIE-26": "budgerigar",
    "BIRD-COCKATIEL-26": "cockatiel",
    "BIRD-LOVEBIRD-26": "lovebird",
    "BIRD-FINCH-26": "zebra finch",
    "REP-LEOPARD-26": "leopard gecko",
    "REP-CRESTED-26": "crested gecko",
    "DOG-CORGI-26": "Welsh corgi",
    "FISH-GUPPY-26": "guppy fish",
    "CAT-RAGDOLL-26": "Ragdoll cat",
}

# A final broad species query is used only for live-pet products when both the
# approved breed query and its shorter breed name have no unused result.
LAST_RESORT_KEYWORDS: dict[str, str] = {
    "FISH-NEON-26": "tropical fish",
    "FISH-GOLDFISH-26": "aquarium fish",
    "BIRD-BUDGIE-26": "parakeet bird",
    "BIRD-COCKATIEL-26": "parrot bird",
    "BIRD-LOVEBIRD-26": "small parrot",
    "BIRD-FINCH-26": "small bird",
    "REP-LEOPARD-26": "gecko lizard",
    "REP-CRESTED-26": "gecko lizard",
    "DOG-CORGI-26": "corgi dog",
    "FISH-GUPPY-26": "tropical fish",
    "CAT-RAGDOLL-26": "longhair cat",
}


class RateLimitError(RuntimeError):
    """Raised when Unsplash returns HTTP 403 or 429."""


class NoUnusedPhotoError(RuntimeError):
    """Raised when a search query contains no unused photo."""


def request_bytes(url: str, access_key: str | None = None) -> tuple[bytes, Any]:
    headers = {"User-Agent": USER_AGENT, "Accept": "application/json"}
    if access_key:
        headers["Authorization"] = f"Client-ID {access_key}"
    request = Request(url, headers=headers)
    try:
        with urlopen(request, timeout=45) as response:
            return response.read(), response.headers
    except HTTPError as error:
        if error.code in (403, 429):
            reset = error.headers.get("X-Ratelimit-Reset", "unknown")
            remaining = error.headers.get("X-Ratelimit-Remaining", "0")
            raise RateLimitError(
                f"Unsplash rate limit reached (HTTP {error.code}, "
                f"remaining={remaining}, reset={reset})"
            ) from error
        detail = error.read().decode("utf-8", errors="replace")[:500]
        raise RuntimeError(f"HTTP {error.code} for {url}: {detail}") from error
    except URLError as error:
        raise RuntimeError(f"Network error for {url}: {error.reason}") from error


def request_json(url: str, access_key: str) -> tuple[dict[str, Any], Any]:
    body, headers = request_bytes(url, access_key)
    try:
        value = json.loads(body)
    except json.JSONDecodeError as error:
        raise RuntimeError(f"Unsplash returned invalid JSON for {url}") from error
    if not isinstance(value, dict):
        raise RuntimeError(f"Unexpected Unsplash response for {url}")
    return value, headers


def add_image_parameters(url: str) -> str:
    parts = urlsplit(url)
    params = dict(parse_qsl(parts.query, keep_blank_values=True))
    params.update({"w": "1200", "h": "1200", "fit": "crop", "fm": "jpg", "q": "90"})
    return urlunsplit((parts.scheme, parts.netloc, parts.path, urlencode(params), parts.fragment))


def find_unused_photo(keyword: str, used_ids: set[str], access_key: str) -> dict[str, Any]:
    for page in range(1, MAX_SEARCH_PAGES + 1):
        query = urlencode(
            {
                "query": keyword,
                "orientation": "squarish",
                "per_page": SEARCH_RESULTS_PER_PAGE,
                "page": page,
            }
        )
        payload, _ = request_json(f"{SEARCH_URL}?{query}", access_key)
        results = payload.get("results", [])
        if not isinstance(results, list):
            raise RuntimeError(f"Malformed search results for keyword: {keyword}")
        for photo in results:
            if isinstance(photo, dict) and photo.get("id") not in used_ids:
                return photo
        if not results or page >= int(payload.get("total_pages", page)):
            break
    raise NoUnusedPhotoError(f"No unused Unsplash image found for keyword: {keyword}")


def find_photo_for_product(
    sku: str, keyword: str, used_ids: set[str], access_key: str
) -> tuple[dict[str, Any], str]:
    queries = [
        keyword,
        FALLBACK_KEYWORDS.get(sku),
        LAST_RESORT_KEYWORDS.get(sku),
    ]
    attempted: list[str] = []
    last_error: NoUnusedPhotoError | None = None
    for query in queries:
        if not query or query in attempted:
            continue
        if attempted:
            print(f"FALLBACK {sku}: {query}")
        attempted.append(query)
        try:
            return find_unused_photo(query, used_ids, access_key), query
        except NoUnusedPhotoError as error:
            last_error = error
    if last_error:
        raise last_error
    raise NoUnusedPhotoError(f"No search keyword configured for product: {sku}")


def save_square_jpeg(image_data: bytes, destination: Path) -> None:
    try:
        from PIL import Image, ImageOps
    except ImportError as error:
        raise RuntimeError(
            "Pillow is required. Install it with: python3 -m pip install Pillow"
        ) from error

    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(destination.suffix + ".part")
    try:
        with Image.open(io.BytesIO(image_data)) as source:
            source = ImageOps.exif_transpose(source).convert("RGB")
            result = ImageOps.fit(source, OUTPUT_SIZE, method=Image.Resampling.LANCZOS)
            result.save(temporary, format="JPEG", quality=90, optimize=True, progressive=True)
        os.replace(temporary, destination)
    finally:
        temporary.unlink(missing_ok=True)


def load_progress(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {"version": 1, "completed": {}}
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise RuntimeError(f"Cannot read progress file {path}: {error}") from error
    if not isinstance(value.get("completed"), dict):
        raise RuntimeError(f"Invalid progress file: {path}")
    return value


def save_progress(path: Path, progress: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(progress, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    os.replace(temporary, path)


def write_credits(path: Path, progress: dict[str, Any]) -> None:
    completed = progress["completed"]
    lines = [
        "# MyPetStore image credits",
        "",
        "Images are provided by [Unsplash](https://unsplash.com).",
        "",
        "| Product | Image | Photographer | Source |",
        "|---|---|---|---|",
    ]
    for sku, name, _ in PRODUCTS:
        item = completed.get(sku)
        if not item:
            continue
        photographer = item["photographer"].replace("|", "\\|")
        lines.append(
            f"| {sku} · {name} | `{item['filename']}` | "
            f"[{photographer}]({item['photographer_url']}) | "
            f"[Unsplash photo]({item['source_url']}) |"
        )
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def photo_metadata(
    sku: str, name: str, keyword: str, filename: str, photo: dict[str, Any]
) -> dict[str, str]:
    user = photo.get("user") or {}
    links = photo.get("links") or {}
    return {
        "sku": sku,
        "name": name,
        "keyword": keyword,
        "filename": filename,
        "photo_id": str(photo["id"]),
        "photographer": str(user.get("name") or user.get("username") or "Unknown"),
        "photographer_url": str(
            links_or(user.get("links") if isinstance(user, dict) else None, "html", "https://unsplash.com")
        ),
        "source_url": str(links_or(links, "html", "https://unsplash.com")),
        "download_location": str(links_or(links, "download_location", "")),
    }


def links_or(mapping: Any, key: str, fallback: str) -> str:
    return mapping.get(key, fallback) if isinstance(mapping, dict) else fallback


def validate_photo(photo: dict[str, Any]) -> None:
    missing = []
    if not photo.get("id"):
        missing.append("id")
    if not links_or(photo.get("urls"), "raw", ""):
        missing.append("urls.raw")
    if not links_or(photo.get("links"), "download_location", ""):
        missing.append("links.download_location")
    if missing:
        raise RuntimeError(f"Unsplash photo is missing required fields: {', '.join(missing)}")


def parse_args() -> argparse.Namespace:
    project_root = Path(__file__).resolve().parent.parent
    default_output = project_root / "src/main/resources/static/images/catalog"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=default_output,
        help=f"image output directory (default: {default_output})",
    )
    parser.add_argument(
        "--max-items",
        type=int,
        default=None,
        help="maximum new images to download in this run",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    access_key = os.environ.get("UNSPLASH_ACCESS_KEY", "").strip()
    if not access_key:
        print("Error: environment variable UNSPLASH_ACCESS_KEY is not set.", file=sys.stderr)
        return 2
    if args.max_items is not None and args.max_items < 1:
        print("Error: --max-items must be at least 1.", file=sys.stderr)
        return 2

    output_dir: Path = args.output_dir.resolve()
    output_dir.mkdir(parents=True, exist_ok=True)
    progress_path = output_dir / ".unsplash-progress.json"
    credits_path = output_dir / "credits.md"
    progress = load_progress(progress_path)
    completed: dict[str, Any] = progress["completed"]
    used_ids = {
        str(item["photo_id"])
        for item in completed.values()
        if isinstance(item, dict) and item.get("photo_id")
    }

    downloaded_this_run = 0
    try:
        for sku, name, keyword in PRODUCTS:
            destination = output_dir / f"{sku}.jpg"
            if sku in completed and destination.is_file():
                print(f"SKIP {sku}: already completed")
                continue
            if destination.is_file() and sku not in completed:
                print(
                    f"SKIP {sku}: {destination.name} already exists but has no progress metadata; "
                    "leaving it untouched",
                    file=sys.stderr,
                )
                continue
            if args.max_items is not None and downloaded_this_run >= args.max_items:
                break

            print(f"SEARCH {sku} · {name}: {keyword}")
            photo, actual_keyword = find_photo_for_product(
                sku, keyword, used_ids, access_key
            )
            validate_photo(photo)

            # Required by the Unsplash API guidelines for every downloaded image.
            download_location = links_or(photo.get("links"), "download_location", "")
            request_json(download_location, access_key)

            raw_url = links_or(photo.get("urls"), "raw", "")
            image_data, _ = request_bytes(add_image_parameters(raw_url))
            save_square_jpeg(image_data, destination)

            metadata = photo_metadata(
                sku, name, actual_keyword, destination.name, photo
            )
            metadata["sha256"] = hashlib.sha256(destination.read_bytes()).hexdigest()
            completed[sku] = metadata
            used_ids.add(metadata["photo_id"])
            save_progress(progress_path, progress)
            write_credits(credits_path, progress)
            downloaded_this_run += 1
            print(f"SAVED {destination} ({downloaded_this_run} new this run)")
    except RateLimitError as error:
        save_progress(progress_path, progress)
        write_credits(credits_path, progress)
        print(f"STOPPED: {error}", file=sys.stderr)
        print(f"Progress saved to {progress_path}", file=sys.stderr)
        return 75
    except KeyboardInterrupt:
        save_progress(progress_path, progress)
        write_credits(credits_path, progress)
        print(f"\nInterrupted. Progress saved to {progress_path}", file=sys.stderr)
        return 130
    except Exception as error:
        save_progress(progress_path, progress)
        write_credits(credits_path, progress)
        print(f"FAILED: {error}", file=sys.stderr)
        print(f"Progress saved to {progress_path}", file=sys.stderr)
        return 1

    write_credits(credits_path, progress)
    print(f"Done: {len(completed)}/{len(PRODUCTS)} products completed.")
    print(f"Credits: {credits_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
