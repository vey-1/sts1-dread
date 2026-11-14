"""Generate dread power icons (48/128) with pure stdlib: SDF + smoothstep AA."""
import math, struct, zlib, os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources",
                   "dreadResources", "images", "powers")

def circle(cx, cy, r):
    return lambda x, y: math.hypot(x - cx, y - cy) - r

def ellipse(cx, cy, rx, ry):
    return lambda x, y: (math.hypot((x - cx) / rx, (y - cy) / ry) - 1) * min(rx, ry)

def cone(ax, ay, ra, bx, by, rb):
    dx, dy = bx - ax, by - ay
    l2 = dx * dx + dy * dy
    def f(x, y):
        t = max(0.0, min(1.0, ((x - ax) * dx + (y - ay) * dy) / l2))
        return math.hypot(x - ax - t * dx, y - ay - t * dy) - (ra + (rb - ra) * t)
    return f

def rhombus(cx, cy, a, b):
    n = math.hypot(1 / a, 1 / b)
    return lambda x, y: (abs(x - cx) / a + abs(y - cy) / b - 1) / n

def union(*fs):
    return lambda x, y: min(f(x, y) for f in fs)

def smin(f, g, k):
    def h(x, y):
        a, b = f(x, y), g(x, y)
        t = max(k - abs(a - b), 0) / k
        return min(a, b) - t * t * k * 0.25
    return h

def ring(cx, cy, r, w):
    return lambda x, y: abs(math.hypot(x - cx, y - cy) - r) - w

def smooth(e0, e1, x):
    t = max(0.0, min(1.0, (x - e0) / (e1 - e0)))
    return t * t * (3 - 2 * t)

# layers: (sdf, fill, outline_color or None, outline_width)
def eye():
    ink = (18, 4, 8)
    return [
        (circle(64, 64, 45), (38, 6, 12), ink, 4),
        (ring(64, 64, 36, 5), (150, 18, 28), None, 0),
        (rhombus(64, 64, 11, 31), (235, 40, 36), ink, 3.5),
        (rhombus(64, 64, 3.5, 20), (255, 170, 120), None, 0),
    ]

def grad(y0, y1, c0, c1):
    def f(x, y):
        t = max(0.0, min(1.0, (y - y0) / (y1 - y0)))
        return tuple(c0[k] + (c1[k] - c0[k]) * t for k in range(3))
    return f

def box(cx, cy, hx, hy, r):
    def f(x, y):
        qx, qy = abs(x - cx) - hx + r, abs(y - cy) - hy + r
        return math.hypot(max(qx, 0), max(qy, 0)) + min(max(qx, qy), 0) - r
    return f

def inter(*fs):
    return lambda x, y: max(f(x, y) for f in fs)

def below(y0):
    return lambda x, y: y - y0

def rot(f, cx, cy, deg):
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    return lambda x, y: f(cx + (x - cx) * c + (y - cy) * s, cy - (x - cx) * s + (y - cy) * c)

def poly(pts):
    n = len(pts)
    def f(x, y):
        d = float("inf"); sgn = 1.0
        j = n - 1
        for i in range(n):
            ax, ay = pts[j]; bx, by = pts[i]
            ex, ey = bx - ax, by - ay
            wx, wy = x - ax, y - ay
            t = max(0.0, min(1.0, (wx * ex + wy * ey) / (ex * ex + ey * ey)))
            d = min(d, math.hypot(wx - ex * t, wy - ey * t))
            if (ay > y) != (by > y) and x < ax + (y - ay) * ex / ey:
                sgn = -sgn
            j = i
        return d * sgn
    return f

def mirror(pts):
    return [(128 - x, y) for x, y in reversed(pts)]

def drop(cx, cy, r, tx, ty):
    """尖头毒滴：圆肚 + 收到一个尖。"""
    return smin(circle(cx, cy, r), cone(cx, cy, r * 0.82, tx, ty, 0.0), r * 0.5)

