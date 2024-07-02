import glob
import os
import sys
from datetime import datetime
from time import time
from zoneinfo import ZoneInfo

import mysql.connector as mysql
from dotenv import load_dotenv

from _utils import get_changelog, get_commit_log, get_project_dir, read_gradle_version


def save_version(
    project_dir: str,
    db_host: str,
    db_user: str,
    db_pass: str,
    db_name: str,
    apk_server_release: str,
    apk_server_nightly: str,
):
    db = mysql.connect(
        host=db_host,
        user=db_user,
        password=db_pass,
        database=db_name,
        auth_plugin="mysql_native_password",
    )

    (version_code, version_name) = read_gradle_version(project_dir)
    (_, changelog) = get_changelog(project_dir, format="html")

    types = ["dev", "beta", "nightly", "daily", "rc", "release"]
    build_type = [x for x in types if x in version_name]
    build_type = build_type[0] if build_type else "release"

    if "+nightly." in version_name or "+daily." in version_name:
        changelog = get_commit_log(project_dir, format="html")
        build_type = "nightly"
    elif "-dev" in version_name:
        build_type = "dev"
    elif "-beta." in version_name:
        build_type = "beta"
    elif "-rc." in version_name:
        build_type = "rc"

    build_date = int(time())
    apk_name = None
    bundle_name_play = None

    files = glob.glob(f"{project_dir}/app/release/*.*")
    output_apk = f"Edziennik_{version_name}_official.apk"
    output_aab_play = f"Edziennik_{version_name}_play.aab"
    for file in files:
        if output_apk in file:
            build_date = int(os.stat(file).st_mtime)
            apk_name = output_apk
        if output_aab_play in file:
            build_date = int(os.stat(file).st_mtime)
            bundle_name_play = output_aab_play

    build_date = datetime.fromtimestamp(
        build_date,
        tz=ZoneInfo("Europe/Warsaw"),
    ).strftime("%Y-%m-%d %H:%M:%S")

    if build_type in ["nightly", "daily"]:
        download_url = apk_server_nightly + apk_name if apk_name else None
    else:
        # download_url = apk_server_release + apk_name if apk_name else None
        download_url = (
            f"https://github.com/szkolny-eu/szkolny-android/releases/download/v{version_name}/{apk_name}"
            if apk_name
            else None
        )
    if download_url:
        print("downloadUrl=" + download_url)

    cols = [
        "versionCode",
        "versionName",
        "releaseDate",
        "releaseNotes",
        "releaseType",
        "downloadUrl",
        "apkName",
        "bundleNamePlay",
    ]
    updated = {
        "versionCode": version_code,
        "downloadUrl": download_url,
        "apkName": apk_name,
        "bundleNamePlay": bundle_name_play,
    }

    values = [
        version_code,
        version_name,
        build_date,
        changelog,
        build_type,
        download_url,
        apk_name,
        bundle_name_play,
    ]
    values.extend(val for val in updated.values() if val)

    updated = ", ".join(f"{col} = %s" for (col, val) in updated.items() if val)

    sql = f"INSERT INTO updates ({', '.join(cols)}) VALUES ({'%s, ' * (len(cols) - 1)}%s) ON DUPLICATE KEY UPDATE {updated};"

    c = db.cursor()
    c.execute(sql, tuple(values))
    db.commit()


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: save_version.py <project dir>")
        exit(-1)

    project_dir = get_project_dir()

    load_dotenv()
    DB_HOST = os.getenv("DB_HOST")
    DB_USER = os.getenv("DB_USER")
    DB_PASS = os.getenv("DB_PASS")
    DB_NAME = os.getenv("DB_NAME")
    APK_SERVER_RELEASE = os.getenv("APK_SERVER_RELEASE")
    APK_SERVER_NIGHTLY = os.getenv("APK_SERVER_NIGHTLY")

    save_version(
        project_dir,
        DB_HOST,
        DB_USER,
        DB_PASS,
        DB_NAME,
        APK_SERVER_RELEASE,
        APK_SERVER_NIGHTLY,
    )
