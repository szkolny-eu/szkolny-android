import os
import sys
from datetime import datetime

import requests
from dotenv import load_dotenv

from _utils import get_changelog, get_commit_log, get_project_dir, read_gradle_version


def post_webhook(
    project_dir: str,
    apk_file: str,
    download_url: str,
    webhook_release: str,
    webhook_testing: str,
):
    (_, version_name) = read_gradle_version(project_dir)

    types = ["dev", "beta", "nightly", "daily", "rc", "release"]
    build_type = [x for x in types if x in version_name]
    build_type = build_type[0] if build_type else None

    testing = ["dev", "beta", "nightly", "daily"]
    testing = build_type in testing

    if testing:
        build_date = int(os.stat(apk_file).st_mtime)
        if build_date:
            build_date = datetime.fromtimestamp(build_date).strftime("%Y-%m-%d %H:%M")

        # untagged release, get commit log
        if build_type in ["nightly", "daily"]:
            changelog = get_commit_log(project_dir, format="markdown", max_lines=5)
        else:
            changelog = get_changelog(project_dir, format="markdown")

        webhook = get_webhook_testing(
            version_name, build_type, changelog, download_url, build_date
        )
        requests.post(url=webhook_testing, json=webhook)
    else:
        changelog = get_changelog(project_dir, format="markdown")
        webhook = get_webhook_release(version_name, changelog, download_url)
        requests.post(url=webhook_release, json=webhook)


def get_webhook_release(version_name: str, changelog: str, download_url: str):
    (title, content) = changelog
    return {
        "content": (
            f"__**{title}**__\n{content}\n[Szkolny.eu {version_name}]({download_url})"
        ),
    }


def get_webhook_testing(
    version_name: str,
    build_type: str,
    changelog: str,
    download_url: str,
    build_date: str,
):
    return {
        "embeds": [
            {
                "title": f"Nowa wersja {build_type} aplikacji Szkolny.eu",
                "description": f"Dostępna jest nowa wersja testowa **{build_type}**.",
                "color": 2201331,
                "fields": [
                    {
                        "name": f"Wersja `{version_name}`",
                        "value": (
                            f"[Pobierz .APK]({download_url})"
                            if download_url
                            else "*Pobieranie niedostępne*"
                        ),
                        "inline": False,
                    },
                    {
                        "name": "Data kompilacji",
                        "value": build_date or "-",
                        "inline": False,
                    },
                    {
                        "name": "Ostatnie zmiany",
                        "value": changelog or "-",
                        "inline": False,
                    },
                ],
            }
        ]
    }


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: webhook_discord.py <project dir>")
        exit(-1)

    project_dir = get_project_dir()

    load_dotenv()
    APK_FILE = os.getenv("APK_FILE")
    DOWNLOAD_URL = os.getenv("DOWNLOAD_URL")
    WEBHOOK_RELEASE = os.getenv("WEBHOOK_RELEASE")
    WEBHOOK_TESTING = os.getenv("WEBHOOK_TESTING")

    post_webhook(
        project_dir,
        APK_FILE,
        DOWNLOAD_URL,
        WEBHOOK_RELEASE,
        WEBHOOK_TESTING,
    )
