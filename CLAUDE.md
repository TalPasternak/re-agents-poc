# RE-Agents PoC — Project Guide for Claude Code

This project is a **proof-of-concept** for using AI agents to facilitate
**requirements engineering**, built on Claude Code subagents.

## Mission

Help a human analyst build a **requirements model** through structured
conversation, then emit a **System/Subsystem Specification (SSS)** as
Markdown with embedded Mermaid.

## The model

The model is a typed graph:

- **5 entity types**: Component, Capability, Requirement, Interface,
  Process
- **8 relationship types** with strict source/target type pairs
- **8 semantic rules** enforced on every save and before SSS generation

Full details in `.claude/skills/metamodel/SKILL.md`. The metamodel skill
is **mandatory reading** for every activity agent.

## The agents

| Agent | Purpose |
|---|---|
| `chief-analyst` | First-level analysis: system boundary, externals, interfaces, top-level capabilities |
| `capability-decomposer` | Decompose root capabilities into sub-capability trees so leaves can carry requirements |
| `process-analyst` | Analyse a process: produce Mermaid + requirements per activated capability |
| `domain-modeler` | Future work — domain vocabulary modelling (stub) |
| `sss-generator` | Read-only: validate the model and emit `output/sss.md` |

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
| `.claude/skills/sss-template/SKILL.md` | Section mappings and generation procedure |
| `model/components/`, `capabilities/`, `requirements/`, `interfaces/`, `processes/`, `relationships/` | One YAML per entity / relationship |
| `templates/sss-template.md` | The SSS template with placeholders |
| `output/sss.md` | The generated SSS |
| `output/generation-report.md` | Per-run report (gitignored) |
| `docs/` | Decision log and notes |
| `src/main/java/` | Java stub for future SDK migration |
| `examples/` | Test inputs (we dogfood: the example is this tool) |

## Working principles

- **Model first, prose second.** No content in the SSS that doesn't
  trace to an entity or relationship.
- **Validate after every save.** The eight semantic rules in the
  metamodel skill apply continuously, not just at generation time.
- **Mermaid for processes is part of the model.** The `process-analyst`
  writes `mermaid_code` directly into the `Process` work-item; the
  generator embeds it verbatim. Context and capability diagrams are
  computed by the generator from relationships.

## Status

Working scaffold. The metamodel and three of four activity agents
(chief-analyst, capability-decomposer, process-analyst) are in place.
The domain-modeler is a stub; the SSS template placeholders are defined
but the rendering logic is described in the skill rather than coded yet.
