import glob
import os
import sys

from _utils import get_project_dir

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: rename_artifacts.py <project dir>")
        exit(-1)

    project_dir = get_project_dir()

    files = glob.glob(f"{project_dir}/app/release/*.*")
    for file in files:
        file_relative = file.replace(os.getenv("GITHUB_WORKSPACE") + "/", "")
        if "-aligned.apk" in file:
            os.unlink(file)
        elif "-signed.apk" in file:
            new_file = file.replace("-signed.apk", ".apk")
            if os.path.isfile(new_file):
                os.unlink(new_file)
            os.rename(file, new_file)
        elif ".apk" in file or ".aab" in file:
            print("::set-output name=signedReleaseFile::" + file)
            print("::set-output name=signedReleaseFileRelative::" + file_relative)
