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
- **Lifecycle:** Always branch from the latest `main`. Once a Pull Request is approved and merged, delete the branch immediately.

---

## 2. Commit Guidelines
- Write concise, imperative commit messages describing the change (e.g., `Add validation for announcement form`).
- Avoid vague messages like `update`, `fix`, or `WIP`.

---

## 3. Pull Requests (PR) Protocol
- **Sync before PR:** Pull the latest changes from `main` into your local branch before opening a PR. Verify that the build and tests pass locally.
- **PR Description:** Clearly describe what was changed, why, and how to verify it.
- **Review Requirements:**
  - Minimum of 1 peer approval required.
  - Automated CI test suites must pass.
  - Minimum 70% code coverage must be maintained or improved.
- Never merge unapproved PRs.

---

## 4. Code Quality & Clean Code Standards
- **Language:** All identifiers, comments, documentation, and commit messages must be strictly in English.
- **Naming:** Use clear, self-descriptive names (e.g., `announcementList`, `findPetById`). Avoid cryptic abbreviations.
- **Cleanliness:**
  - Remove dead, commented-out code before opening a PR.
  - Write comments only when explaining complex domain logic, not self-evident code.
  - Remove all debugging artifacts before committing (no leftover `System.out.println()`, `console.log()`, or debug dumps).
  - **Zero Credentials Policy:** Never hardcode passwords, API tokens, database credentials, or secret keys. Use environment variables and configuration files (`.env`, `application.yml` profiles excluded via `.gitignore`).
- **Formatting:** Format all modified files before committing:
  - **IntelliJ IDEA (Backend):** `Ctrl + Alt + L` (Windows/Linux) or `Cmd + Option + L` (macOS).
  - **VS Code (Frontend):** `Shift + Alt + F` (Windows/Linux) or `Shift + Option + F` (macOS).

---

## 5. Trello Board Management
- Move your card to **In Progress** as soon as you start working on it.
- Move your card to **Done** only after the code is tested, peer-reviewed, approved, and merged into `main`.

---

## 6. Documentation Sync
Whenever you finish an important feature, module, or schema change, send Tudor a 2–3 line summary of what was completed so the project documentation (`docs/`) stays continuously aligned with the codebase.
