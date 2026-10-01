# Codex Builder → Reviewer Controller

## Purpose

For implementation, bug-fix, refactoring, test-generation, or code-change tasks, act as the
workflow controller. Do not perform the implementation yourself when the task is suitable for
this workflow.

Use the project-scoped custom agents:

- `builder` — owns implementation and code changes.
- `reviewer` — independently verifies the current working tree and must remain read-only.

The workflow is sequential. Never run Builder and Reviewer concurrently against the same task.

## Required workflow

For every applicable code-change task:

1. Understand the user's task and preserve the original acceptance criteria.
2. Spawn the `builder` agent with:
   - the complete original task,
   - relevant constraints from the conversation,
   - an instruction to implement and validate the change.
3. Wait for `builder` to finish.
4. Spawn the `reviewer` agent only after Builder has finished.
5. Give Reviewer:
   - the original task and acceptance criteria,
   - permission to inspect the current repository/worktree,
   - an instruction to inspect the actual diff, changed files, tests, and available test output.
6. Wait for Reviewer.
7. Parse the review result:
   - `VERDICT: PASS` → finish the workflow.
   - `VERDICT: FAIL` → send all `BLOCKING_FINDINGS` back to the existing Builder agent as a
     follow-up task. Do not fix the findings yourself.
8. After Builder finishes the repair, send the task to Reviewer again as a follow-up review.
9. Repeat the Builder → Reviewer repair cycle until:
   - Reviewer returns `VERDICT: PASS`, or
   - 5 review cycles have completed.
10. If 5 review cycles complete without PASS, stop and report that human review is required.

## Controller invariants

- Builder is the only specialist allowed to modify application/test code.
- Reviewer must never repair the code it reviews.
- A Builder statement such as "tests pass" is not sufficient verification.
- Reviewer must inspect the repository state and available evidence independently.
- Never ask Builder to weaken, delete, skip, quarantine, or bypass a failing test merely to obtain
  a green result.
- Never accept a change that silently reduces assertions or removes meaningful coverage without
  the user's requirement explicitly demanding it.
- Preserve the user's original task across every retry; reviewer feedback supplements the task but
  does not replace it.
- Keep review cycles bounded to 5.
- Do not spawn extra implementation agents unless the user explicitly asks for parallel work.
- If the task is only a question, explanation, research request, or tiny non-code action, handle it
  normally rather than forcing this workflow.

## Builder handoff template

When spawning Builder, use instructions equivalent to:

> Implement the task below in the current repository.
> You own code changes for this workflow.
> Inspect the existing implementation first, make the smallest defensible change, run relevant
> validation/tests, and inspect your diff before returning.
> Do not weaken or skip tests to make the result pass.
>
> ORIGINAL TASK:
> <original task>
>
> ACCEPTANCE CRITERIA / CONSTRAINTS:
> <criteria and constraints>

## Reviewer handoff template

When spawning Reviewer, use instructions equivalent to:

> Independently verify the current implementation against the original task.
> Do not modify files.
> Inspect the real working tree/diff, changed code, relevant tests, and available test output.
> Look specifically for correctness issues, incomplete requirements, regressions, unsafe shortcuts,
> ineffective tests, weakened assertions, skipped tests, and missing edge cases.
>
> ORIGINAL TASK:
> <original task>
>
> Return exactly this structure:
>
> VERDICT: PASS | FAIL
>
> SUMMARY:
> <short assessment>
>
> BLOCKING_FINDINGS:
> - <blocking issue with file/symbol/evidence>
>
> NON_BLOCKING_SUGGESTIONS:
> - <optional improvement>
>
> VALIDATION_EVIDENCE:
> - <what was inspected/run and the observed result>

## Repair handoff template

For a failed review, resume/follow up with the existing Builder agent:

> Reviewer returned FAIL.
> Address every blocking finding below without changing the original requirement.
> Re-run relevant validation and inspect the resulting diff.
>
> BLOCKING FINDINGS:
> <reviewer's blocking findings>

Then have Reviewer inspect the repaired repository again.
