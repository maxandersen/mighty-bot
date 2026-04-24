# Java Coding Agent — Mighty (Almighty) Design Document

## Overview

This document defines a Java-based coding agent inspired by:

- **pi.dev extensions** (rich in-process plugin model)
- **Claude Code hooks** (narrow, out-of-process automation gates)

Design goal: keep the core minimal, and push most behavior into extensions and hooks.

## Prior Art Summary

### pi.dev (`pi-coding-agent`)

- Layered TypeScript architecture (`pi-ai`, `pi-agent-core`, `pi-coding-agent`, `pi-tui`)
- In-process extensions with broad control via an `ExtensionAPI`
- Extension capabilities include lifecycle subscriptions, tool/command registration, prompt modification, TUI widgets, compaction control, and persistent state
- Extension discovery from global and project-local directories
- Zero-build extension execution (`jiti`)
- Session storage in JSONL trees (`id` + `parentId`)
- Core ships with only four tools: `read`, `write`, `edit`, `bash`

### Claude Code Hooks

- Out-of-process hooks only (shell/HTTP/LLM/sub-agent style)
- Structured JSON context and exit-code-based control
- Lifecycle hook points such as `SessionStart`, `PreToolUse`, `PostToolUse`, `Stop`, etc.
- Matcher-based routing for tool-specific hooks
- Strong fit for security gates and automation
- Deliberately narrow surface area: no tool registration, UI rendering, or prompt rewriting

### Design Positioning

- **Extensions**: rich, in-process Java plugin system for deep integration
- **Hooks**: language-agnostic, out-of-process automation and policy gates
- The two systems are complementary, not competing.

---

## Layered Architecture

```text
┌─────────────────────────────────────────┐
│          Host Application               │
│  (CLI, IDE plugin, Slack bot, etc.)    │
├────────────────────┬────────────────────┤
│   mighty-agent     │   mighty-tui       │
│   Sessions, tools, │   Terminal UI      │
│   extensions       │                    │
├────────────────────┴────────────────────┤
│           mighty-core                   │
│   Agent loop, tool execution, events    │
├─────────────────────────────────────────┤
│            mighty-ai                    │
│   Multi-provider LLM abstraction        │
└─────────────────────────────────────────┘
```

## Module Responsibilities

### `mighty-ai`

Unified multi-provider LLM abstraction:

- Model-agnostic message types
- Streaming output support
- Tool-calling support
- Provider adapters (Anthropic/OpenAI/Google/Ollama, etc.)

Potential implementation strategies:

- **LangChain4j with Quarkus** for broad provider support and mature ecosystem
- **Thin in-house client** (Java `HttpClient`, virtual threads) for minimal footprint

Illustrative API:

```java
record Message(Role role, List<Content> content) {}
record ToolCall(String id, String name, String argumentsJson) {}
record StreamChunk(String text, ToolCall toolCall) {}

interface LlmClient {
    Flow.Publisher<StreamChunk> stream(
        List<Message> messages,
        List<ToolSchema> tools,
        ModelConfig config
    );
}
```

### `mighty-core`

Contains the loop: prompt → LLM → tool calls → tool results → repeat.

```java
public final class AgentLoop {
    /**
     * Runs one agent interaction loop while emitting lifecycle events.
     */
    public Multi<AgentEvent> run(String prompt, AgentSession session) {
        // implementation
    }
}
```

Core constraints:

- No product-specific UX logic
- Deterministic tool execution pipeline
- First-class event stream for extensions/hooks

### `mighty-agent`

Higher-level runtime:

- Session persistence and branching
- Extension loading and reloading
- Hook runner orchestration
- Command routing
- CLI entry point

### `mighty-tui`

Optional terminal UI package:

- Renders conversation, tool activity, and status
- Supports extension-provided widgets and shortcuts

---

## Lifecycle Events (Extension Bus)

Events mirror pi.dev’s extensibility model and are emitted by `mighty-core`.

