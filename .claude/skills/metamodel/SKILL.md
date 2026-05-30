---
name: metamodel
description: The requirements metamodel — defines all entity types, their attributes, the relationship taxonomy, the allowed-source/target type matrix, and the eight semantic validation rules. Consult this skill before creating, modifying, or validating any entity or relationship in the model. Every activity agent and the SRS generator depend on this skill. Mirrors the Python SystemMetaModel in the parent project.
---

# Requirements Metamodel

This metamodel is a faithful YAML representation of the Python
`SystemMetaModel` in the parent project. The Python code is the source of
truth; this document is the on-disk projection used by the subagents.
When the two diverge, fix the YAML, not the Python.

## 1. Overview

The model is a typed graph:

- **Five entity types** — Component, Capability, Requirement, Interface,
  Process.
- **Eight relationship types**, each with a strict allowed (source, target)
  type pair.
- **Eight semantic rules** beyond pure type-checking, listed in §5.

There is no "context-element" or "domain-concept" entity type — those
concepts are expressed using the existing types:

- **System context** = a `Component` of type `SYSTEM` (exactly one)
  surrounded by `Component`s of type `ACTOR` / `EXTERNAL_SYSTEM` /
  `EXTERNAL_*`, linked by `Interface` work-items.
- **Domain modelling** is not yet part of the metamodel (see §7).

## 2. Storage layout

```
model/
├── components/<id>.yaml
├── capabilities/<id>.yaml
├── requirements/<id>.yaml
├── interfaces/<id>.yaml
├── processes/<id>.yaml
└── relationships/<id>.yaml
```

IDs are stable, human-readable slugs (e.g. `c-system-re-agents`,
`cap-analyse-process`, `rel-impl-cap-by-system`). The Python code uses
UUIDs; subagents prefer slugs because they're far easier to reference in
Markdown prompts and across files. The Java side (when built) can map
slugs to UUIDs.

## 3. Entity types

All entities share these base attributes:

```yaml
id: <slug>
entity_type: COMPONENT | CAPABILITY | REQUIREMENT | INTERFACE | PROCESS
title: <short label>
description: <one or more sentences>      # optional
tags: [<tag>, ...]                        # optional
provenance:                               # subagent extension, not in Python
  activity: context | capability | process | domain
  mode: 1 | 2 | 3 | 4                     # see interaction-modes skill
  rationale: <one line>
  created: <ISO-8601 date>
  last_modified: <ISO-8601 date>
```

`provenance` is a subagent-side extension to capture which activity and
which interaction mode produced or last modified the entity. The Python
side ignores it on load; the Java side will read it when we migrate.

### 3.1 Component

```yaml
id: c-<slug>
entity_type: COMPONENT
title: ...
description: ...
component_type: SYSTEM | SUBSYSTEM | EXTERNAL_SYSTEM | ACTOR | SERVICE | DATABASE | UI | OTHER
tags: [...]
provenance: {...}
```

Notes:
- **Exactly one** component must have `component_type: SYSTEM` — this is
  the system under design. The chief-analyst agent enforces this.
- `ACTOR` and `EXTERNAL_SYSTEM` (and other external-* values if added)
  represent the system's environment.
- `SUBSYSTEM` components are linked to their parent via
  `SUB_COMPONENT_OF`.

### 3.2 Capability

```yaml
id: cap-<slug>
entity_type: CAPABILITY
title: ...
description: ...
tags: [...]
provenance: {...}
```

Capabilities form a hierarchy via `SUB_CAPABILITY_OF`. Two key rules
(see §5):
- A **root** capability (no parent) is what gets `IMPLEMENTED_BY` a
  component.
- A **leaf** capability (no children) is what gets `REALIZED_BY` a
  requirement.

Non-root, non-leaf capabilities are pure structural nodes — neither
implemented by anything directly, nor realised by requirements directly.

### 3.3 Requirement

```yaml
id: req-<slug>
entity_type: REQUIREMENT
title: <short label>
description: <IEEE 29148-style "The system shall ..." statement>
requirement_type: FUNCTIONAL | NON_FUNCTIONAL | CONSTRAINT
priority: CRITICAL | HIGH | MEDIUM | LOW    # default MEDIUM
rationale: <why this requirement exists>     # optional
acceptance_criteria:                         # optional
  - <criterion>
  - <criterion>
tags: [...]
provenance: {...}
```

Requirements must be linked to a leaf capability via `REALIZED_BY`.

### 3.4 Interface

```yaml
id: if-<slug>
entity_type: INTERFACE
title: ...
description: ...
interface_type: API | UI | DATABASE | MESSAGE_BUS | FILE | NETWORK | HUMAN | OTHER
protocol: <e.g. HTTP/JSON, gRPC, OAuth2>     # optional
connected_component_ids:
  - <component-id>
  - <component-id>
  # must be at least 2
tags: [...]
provenance: {...}
```

