"""生成恐惧模式开关图标（纯标准库，无需 PIL）。

设计：黑色圆盘 + 暗红粗环 + 中间一道竖直的红色裂瞳，开启态带暗红光晕；关闭态为灰色。
用法：python tools/make_icons.py  → 写入 src/main/resources/dreadResources/images/
"""
import math
import os
import struct
import zlib

N = 128


def png(path, pixels):
    raw = b"".join(b"\x00" + bytes(row) for row in pixels)

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", N, N, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(data)


def clamp(x):
    return max(0, min(255, int(round(x))))


def smooth(edge0, edge1, x):
    t = max(0.0, min(1.0, (x - edge0) / (edge1 - edge0)))
    return t * t * (3 - 2 * t)


def render(on):
    c = (N - 1) / 2.0
    rows = []
    for y in range(N):
        row = []
        for x in range(N):
            dx, dy = (x - c) / c, (y - c) / c
            r = math.hypot(dx, dy)
            # 外光晕
            glow = (1 - smooth(0.80, 1.0, r)) * (0.55 if on else 0.0)
            disk = 1 - smooth(0.80, 0.84, r)
            ring = disk * smooth(0.62, 0.66, r)
            # 裂瞳：竖直细长的菱形，中间最宽
            w = 0.16 * (1 - (abs(dy) / 0.62) ** 1.6) if abs(dy) < 0.62 else 0.0
            slit = 1 - smooth(w * 0.75, w, abs(dx)) if w > 0 else 0.0
            slit_glow = (1 - smooth(w, w + 0.18, abs(dx))) * 0.6 if w > 0 else 0.0
            if on:
                base = (14, 6, 6)
                ringc = (122, 12, 12)
                slitc = (235, 30, 24)
                glowc = (150, 10, 10)
            else:
                base = (22, 22, 24)
                ringc = (78, 78, 82)
                slitc = (150, 150, 155)
                glowc = (0, 0, 0)
            col = list(base)
            for i in range(3):
                col[i] = col[i] * (1 - ring) + ringc[i] * ring
                col[i] = col[i] * (1 - slit_glow * disk) + slitc[i] * 0.45 * slit_glow * disk
                col[i] = col[i] * (1 - slit) + slitc[i] * slit
            a = disk
            if glow > 0 and disk < 1:
                ga = glow * (1 - disk)
                col = [col[i] * disk + glowc[i] * (1 - disk) for i in range(3)]
                a = disk + ga
            row += [clamp(col[0]), clamp(col[1]), clamp(col[2]), clamp(a * 255)]
        rows.append(row)
    return rows


if __name__ == "__main__":
    out = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "dreadResources", "images")
    os.makedirs(out, exist_ok=True)
    png(os.path.join(out, "dread_on.png"), render(True))
    png(os.path.join(out, "dread_off.png"), render(False))
    print("ok", os.path.abspath(out))
