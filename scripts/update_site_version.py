#!/usr/bin/env python3
"""把网站首页里的版本号同步成最新 Release 的版本。

用法：
    python scripts/update_site_version.py v1.3.2-mc21.1
    python scripts/update_site_version.py v1.3.2-mc21.1 --file site/index.html

标签格式约定为 `v<mod版本>-mc<MC版本>`，例如 v1.3.2-mc21.1。
若格式不符，脚本直接失败退出，避免把奇怪的字符串写进页面。
"""

import argparse
import re
import sys
from pathlib import Path

TAG_RE = re.compile(r"^v?(\d+\.\d+\.\d+)-mc(\d+\.\d+)$")

# (说明, 正则, 替换模板)
# 注意：功能卡片上的「x.y.z 新增」角标记录的是该功能首次引入的版本，
# 不随版本升级变化，因此不在这里替换。
RULES = [
    ("顶部下载按钮", re.compile(r"(下载\s+)\d+\.\d+\.\d+"), r"\g<1>{ver}"),
    ("jar 文件名", re.compile(r"zongheng_railway-\d+\.\d+\.\d+\+mc\d+\.\d+\.jar"),
     "zongheng_railway-{ver}+mc{mc}.jar"),
    ("当前版本行", re.compile(r"(当前版本\s+)\d+\.\d+\.\d+"), r"\g<1>{ver}"),
]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("tag", help="Release 标签，例如 v1.3.2-mc21.1")
    parser.add_argument("--file", default="site/index.html")
    args = parser.parse_args()

    m = TAG_RE.match(args.tag.strip())
    if not m:
        print(f"标签格式不符合 v<版本>-mc<MC版本>：{args.tag}", file=sys.stderr)
        return 1
    ver, mc = m.group(1), m.group(2)

    path = Path(args.file)
    # newline="" 保持原有换行风格（这个仓库的 index.html 是 LF）
    with path.open("r", encoding="utf-8", newline="") as fp:
        text = fp.read()
    original = text

    for name, pattern, template in RULES:
        text, n = pattern.subn(template.format(ver=ver, mc=mc), text)
        print(f"{name}: 替换 {n} 处")

    if text == original:
        print("页面已经是最新版本，无需改动")
        return 0

    with path.open("w", encoding="utf-8", newline="") as fp:
        fp.write(text)
    print(f"已更新 {path} -> {ver} (+mc{mc})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
