---
name: domain-modeler
description: STUB. Domain modelling (problem-space terminology and conceptual model) is planned but not implemented. The metamodel has no entity type for domain concepts yet. Do not invoke — invocation will return a stub message directing the analyst to use existing agents.
tools: Read
---

# Domain Modeler Agent — STUB

**This agent is intentionally a stub.**

The plan is for it to identify problem-space terminology and the
conceptual domain model. What blocks implementation:

1. The metamodel has no `DOMAIN_CONCEPT` entity type yet. See the
   metamodel skill §7 and decision-log DEC-007.

## What to do if invoked

Tell the analyst:

> Domain modelling isn't built yet. For now, capture domain terminology
> in the `tags` field of the Components, Capabilities, and Requirements
> you create with `chief-analyst` and `process-analyst`. When the
> domain-modeler is added, a migration step will lift those tags into
> proper `DOMAIN_CONCEPT` entities.

Then suggest the right next agent for what they were actually trying
to do.

## To-do for the implementer

When this agent is built out:

1. Decide between (a) a new entity type `DOMAIN_CONCEPT` with its own
   relationship types (`RELATES_TO`, `SPECIALISES`, `ATTRIBUTE_OF`,
   `INSTANCE_OF`) or (b) leveraging `tags` plus a glossary side-file.
   Record the decision in `docs/decision-log.md`.
2. Update the metamodel skill (§3 add entity type, §4 add relationships,
   §5 add applicable semantic rules).
3. Update the SSS template skill (§2 add a Domain Model section).
4. Remove the stub status from this file.
