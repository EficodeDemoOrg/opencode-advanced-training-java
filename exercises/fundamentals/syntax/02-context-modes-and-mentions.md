# 02: Context, @ Mentions, and Modes (Build & Plan)

This section explains three pieces of opencode's core interaction model:

- **`@` mentions** — how you tell opencode exactly which files (or subagents) to look at, instead of hoping it finds the right thing on its own.
- **Build mode** — opencode's default primary agent, with full read/edit/run access.
- **Plan mode** — a restricted, analysis-only primary agent for reviewing an approach before anything is changed.

No exercises here — this is a reference for the concepts. Hands-on practice with all three lives in [hands-on/](../hands-on/).

## @ Mentions

opencode does not automatically hold your whole repository in its head. The `@` key is how you tell it exactly what to look at instead of hoping it finds the right file on its own.

- Type `@` followed by part of a filename (e.g. `@InventoryItem`) and opencode fuzzy-searches the project, showing matching files. Arrow keys + Enter (or Tab) select one and insert it as a file reference, e.g.:
    ```text
    @src/main/java/com/atlas/inventory/InventoryItem.java What does the quote()
    helper method do, and why does it exist?
    ```
- You can mention **more than one file** in the same prompt when you need opencode to compare or cross-reference them:
    ```text
    Compare @src/main/java/com/atlas/inventory/InventoryItem.java and
    @src/main/java/com/atlas/inventory/InventoryServer.java. Both files contain
    JSON string-escaping logic. Are they identical? If not, what's different?
    ```
- The same `@` key also addresses a **subagent** directly, instead of waiting for Build mode to decide whether to delegate. Typing `@` on its own shows built-in subagents (such as `explore`) alongside file suggestions:
    ```text
    @explore where in this codebase is a duplicate part number rejected, and with
    what HTTP status code?
    ```
  `explore` is read-only — it can search and read files but cannot edit them, which makes it safe to reach for whenever you want an answer without any risk of a side effect. You'll go deeper on subagents — including creating your own — in [intermediate/03-subagents.md](../intermediate/03-subagents.md).

opencode also ships a handful of built-in slash commands, separate from anything you configure yourself:

- `/undo` — reverts the agent's last change and brings your prompt back so you can try again.
- `/redo` — brings a reverted change back.
- `/share` — creates a link to the current conversation (read-only). Treat it like posting a chat transcript, because that's exactly what it is.
- `/help` — lists available commands.

## Build mode (Agent mode)

**Build** is opencode's default primary agent. It has full tool access: it can read, search, edit files, and run commands. This is where you implement complete, small changes.

Build mode is also useful for small, everyday refactors on code you point it at directly — reference the function or file with `@`, describe the change, and review the diff before approving each edit.

Check the mode indicator in the lower-right corner of the opencode TUI to confirm you're in Build; press `Tab` to switch modes.

## Plan mode

**Plan** is opencode's restricted, analysis-only primary agent. It sets `edit` and `bash` permissions to `ask` by default, so it can inspect and reason about the codebase but will stop and ask before it changes or runs anything.

Typical flow:

1. Press `Tab` to switch to **Plan**.
2. Describe the feature or change you want planned. Plan mode may ask clarifying questions — answer them.
3. Read the plan fully. Push back on anything unclear or wrong, and ask for a revision — Plan mode will not sneak in an edit while you're still evaluating the approach.
4. Once satisfied, either:
   - Press `Tab` to switch back to **Build** and hand the plan off for implementation, or
   - Ask opencode to save the plan to a file (e.g. `docs/some-plan.md`) instead of implementing it, so a bigger or riskier change can be reviewed by the rest of the team before anyone touches a line of code.

The judgment call of *when* to hand a plan straight to Build versus stopping at the plan and saving it for review is the real skill Plan mode teaches — not the mode switch itself.

## Where to practice this

Hands-on exercises covering context, mentions, Build mode, and Plan mode together are in [hands-on/](../hands-on/).