`CONNECTS_TO` relationships are derived from `connected_component_ids` —
the chief-analyst writes them explicitly when creating the interface so
the graph stays self-consistent.

### 3.5 Process

```yaml
id: proc-<slug>
entity_type: PROCESS
title: ...
description: ...
diagram_type: SEQUENCE | STATE | ACTIVITY
scope_component_id: <component-id>           # defaults to the SYSTEM if omitted
mermaid_code: |
  sequenceDiagram
    Actor->>System: ...
    System-->>Actor: ...
tags: [...]
provenance: {...}
```

The Mermaid source is **part of the model**, written by the
process-analyst when the process is created. The SRS generator embeds
it as-is; it does not regenerate diagrams from relationships.

### `scope_component_id` — the analysis level

Every process is analysed *from inside* some containing Component — its
**scope**. At the top level of analysis the scope is the SYSTEM
component (this is the default if the field is omitted, to keep
top-level process YAML compact). At lower levels the scope is an
internal `SUBSYSTEM` (or other internal-typed) Component, and the
process describes interactions *among that scope's sub-components and
its environment at that level*.

This field has no equivalent in the Python parent project today; it's
a planned addition there too (see `docs/python-parent.md`). The
metamodel divergence is deliberate and forward-looking.

## 4. Relationship types

Every relationship is:

```yaml
id: rel-<slug>
relationship_type: <see table>
source_id: <entity-id>
target_id: <entity-id>
description: ...               # optional
provenance: {...}              # same extension as entities
```

The allowed `(source.entity_type, target.entity_type)` for each
relationship type is **strict**:

| Relationship | Source type | Target type | Meaning |
|---|---|---|---|
| `SUB_COMPONENT_OF` | COMPONENT | COMPONENT | Source is a child component of target |
| `SUB_CAPABILITY_OF` | CAPABILITY | CAPABILITY | Source is a child capability of target |
| `REFINES` | REQUIREMENT | REQUIREMENT | Source is a finer-grained version of target |
| `IMPLEMENTED_BY` | CAPABILITY | COMPONENT | A root capability is realised by a component |
| `REALIZED_BY` | REQUIREMENT | CAPABILITY | A requirement is realised by a leaf capability |
| `ACTIVATES` | PROCESS | CAPABILITY | The process exercises this capability |
| `CONNECTS_TO` | INTERFACE | COMPONENT | The interface attaches to this component |

Any relationship that doesn't match its row is invalid.

The Python parent has a ninth value, `PARTICIPATES_IN_PROCESS`
(COMPONENT → PROCESS), declared but unused. The subagent metamodel
omits it because participation is **derivable** from `ACTIVATES` +
`IMPLEMENTED_BY` + the capability hierarchy — see §4a below. Recommend
deleting the unused enum value from the Python on next touch.

### 4a. Derived: process participants

A Component is a **participant** in a Process when one of its
capabilities is activated by that process, evaluated within the
process's scope. Formally:

```
participants(P) = derived_participants(P, scope(P))

derived_participants(P, S) =
    activating_components(P, S) ∪ environment_components(S, P)

activating_components(P, S) =
    { C : C is a direct child of S in the SUB_COMPONENT_OF tree
          (or C = S itself when S has no children and is acting on its own)
          AND some capability ACTIVATED by P has
              its root capability IMPLEMENTED_BY C }

environment_components(S, P) =
    { E : E is outside S in the SUB_COMPONENT_OF tree
          AND E is reachable from S via at least one Interface
              (CONNECTS_TO from an interface that lists both S, or some
               descendant of S, and E in connected_component_ids)
          AND P's mermaid_code references E as a participant }
```

Two important properties:

- **At top level** (scope = SYSTEM), `activating_components` collapses
  to `{SYSTEM}` because the SYSTEM has no internal-component children
  yet decomposed, and *only* the SYSTEM owns capabilities. The
  environment is the set of `ACTOR` / `EXTERNAL_SYSTEM` components.
  Participants are therefore `{SYSTEM} ∪ (some externals)`.
- **At lower level** (scope = some `SUBSYSTEM` S), `activating_components`
  is the set of S's direct child components whose capabilities are
  activated. The environment is the set of things outside S that S
  interfaces with — which includes the system's true externals *and*
  any siblings of S that S talks to.

Process participation is **not** stored as edges. Any agent or
renderer that needs the participant set computes it.

## 5. Semantic rules

Beyond type-checking, the validator enforces eight rules. The
`srs-generator` runs them before producing output and stops if any
fails. Activity agents should also check them after every save.

1. **No component cycles.** The `SUB_COMPONENT_OF` graph must be acyclic.
2. **No capability cycles.** The `SUB_CAPABILITY_OF` graph must be
   acyclic.
