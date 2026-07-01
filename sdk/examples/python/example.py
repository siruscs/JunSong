"""
JunSong开放平台 Python SDK 使用示例

演示如何使用HMAC-SHA256签名调用开放API
"""
import hmac
import hashlib
import time
import uuid
import urllib.request
import json


def call_openapi(app_key, app_secret, method, path, body=""):
    """调用开放平台API"""
    timestamp = str(int(time.time() * 1000))
    nonce = uuid.uuid4().hex
    sign_str = method + "/openapi/v1" + path + timestamp + nonce + body
    signature = hmac.new(
        app_secret.encode(), sign_str.encode(), hashlib.sha256
    ).hexdigest()

    url = f"http://localhost:8081/openapi/v1{path}"
    req = urllib.request.Request(url, method=method)
    req.add_header("X-App-Key", app_key)
    req.add_header("X-App-Timestamp", timestamp)
    req.add_header("X-App-Nonce", nonce)
    req.add_header("X-App-Signature", signature)

    resp = urllib.request.urlopen(req, timeout=15)
    data = json.loads(resp.read().decode())
    print(f"状态码: {resp.status}")
    print(f"X-API-Version: {resp.headers.get('X-API-Version', '无')}")
    print(f"X-RateLimit-Limit: {resp.headers.get('X-RateLimit-Limit', '无')}")
    print(f"X-RateLimit-Remaining: {resp.headers.get('X-RateLimit-Remaining', '无')}")
    print(f"响应: {json.dumps(data, ensure_ascii=False, indent=2)}")


if __name__ == "__main__":
    APP_KEY = "js_d30da74ff5b14d5cb6fa2bf97789d1dc"
    APP_SECRET = "f487dccac151496f8b9d33e6977d80aceec4591be3c64d4188d8dfb9b4d3d707"

    print("=== 查询应用列表 ===")
    call_openapi(APP_KEY, APP_SECRET, "GET", "/app/list")
