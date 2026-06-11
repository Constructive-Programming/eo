#!/usr/bin/env python3
"""Generate the isometric 3-axis optic-family taxonomy SVG for site/docs/optics.md.

Axes:
  u (lower-right)  : FOCUS nature   — total, fallible, multiple   (the ReadCompose join lattice)
  v (upper-right)  : SOURCE nature  — total, contextual, fallible (fallible = WriteCompose /
                                                                   BiAffine plan)
  vertical (layers): capability     — read-only (top), read-write (middle),
                                      write/build-only (bottom)

One-way layers collapse to the focus axis (read-only has no `from`; write-only
rails by the focus shape its build consumes), so both rails run parallel along u.
The middle layer is the FULL focus x source grid — every cell is shipped or
scoped to the failure-typed-build (BiAffine) plan; no accidental holes.
"""
import math, os

S = 112                       # tile edge length driver
UX, UY = 0.866 * S, 0.5 * S   # u: read-cardinality direction (lower-right)
VX, VY = 0.866 * S, -0.5 * S  # v: write-cardinality direction (upper-right)
LD = 300                      # vertical gap between layer origins
MX, MY = 190, 180             # margins (left margin holds the capability-axis label)

TOP, MID, BOT = 0, LD, 2 * LD

def pt(i, j, dy=0.0):
    return (MX + i * UX + j * VX, MY + i * UY + j * VY + dy)

def tile(i, j, dy, cls, dash=False, uspan=1, vspan=1):
    p0, p1 = pt(i, j, dy), pt(i + uspan, j, dy)
    p2, p3 = pt(i + uspan, j + vspan, dy), pt(i, j + vspan, dy)
    d = ' '.join(f"{x:.1f},{y:.1f}" for x, y in (p0, p1, p2, p3))
    dash_attr = ' stroke-dasharray="6 5"' if dash else ''
    return f'<polygon class="{cls}" points="{d}"{dash_attr}/>'

def center(i, j, dy, uspan=1, vspan=1):
    x0, y0 = pt(i, j, dy)
    x1, y1 = pt(i + uspan, j + vspan, dy)
    return ((x0 + x1) / 2, (y0 + y1) / 2)

def label(i, j, dy, name, sub=None, cls="fam", uspan=1, vspan=1):
    cx, cy = center(i, j, dy, uspan, vspan)
    if sub:
        return (f'<text class="{cls}" x="{cx:.1f}" y="{cy - 7:.1f}">{name}</text>\n'
                f'<text class="sub" x="{cx:.1f}" y="{cy + 13:.1f}">{sub}</text>')
    return f'<text class="{cls}" x="{cx:.1f}" y="{cy + 5:.1f}">{name}</text>'

parts = []

parts.append('''<style>
  text { font-family: -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
         text-anchor: middle; fill: #23272e; }
  .fam   { font-size: 16.5px; font-weight: 600; }
  .ghost-t { font-size: 13.5px; font-weight: 600; fill: #8585c5; }
  .sub   { font-size: 11px; fill: #5b6068; }
  .axis  { font-size: 13px; font-weight: 600; fill: #3b3b8c; }
  .tick  { font-size: 11.5px; fill: #5b6068; font-style: italic; }
  .layer { font-size: 14px; font-weight: 700; fill: #3b3b8c; }
  .ro    { fill: #f2f2fa; stroke: #8585c5; stroke-width: 1.6; }
  .rw    { fill: #dcdcf2; stroke: #3b3b8c; stroke-width: 1.6; }
  .bo    { fill: #e3ecf6; stroke: #5e5eb8; stroke-width: 1.6; }
  .mod   { fill: #ebebee; stroke: #6a6f78; stroke-width: 1.6; }
  .plan  { fill: #faf3e8; stroke: #b8893a; stroke-width: 1.3; }
  .plan-t { font-size: 12.5px; font-weight: 600; fill: #a3742a; }
  .empty { fill: none; stroke: #b9bdd8; stroke-width: 1; }
  .empty-t { font-size: 13px; fill: #b9bdd8; }
  .drop  { stroke: #8585c5; stroke-width: 1; stroke-dasharray: 3 5; fill: none; }
  .arrow { stroke: #3b3b8c; stroke-width: 1.6; fill: none; }
  .ahead { fill: #3b3b8c; stroke: none; }
  @media (prefers-color-scheme: dark) {
    text   { fill: #e6edf3; }
    .sub, .tick { fill: #9aa4b2; }
    .axis, .layer { fill: #a3a3e6; }
    .ghost-t { fill: #7878d6; }
    .ro    { fill: #2b2b3a; stroke: #7878d6; }
    .rw    { fill: #34344f; stroke: #7878d6; }
    .bo    { fill: #2d3a4a; stroke: #a3a3e6; }
    .mod   { fill: #30343a; stroke: #8b93a1; }
    .plan  { fill: #3a3326; stroke: #c89a4a; }
    .plan-t { fill: #d8ae62; }
    .empty { stroke: #4a4f66; }
    .empty-t { fill: #4a4f66; }
    .drop  { stroke: #7878d6; }
    .arrow { stroke: #a3a3e6; }
    .ahead { fill: #a3a3e6; }
  }
</style>''')