3. **Every capability belongs to a component.** Walk up the capability
   hierarchy to the root; that root must have `IMPLEMENTED_BY` to some
   component.
4. **Every requirement belongs to a component.** Walk from the
   requirement via `REALIZED_BY` to its leaf capability, then up the
   capability tree to a root that's `IMPLEMENTED_BY` a component.
5. **Sub-component requirements refine parent-component requirements.**
   If component B is `SUB_COMPONENT_OF` A, every requirement attached
   (via the capability tree) to B must `REFINES` some requirement
   attached to A.
6. **Only leaf capabilities may be `REALIZED_BY` requirements.** A
   capability with at least one `SUB_CAPABILITY_OF` incoming edge is
   non-leaf and may not carry requirements directly — push them down.
7. **Only root capabilities may be `IMPLEMENTED_BY` components.** A
   capability with an outgoing `SUB_CAPABILITY_OF` edge is non-root and
   may not be implemented directly — only its top ancestor is.
8. **Sequence-diagram participants match derived participants.** For
   every Process P with `diagram_type: SEQUENCE`, the set of
   participants named in `mermaid_code` must equal the set computed by
   `participants(P)` in §4a — no more, no less. STATE and ACTIVITY
   diagrams are exempt from this rule (their nodes are states or
   actions, not components).

Plus, validator-level: **Every process must activate at least one
capability** (have at least one outgoing `ACTIVATES` edge).

### Level-of-analysis vs. metamodel rules

The chief-analyst agent enforces an additional rule for **top-level**
work: *only the SYSTEM component has capabilities; external entities
do not.* This is a **rule of level-1 analysis**, not a metamodel rule.
The metamodel permits any Component to be `IMPLEMENTED_BY` a
capability — at lower levels, internal `SUBSYSTEM` components own
their own capability hierarchies. Do not generalise the chief-analyst
rule to the validator.

## 6. Worked example (excerpt — dogfooding)

A minimal slice of the RE-Agents PoC modelled as itself:

```yaml
# components/c-system-re-agents.yaml
id: c-system-re-agents
entity_type: COMPONENT
title: RE-Agents PoC
description: A subagent-based tool that facilitates requirements engineering.
component_type: SYSTEM
tags: [pocsystem]
provenance:
  activity: context
  mode: 1
  rationale: System under design identified from project README.
  created: 2026-05-29
  last_modified: 2026-05-29
```

```yaml
# components/c-actor-analyst.yaml
id: c-actor-analyst
entity_type: COMPONENT
title: Requirements Analyst
description: The human user driving the tool.
component_type: ACTOR
provenance:
  activity: context
  mode: 1
  rationale: Primary user role.
  created: 2026-05-29
  last_modified: 2026-05-29
```

```yaml
# interfaces/if-analyst-chat.yaml
id: if-analyst-chat
entity_type: INTERFACE
title: Analyst chat interface
description: Conversational interface through which the analyst directs activities.
interface_type: HUMAN
connected_component_ids: [c-system-re-agents, c-actor-analyst]
provenance:
  activity: context
  mode: 1
  rationale: Single channel for analyst interaction.
  created: 2026-05-29
  last_modified: 2026-05-29
```

```yaml
# capabilities/cap-analyse-process.yaml
id: cap-analyse-process
entity_type: CAPABILITY
title: Analyse a process
description: Expand a capability into a sequence of steps with actors, data, and decisions.
provenance: {activity: context, mode: 1, rationale: Top-level capability per spec., created: 2026-05-29, last_modified: 2026-05-29}
```

```yaml
# relationships/rel-impl-analyse-process.yaml
id: rel-impl-analyse-process
relationship_type: IMPLEMENTED_BY
source_id: cap-analyse-process
target_id: c-system-re-agents
provenance: {activity: context, mode: 1, rationale: Capability owned by the system., created: 2026-05-29, last_modified: 2026-05-29}
```

## 7. Open questions / gaps

- **Domain modelling** is not in the metamodel. Options when we add it:
  introduce a new entity type `DOMAIN_CONCEPT` with relationships like
  `RELATES_TO`, `SPECIALISES`; or extend `tags` to express domain
  vocabulary. Decision deferred (see `docs/decision-log.md` DEC-007).
- **IEEE 29148 phrasing rules** ("The system shall ...") are currently
  enforced inside the process-analyst's prompt, not the metamodel.
  They belong here if we want them validated independently.
- **Participant rule for STATE and ACTIVITY diagrams.** Rule 8 covers
  SEQUENCE only. A weaker form ("any Component named in the diagram
  must be in the derived participant set") may be worth adding once
  we have real STATE/ACTIVITY examples to test against.
- **Python parent divergences** (tracked in `docs/python-parent.md`):
  the subagent metamodel drops the unused `PARTICIPATES_IN_PROCESS`
  relationship and adds `Process.scope_component_id`. Both should be
  reconciled on migration.

