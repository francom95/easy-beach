## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- Keep the graph updated at commit time: right before creating a new git commit, if tracked files changed since the graph's last update, run the graphify update flow first so the commit includes a fresh graph.
  - **Code files:** run `graphify update .` (AST-only, free, no LLM).
  - **Markdown/doc files:** these need semantic extraction (LLM), which only I can trigger. Run the incremental update flow (`detect_incremental` → dispatch extraction subagent(s) for the changed files → merge → rebuild/cluster/report).
  - Do not run this mid-turn on every edit — only when about to commit, or when explicitly asked ("actualiza el grafo").
