import base64
import secrets
from hashlib import sha256
from typing import Tuple

import mysql.connector as mysql
from Crypto.Cipher import AES


def get_password(
    version_name: str,
    version_code: int,
    db_host: str,
    db_user: str,
    db_pass: str,
    db_name: str,
) -> Tuple[str, bytes]:
    db = mysql.connect(
        host=db_host,
        user=db_user,
        password=db_pass,
        database=db_name,
        auth_plugin="mysql_native_password",
    )

    print(f"Generating passwords for version {version_name} ({version_code})")

    password = base64.b64encode(secrets.token_bytes(16)).decode()
    iv = secrets.token_bytes(16)

    key = f"{version_name}.{password}.{version_code}"
    key = sha256(key.encode()).digest()
    data = "ThisIsOurHardWorkPleaseDoNotCopyOrSteal(c)2019.KubaSz"
    data = sha256(data.encode()).digest()
    data = data + (chr(16) * 16).encode()

    aes = AES.new(key=key, mode=AES.MODE_CBC, iv=iv)

    app_password = base64.b64encode(aes.encrypt(data)).decode()

    c = db.cursor()
    c.execute(
        "INSERT IGNORE INTO _appPasswords (versionCode, appPassword, password, iv) VALUES (%s, %s, %s, %s);",
        (version_code, app_password, password, iv),
    )
    db.commit()

    c = db.cursor()
    c.execute(
        "SELECT password, iv FROM _appPasswords WHERE versionCode = %s;",
        (version_code,),
    )
    row = c.fetchone()

    db.close()

    return (row[0], row[1])
