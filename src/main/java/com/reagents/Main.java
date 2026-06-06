package com.reagents;

/**
 * Placeholder entry point for the RE-Agents PoC.
 * <p>
 * At PoC stage, all agent work happens via Claude Code subagents
 * (see {@code .claude/agents/}). The Java side is intentionally thin —
 * it will grow when the project migrates to the Anthropic Java SDK to
 * lift the persistence and runtime limitations of subagents.
 * <p>
 * Likely future responsibilities of this Java module:
 * <ul>
 *   <li>Loading and validating the requirements model from {@code model/}</li>
 *   <li>Programmatic SSS generation (independent of an interactive agent session)</li>
 *   <li>Hosting the runtime agent orchestrator once we move to the SDK</li>
 * </ul>
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("RE-Agents PoC — Java side is a placeholder.");
        System.out.println("Use Claude Code and the subagents in .claude/agents/.");
    }
}
