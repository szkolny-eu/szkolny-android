import os
import re
import sys

from dotenv import load_dotenv

from _get_password import get_password
from _utils import get_project_dir, read_gradle_version


def sign(
    project_dir: str,
    version_name: str,
    version_code: int,
    password: str,
    iv: bytes,
    commit: bool = False,
):
    SIGNING_PATH = f"{project_dir}/app/src/main/java/pl/szczodrzynski/edziennik/data/api/szkolny/interceptor/Signing.kt"
    CPP_PATH = f"{project_dir}/app/src/main/cpp/szkolny-signing.cpp"

    with open(SIGNING_PATH, "r") as f:
        signing = f.read()

    with open(CPP_PATH, "r") as f:
        cpp = f.read()

    SIGNING_REGEX = r"\$param1\..*\.\$param2"
    CPP_REGEX = r"(?s)/\*.+?toys AES_IV\[16\] = {.+?};"

    SIGNING_FORMAT = "$param1.{}.$param2"
    CPP_FORMAT = "/*{}*/\nstatic toys AES_IV[16] = {{\n\t{} }};"

    iv_hex = " ".join(["{:02x}".format(x) for x in iv])
    iv_cpp = ", ".join(["0x{:02x}".format(x) for x in iv])

    signing = re.sub(SIGNING_REGEX, SIGNING_FORMAT.format(password), signing)
    cpp = re.sub(CPP_REGEX, CPP_FORMAT.format(iv_hex, iv_cpp), cpp)

    with open(SIGNING_PATH, "w") as f:
        f.write(signing)

    with open(CPP_PATH, "w") as f:
        f.write(cpp)

    if commit:
        os.chdir(project_dir)
        os.system("git add .")
        os.system(
            f'git commit -m "[{version_name}] Update build.gradle, signing and changelog."'
        )


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: sign.py <project dir> [commit]")
        exit(-1)

    project_dir = get_project_dir()

    load_dotenv()
    DB_HOST = os.getenv("DB_HOST")
    DB_USER = os.getenv("DB_USER")
    DB_PASS = os.getenv("DB_PASS")
    DB_NAME = os.getenv("DB_NAME")

    (version_code, version_name) = read_gradle_version(project_dir)
    (password, iv) = get_password(
        version_name, version_code, DB_HOST, DB_USER, DB_PASS, DB_NAME
    )

    print("appVersionName=" + version_name)
    print("appVersionCode=" + str(version_code))

    sign(
        project_dir,
        version_name,
        version_code,
        password,
        iv,
        commit="commit" in sys.argv,
    )
