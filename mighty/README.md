# mighty

Minimal Java coding-agent harness built with Quarkus + LangChain4j (Anthropic-first).

This is Phase 1 scaffolding and currently includes:

- CLI entrypoint (`mighty`)
- basic agent loop (single-turn for now)
- built-in tools: `read`, `write`, `edit`, `bash`
- JSONL session persistence in `.mighty/sessions/`
- `AGENTS.md` loading from parent directories

## Prerequisites

- Java 21+
- Anthropic API key

Set your API key as an environment variable:

```bash
export QUARKUS_LANGCHAIN4J_ANTHROPIC_API_KEY=your-key
```

## Run

Development mode:

```bash
./mvnw compile quarkus:dev -Dquarkus.args='Summarize this repository --session demo'
```

Package and run:

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar "List available tools" --session demo
```

## Useful flags

- `--session <id>` resume/create a named session

Example:

```bash
java -jar target/quarkus-app/quarkus-run.jar "Inspect README and suggest edits" --session demo
```
