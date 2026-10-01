# Start Prompt

Open the repository in Codex and start a normal project chat/thread.

Paste a task such as:

```text
Use the Builder → Reviewer workflow defined by this repository.

Implement contract coverage provenance by carrying the current OpenAPI SHA-256 fingerprint into
coverage evidence. Preserve existing behavior, add appropriate tests, and stop only when the
independent Reviewer returns PASS or the controller reaches the 5-review limit.
```

Because `AGENTS.md` defines the controller behavior, you can also give Codex a normal implementation
request, for example:

```text
Add input validation to the create-user endpoint and cover the edge cases with tests.
```

For applicable code-change tasks, the main Codex thread should:
1. spawn `builder`,
2. wait for it,
3. spawn `reviewer`,
4. send blocking findings back to Builder,
5. re-run Reviewer,
6. repeat up to 5 review cycles.

## Inspecting agents in Codex

The Codex app surfaces spawned subagent activity. In the CLI, `/agent` can be used to inspect and
switch among agent threads in versions that support current subagent workflows.
