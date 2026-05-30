---
name: interaction-modes
description: The four interaction modes used by every RE activity agent. Consult this skill whenever an activity agent is conducting a session with the analyst, deciding how to advance the conversation, or handling analyst input. Each mode has a specific purpose and a specific way it updates the requirements model.
---

# Interaction Modes

All activity agents in this project (`context-identifier`, `domain-modeler`,
`capability-identifier`, `process-analyst`) share the same four modes of
interaction with the analyst. These modes are **orthogonal to the activity**:
any activity can use any mode, and modes can be mixed within a single session.

## The four modes

### Mode 1 — User provides input

The analyst supplies free-form information (description, document excerpt,
domain knowledge). The agent's job is to:

1. **Extract** candidate work-items and links from the input
2. **Show** what was extracted to the analyst before saving
3. **Save** confirmed items to the model
4. **Flag** ambiguities or gaps and offer to move to Mode 2 to resolve them

The agent does *not* silently invent details. If the input is ambiguous it
either asks (Mode 2) or offers interpretations (Mode 3).

### Mode 2 — Agent asks clarification questions

The agent drives a structured Q&A to fill specific gaps in the model.
Guidelines:

- Ask **one question at a time** unless questions are tightly related
- Make questions **specific** and **answerable** — never "tell me more about X"
- After ~3 questions, **summarise** what's been learned and confirm before
  continuing
- Each answer should map to a concrete model update; explain the mapping

### Mode 3 — Agent offers options

The agent proposes 2–5 concrete alternatives for the analyst to choose from.
Use this when:

- The analyst's input under-specifies a decision the model requires
- Multiple reasonable interpretations exist
- A standard pattern probably applies and you want to confirm which

Format: numbered list, each option a short label plus a one-line explanation.
Always include an "*other / none of these*" escape hatch.

### Mode 4 — User makes suggestions

The analyst proposes a change ("add a work-item X", "rename Y to Z",
"these two should be linked"). The agent's job is to:

1. **Evaluate** the suggestion against the metamodel and the existing model
2. **Identify side-effects** (links that become invalid, contradictions with
   other work-items, etc.)
3. **Confirm or push back** — if the suggestion is sound, apply it; if it
   creates problems, explain them and propose alternatives (often returning
   to Mode 3)

## Mode-switching

The analyst can switch modes mid-session. Common transitions:

- Mode 1 → Mode 2: input had gaps, agent needs to ask
- Mode 2 → Mode 3: a question has a standard set of answers, offer them
- Mode 3 → Mode 4: analyst didn't like the options, proposes their own
- Any mode → Mode 1: analyst pastes in new source material

Agents should **announce** mode switches: *"Switching to clarification mode —
I have three questions about the boundary."*

## Logging

Every model change must record:

- Which activity it belongs to (context / domain / capability / process)
- Which mode produced it (1 / 2 / 3 / 4)
- A one-line rationale

This metadata lives on the work-item or link itself (see the `metamodel`
skill for the exact field name).
