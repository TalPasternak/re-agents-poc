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
| 1. Introduction | the `SYSTEM` umbrella `Component` (title + description) — as the **document subject** only, never listed as a component | — | — |
| 2. System Description — 2.1 System Context | the `INTERNAL` parts (every `INTERNAL` component **except** the `SYSTEM` umbrella) + all `EXTERNAL` components + their `Interface`s | `CONNECTS_TO` | Mermaid `graph` (computed) |
| 2. System Description — 2.2/2.3 Capabilities | all `Capability`s (owned by any `INTERNAL` component), **grouped by owning component**; umbrella-owned capabilities grouped under *System-wide* | `IMPLEMENTED_BY`, `SUB_CAPABILITY_OF` | Mermaid `graph TD` of capability tree (computed) |
| 3. Requirements | all `Requirement`s, **nested by component → capability → sub-capability** | `REALIZED_BY`, `REFINES` | — |
| 4. Processes | all `Process`es, each in its own subsection | `ACTIVATES` (participants derived per metamodel §4a) | Embedded `mermaid_code` from each Process (verbatim) |

The `SYSTEM` umbrella component is **never rendered as a component** in the
body — not in the §2.1.1 component list, not as a node in the context
diagram, and not as a named capability owner. It appears only as the
document's title/subject (§1). Capabilities it owns are rendered under a
*System-wide / cross-cutting capabilities* heading that does not name the
umbrella.

Domain modelling has no section yet (see metamodel skill §7).

## Placeholder syntax

Placeholders in `templates/sss-template.md` use `{{ ... }}` and are
evaluated by the generator:

| Placeholder | Resolves to |
|---|---|
| `{{system.title}}` | Title of the SYSTEM umbrella component (document subject) |
| `{{system.description}}` | Description of the SYSTEM umbrella component (document subject) |
| `{{components: boundary=INTERNAL}}` | List of internal components — the internal parts, **excluding** the SYSTEM umbrella |
| `{{components: boundary=EXTERNAL}}` | List of external components |
| `{{interfaces}}` | All interfaces (table: title, type, connected components) |
| `{{capability-tree}}` | Capability hierarchy grouped by owning internal component (umbrella-owned under *System-wide*); see conventions below |
| `{{capability-diagram}}` | Computed Mermaid `graph TD` of the capability tree |
| `{{context-diagram}}` | Computed Mermaid `graph` of the internal parts + externals + interfaces (no umbrella node) |
| `{{requirements: requirement_type=FUNCTIONAL}}` | Functional requirements nested by component → capability → sub-capability → requirement |
| `{{requirements: requirement_type=NON_FUNCTIONAL}}` | Non-functional requirements nested likewise |
| `{{requirements: requirement_type=CONSTRAINT}}` | Constraints nested likewise |
| `{{processes}}` | Each process in its own numbered subsection: description, embedded `mermaid_code`, a Component × Capability table of activated capabilities, and the derived participants |

Empty results render as `*(no items)*` unless the template marks the
section with `{{omit-if-empty}}`.

## Grouping and nesting conventions

These govern the **text** placeholders (not the diagrams):

- **Capabilities (`{{capability-tree}}`).** When more than one internal
  component owns capabilities, render a subheading per **owning internal
  component** (ordered by `(component_type, id)`), and under each, that
  component's root capabilities with their sub-capability trees. Drop the
  per-root "owned by …" annotation — the grouping conveys ownership.
  Capabilities owned by the `SYSTEM` umbrella are rendered last under a
  **System-wide / cross-cutting capabilities** heading that does **not**
  name the umbrella. When only one internal component owns capabilities,
  render a single flat tree with no component subheadings.
- **Requirements (`{{requirements: ...}}`).** Nest to mirror the
  capability hierarchy: owning component (if more than one) → root
  capability → sub-capability → the requirement(s) under their realising
  **leaf** capability. Umbrella-owned branches go under the same
  *System-wide / cross-cutting* heading. Requirement text, priority,
  rationale, and acceptance criteria render verbatim. This applies equally
  to functional, non-functional, and constraint requirement types.
  Render each leaf requirement so the **title and `(TYPE, PRIORITY)` are on
  the first bullet line, and the requirement description on its own
  following line** (an indented continuation paragraph under the bullet —
  not on the same line as the title, no em-dash join). Rationale and
  acceptance criteria follow as sub-bullets, e.g.:

  ```markdown
  - *Validate package authenticity* (FUNCTIONAL, CRITICAL)
    The system shall verify the cryptographic signature of the acquired
    update package and reject the package if the signature is invalid.
    - Rationale: …
    - Acceptance criteria:
      - …
  ```
- **Activated capabilities (`{{processes}}`).** Render the capabilities a
  process `ACTIVATES` as a **table with columns Component | Capability**,
  one row per activated leaf capability, grouped by its owning internal
  component (umbrella-owned → *System-wide*). Do not render them as a bare
  bullet list.

## Generated-diagram conventions

When the generator computes a diagram from relationships:

- **Always double-quote every label.** Node labels, subgraph titles, and
  edge labels in any computed diagram must be wrapped in `"..."`, because
  component and interface titles routinely contain `(`, `)`, `/`, and `–`,
  which break Mermaid's parser when left unquoted (e.g. a parenthesised
  interface title in an unquoted `---|...|` edge label is a parse error).
  Use `id["Title"]` for boxes, `id("Title")` for rounded nodes,
  `subgraph ID["Title"]` for subgraphs, and `---|"Title"|` for edge
  labels. Never emit a bare unquoted title.
- **Context diagram** (`{{context-diagram}}`): the `INTERNAL` parts as
  plain boxes (`id["Title"]`) inside a `subgraph ID["System under design"]`
  (the subgraph label stands in for the system — the `SYSTEM` umbrella is
  **not** drawn as a node); one node per `EXTERNAL` component (rounded —
  `id("Title")`); one labelled edge per interface using the (quoted)
  interface title, e.g.
  `c_backend ---|"OTA network link (Back-end–Master Update ECU)"| c_master_update_ecu`.
- **Capability tree** (`{{capability-diagram}}`): `graph TD` with the
  parent at top; `SUB_CAPABILITY_OF` becomes a parent→child edge; node
  labels quoted per the rule above. Each root capability gets a side-note
  naming its **owning internal component**; roots owned by the `SYSTEM`
  umbrella are noted as `(System-wide)` rather than naming the umbrella.
  The implementation link is not an edge in this diagram (it would
  clutter).
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
