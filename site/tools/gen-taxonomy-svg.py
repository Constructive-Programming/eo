#!/usr/bin/env python3
"""Generate the isometric 3-axis optic-family taxonomy SVG for site/docs/optics.md.

Axes:
  u (lower-right)  : focus arity        — one, 0-or-1, many
  v (upper-right)  : from-side          — contextual write, total build, none
  vertical (layers): read side          — present (top), absent (bottom)
"""

S = 112                      # tile edge length driver
UX, UY = 0.866 * S, 0.5 * S  # u: arity direction (lower-right)
VX, VY = 0.866 * S, -0.5 * S # v: from-side direction (upper-right)
LAYER_DY = 330               # vertical gap between the two layer origins
MX, MY = 180, 175            # margins (left margin holds the read-axis label)

def pt(i, j, dy=0.0):
    """Screen position of grid node (i along u/arity, j along v/from-side)."""
    return (MX + i * UX + j * VX, MY + i * UY + j * VY + dy)

def diamond(i, j, dy, cls, dash=False, span=1):
    """Iso tile at (i, j); span>1 stretches along u (the Modify bar)."""
    p0 = pt(i, j, dy)
    p1 = pt(i + span, j, dy)
    p2 = pt(i + span, j + 1, dy)
    p3 = pt(i, j + 1, dy)
    d = ' '.join(f"{x:.1f},{y:.1f}" for x, y in (p0, p1, p2, p3))
    dash_attr = ' stroke-dasharray="6 5"' if dash else ''
    return f'<polygon class="{cls}" points="{d}"{dash_attr}/>'

def center(i, j, dy, span=1):
    x0, y0 = pt(i, j, dy)
    x1, y1 = pt(i + span, j + 1, dy)
    return ((x0 + x1) / 2, (y0 + y1) / 2)

def label(i, j, dy, name, sub=None, cls="fam", span=1):
    cx, cy = center(i, j, dy, span)
    out = []
    if sub:
        out.append(f'<text class="{cls}" x="{cx:.1f}" y="{cy - 7:.1f}">{name}</text>')
        out.append(f'<text class="sub" x="{cx:.1f}" y="{cy + 13:.1f}">{sub}</text>')
    else:
        out.append(f'<text class="{cls}" x="{cx:.1f}" y="{cy + 5:.1f}">{name}</text>')
    return '\n'.join(out)

parts = []

# ---------- styles ----------
parts.append('''<style>
  text { font-family: -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
         text-anchor: middle; fill: #23272e; }
  .fam   { font-size: 17px; font-weight: 600; }
  .ghost-t { font-size: 15px; font-weight: 600; fill: #8585c5; }
  .sub   { font-size: 11.5px; fill: #5b6068; }
  .axis  { font-size: 13px; font-weight: 600; fill: #3b3b8c; }
  .tick  { font-size: 11.5px; fill: #5b6068; font-style: italic; }
  .layer { font-size: 14px; font-weight: 700; fill: #3b3b8c; }
  .rw    { fill: #dcdcf2; stroke: #3b3b8c; stroke-width: 1.6; }
  .rb    { fill: #e9e3f6; stroke: #5e5eb8; stroke-width: 1.6; }
  .ro    { fill: #f2f2fa; stroke: #8585c5; stroke-width: 1.6; }
  .bo    { fill: #e3ecf6; stroke: #5e5eb8; stroke-width: 1.6; }
  .mod   { fill: #ebebee; stroke: #6a6f78; stroke-width: 1.6; }
  .ghost { fill: none; stroke: #8585c5; stroke-width: 1.2; }
  .drop  { stroke: #8585c5; stroke-width: 1; stroke-dasharray: 3 5; fill: none; }
  .arrow { stroke: #3b3b8c; stroke-width: 1.6; fill: none; }
  .ahead { fill: #3b3b8c; stroke: none; }
  @media (prefers-color-scheme: dark) {
    text   { fill: #e6edf3; }
    .sub, .tick { fill: #9aa4b2; }
    .axis, .layer { fill: #a3a3e6; }
    .ghost-t { fill: #7878d6; }
    .rw    { fill: #34344f; stroke: #7878d6; }
    .rb    { fill: #3a3450; stroke: #a3a3e6; }
    .ro    { fill: #2b2b3a; stroke: #7878d6; }
    .bo    { fill: #2d3a4a; stroke: #a3a3e6; }
    .mod   { fill: #30343a; stroke: #8b93a1; }
    .ghost { stroke: #7878d6; }
    .drop  { stroke: #7878d6; }
    .arrow { stroke: #a3a3e6; }
    .ahead { fill: #a3a3e6; }
  }
</style>''')

# ---------- drop lines between layers (drawn first, behind tiles) ----------
for (i, j) in [(0, 1), (3, 1), (0, 2), (3, 2)]:
    x0, y0 = pt(i, j, 0)
    x1, y1 = pt(i, j, LAYER_DY)
    parts.append(f'<line class="drop" x1="{x0:.1f}" y1="{y0:.1f}" x2="{x1:.1f}" y2="{y1:.1f}"/>')

