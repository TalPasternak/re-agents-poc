# Mapping to the Python Parent Project

The PoC has two sides:

- This repo: **Claude Code subagents** authoring YAML on disk.
- The Python parent: **Anthropic SDK** with Pydantic models in
  `SystemMetaModel.py`, agents in `chief_analyst.py` and
  `process_agent.py`, and a workflow in `workflow.py`.

The two are designed to stay in lock-step so we can graduate from
subagents to the SDK without rewriting the metamodel or the prompts.

## Entity mapping

| Subagent YAML | Python class | Notes |
|---|---|---|
| `model/components/*.yaml` | `Component` | `component_type` enum identical |
| `model/capabilities/*.yaml` | `Capability` | — |
| `model/requirements/*.yaml` | `Requirement` | `requirement_type`, `priority`, `rationale`, `acceptance_criteria` |
| `model/interfaces/*.yaml` | `Interface` | `connected_component_ids` is a list ≥ 2 |
| `model/processes/*.yaml` | `Process` | `diagram_type`, `mermaid_code`, **plus `scope_component_id`** (see below) |
| `model/relationships/*.yaml` | `Relationship` | One file per edge, `relationship_type` enum identical *except* `PARTICIPATES_IN_PROCESS` (see below) |

The subagent YAML additionally carries a `provenance` block (activity,
mode, rationale, dates). The Python side currently ignores it on load;
on migration we'll add an optional `provenance` field to `BaseEntity`.

## Metamodel divergences (subagent → Python on migration)

The subagent metamodel intentionally diverges from the Python parent
in two places. Both should be reconciled on migration:

### 1. `Process.scope_component_id` (subagent has, Python does not)

The subagent `Process` schema has a `scope_component_id` field that
declares the Component the process is being analysed *from inside*. It
defaults to the SYSTEM component for top-level processes. The Python
`Process` class has no equivalent; add it as
`scope_component_id: Optional[str] = None` and have the validator
default-resolve to the SYSTEM component when None.

Why it matters: the subagent metamodel uses scope to derive the
participant set for sequence diagrams (Rule 8). Without scope, the
Python can't enforce that rule at lower levels of analysis. (It still
works at top level because scope defaults.)

### 2. `PARTICIPATES_IN_PROCESS` (Python has, subagent does not)

The Python `RelationshipType` enum and `ALLOWED_RELATIONSHIPS` matrix
include `PARTICIPATES_IN_PROCESS: (COMPONENT, PROCESS)`. Nothing in
the validator or `process_builder.py` creates or reads it.

The subagent metamodel **omits it**: process participation is
**derived** from `ACTIVATES` + `IMPLEMENTED_BY` + the capability
hierarchy, evaluated against the process's scope. Recommend deleting
the enum value, the matrix entry, and the (unused) reference to it on
next touch.

### 3. Semantic rule 8 (subagent has, Python does not)

The subagent metamodel adds **Rule 8 — sequence-diagram participants
match derived participants**. Implementing this in Python requires (a)
the `scope_component_id` field above, (b) parsing `mermaid_code` to
extract participants, (c) comparing against the derivation procedure
in the metamodel skill §4a. STATE and ACTIVITY diagrams are exempt.

## Agent mapping

| Subagent | Python | Status |
|---|---|---|
| `chief-analyst` | `chief_analyst.analyze_requirements` + `refine_with_answers` | Same JSON contract |
| `capability-decomposer` | *(none yet)* | Subagent-only for now |
| `process-analyst` | `process_agent.analyze_process` | Same JSON contract + `scope_component_id` (new), plus the bug fix below |
| `domain-modeler` | *(none yet)* | Stub on both sides |
| `srs-generator` | *(none yet — current Python just prints to stdout)* | Subagent-only for now |

## Known bug being corrected on the subagent side

In `process_builder.py`:

```python
# attach to FIRST capability (simple heuristic for now)
if response.activated_capabilities:
    cap_name = response.activated_capabilities[0]
    ...
```

Every requirement gets linked to the first activated capability,
regardless of the `capability` field on each `RequirementSchema`. The
JSON output from the LLM is correct (each requirement specifies its
capability); only the builder ignores it.

The subagent `process-analyst` honours the per-requirement field. When
porting, fix the Python builder to do the same.

## Migration path (subagents → SDK)

When persistence, multi-user use, or runtime tool calls become
limiting, the migration is:

1. Add a `provenance` field to `BaseEntity` in `SystemMetaModel.py`.
2. Add YAML loaders/writers next to the in-memory `SystemModel` — keep
   slug-style IDs as a separate field alongside the UUID, or replace
   the UUID factory with a slug generator.
3. **Reconcile the metamodel divergences** above:
   - Add `scope_component_id: Optional[str] = None` to `Process`.
   - Remove `PARTICIPATES_IN_PROCESS` from `RelationshipType` and
     `ALLOWED_RELATIONSHIPS`.
   - Implement Rule 8 in `SemanticValidator` (sequence-diagram
     participant set matches derived participants).
   - Fix the `process_builder.py` first-capability bug.
4. Translate each agent's system prompt from
   `.claude/agents/<name>.md` body into a Python string constant. The
   "hard rules" and "JSON shape" sections are designed to drop in
   directly.
5. Build out `srs-generator` and `capability-decomposer` in Python.
6. Decide on domain modelling (the metamodel skill §7 and DEC-007).

The `src/main/java/` tree is reserved for when (or if) the runtime
goes to Java instead.