def slime():
    """增殖：暗底徽记，一大两小三枚尖头毒滴向外迸开。"""
    ink = (4, 16, 8)
    G = lambda y0, y1: grad(y0, y1, (214, 255, 96), (22, 132, 56))
    core = (240, 255, 200)
    return [
        (circle(64, 64, 45), (10, 30, 18), ink, 4),
        (ring(64, 64, 37, 3.5), (36, 120, 60), None, 0),
        (drop(34, 84, 11.5, 17, 50), G(48, 98), ink, 3.5),
        (drop(94, 84, 11.5, 111, 50), G(48, 98), ink, 3.5),
        (drop(64, 78, 21, 64, 14), G(14, 102), ink, 4),
        (cone(57, 78, 3.2, 60, 46, 0.6), core, None, 0),
        (cone(30, 84, 1.8, 24, 66, 0.4), core, None, 0),
        (cone(98, 84, 1.8, 104, 66, 0.4), core, None, 0),
    ]

def blades():
    """同仇：暗底徽记，棱角骷髅，眼里烧着红光。"""
    ink = (8, 4, 6)
    skull = poly([(48, 26), (80, 26), (96, 38), (100, 60), (92, 74), (82, 80), (80, 102),
                  (48, 102), (46, 80), (36, 74), (28, 60), (32, 38)])
    eye_l = [(37, 54), (58, 62), (56, 73), (42, 72)]
    glow_l = [(42, 59), (55, 64), (54, 70), (45, 69)]
    nose = poly([(64, 71), (59, 82), (69, 82)])
    red = grad(56, 72, (255, 110, 48), (200, 18, 26))
    layers = [
        (circle(64, 64, 45), (22, 8, 12), ink, 4),
        (ring(64, 64, 37, 3.5), (150, 18, 28), None, 0),
        (skull, grad(24, 104, (248, 242, 228), (146, 136, 126)), ink, 4.5),
        (poly(eye_l), ink, None, 0),
        (poly(mirror(eye_l)), ink, None, 0),
        (poly(glow_l), red, None, 0),
        (poly(mirror(glow_l)), red, None, 0),
        (nose, ink, None, 0),
        (box(64, 88.5, 16, 1.0, 0.4), ink, None, 0),
    ]
    for x in (53.5, 60.5, 67.5, 74.5):
        layers.append((box(x, 95, 1.0, 6.5, 0.4), ink, None, 0))
    return layers

def render(layers, size):
    px = 128.0 / size
    buf = bytearray()
    for j in range(size):
        buf.append(0)
        for i in range(size):
            x, y = (i + 0.5) * px, (j + 0.5) * px
            r = g = b = a = 0.0
            def over(col, al):
                nonlocal r, g, b, a
                if al <= 0: return
                na = al + a * (1 - al)
                r = (col[0] * al + r * a * (1 - al)) / na
                g = (col[1] * al + g * a * (1 - al)) / na
                b = (col[2] * al + b * a * (1 - al)) / na
                a = na
            for f, fill, oc, ow in layers:
                d = f(x, y)
                if oc is not None:
                    over(oc, smooth(px * 0.5, -px * 0.5, d - ow))
                over(fill(x, y) if callable(fill) else fill, smooth(px * 0.5, -px * 0.5, d))
            buf += bytes((round(r), round(g), round(b), round(a * 255)))
    return bytes(buf)

def png(path, size, raw):
    def ch(t, d):
        c = struct.pack(">I", len(d)) + t + d
        return c + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
    data = b"\x89PNG\r\n\x1a\n" + ch(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)) \
        + ch(b"IDAT", zlib.compress(raw, 9)) + ch(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(data)

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, fn in (("dread_boss", eye), ("proliferate", slime), ("vengeance", blades)):
        L = fn()
        for s in (48, 128):
            png(os.path.join(OUT, f"{name}_{s}.png"), s, render(L, s))
    print("ok")
