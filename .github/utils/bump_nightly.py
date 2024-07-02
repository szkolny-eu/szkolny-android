import re
import sys
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

from _utils import (
    get_commit_log,
    get_project_dir,
    read_gradle_version,
    write_gradle_version,
)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: bump_nightly.py <project dir>")
        exit(-1)

    project_dir = get_project_dir()

    (version_code, version_name) = read_gradle_version(project_dir)
    version_name = version_name.split("+")[0]

    date = datetime.now(tz=ZoneInfo("Europe/Warsaw"))
    if date.hour > 6:
        version_name += "+daily." + date.strftime("%Y%m%d-%H%M")
    else:
        date -= timedelta(days=1)
        version_name += "+nightly." + date.strftime("%Y%m%d")

    print("appVersionName=" + version_name)
    print("appVersionCode=" + str(version_code))

    write_gradle_version(project_dir, version_code, version_name)

    commit_log = get_commit_log(project_dir, format="html", max_lines=10)

    with open(
        f"{project_dir}/app/src/main/assets/pl-changelog.html", "r", encoding="utf-8"
    ) as f:
        changelog = f.read()

    changelog = re.sub(r"<h3>(.+?)</h3>", f"<h3>{version_name}</h3>", changelog)
    changelog = re.sub(r"(?s)<ul>(.+)</ul>", f"<ul>\n{commit_log}\n</ul>", changelog)

    with open(
        f"{project_dir}/app/src/main/assets/pl-changelog.html", "w", encoding="utf-8"
    ) as f:
        f.write(changelog)
