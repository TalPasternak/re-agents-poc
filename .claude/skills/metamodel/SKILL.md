---
name: metamodel
description: The requirements metamodel — defines all entity types, their attributes, the relationship taxonomy, the allowed-source/target type matrix, and the eight semantic validation rules. Consult this skill before creating, modifying, or validating any entity or relationship in the model. Every activity agent and the SSS generator depend on this skill.
---

# Requirements Metamodel

This metamodel defines the requirements model used throughout the
project. **This document is the source of truth**; the YAML files under
`model/` are its on-disk projection, and every activity agent and the
SSS generator conform to it.

## 1. Overview

The model is a typed graph:

- **Five entity types** — Component, Capability, Requirement, Interface,
  Process.
- **Eight relationship types**, each with a strict allowed (source, target)
  type pair.
- **Eight semantic rules** beyond pure type-checking, listed in §5.

There is no "context-element" or "domain-concept" entity type — those
concepts are expressed using the existing types:

- **System context** = the **system under design** — the set of
  `Component`s with `boundary: INTERNAL`, one of which is the `SYSTEM`
  umbrella that names the whole — surrounded by `Component`s with
  `boundary: EXTERNAL` (type `ACTOR` / `EXTERNAL_SYSTEM` / `EXTERNAL_*`),
  linked by `Interface` work-items. The system may comprise **several**
  internal components, and any of them may own capabilities; it is not
  restricted to a single component.
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
`cap-analyse-process`, `rel-impl-cap-by-system`). Slugs are preferred
because they're far easier to reference in Markdown prompts and across
files. The Java side (when built) can map slugs to its own IDs.

## 3. Entity types

All entities share these base attributes:

```yaml
id: <slug>
entity_type: COMPONENT | CAPABILITY | REQUIREMENT | INTERFACE | PROCESS
title: <short label>
description: <one or more sentences>      # optional
tags: [<tag>, ...]                        # optional
provenance:                               # provenance block (see below)
  activity: context | capability | process | domain
  mode: 1 | 2 | 3 | 4                     # see interaction-modes skill
  rationale: <one line>
  created: <ISO-8601 date>
  last_modified: <ISO-8601 date>
```

`provenance` captures which activity and which interaction mode produced
or last modified the entity. The Java side will read it when we migrate.

### 3.1 Component

```yaml
id: c-<slug>
entity_type: COMPONENT
title: ...
description: ...
component_type: SYSTEM | SUBSYSTEM | EXTERNAL_SYSTEM | ACTOR | SERVICE | DATABASE | UI | OTHER
boundary: INTERNAL | EXTERNAL      # membership in the system under design
tags: [...]
provenance: {...}
```

Notes:
- **`boundary`** declares whether the component is **part of the system
  under design** (`INTERNAL`) or in the **environment** (`EXTERNAL`). It
  is orthogonal to `component_type`, which describes the *kind* of thing.
  - Only `INTERNAL` components may own capabilities (be `IMPLEMENTED_BY`
    targets — see §5 rules 3 and 7).
  - The field is **inferred** when omitted for the unambiguous types:
    `SYSTEM` ⇒ `INTERNAL`; `ACTOR` and `EXTERNAL_SYSTEM` ⇒ `EXTERNAL`.
    For the ambiguous types (`SUBSYSTEM`, `SERVICE`, `DATABASE`, `UI`,
    `OTHER`) `boundary` **must be stated explicitly**.
- **The system under design may be several components.** All `INTERNAL`
  components together constitute it. **Exactly one** of them has
  `component_type: SYSTEM` — the **umbrella** that names the whole (used
  for the SSS title and Section 1). The umbrella does *not* mean the
  system is a single component; the other internal parts have other
  types with `boundary: INTERNAL` and are the ones that **own
  capabilities**. The chief-analyst agent enforces the single-umbrella
  rule.
