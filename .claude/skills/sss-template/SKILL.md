---
name: sss-template
description: How to generate the System/Subsystem Specification (SSS) as Markdown with embedded Mermaid diagrams. Defines section-to-entity-type mappings and how Process.mermaid_code is embedded directly. Consult this skill before running sss-generator or editing templates/sss-template.md.
---

# SSS Generation

The SSS is **generated** from the requirements model, never authored by
hand. The generator (`sss-generator` agent) reads the model in `model/`,
applies the template in `templates/sss-template.md`, and writes
`output/sss.md`.

## Generation principles

1. **No content without source.** Every paragraph, list item, and table
   row must trace to an entity or relationship. If the model has nothing
   for a section, render the section's "no items" fallback or omit the
   section per the template.
2. **Deterministic.** Same model → byte-identical output. Sort entities
   by id; sort relationships by (relationship_type, source_id, target_id);
   use entity `description` verbatim — never paraphrase. No timestamps
   in the body (they go in the generation report).
3. **Mermaid is part of the model, not regenerated.** Processes carry
   their own `mermaid_code`. The generator embeds it verbatim inside a
   fenced code block. Other diagrams (context, capability hierarchy,
   etc.) are computed by the generator from the relationship graph using
   the conventions in §3.

## Section → entity mapping

| Section | Entities used | Relationships used | Diagram |
|---|---|---|---|
| 1. Introduction | the `SYSTEM` umbrella `Component` (title + description) | — | — |
| 2. System Description — 2.1 System Context | all `INTERNAL` components (umbrella + parts) + all `EXTERNAL` components + their `Interface`s | `CONNECTS_TO` | Mermaid `graph` (computed) |
| 2. System Description — 2.2/2.3 Capabilities | all `Capability`s (owned by any `INTERNAL` component) | `IMPLEMENTED_BY`, `SUB_CAPABILITY_OF` | Mermaid `graph TD` of capability tree (computed) |
| 3. Requirements | all `Requirement`s | `REALIZED_BY`, `REFINES` | — |
| 4. Processes | all `Process`es | `ACTIVATES` (participants derived per metamodel §4a) | Embedded `mermaid_code` from each Process (verbatim) |
| 5. Traceability | all | `IMPLEMENTED_BY`, `REALIZED_BY`, `ACTIVATES` | — |

Domain modelling has no section yet (see metamodel skill §7).

## Placeholder syntax

Placeholders in `templates/sss-template.md` use `{{ ... }}` and are
evaluated by the generator:

| Placeholder | Resolves to |
|---|---|
| `{{system.title}}` | Title of the SYSTEM umbrella component |
| `{{system.description}}` | Description of the SYSTEM umbrella component |
| `{{components: boundary=INTERNAL}}` | List of internal components (the umbrella + its parts) |
| `{{components: boundary=EXTERNAL}}` | List of external components |
| `{{interfaces}}` | All interfaces (table: title, type, connected components) |
| `{{capability-tree}}` | Markdown nested list of the capability hierarchy |
| `{{capability-diagram}}` | Computed Mermaid `graph TD` of the capability tree |
| `{{context-diagram}}` | Computed Mermaid `graph` of SYSTEM + externals + interfaces |
| `{{requirements: requirement_type=FUNCTIONAL}}` | Functional requirements grouped by realising capability |
| `{{requirements: requirement_type=NON_FUNCTIONAL}}` | Non-functional requirements grouped likewise |
| `{{processes}}` | Each process, its description, its embedded `mermaid_code`, and a list of activated capabilities |
| `{{traceability-matrix}}` | Capability × Requirement matrix, with cells indicating `REALIZED_BY` |

Empty results render as `*(no items)*` unless the template marks the
section with `{{omit-if-empty}}`.

## Generated-diagram conventions

When the generator computes a diagram from relationships:

- **Context diagram** (`{{context-diagram}}`): the `SYSTEM` umbrella as a
  distinct node (`[[...]]`); any other `INTERNAL` components as plain
  boxes (`[...]`) grouped with the umbrella (e.g. inside a
  `subgraph` labelled "System under design"); one node per `EXTERNAL`
  component (rounded — `(...)`); one labelled edge per interface using
  the interface title. When the system is a single component, only the
  umbrella appears on the internal side.
- **Capability tree** (`{{capability-diagram}}`): `graph TD` with the
  parent at top; `SUB_CAPABILITY_OF` becomes a parent→child edge. Each
  root capability `IMPLEMENTED_BY` a component gets a side-note naming its
  **owning internal component** (roots owned by different internal
  components are distinguished), but the implementation link is not an
  edge in this diagram (it would clutter).
- Sequence/state/activity diagrams come **from the Process** verbatim —
  the generator does not synthesise them.

## Generation steps

1. Load all YAML under `model/`.
2. Run all eight semantic rules from the metamodel skill. If any fails,
   emit a validation report to `output/generation-report.md` and stop.
3. Load `templates/sss-template.md`.
4. Resolve each placeholder in order.
5. Write `output/sss.md`.
6. Write `output/generation-report.md` listing:
   - Sections populated, with entity/relationship counts
   - Sections skipped (and why)
   - Entities in the model not referenced by any section (potential
     model gap or template gap)
   - Validation warnings (non-fatal)

## If the model is too thin to generate

If the model has no SYSTEM component, or no capabilities, the generator
does **not** emit a skeleton SSS. It writes a one-paragraph note to
`output/generation-report.md` saying what's missing and recommending
which agent to run next.