| Event                   | Trigger timing                              | Extension control                                  |
|-------------------------|---------------------------------------------|----------------------------------------------------|
| `SessionStartEvent`     | Session created/resumed                     | Inject startup context                             |
| `BeforeAgentStartEvent` | After user prompt, before loop starts       | Rewrite system prompt, inject messages             |
| `BeforeLlmCallEvent`    | Before each provider call                   | Filter/rewrite messages                            |
| `TurnStartEvent`        | Beginning of each response cycle            | Read-only                                          |
| `ToolCallEvent`         | LLM requested tool invocation               | Block call, mutate arguments, add metadata         |
| `ToolResultEvent`       | Tool execution completed                    | Augment output metadata                            |
| `MessageUpdateEvent`    | Streaming token/chunk emitted               | Read-only                                          |
| `BeforeCompactionEvent` | Before context summarization                | Cancel/replace compaction strategy                 |
| `TurnEndEvent`          | End of cycle                                | Read-only                                          |
| `SessionEndEvent`       | Session shutdown/close                      | Cleanup and state flush                            |

---

## In-Process Extensions (Java + JBang)

### Discovery

Auto-discover extension source files from:

- `~/.mighty/extensions/` (global)
- `.mighty/extensions/` (project local)
- explicit CLI args: `mighty -e ./my-extension.java`

Each extension is a **JBang-compatible single-file Java source** (`//DEPS` supported).

### Extension API

```java
//DEPS io.mighty:mighty-api:1.0
import io.mighty.api.*;

public class PermissionGate implements MightyExtension {
    @Override
    public void init(MightyAPI mighty) {
        mighty.on(ToolCallEvent.class, (event, ctx) -> {
            if (event.toolName().equals("bash")
                    && event.input().contains("rm -rf")) {
                return EventResult.block("Dangerous command blocked");
            }
            return EventResult.proceed();
        });

        mighty.registerTool(Tool.builder()
            .name("deploy")
            .description("Deploy to an environment")
            .parameter("env", Schema.string("Target environment"))
            .parameter("version", Schema.string("Version to deploy"))
            .execute((params, ctx) -> {
                String env = params.getString("env");
                return ToolResult.text("Deployed to " + env);
            })
            .build());

        mighty.registerCommand("/stats", "Show session statistics", (args, ctx) -> {
            var usage = ctx.getContextUsage();
            ctx.ui().notify("Tokens used: " + usage.tokens());
        });
    }
}
```

### Supported Extension Capabilities

- Subscribe to lifecycle events (observe and mutate behavior)
- Register tools callable by the model
- Register slash commands and optional keybindings
- Modify message lists before LLM calls
- Append/replace system prompts
- Customize compaction/summarization strategy
- Persist extension-scoped session state
- Render custom widgets in `mighty-tui`

### Hot Reload

On extension source change:

1. Recompile via JBang
2. Load in a fresh classloader
3. Re-initialize extension graph
4. Emit `SessionEndEvent` then `SessionStartEvent`

---

## Out-of-Process Hooks (Polyglot)

Hooks are designed for automation, policy, and integration outside the JVM process.