# ---------- vertical guides through the three layers (behind everything) ----------
for (i, j) in [(0, 0), (3, 3)]:
    x0, y0 = pt(i, j, TOP)
    x1, y1 = pt(i, j, BOT)
    parts.append(f'<line class="drop" x1="{x0:.1f}" y1="{y0:.1f}" x2="{x1:.1f}" y2="{y1:.1f}"/>')

# ---------- TOP layer: read-only (rail along u; no write side, v collapses) ----------
JR = 1.0  # rail sits on the middle v-row so it hovers over the grid's centre
for i, name in enumerate(['Getter', 'AffineFold', 'Fold']):
    parts.append(tile(i, JR, TOP, 'ro'))
    parts.append(label(i, JR, TOP, name))

# ---------- MIDDLE layer: read-write (full grid: u = focus nature, v = source nature) ----------
# u index: 0 → total, 1 → fallible, 2 → multiple
# v index: 0 → total, 1 → contextual, 2 → fallible
parts.append(tile(0, 0, MID, 'rw'))
parts.append(label(0, 0, MID, 'Iso', None))
parts.append(tile(0, 1, MID, 'rw'))
parts.append(label(0, 1, MID, 'Lens', None))
parts.append(tile(1, 0, MID, 'rw'))
parts.append(label(1, 0, MID, 'Prism', None))
parts.append(tile(1, 1, MID, 'rw'))
parts.append(label(1, 1, MID, 'Optional', 'Affine carrier'))
parts.append(tile(2, 0, MID, 'rw', vspan=2))
parts.append(label(2, 0, MID, 'Traversal', 'fixed/Grate: total · each: contextual', vspan=2))
# fallible-source row — the failure-typed-build (BiAffine) plan
parts.append(tile(0, 2, MID, 'plan', dash=True))
parts.append(label(0, 2, MID, 'fallible write', 'planned', cls='plan-t'))
parts.append(tile(1, 2, MID, 'plan', dash=True))
parts.append(label(1, 2, MID, 'BiAffine', 'planned', cls='plan-t'))
parts.append(tile(2, 2, MID, 'plan', dash=True))
parts.append(label(2, 2, MID, 'fallible each', 'planned', cls='plan-t'))

# ---------- BOTTOM layer: write/build-only — a FULL plane (from is real, to is vestigial) ----------
# Each cell sits directly below the read-write cell(s) whose build half it is.
parts.append(tile(0, 0, BOT, 'bo'))
parts.append(label(0, 0, BOT, 'Review', None))
parts.append(tile(1, 0, BOT, 'empty', dash=True))
parts.append(label(1, 0, BOT, '≡ Review', "a Prism's mend is total", cls='ghost-t'))
parts.append(tile(2, 0, BOT, 'bo'))
parts.append(label(2, 0, BOT, 'Unfold', None))
parts.append(tile(0, 1, BOT, 'mod', uspan=3))
parts.append(label(0, 1, BOT, 'Modify', 'the contextual write half — focus-agnostic', uspan=3))
parts.append(tile(0, 2, BOT, 'plan', dash=True, uspan=3))
parts.append(label(0, 2, BOT, 'fallible build', 'planned', cls='plan-t', uspan=3))

