# {{system.title}} — System/Subsystem Specification

## 1. Introduction

This document is the System/Subsystem Specification (SSS) for
{{system.title}}. It is generated from the requirements model and
describes the system under design, its context, its capabilities, the
requirements they realise, and the processes that exercise them.

{{system.description}}

## 2. System Description

### 2.1 System Context

This section describes the system boundary, the components that make up
the system under design, the external entities, and the interfaces
between them.

#### 2.1.1 System components (internal)

The system under design comprises the following internal components (the
umbrella plus its parts). Each may own capabilities (see §2.2).

{{components: boundary=INTERNAL}}

#### 2.1.2 External entities

{{components: boundary=EXTERNAL}}

#### 2.1.3 Interfaces

{{interfaces}}

#### 2.1.4 Context diagram

{{context-diagram}}

### 2.2 Capability hierarchy

The system provides the following capabilities, organised hierarchically.
Top-level (root) capabilities are owned by the internal components of the
system under design (the capability diagram notes each root's owning
component); sub-capabilities decompose them. Only leaf capabilities carry
requirements.

{{capability-tree}}

### 2.3 Capability diagram

{{capability-diagram}}

## 3. Requirements

### 3.1 Functional requirements

{{requirements: requirement_type=FUNCTIONAL}}

### 3.2 Non-functional requirements

{{requirements: requirement_type=NON_FUNCTIONAL}} {{omit-if-empty}}

### 3.3 Constraints

{{requirements: requirement_type=CONSTRAINT}} {{omit-if-empty}}

## 4. Processes

Each process below describes a system-level interaction. Participants are
the internal components of the system under design whose capabilities the
process exercises, plus the external entities they interact with
(derived per metamodel §4a).

{{processes}}

## 5. Traceability

The matrix maps capabilities to the requirements that realise them.

{{traceability-matrix}}

---

<!--
Placeholders are resolved by the sss-generator agent. See
`.claude/skills/sss-template/SKILL.md` for the full syntax reference
and section-to-entity mapping.
-->
