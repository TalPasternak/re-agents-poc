# Decision Log

A running record of design decisions for the RE-Agents PoC.

## DEC-001 — Use Claude Code subagents for the PoC
**Decision:** Build the PoC on Claude Code subagents
(`.claude/agents/*.md`) rather than the Anthropic SDK.
**Rationale:** Fastest path to validating the *methodology*. Subagents
are Markdown + YAML; no build pipeline, no API key management.
**Reconsider when:** persistence across sessions, multi-user access, or
runtime tool integration become limiting. Migrate to the Anthropic Java
SDK at that point.

## DEC-002 — Plain Java (no build tool yet)
**Decision:** No Maven or Gradle until the Java side does real work.
**Rationale:** Avoid premature scaffolding. The Java tree exists to
reserve the package layout for the eventual SDK migration.

## DEC-003 — Model stored as YAML files
**Decision:** YAML, one file per entity and per relationship.
**Rationale:** Human-readable, git-friendly. YAML is the on-disk
projection of the metamodel.

## DEC-004 — Metamodel is self-contained
**Decision:** Entity types, relationship types, the allowed-relationship
matrix, and the semantic rules are defined directly in
`.claude/skills/metamodel/SKILL.md`, which is the single source of
truth. The YAML schema and the activity agents follow it.
**Rationale:** The metamodel was originally seeded from an earlier
prototype but is now independent; one authoritative definition avoids
drift between the schema, the agents, and the generator.

## DEC-005 — Five agents (chief-analyst absorbs context-identification)
**Decision:** Replace the originally-planned `context-identifier` with
a `chief-analyst` that performs first-level analysis end-to-end (system
+ externals + interfaces + top-level capabilities). Add a separate
`capability-decomposer` to grow the capability tree downward. Keep
`process-analyst` and `sss-generator`. `domain-modeler` is a stub.
**Rationale:** First-level analysis is naturally one activity (system +
externals + interfaces + top-level capabilities); splitting it adds no
methodological benefit. The decomposer is the natural home for the
hierarchy work that grows the capability tree downward.

## DEC-006 — Provenance is a subagent-side extension
**Decision:** Every YAML file carries a `provenance` block recording
the activity and interaction mode that produced it.
**Rationale:** Subagents need it for traceability and for honouring
the four interaction modes. It is a straightforward optional field to
carry into the SDK at migration time.

## DEC-007 — Domain modelling deferred
**Decision:** No `DOMAIN_CONCEPT` entity type yet. Domain vocabulary is
captured in the `tags` field on existing entities until a proper design
is settled. The `domain-modeler` agent is a stub.
**Rationale:** Domain modelling needs a proper design before we commit
new entity types; deferring avoids premature, hard-to-reverse schema
changes. When we revisit, the choice is between (a) new entity +
relationships (`RELATES_TO`, `SPECIALISES`, `ATTRIBUTE_OF`,
`INSTANCE_OF`) or (b) tags + a glossary side-file.

## DEC-008 — Mermaid lives on the Process work-item
**Decision:** `Process.mermaid_code` carries the diagram, written by
`process-analyst` at process-creation time. The `sss-generator`
embeds it verbatim. Other diagrams (context, capability tree) are
computed from relationships at generation time.
**Rationale:** Lets the process-analyst control the storytelling of the
diagram (participant ordering, message phrasing) rather than
reverse-engineering from links. The cost is that
two runs of `process-analyst` may produce different Mermaid; the
analyst confirms before save.

## DEC-009 — Slug IDs, not UUIDs, on the YAML side
**Decision:** YAML uses stable slugs (`cap-analyse-process`,
`c-system-re-agents`) rather than UUIDs.
**Rationale:** Slugs are dramatically easier to reference in Markdown
prompts and across files in git. The SDK migration can add a slug field
or map slugs to its own IDs.

## DEC-010 — Per-requirement capability mapping
**Decision:** `process-analyst` honours the `capability` field on each
requirement, linking it to its own leaf capability — not all
requirements to the first activated capability.
**Rationale:** Each requirement must trace to the specific leaf
capability that realises it; collapsing every requirement onto the
first activated capability loses that information and breaks
traceability (rules 4 and 6).

## DEC-011 — No `PARTICIPATES_IN_PROCESS` relationship
**Decision:** The metamodel has no `PARTICIPATES_IN_PROCESS`
relationship. Participation is **derived** from `ACTIVATES` +
`IMPLEMENTED_BY` + the capability hierarchy, evaluated against a
process's scope.
**Rationale:** Storing derived data is a smell — it can drift from the
underlying truth and doubles the write path. Deriving participation on
demand keeps the model honest.

## DEC-012 — Add `Process.scope_component_id` (field, not relationship)
**Decision:** Every Process declares the Component it's being analysed
*from inside* via `scope_component_id`. Top-level processes may omit
the field; it defaults to the SYSTEM component.
**Rationale:** The participant set for a process's sequence diagram
depends on the level of analysis — top-level participants are
{SYSTEM} + selected externals; lower-level participants are an
internal component's children + its environment at that level. Scope
makes the level explicit.
**Form:** Field on Process, not a `SCOPED_TO` relationship. The
process is intrinsically scoped to one Component; embedding the link
keeps the Process self-contained and avoids a one-to-one relationship
type that would never be many-to-many.
**Default:** Implicit default to SYSTEM minimises ceremony for the
common case. We accept the cost that introducing a lower-level
process requires explicitly setting the field — a worthwhile prompt
for the analyst to think about level.

## DEC-013 — Rule 8: diagram participants = derived participants
**Decision:** Add an eighth semantic rule: for every Process with
`diagram_type: SEQUENCE`, the participants named in `mermaid_code`
exactly equal the set computed by the participant-derivation
procedure (metamodel §4a). STATE and ACTIVITY diagrams are exempt
for now (their nodes aren't components).
**Rationale:** Mermaid lives on the Process (DEC-008) precisely
because the agent should control phrasing — but unconstrained that
risks the diagram drifting from the relationship graph. Rule 8 ties
them together: the agent picks the participant *names and ordering*,
the model dictates the *set*.
**Implementation:** Validated at SSS generation time (parsing the
Mermaid). Activity agents should also self-check after writing a
Process.

## DEC-014 — "Only the system has capabilities" is a level-1 rule, not metamodel
**Decision:** The chief-analyst enforces "only the SYSTEM component
has capabilities at this level." The metamodel itself permits any
Component to be `IMPLEMENTED_BY` a capability; lower-level analysis
(subsystem-analyst, when built) will use the same rule scoped to its
own containing component.
**Rationale:** Earlier framing accidentally implied the metamodel
forbade non-SYSTEM components from owning capabilities. It doesn't.
The constraint applies at level 1 because at level 1 there are no
internal sub-components yet. Documenting the rule's scope correctly
avoids future confusion when we add lower-level analysis.

---

## Pending decisions

- **DEC-015 (open):** Final placeholder syntax for
  `templates/sss-template.md` once the template renderer is built (in
  the subagent or in Java).
- **DEC-016 (open):** Whether IEEE 29148 phrasing rules belong in the
  metamodel (validator) or stay in the process-analyst's system prompt.
  Currently the latter.
- **DEC-017 (open):** Whether to extend Rule 8 to STATE and ACTIVITY
  diagrams once we have real examples to test against. Current scope:
  SEQUENCE only.
