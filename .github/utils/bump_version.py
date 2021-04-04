import os

from dotenv import load_dotenv

from _get_password import get_password
from _utils import build_version_code, write_gradle_version
from sign import sign

if __name__ == "__main__":
    version_name = input("Enter version name: ")
    version_code = build_version_code(version_name)

    print(f"Bumping version to {version_name} ({version_code})")

    project_dir = "../.."

    load_dotenv()
    DB_HOST = os.getenv("DB_HOST")
    DB_USER = os.getenv("DB_USER")
    DB_PASS = os.getenv("DB_PASS")
    DB_NAME = os.getenv("DB_NAME")

    write_gradle_version(project_dir, version_code, version_name)
    (password, iv) = get_password(
        version_name, version_code, DB_HOST, DB_USER, DB_PASS, DB_NAME
    )

    sign(project_dir, version_name, version_code, password, iv, commit=False)

    print("Writing mock passwords")
    os.chdir(project_dir)
    os.system(
        "sed -i -E 's/\/\*([0-9a-f]{2} ?){16}\*\//\/*secret password - removed for source code publication*\//g' app/src/main/cpp/szkolny-signing.cpp"
    )
    os.system(
        "sed -i -E 's/\\t0x.., 0x(.)., 0x.(.), 0x.(.), 0x.., 0x.., 0x.., 0x.(.), 0x.., 0x.(.), 0x(.)., 0x(.)., 0x.., 0x.., 0x.., 0x.(.)/\\t0x\\3\\6, 0x\\7\\4, 0x\\1\\8, 0x\\2\\5, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff /g' app/src/main/cpp/szkolny-signing.cpp"
    )
    os.system(
        "sed -i -E 's/param1\..(.).(.).(.).(.)..(.)..(.)..(.)..(.).../param1.MTIzNDU2Nzg5MD\\5\\2\\7\\6\\1\\3\\4\8==/g' app/src/main/java/pl/szczodrzynski/edziennik/data/api/szkolny/interceptor/Signing.kt"
    )
    input("Press any key to finish")
