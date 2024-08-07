import os
import sys

from _utils import get_changelog, get_commit_log, get_project_dir, read_gradle_version

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: extract_changelogs.py <project dir>")
        exit(-1)

    project_dir = get_project_dir()

    (version_code, version_name) = read_gradle_version(project_dir)

    print("appVersionName=" + version_name)
    print("appVersionCode=" + str(version_code))

    dir = f"{project_dir}/app/release/whatsnew-{version_name}/"
    os.makedirs(dir, exist_ok=True)

    print("changelogDir=" + dir)

    (title, changelog) = get_changelog(project_dir, format="plain")

    # plain text changelog - Firebase App Distribution
    with open(dir + "whatsnew_titled.txt", "w", encoding="utf-8") as f:
        f.write(title)
        f.write("\n")
        f.write(changelog)
    print("changelogPlainTitledFile=" + dir + "whatsnew_titled.txt")

    print("changelogTitle=" + title)

    # plain text changelog, max 500 chars - Google Play
    with open(dir + "whatsnew-pl-PL", "w", encoding="utf-8") as f:
        changelog_lines = changelog.split("\n")
        changelog = ""
        for line in changelog_lines:
            if len(changelog) + len(line) < 500:
                changelog += "\n" + line
        changelog = changelog.strip()
        f.write(changelog)

    print("changelogPlainFile=" + dir + "whatsnew-pl-PL")

    # markdown changelog - Discord webhook
    (_, changelog) = get_changelog(project_dir, format="markdown")
    with open(dir + "whatsnew.md", "w", encoding="utf-8") as f:
        f.write(changelog)
    print("changelogMarkdownFile=" + dir + "whatsnew.md")

    # html changelog - version info in DB
    (_, changelog) = get_changelog(project_dir, format="html")
    with open(dir + "whatsnew.html", "w", encoding="utf-8") as f:
        f.write(changelog)
    print("changelogHtmlFile=" + dir + "whatsnew.html")

    changelog = get_commit_log(project_dir, format="plain", max_lines=10)
    with open(dir + "commit_log.txt", "w", encoding="utf-8") as f:
        f.write(changelog)
    print("commitLogPlainFile=" + dir + "commit_log.txt")

    changelog = get_commit_log(project_dir, format="markdown", max_lines=10)
    with open(dir + "commit_log.md", "w", encoding="utf-8") as f:
        f.write(changelog)
    print("commitLogMarkdownFile=" + dir + "commit_log.md")

    changelog = get_commit_log(project_dir, format="html", max_lines=10)
    with open(dir + "commit_log.html", "w", encoding="utf-8") as f:
        f.write(changelog)
    print("commitLogHtmlFile=" + dir + "commit_log.html")
