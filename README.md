# RE-Agents PoC

A proof-of-concept for using AI agents to facilitate **software requirements
engineering**. Built on Claude Code subagents.

## What it does

Given a software system to specify, the tool guides a human analyst through
four activities, building up a structured **requirements model** and finally
emitting a Markdown + Mermaid **System/Subsystem Specification**.

### Four activities

1. **Identify system context** — what's inside the system, what's outside,
   what interactions cross the boundary
2. **Identify terminology and domain model** — the conceptual problem-space
   vocabulary and how concepts relate
3. **Identify system capabilities** — what the system must be able to do
4. **Analyse requirements for a process** — detailed requirements for
   specific processes the system supports

### Four interaction modes

All activities support these modes, mixed freely:

1. User provides input
2. Agent asks clarification questions
3. Agent offers options for user to select
4. User makes suggestions

## How to use

Open this project in VS Code with the Claude Code extension installed.
The subagents in `.claude/agents/` are automatically discovered.

Start a Claude Code session and try things like:

- *"Use the context-identifier to help me define the system boundary for [your system]"*
- *"Use the domain-modeler — here's a description: [paste]"*
- *"Generate the SSS"*

## Project layout

See `CLAUDE.md` for the full layout and design principles.

## Status

PoC scaffold. The meta-model and SSS template are placeholders awaiting
detailed specification.

## Why subagents (not the SDK)?

We chose Claude Code subagents to get to a working PoC quickly and validate
the *methodology* before investing in production infrastructure. When
persistence, multi-user access, or runtime tool integration become limiting,
we'll migrate to the Anthropic Java SDK — at which point the Java side of
this repo (currently a stub) will grow.