# ---------- TOP layer: read side present ----------
top = [
    # (i, j, class, name, sub)
    (0, 0, 'rw', 'Lens',      None),
    (1, 0, 'rw', 'Optional',  None),
    (2, 0, 'rw', 'Traversal', None),
    (0, 1, 'rb', 'Iso',       None),
    (1, 1, 'rb', 'Prism',     None),
    (0, 2, 'ro', 'Getter',     None),
    (1, 2, 'ro', 'AffineFold', None),
    (2, 2, 'ro', 'Fold',       None),
]
for i, j, cls, name, sub in top:
    parts.append(diamond(i, j, 0, cls))
    parts.append(label(i, j, 0, name, sub))
# uninhabited (many, read, total-build)
parts.append(diamond(2, 1, 0, 'ghost', dash=True))
parts.append(label(2, 1, 0, '∅', 'no many-focus Iso', cls='ghost-t'))

# ---------- BOTTOM layer: read side absent ----------
# build column
parts.append(diamond(0, 1, LAYER_DY, 'bo'))
parts.append(label(0, 1, LAYER_DY, 'Review', None))
parts.append(diamond(1, 1, LAYER_DY, 'ghost', dash=True))
parts.append(label(1, 1, LAYER_DY, '≡ Review', 'mend is total', cls='ghost-t'))
parts.append(diamond(2, 1, LAYER_DY, 'bo'))
parts.append(label(2, 1, LAYER_DY, 'Unfold', None))
# write column: Modify bar spanning all three arities
parts.append(diamond(0, 0, LAYER_DY, 'mod', span=3))
parts.append(label(0, 0, LAYER_DY, 'Modify', 'write-only — arity-agnostic', span=3))

# ---------- axis arrows + labels ----------
def arrow(x0, y0, x1, y1):
    import math
    ang = math.atan2(y1 - y0, x1 - x0)
    ax, ay = x1, y1
    l = 9
    a1 = (ax - l * math.cos(ang - 0.42), ay - l * math.sin(ang - 0.42))
    a2 = (ax - l * math.cos(ang + 0.42), ay - l * math.sin(ang + 0.42))
    return (f'<line class="arrow" x1="{x0:.1f}" y1="{y0:.1f}" x2="{ax:.1f}" y2="{ay:.1f}"/>'
            f'<polygon class="ahead" points="{ax:.1f},{ay:.1f} {a1[0]:.1f},{a1[1]:.1f} {a2[0]:.1f},{a2[1]:.1f}"/>')

# arity axis: along u, drawn below-left of the bottom layer's write column
ax0 = pt(0, -0.42, LAYER_DY)
ax1 = pt(3.05, -0.42, LAYER_DY)
parts.append(arrow(*ax0, *ax1))
mid = ((ax0[0] + ax1[0]) / 2, (ax0[1] + ax1[1]) / 2)
parts.append(f'<text class="axis" x="{mid[0] - 14:.1f}" y="{mid[1] + 30:.1f}" transform="rotate(30 {mid[0] - 14:.1f} {mid[1] + 30:.1f})">focus arity:  one  →  0-or-1  →  many</text>')

# from-side axis: along v, drawn upper-left of the top layer
fx0 = pt(-0.42, 0, 0)
fx1 = pt(-0.42, 3.05, 0)
parts.append(arrow(*fx0, *fx1))
fmid = ((fx0[0] + fx1[0]) / 2, (fx0[1] + fx1[1]) / 2)
parts.append(f'<text class="axis" x="{fmid[0] - 12:.1f}" y="{fmid[1] - 26:.1f}" transform="rotate(-30 {fmid[0] - 12:.1f} {fmid[1] - 26:.1f})">from side:  write  →  build  →  none</text>')

# read axis: vertical, on the far left
rx = MX - 118
ry0, ry1 = MY + 60, MY + LAYER_DY + 10
parts.append(arrow(rx, ry0, rx, ry1))
parts.append(f'<text class="layer" x="{rx:.1f}" y="{ry0 - 44:.1f}">read side</text>')
parts.append(f'<text class="tick" x="{rx:.1f}" y="{ry0 - 26:.1f}">present (top)</text>')
parts.append(f'<text class="tick" x="{rx:.1f}" y="{ry1 + 22:.1f}">absent (bottom)</text>')

# layer captions on the right
cap_x = pt(3.3, 1.35, 0)
parts.append(f'<text class="layer" x="{cap_x[0]:.1f}" y="{cap_x[1] + 5:.1f}" text-anchor="start">reads</text>')
cap_xb = pt(3.3, 1.35, LAYER_DY)
parts.append(f'<text class="layer" x="{cap_xb[0]:.1f}" y="{cap_xb[1] + 5:.1f}" text-anchor="start">build / write</text>')
parts.append(f'<text class="tick" x="{cap_xb[0]:.1f}" y="{cap_xb[1] + 24:.1f}" text-anchor="start">only</text>')

W = int(MX + 3 * UX + 3 * VX + 120)
H = int(MY + 3 * UY + LAYER_DY + 95)
svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" '
       f'font-size="14" role="img" aria-label="The three-axis optic family taxonomy: '
       f'focus arity by from-side by read-side">\n' + '\n'.join(parts) + '\n</svg>\n')

import os
out = os.path.join(os.path.dirname(__file__), '..', 'laika-static', 'static', 'optic-taxonomy-3d.svg')
open(out, 'w').write(svg)
print(f"wrote {out}  viewBox 0 0 {W} {H}")
