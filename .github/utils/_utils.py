import re
import subprocess
import sys
from datetime import datetime
from typing import Tuple

VERSION_NAME_REGEX = r'versionName: "(.+?)"'
VERSION_CODE_REGEX = r"versionCode: ([0-9]+)"
VERSION_NAME_FORMAT = 'versionName: "{}"'
VERSION_CODE_FORMAT = "versionCode: {}"


def get_project_dir() -> str:
    project_dir = sys.argv[1]
    if project_dir[-1:] == "/" or project_dir[-1:] == "\\":
        project_dir = project_dir[:-1]
    return project_dir


def read_gradle_version(project_dir: str) -> Tuple[int, str]:
    GRADLE_PATH = f"{project_dir}/build.gradle"

    with open(GRADLE_PATH, "r") as f:
        gradle = f.read()

    version_name = re.search(VERSION_NAME_REGEX, gradle).group(1)
    version_code = int(re.search(VERSION_CODE_REGEX, gradle).group(1))

    return (version_code, version_name)


def write_gradle_version(project_dir: str, version_code: int, version_name: str):
    GRADLE_PATH = f"{project_dir}/build.gradle"

    with open(GRADLE_PATH, "r") as f:
        gradle = f.read()

    gradle = re.sub(
        VERSION_NAME_REGEX, VERSION_NAME_FORMAT.format(version_name), gradle
    )
    gradle = re.sub(
        VERSION_CODE_REGEX, VERSION_CODE_FORMAT.format(version_code), gradle
    )

    with open(GRADLE_PATH, "w") as f:
        f.write(gradle)


def build_version_code(version_name: str) -> int:
    version = version_name.split("+")[0].split("-")
    version_base = version[0]
    version_suffix = version[1] if len(version) == 2 else ""

    base_parts = version_base.split(".")
    major = int(base_parts[0]) or 0
    minor = int(base_parts[1]) if len(base_parts) > 1 else 0
    patch = int(base_parts[2]) if len(base_parts) > 2 else 0

    beta = 9
    rc = 9
    if "dev" in version_suffix:
        beta = 0
        rc = 0
    elif "beta." in version_suffix:
        beta = int(version_suffix.split(".")[1])
        rc = 0
    elif "rc." in version_suffix:
        beta = 0
        rc = int(version_suffix.split(".")[1])

    version_code = beta + rc * 10 + patch * 100 + minor * 10000 + major * 1000000
    return version_code


def get_changelog(project_dir: str, format: str) -> Tuple[str, str]:
    with open(
        f"{project_dir}/app/src/main/assets/pl-changelog.html", "r", encoding="utf-8"
    ) as f:
        changelog = f.read()

    title = re.search(r"<h3>(.+?)</h3>", changelog).group(1)
    content = re.search(r"(?s)<ul>(.+)</ul>", changelog).group(1).strip()
    content = "\n".join(line.strip() for line in content.split("\n"))

    if format != "html":
        content = content.replace("<li>", "- ")
        content = content.replace("<br>", "\n")
        if format == "markdown":
            content = re.sub(r"<u>(.+?)</u>", "__\\1__", content)
            content = re.sub(r"<i>(.+?)</i>", "*\\1*", content)
            content = re.sub(r"<b>(.+?)</b>", "**\\1**", content)
        content = re.sub(r"</?.+?>", "", content)

    return (title, content)


def get_commit_log(project_dir: str, format: str, max_lines: int = None) -> str:
    last_tag = (
        subprocess.check_output("git describe --tags --abbrev=0".split(" "))
        .decode()
        .strip()
    )

    log = subprocess.run(
        args=f"git log {last_tag}..HEAD --format=%an%x00%at%x00%h%x00%s%x00%D".split(
            " "
        ),
        cwd=project_dir,
        stdout=subprocess.PIPE,
    )
    log = log.stdout.strip().decode()

    commits = [line.split("\x00") for line in log.split("\n")]
    if max_lines:
        commits = commits[:max_lines]

    output = []
    valid = False

    for commit in commits:
        if not commit[0]:
            continue
        if "origin/" in commit[4]:
            valid = True
        if not valid:
            continue
        date = datetime.fromtimestamp(float(commit[1]))
        date = date.strftime("%Y-%m-%d %H:%M:%S")
        if format == "html":
            output.append(f"<li>{commit[3]} <i> - {commit[0]}</i></li>")
        elif format == "markdown":
            output.append(f"[{date}] {commit[0]}\n    {commit[3]}")
        elif format == "markdown_full":
            output.append(
                f"_[{date}] {commit[0]}_\n`    `__`{commit[2]}`__  **{commit[3]}**"
            )
        elif format == "plain":
            output.append(f"- {commit[3]}")

    if format == "markdown":
        output.insert(0, "```")
        output.append("```")

    return "\n".join(output)
