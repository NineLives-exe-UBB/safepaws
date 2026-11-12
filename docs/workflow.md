# Team Workflow & Engineering Guidelines

This document outlines the standard development process, coding conventions, and collaboration rules for **NineLives.exe** on the SafePaws project.

---

## 1. Branching Strategy
We follow a feature-branch workflow derived from `main`:
- **Naming convention:** `<type>/<short-description>` (lowercase, hyphen-separated).
  - `feature/` — New features (e.g., `feature/post-announcement`)
  - `fix/` — Bug fixes (e.g., `fix/photo-upload-size`)
  - `docs/` — Documentation updates (e.g., `docs/database-schema`)
  - `test/` — Adding or refactoring tests (e.g., `test/announcement-api`)
  - `chore/` — Maintenance, dependencies, tooling, or refactoring without functional changes (e.g., `chore/backend-cleanup`)
- **Lifecycle:** Always branch from the latest `main`. Once a Pull Request is approved and merged, delete the branch immediately: on GitHub with **Delete branch**, and locally with `git checkout main`, `git pull`, `git fetch --prune` and `git branch -d <branch>`.
- **Building on an open PR:** if your work needs a branch that is not merged yet, branch from it and open your PR with that branch as the base. Merge the PRs in order; GitHub then moves the base of the next PR to `main`.

---

## 2. Commit Guidelines
- Write concise, imperative commit messages describing the change (e.g., `Add validation for announcement form`).
- Avoid vague messages like `update`, `fix`, or `WIP`.

---

## 3. Pull Requests (PR) Protocol
- **Sync before PR:** Pull the latest changes from `main` into your local branch before opening a PR.
- **Run tests before opening PR:** Always run all test suites locally to ensure zero regressions:
  - Backend: `cd backend && ./mvnw verify` (Docker must be running, because the tests start PostgreSQL with Testcontainers)
  - Frontend: `cd frontend && npm run test:coverage`, `npm run build` (no warnings) and `npx prettier --check src`
- **PR Description:** Clearly describe what was changed, why, and how to verify it.
- **Review Requirements:**
  - Minimum of 1 peer approval required.
  - Automated CI test suites must pass.
  - Minimum 70% code coverage must be maintained or improved.
- Never merge unapproved PRs.
- **Merging:** use **Create a merge commit**, not **Squash and merge**, so that branches built on top of the PR do not get conflicts. If GitHub says the branch is out of date, press **Update branch** and wait for the checks first.

---

## 4. Code Quality & Clean Code Standards
- **Language:** All identifiers, comments, documentation, and commit messages must be strictly in English.
- **Naming:** Use clear, self-descriptive names (e.g., `announcementList`, `findPetById`). Avoid cryptic abbreviations.
- **Cleanliness:**
  - Remove dead, commented-out code before opening a PR.
  - Write comments only when explaining complex domain logic, not self-evident code.
  - Remove all debugging artifacts before committing (no leftover `System.out.println()`, `console.log()`, or debug dumps).
  - **No Build Artifacts in Git:** Never commit build artifacts, compiled binaries, or temporary directories (`target/`, `node_modules/`, `dist/`, `coverage/`, `.class`). Verify with `git status` that only intended source files are staged.
  - **Zero Credentials Policy:** Never hardcode passwords, API tokens, database credentials, or secret keys. Put them in `.env`, which is excluded via `.gitignore`, and read them in `application.properties` with `${NAME:default}`. Every new variable is also added to `.env.example`, with a safe local value (never a real password or your own IP address).
- **Formatting:** Format all modified files before committing:
  - **IntelliJ IDEA (Backend):** `Ctrl + Alt + L` (Windows/Linux) or `Cmd + Option + L` (macOS). Use single imports, never `import java.awt.*`.
  - **VS Code (Frontend):** Prettier, with the settings in `frontend/.prettierrc`. Set Prettier as the default formatter and turn on **Format On Save**, or run `npx prettier --write src` before committing.

---

## 5. Project Conventions

### Database
- **Migrations are never edited once merged.** A fix goes in a new migration. The next free version is in the last database decision (`V6` after DEMO 1).
- SQL and Java migrations share version numbers, and the CI guard only checks the SQL ones, so check `backend/src/main/java/db/migration/` too before choosing a number.

### Frontend
- **Colors, shadows and corners** come only from the CSS variables in `frontend/src/styles.css` (`var(--color-primary)`, `var(--radius-control)`...). A new color is first added there as a variable, named by its role.
- **Dates** are shown with `DATE_FORMAT` from `shared/date-format.ts` ("30 Oct 2026, 18:00").
- **Types and species** are shown with `TYPE_LABELS` and `SPECIES_LABELS` ("Lost", "Cat"), never the raw `LOST` or `CAT`.
- **Wording:** "lost pet" for LOST, "found animal" for FOUND, "animal" when a text is about both, "Area" for the area field, and a full stop at the end of every message.

---

## 6. Trello Board Management
- Move your card to **In Progress** as soon as you start working on it.
- Move your card to **Done** only after the code is tested, peer-reviewed, approved, and merged into `main`.

---

## 7. Documentation Sync
- Whenever you finish an important feature, module, or schema change, write your decisions and the messages for the team in your own `<name>-decisions.md`: what you decided, why, and what it changes for the others.
- Update the files in `docs/` that your change affects (`api.md`, `database.md`, `architecture.md`, `testing.md`, `usage-guide.md`) in the same PR, or in a `docs/` branch right after, so the documentation stays aligned with the codebase.