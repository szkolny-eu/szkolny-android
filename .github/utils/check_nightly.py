import json
import os

import requests

if __name__ == "__main__":
    repo = os.getenv("GITHUB_REPOSITORY")
    sha = os.getenv("GITHUB_SHA")

    if not repo or not sha:
        print("Missing GitHub environment variables.")
        exit(-1)

    with requests.get(
        f"https://api.github.com/repos/{repo}/actions/runs?per_page=5&status=success"
    ) as r:
        data = json.loads(r.text)
        runs = [run for run in data["workflow_runs"] if run["head_sha"] == sha]
        if runs:
            print("hasNewChanges=false")
            exit(0)

    print("hasNewChanges=true")
