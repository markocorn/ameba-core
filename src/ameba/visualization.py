"""Dependency-free standalone HTML visualization for AMEBA graphs."""

from __future__ import annotations

import json
from pathlib import Path

from ameba_graph import Graph
from ameba_graph.serialization import graph_to_dict


def graph_to_html(graph: Graph, title: str = "AMEBA graph") -> str:
    data = json.dumps(graph_to_dict(graph), allow_nan=False, sort_keys=True).replace(
        "<", "\\u003c"
    )
    safe_title = (
        title.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )
    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{safe_title}</title>
<style>
  body {{ margin: 0; font-family: system-ui, sans-serif; background: #111827; color: #e5e7eb; }}
  header {{ padding: 1rem 1.25rem; border-bottom: 1px solid #374151; }}
  h1 {{ margin: 0; font-size: 1.2rem; }}
  #summary {{ color: #9ca3af; margin-top: .35rem; }}
  svg {{ width: 100vw; height: calc(100vh - 76px); display: block; }}
  .edge {{ stroke: #6b7280; stroke-width: 1.5; marker-end: url(#arrow); }}
  .edge-label {{ fill: #9ca3af; font-size: 11px; text-anchor: middle; }}
  .node {{ fill: #2563eb; stroke: #93c5fd; stroke-width: 2; }}
  .node.input {{ fill: #059669; }} .node.output {{ fill: #dc2626; }}
  .node-label {{ fill: white; text-anchor: middle; font-size: 12px; pointer-events: none; }}
</style>
</head>
<body>
<header><h1>{safe_title}</h1><div id="summary"></div></header>
<svg id="graph" viewBox="0 0 1200 720" role="img" aria-label="AMEBA graph"></svg>
<script>
const graph = {data};
const svg = document.getElementById("graph");
const ns = "http://www.w3.org/2000/svg";
const width = 1200, height = 720, radius = Math.min(width, height) * 0.36;
document.getElementById("summary").textContent = `${{graph.nodes.length}} nodes · ${{graph.edges.length}} edges`;
const defs = document.createElementNS(ns, "defs");
defs.innerHTML = '<marker id="arrow" viewBox="0 0 10 10" refX="19" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="#6b7280"/></marker>';
svg.appendChild(defs);
const positions = new Map(graph.nodes.map((node, index) => {{
  const angle = (Math.PI * 2 * index / Math.max(graph.nodes.length, 1)) - Math.PI / 2;
  return [node.id, {{x: width / 2 + radius * Math.cos(angle), y: height / 2 + radius * Math.sin(angle)}}];
}}));
for (const edge of graph.edges) {{
  const a = positions.get(edge.source), b = positions.get(edge.target);
  if (!a || !b) continue;
  const line = document.createElementNS(ns, "line");
  line.setAttribute("x1", a.x); line.setAttribute("y1", a.y);
  line.setAttribute("x2", b.x); line.setAttribute("y2", b.y); line.setAttribute("class", "edge");
  svg.appendChild(line);
  if (edge.attributes.weight !== undefined) {{
    const label = document.createElementNS(ns, "text");
    label.setAttribute("x", (a.x + b.x) / 2); label.setAttribute("y", (a.y + b.y) / 2 - 5);
    label.setAttribute("class", "edge-label"); label.textContent = Number(edge.attributes.weight).toPrecision(4);
    svg.appendChild(label);
  }}
}}
for (const node of graph.nodes) {{
  const p = positions.get(node.id);
  const circle = document.createElementNS(ns, "circle");
  circle.setAttribute("cx", p.x); circle.setAttribute("cy", p.y); circle.setAttribute("r", 34);
  circle.setAttribute("class", `node ${{node.kind}}`); circle.appendChild(document.createElementNS(ns, "title")).textContent = JSON.stringify(node.attributes);
  svg.appendChild(circle);
  const label = document.createElementNS(ns, "text");
  label.setAttribute("x", p.x); label.setAttribute("y", p.y + 4); label.setAttribute("class", "node-label");
  label.textContent = `${{node.kind}}:${{node.id}}`; svg.appendChild(label);
}}
</script>
</body>
</html>
"""


def write_graph_html(graph: Graph, path: str | Path, title: str = "AMEBA graph") -> None:
    Path(path).write_text(graph_to_html(graph, title), encoding="utf-8")