- **The umbrella does not own capabilities** whenever the system has any
  other internal part. Capabilities are `IMPLEMENTED_BY` the real
  internal parts, never the `SYSTEM` umbrella. A **cross-cutting /
  system-wide** capability that applies to every (or several) parts is
  `IMPLEMENTED_BY` **each applicable part** — one `IMPLEMENTED_BY` edge
  per owning component — *not* by the umbrella. The only case in which a
  capability may be `IMPLEMENTED_BY` the umbrella is a **single-component
  system**, where the umbrella is the sole `INTERNAL` component and there
  is nowhere else to put capabilities.
- `ACTOR` and `EXTERNAL_SYSTEM` (and other external-* values if added)
  are `EXTERNAL` and represent the system's environment.
- `SUBSYSTEM` components are linked to their parent via
  `SUB_COMPONENT_OF`; internal parts are conventionally `SUB_COMPONENT_OF`
  the umbrella (directly or transitively).

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
- A **root** capability (no parent) is what gets `IMPLEMENTED_BY` an
  **INTERNAL** component. Different roots may be owned by different
  internal components — capabilities are not all attached to one
  component — and a single root may be `IMPLEMENTED_BY` **more than one**
  component when it is cross-cutting (system-wide). The owning components
  are never the `SYSTEM` umbrella unless the umbrella is the only
  internal component.
- A **leaf** capability (no children) is what gets `REALIZED_BY` a
  requirement. Shape the hierarchy so each leaf is broad enough to
  anticipate **about five** requirements, not one (see §5a).

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
process-analyst when the process is created. The SSS generator embeds
it as-is; it does not regenerate diagrams from relationships.

### `scope_component_id` — the analysis level

Every process is analysed *from inside* some containing Component — its
**scope**. At the top level of analysis the scope is the SYSTEM
component (this is the default if the field is omitted, to keep
top-level process YAML compact). At lower levels the scope is an
internal `SUBSYSTEM` (or other internal-typed) Component, and the
process describes interactions *among that scope's sub-components and
its environment at that level*.

The scope field makes the level of analysis explicit, which is what the
participant-derivation procedure (§4a) needs to compute the participant
set for a process's sequence diagram.

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

This metamodel has **no** `PARTICIPATES_IN_PROCESS` relationship.
Process participation is **derivable** from `ACTIVATES` +
`IMPLEMENTED_BY` + the capability hierarchy — see §4a below — so storing
it as an edge would only risk drift.

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

- **At top level** (scope = the system under design), `activating_components`
  is the set of **INTERNAL** components whose activated capabilities are
  exercised. When the system under design is a single component (just the
  umbrella) this is `{umbrella}`; when it comprises several internal
  components, it is the subset of them that own an activated capability.
  The environment is the set of `EXTERNAL` components (`ACTOR` /
  `EXTERNAL_SYSTEM`) the process interacts with. Participants are
  therefore `(internal components owning an activated capability) ∪
  (some externals)`.
- **At lower level** (scope = some `SUBSYSTEM` S), `activating_components`
  is the set of S's direct child components whose capabilities are
  activated. The environment is the set of things outside S that S
  interfaces with — which includes the system's true externals *and*
  any siblings of S that S talks to.

Process participation is **not** stored as edges. Any agent or
renderer that needs the participant set computes it.

## 5. Semantic rules

Beyond type-checking, the validator enforces eight rules. The
`sss-generator` runs them before producing output and stops if any
fails. Activity agents should also check them after every save.

1. **No component cycles.** The `SUB_COMPONENT_OF` graph must be acyclic.
2. **No capability cycles.** The `SUB_CAPABILITY_OF` graph must be
   acyclic.
3. **Every capability belongs to an internal component.** Walk up the
   capability hierarchy to the root; that root must have `IMPLEMENTED_BY`
   to some component whose `boundary` is `INTERNAL`.
4. **Every requirement belongs to an internal component.** Walk from the
   requirement via `REALIZED_BY` to its leaf capability, then up the
   capability tree to a root that's `IMPLEMENTED_BY` an `INTERNAL`
   component.