### Configuration (`.mighty/settings.json`)

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "bash",
        "hooks": [
          {
            "type": "command",
            "command": "python3 security-check.py"
          }
        ]
      }
    ],
    "PostToolUse": [
      {
        "matcher": "write|edit",
        "hooks": [
          {
            "type": "command",
            "command": "prettier --write \"$MIGHTY_TOOL_INPUT_FILE_PATH\""
          }
        ]
      }
    ]
  }
}
```

### Hook Protocol

Agent sends JSON context to hook stdin:

```json
{
  "event": "PreToolUse",
  "tool_name": "bash",
  "tool_input": { "command": "rm -rf /tmp/build" },
  "session_id": "abc123"
}
```

Exit code contract:

- `0`: allow (optional JSON output on stdout with additional context)
- `1`: non-blocking error (warn and continue)
- `2`: block action (stderr returned as rejection reason)

### Hook Scope vs Extension Scope

| Concern                | Hook (out-of-process) | Extension (in-process) |
|------------------------|------------------------|-------------------------|
| Security gate          | Yes                    | Optional                |
| Auto-formatting        | Yes                    | Optional                |
| Notifications          | Yes                    | Optional                |
| Register tools         | No                     | Yes                     |
| Rewrite prompt/messages| No                     | Yes                     |
| Custom TUI rendering   | No                     | Yes                     |
| Compaction strategy    | No                     | Yes                     |
| Any language runtime   | Yes                    | No (Java API)           |

---

## Sessions and Persistence

### Storage Format

Sessions are newline-delimited JSON objects (`.jsonl`) in `.mighty/sessions/`.

Each entry includes:

- `id`
- `parentId`
- `type`
- payload fields by type

Example:

```jsonl
{"id":"1","parentId":null,"type":"user","text":"Fix login bug"}
{"id":"2","parentId":"1","type":"assistant","text":"Checking auth flow..."}
{"id":"3","parentId":"2","type":"toolCall","tool":"read","input":{"path":"src/Auth.java"}}
{"id":"4","parentId":"3","type":"toolResult","tool":"read","content":"..."}
{"id":"5","parentId":"4","type":"assistant","text":"Found it."}
```

This structure enables branching by pointing a new node to any historical `parentId`.

### Compaction

When context pressure is high:

- summarize older content automatically
- preserve critical artifacts (e.g., file edits/tool outputs) by policy
- allow extension override for domain-specific retention strategies

---

## Built-In Tools

The base distribution intentionally ships with four tools only:

| Tool    | Purpose                       |
|---------|-------------------------------|
| `read`  | Read file contents            |
| `write` | Create/overwrite files        |
| `edit`  | Apply targeted edits to files |
| `bash`  | Execute shell commands        |

Additional tools are expected to come from extensions.

---

## Package and Distribution Model

Packages can bundle extensions, prompts, themes, and hook defaults.

### Distribution channels

- JBang catalogs
- Maven coordinates
- Git URLs

### Example package layout

```text
my-mighty-package/
├── extensions/
│   └── review.java
├── prompts/
│   └── code-review.md
├── hooks/
│   └── settings.json
└── package.json
```

### Example package commands

```bash
mighty install git:github.com/user/mighty-code-review
mighty list
mighty update
```

---

## Project Instruction Files

- `AGENTS.md`: layered instructions loaded from global + parent dirs + CWD
- `SYSTEM.md`: optional project-level system prompt override/append

---

## Skills (On-Demand Capability Packs)

Skill bundles are loaded only when needed to reduce initial prompt size.

```text
.mighty/skills/
├── kubernetes/
│   ├── SKILL.md
│   └── kubectl-tool.java
├── database/
│   ├── SKILL.md
│   └── sql-tool.java
```

Design intent:

- Progressive disclosure of capability instructions
- Better prompt-cache hit rates
- Optional skill-local tools

---

## Java Rationale

- GraalVM native image potential for fast startup
- Virtual threads fit concurrent streaming/tool workloads
- JBang enables single-file extension UX without dedicated builds
- Quarkus ecosystem supports hot reload and provider integration
- Strong typing for tool schemas and event contracts
- Mature Java tooling and diagnostics

---

## Implementation Roadmap

### Phase 1 — Minimal Agent

1. JBang CLI entry point (`mighty.java`)
2. Single provider (Anthropic) with streaming
3. Four core tools (`read`, `write`, `edit`, `bash`)
4. JSONL session persistence (flat)
5. `AGENTS.md` loading

### Phase 2 — Extension System

1. Extension discovery (global + project local)
2. JBang compile/load and classloader isolation
3. Lifecycle event bus
4. Tool registration API
5. Command registration API

### Phase 3 — Hooks and Runtime Polish

1. Out-of-process command hooks
2. Extension hot reload
3. Session branching support
4. Compaction override hooks for extensions
5. Multi-provider LLM support

### Phase 4 — Ecosystem

1. Package manager (`install`, `list`, `update`)
2. Skills system
3. TUI module
4. HTTP hooks
5. Prompt template catalog

---

## Non-Goals (Initial)

- Built-in large tool catalog in core
- Hardcoded provider-specific assumptions in `mighty-core`
- Mandatory TUI dependency for all host applications

Core principle: ship a tiny stable nucleus and evolve behavior through extensions.
