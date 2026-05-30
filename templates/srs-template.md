# {{system.title}} — System Requirements Specification

{{system.description}}

## 1. System Context

This section describes the system boundary, the external entities, and
the interfaces between them.

### 1.1 External entities

{{components: component_type=ACTOR|EXTERNAL_SYSTEM}}

### 1.2 Interfaces

{{interfaces}}

### 1.3 Context diagram

{{context-diagram}}

## 2. Capabilities

The system provides the following capabilities, organised hierarchically.
Top-level (root) capabilities are owned by the system; sub-capabilities
decompose them. Only leaf capabilities carry requirements.

### 2.1 Capability hierarchy

{{capability-tree}}

### 2.2 Capability diagram

{{capability-diagram}}

## 3. Requirements

### 3.1 Functional requirements

{{requirements: requirement_type=FUNCTIONAL}}

### 3.2 Non-functional requirements

{{requirements: requirement_type=NON_FUNCTIONAL}} {{omit-if-empty}}

### 3.3 Constraints

{{requirements: requirement_type=CONSTRAINT}} {{omit-if-empty}}

## 4. Processes

Each process below describes a system-level interaction. Participants
are the system and external entities only (no internal modules).

{{processes}}

## 5. Traceability

The matrix maps capabilities to the requirements that realise them.

{{traceability-matrix}}

---

<!--
Placeholders are resolved by the srs-generator agent. See
`.claude/skills/srs-template/SKILL.md` for the full syntax reference
and section-to-entity mapping.
-->