5. **Sub-component requirements refine parent-component requirements.**
   If component B is `SUB_COMPONENT_OF` A, every requirement attached
   (via the capability tree) to B must `REFINES` some requirement
   attached to A.
   - **Umbrella exemption.** The `SYSTEM` umbrella is an organisational
     container for the system under design, not a requirement-bearing
     component. When B's only parent (via `SUB_COMPONENT_OF`) is the
     `SYSTEM` umbrella and that umbrella owns no requirements of its own,
     B's requirements are exempt — no `REFINES` edge is required. The
     rule still applies in full between any two parts that both own
     requirements, and to the umbrella as a parent once it does own
     requirements.
6. **Only leaf capabilities may be `REALIZED_BY` requirements.** A
   capability with at least one `SUB_CAPABILITY_OF` incoming edge is
   non-leaf and may not carry requirements directly — push them down.
7. **Only root capabilities may be `IMPLEMENTED_BY` an INTERNAL
   component.** A capability with an outgoing `SUB_CAPABILITY_OF` edge is
   non-root and may not be implemented directly — only its top ancestor
   is. The `IMPLEMENTED_BY` **target** must be a component with
   `boundary: INTERNAL`; pointing a capability at an `EXTERNAL` component
   is invalid (external entities own no capabilities). The target must
   **not** be the `SYSTEM` umbrella whenever any other internal component
   exists — the umbrella owns capabilities **only** in a single-component
   system. A root may have **several** `IMPLEMENTED_BY` edges (one per
   owning component) when it is cross-cutting; each target must still be a
   non-umbrella `INTERNAL` component.
8. **Sequence-diagram participants match derived participants.** For
   every Process P with `diagram_type: SEQUENCE`, the set of
   participants named in `mermaid_code` must equal the set computed by
   `participants(P)` in §4a — no more, no less. STATE and ACTIVITY
   diagrams are exempt from this rule (their nodes are states or
   actions, not components).

Plus, validator-level: **Every process must activate at least one
capability** (have at least one outgoing `ACTIVATES` edge).

### 5a. Capability granularity guidance

This is **guidance**, not a hard validator rule, but it governs how
chief-analyst and capability-decomposer shape the tree:

- **Calibrate to ~5 requirements per leaf.** Build the capability
  hierarchy so each leaf is expected to carry **about five** requirements
  — broad enough to gather a coherent cluster of related requirements,
  narrow enough that they share a theme. A leaf that would realise only
  one requirement is **over-decomposed**; merge it up.
- **Decompose lazily.** Only split a leaf when it would plausibly carry
  **many more** than ~5 requirements, or when its requirements fall into
  clearly distinct themes. The hierarchy is meant to be **elaborated
  later** as more requirements emerge — start coarse, deepen on demand.
- **Symptom to avoid.** A model where most leaves carry a single
  requirement indicates the tree was decomposed too far up front.

### Level-of-analysis vs. metamodel rules

The chief-analyst agent enforces an additional rule for **top-level**
work: *only INTERNAL components have capabilities; EXTERNAL entities do
not.* Several internal components may each own capabilities — even at
level 1 the capabilities need not all belong to the umbrella. This is a
**rule of level-1 analysis** layered on top of the metamodel rule that
`IMPLEMENTED_BY` targets must be `INTERNAL` (§5 rule 7); at lower levels,
internal `SUBSYSTEM` components own their own capability hierarchies. Do
not weaken the metamodel rule — `IMPLEMENTED_BY` an `EXTERNAL` component
is always invalid.

## 6. Worked example (excerpt — dogfooding)

A minimal slice of the RE-Agents PoC modelled as itself:

```yaml
# components/c-system-re-agents.yaml
id: c-system-re-agents
entity_type: COMPONENT
title: RE-Agents PoC
description: A subagent-based tool that facilitates requirements engineering.
component_type: SYSTEM
boundary: INTERNAL
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
boundary: EXTERNAL
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
provenance: {activity: context, mode: 1, rationale: Single-component system — the umbrella is the only INTERNAL component, so it owns the capability (the §5 rule-7 single-component exception)., created: 2026-05-29, last_modified: 2026-05-29}
```

> Note: here the umbrella is `IMPLEMENTED_BY`-targeted only because it is
> the **sole** internal component. In a multi-component system, this edge
> would instead target the real internal part(s), never the umbrella.

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