# ---------- axis arrows + labels ----------
def arrow(x0, y0, x1, y1):
    ang = math.atan2(y1 - y0, x1 - x0)
    l = 9
    a1 = (x1 - l * math.cos(ang - 0.42), y1 - l * math.sin(ang - 0.42))
    a2 = (x1 - l * math.cos(ang + 0.42), y1 - l * math.sin(ang + 0.42))
    return (f'<line class="arrow" x1="{x0:.1f}" y1="{y0:.1f}" x2="{x1:.1f}" y2="{y1:.1f}"/>'
            f'<polygon class="ahead" points="{x1:.1f},{y1:.1f} {a1[0]:.1f},{a1[1]:.1f} {a2[0]:.1f},{a2[1]:.1f}"/>')

# read-cardinality axis (u), under the middle grid's front edge
a0, a1 = pt(0, -0.4, MID), pt(3.05, -0.4, MID)
parts.append(arrow(*a0, *a1))
am = ((a0[0] + a1[0]) / 2, (a0[1] + a1[1]) / 2)
parts.append(f'<text class="axis" x="{am[0] - 14:.1f}" y="{am[1] + 22:.1f}" transform="rotate(30 {am[0] - 14:.1f} {am[1] + 22:.1f})">focus nature (ReadCompose):  total  →  fallible  →  multiple</text>')

# write-cardinality axis (v), upper-left of the middle grid
b0, b1 = pt(-0.4, 0, MID), pt(-0.4, 3.05, MID)
parts.append(arrow(*b0, *b1))
bm = ((b0[0] + b1[0]) / 2, (b0[1] + b1[1]) / 2)
parts.append(f'<text class="axis" x="{bm[0] - 12:.1f}" y="{bm[1] - 26:.1f}" transform="rotate(-30 {bm[0] - 12:.1f} {bm[1] - 26:.1f})">source nature:  total  →  contextual  →  fallible (WriteCompose — planned)</text>')

# capability axis: vertical, far left
rx = MX - 120
ry0, ry1 = MY + 40, MY + BOT + 20
parts.append(arrow(rx, ry0, rx, ry1))
parts.append(f'<text class="layer" x="{rx:.1f}" y="{ry0 - 42:.1f}">capability</text>')
parts.append(f'<text class="tick" x="{rx:.1f}" y="{ry0 - 24:.1f}">read-only (top)</text>')
parts.append(f'<text class="tick" x="{rx:.1f}" y="{ry1 + 22:.1f}">write-only (bottom)</text>')

# layer captions on the right
for dy, name, sub in [(TOP, 'read-only', 'no from at all — collapses to the focus axis'),
                      (MID, 'read-write', None),
                      (BOT, 'write / build only', 'the from halves of the layer above')]:
    cx, cy = pt(3.3, 1.6, dy)
    parts.append(f'<text class="layer" x="{cx:.1f}" y="{cy + 5:.1f}" text-anchor="start">{name}</text>')
    if sub:
        parts.append(f'<text class="tick" x="{cx:.1f}" y="{cy + 24:.1f}" text-anchor="start">{sub}</text>')

W = int(MX + 3 * UX + 3 * VX + 235)
H = int(MY + 3 * UY + BOT + 105)
svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" '
       f'font-size="14" role="img" aria-label="The three-axis optic family taxonomy: '
       f'read cardinality by write cardinality by capability layer">\n' + '\n'.join(parts) + '\n</svg>\n')

out = os.path.join(os.path.dirname(__file__), '..', 'laika-static', 'static', 'optic-taxonomy-3d.svg')
open(out, 'w').write(svg)
print(f"wrote {os.path.normpath(out)}  viewBox 0 0 {W} {H}")
