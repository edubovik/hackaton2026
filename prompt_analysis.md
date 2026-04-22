# Prompt History Analysis
**Files analyzed:** prompt_history_0 through prompt_history_12 (13 sessions)
**Project:** hackaton2026 — Full-stack Spring Boot + React + WebSocket + RabbitMQ

---

## What Was Good

### 1. Phased, plan-first discipline (all phase sessions)
Every phase session follows: present task → "approve" → "commit and push". This directly matches the CLAUDE.md rule and prevents surprise code changes. Consistent across phases 1–7.

### 2. Referencing specs by section number (prompt_history_10, prompt_history_12)
> *"browser tab session isolation per REQUIREMENTS.md §2.2.3 and §2.2.4"*
> *"Pay special attention to section 3.1 (Capacity and Scale) and 3.2 (Performance)"*

Anchoring to section numbers eliminates ambiguity about scope and makes it easy to verify the work against requirements.

### 3. Pre-reading documentation before starting (prompt_history_11, prompt_history_12)
Explicitly listing files to read upfront sets the right context before any code is touched. Prompt_history_11 lists four files; prompt_history_12 opens with it as a hard constraint.

### 4. Bug reports with concrete data (prompt_history_11)
- Providing the exact error JSON (`{"error":"Internal server error"}`)
- Supplying test credentials when asked: *"test1 and test2 users have the same password of Password01"*
- Reproducing the exact sequence: actor → action → observed vs. expected

### 5. Good multi-tab bug scenario (prompt_history_11 [12])
> *"test1 user has opened tab1 and tab2. He switches to tab2 and posts the message. Message appears in tab1 and doesn't appear in tab2. Message appears in tab2 after page reload."*

Clear actor, action sequence, and expected vs. actual outcome — the model needs nothing else to start diagnosing.

### 6. Including platform error output verbatim (prompt_history_9 [1])
Pasting the full Docker error message:
> *"! mailhog The requested image's platform (linux/amd64) does not match the detected host platform (linux/arm64/v8)"*

This is far better than paraphrasing — no information is lost in translation.

### 7. Standout prompt: load test design (prompt_history_12 [PROMPT 0])
This is the best prompt in the entire set. It demonstrates nearly every best practice at once:
- Structured into numbered steps with explicit gate conditions (*"Wait for my approval before proceeding"* after each step)
- Hard numerical targets derived from requirements (300 users, 3s message delivery, 2s presence update)
- Defines exact load profile (ramp up / sustained / ramp down durations)
- Specifies folder structure and file naming in advance
- Lists exact metrics to report with target values
- Ends with a smoke-test-first rule and a requirements verdict matrix (✅/❌)
- Includes an explicit RULES section covering what not to do

---

## What Can Be Improved

### 1. Vague bug openers (prompt_history_8 [1])
> *"still nothing."*

Zero context. What was tried? What changed since the last attempt? What is "nothing"? Should be:
> *"After [last fix], X still doesn't work. When I do [action] I see [symptom]. Expected: [outcome]."*

### 2. Phase kickoff prompts are thin (prompt_history_0 through 7)
Every phase starts with just file references and a phase number. There is no statement of priority, constraints, or what "done" looks like. Even one extra sentence helps:
> *"Implement Phase 3 (messaging). Priority: persistence to DB. No auth changes needed yet."*

### 3. Single-word approvals carry no steering
> *"yes" / "approve" / "approve"*

Approvals are wasted opportunities to add constraints or flag concerns. Even a short qualifier raises quality:
> *"approve — keep it minimal, no extra abstractions"*
> *"approve — but make sure tests cover the error case too"*

### 4. Bug reports without reproduction steps (prompt_history_10)
- *"I have friend request already sent error"* — from which user? After what sequence of actions?
- *"I am getting internal server error"* — endpoint? HTTP method? Request payload?
- *"I am still cannot have different sessions of the same user in 2 tabs"* (prompt_history_9) — what had already been tried? What changed?

The minimum useful bug report: **action → symptom → expected outcome**.

### 5. Interrupted prompts leave dead ends (prompt_history_0, prompt_history_2)
Two prompts are marked `[interrupted]` with no follow-up clarification. After cancelling a prompt, the next message should briefly re-state intent so the model isn't picking up a partial thread.

### 6. Repetitive file-management noise (all sessions)
Every session ends with 3–5 prompts about exporting and renaming history files. This is housekeeping, not engineering. It should be a scripted closing step, not interactive prompting.

### 7. Missing browser/OS context on UI bugs (prompt_history_10, prompt_history_9)
Tab isolation and message visibility bugs are often platform-specific. Adding *"Chrome 124, macOS 14, Apple Silicon"* would immediately narrow whether this is a browser storage issue, a WebSocket reconnection issue, or an ARM64-specific problem.

### 8. "continue" without context (prompt_history_9, prompt_history_3)
Three consecutive *"continue"* prompts in prompt_history_9 give no signal about what to continue, or whether the direction should change. If the model is on track, a brief confirmation (*"looks good, continue"*) is more useful. If something was off, this is the moment to say so.

### 9. Duplicate export attempts (prompt_history_12)
Prompts 2–5 in prompt_history_12 are identical re-attempts of the same export command, likely because the previous attempt failed silently. Adding a note about what went wrong would help the model understand the failure mode rather than repeating the same action.

---

## Scoring by Session

| File | Phase / Topic | Prompt Quality | Notes |
|---|---|---|---|
| prompt_history_0 / 1 | Phase 1 | ⭐⭐ | Thin kickoff, standard approval loop |
| prompt_history_2 | Phase 2 | ⭐⭐ | Thin kickoff, includes an interrupted prompt |
| prompt_history_3 | Phase 3 | ⭐⭐ | Thin kickoff, standard loop |
| prompt_history_4 | Phase 4 | ⭐⭐ | Thin kickoff, standard loop |
| prompt_history_5 | Phase 5 | ⭐ | Only rename + export prompts |
| prompt_history_6 | Phase 6 | ⭐⭐ | Thin kickoff, standard loop |
| prompt_history_7 | Phase 7 | ⭐⭐ | Thin kickoff, standard loop |
| prompt_history_8 | Bug fix | ⭐ | "still nothing" — worst single prompt in set |
| prompt_history_9 | Bug fix + Docker | ⭐⭐⭐ | Good verbatim error; weak on follow-through context |
| prompt_history_10 | Bug fix session | ⭐⭐⭐ | Good spec references; some vague bug reports |
| prompt_history_11 | Tests + bug fix | ⭐⭐⭐⭐ | Strong: doc-first, scenario-driven bugs, credentials |
| prompt_history_12 | Load test | ⭐⭐⭐⭐⭐ | Best in set — structured, gated, metric-driven |

---

## Top 3 Actionable Changes

1. **For every bug report:** always include action → symptom → expected outcome, plus browser/OS when it's a UI issue.
2. **For every phase kickoff:** add one sentence of priority/constraint instead of just file references.
3. **For every approval:** add a one-phrase qualifier — either a constraint to carry forward or confirmation that the approach looks right.

The load test prompt (prompt_history_12) is the gold standard for this project. Using that same step-gated, metric-anchored, rules-included structure for complex feature requests would significantly reduce back-and-forth in future sessions.
