# RE-Agents PoC — Project Guide for Claude Code

This project is a **proof-of-concept** for using AI agents to facilitate
**requirements engineering**. It is the subagent-based companion to an
existing **Python SDK-based prototype** (see `docs/python-parent.md` for
the mapping between the two).

## Mission

Help a human analyst build a **requirements model** through structured
conversation, then emit a **System Requirements Specification (SRS)** as
Markdown with embedded Mermaid.

## The model

The model is a typed graph mirroring the Python `SystemMetaModel`:

- **5 entity types**: Component, Capability, Requirement, Interface,
  Process
- **8 relationship types** with strict source/target type pairs
- **8 semantic rules** enforced on every save and before SRS generation

Full details in `.claude/skills/metamodel/SKILL.md`. The metamodel skill
is **mandatory reading** for every activity agent.

## The agents

| Agent | Mirrors Python | Purpose |
|---|---|---|
| `chief-analyst` | `chief_analyst.py` | First-level analysis: system boundary, externals, interfaces, top-level capabilities |
| `capability-decomposer` | *(new)* | Decompose root capabilities into sub-capability trees so leaves can carry requirements |
| `process-analyst` | `process_agent.py` | Analyse a process: produce Mermaid + requirements per activated capability |
| `domain-modeler` | *(stub)* | Future work — domain vocabulary modelling |
| `srs-generator` | *(new)* | Read-only: validate the model and emit `output/srs.md` |

## The four interaction modes

Every activity agent supports all four modes:

1. **User provides input** — analyst supplies free-form information
2. **Agent asks clarification questions** — agent drives focused Q&A
3. **Agent offers options** — agent proposes choices, analyst selects
4. **User makes suggestions** — analyst proposes, agent validates

Detail in `.claude/skills/interaction-modes/SKILL.md`.

## Where things live

| Path | Purpose |
|---|---|
| `.claude/agents/*.md` | The five agents (one is a stub) |
| `.claude/skills/interaction-modes/SKILL.md` | The four modes |
| `.claude/skills/metamodel/SKILL.md` | Entity types, link types, schemas, semantic rules |
| `.claude/skills/srs-template/SKILL.md` | Section mappings and generation procedure |
| `model/components/`, `capabilities/`, `requirements/`, `interfaces/`, `processes/`, `relationships/` | One YAML per entity / relationship |
| `templates/srs-template.md` | The SRS template with placeholders |
| `output/srs.md` | The generated SRS |
| `output/generation-report.md` | Per-run report (gitignored) |
| `docs/` | Decision log, mapping to the Python parent, notes |
| `src/main/java/` | Java stub for future SDK migration |
| `examples/` | Test inputs (we dogfood: the example is this tool) |

## Working principles

- **Model first, prose second.** No content in the SRS that doesn't
  trace to an entity or relationship.
- **Validate after every save.** The eight semantic rules in the
  metamodel skill apply continuously, not just at generation time.
- **Mermaid for processes is part of the model.** The `process-analyst`
  writes `mermaid_code` directly into the `Process` work-item; the
  generator embeds it verbatim. Context and capability diagrams are
  computed by the generator from relationships.
- **Stay in lock-step with the Python.** When the metamodel changes,
  update both sides. See `docs/python-parent.md`.

## Status

Working scaffold. The metamodel and three of four activity agents
(chief-analyst, capability-decomposer, process-analyst) are aligned
with the Python parent. The domain-modeler is a stub; the SRS template
placeholders are defined but the rendering logic is described in the
skill rather than coded yet.
