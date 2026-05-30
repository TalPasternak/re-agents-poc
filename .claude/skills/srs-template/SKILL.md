---
name: srs-template
description: How to generate the System Requirements Specification (SRS) as Markdown with embedded Mermaid diagrams. Defines section-to-entity-type mappings and how Process.mermaid_code is embedded directly. Consult this skill before running srs-generator or editing templates/srs-template.md.
---

# SRS Generation

The SRS is **generated** from the requirements model, never authored by
hand. The generator (`srs-generator` agent) reads the model in `model/`,
applies the template in `templates/srs-template.md`, and writes
`output/srs.md`.

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
| 1. Introduction | the single `Component` of type `SYSTEM` | — | — |
| 2. System context | `SYSTEM` + all external components + their `Interface`s | `CONNECTS_TO` | Mermaid `graph` (computed) |
| 3. Capabilities | all `Capability`s | `IMPLEMENTED_BY`, `SUB_CAPABILITY_OF` | Mermaid `graph TD` of capability tree (computed) |
| 4. Requirements | all `Requirement`s | `REALIZED_BY`, `REFINES` | — |
| 5. Processes | all `Process`es | `ACTIVATES` (participants derived per metamodel §4a) | Embedded `mermaid_code` from each Process (verbatim) |
| 6. Traceability | all | `IMPLEMENTED_BY`, `REALIZED_BY`, `ACTIVATES` | — |

Domain modelling has no section yet (see metamodel skill §7).

## Placeholder syntax

Placeholders in `templates/srs-template.md` use `{{ ... }}` and are
evaluated by the generator:

| Placeholder | Resolves to |
|---|---|
| `{{system.title}}` | Title of the SYSTEM component |
| `{{system.description}}` | Description of the SYSTEM component |
| `{{components: component_type=ACTOR\|EXTERNAL_SYSTEM}}` | List of external components |
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

- **Context diagram** (`{{context-diagram}}`): one node for the SYSTEM
  component (distinct shape — `[[...]]`), one node per external
  component (rounded — `(...)`), one labelled edge per interface using
  the interface title.
- **Capability tree** (`{{capability-diagram}}`): `graph TD` with the
  parent at top; `SUB_CAPABILITY_OF` becomes a parent→child edge.
  Capabilities `IMPLEMENTED_BY` a component get a side-note indicating
  the component, but the implementation link is not an edge in this
  diagram (it would clutter).
- Sequence/state/activity diagrams come **from the Process** verbatim —
  the generator does not synthesise them.

## Generation steps

1. Load all YAML under `model/`.
2. Run all eight semantic rules from the metamodel skill. If any fails,
   emit a validation report to `output/generation-report.md` and stop.
3. Load `templates/srs-template.md`.
4. Resolve each placeholder in order.
5. Write `output/srs.md`.
6. Write `output/generation-report.md` listing:
   - Sections populated, with entity/relationship counts
   - Sections skipped (and why)
   - Entities in the model not referenced by any section (potential
     model gap or template gap)
   - Validation warnings (non-fatal)

## If the model is too thin to generate

If the model has no SYSTEM component, or no capabilities, the generator
does **not** emit a skeleton SRS. It writes a one-paragraph note to
`output/generation-report.md` saying what's missing and recommending
which agent to run next.
