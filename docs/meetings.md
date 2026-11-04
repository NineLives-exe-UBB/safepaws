# Meeting Notes & Logs

This document tracks team syncs, sprint planning sessions, and mentor check-ins for **NineLives.exe**.

---

## Meeting Cadence & Schedule
- **Frequency:** Bi-weekly (every other week) on **Wednesdays**.
- **Focus:** Sprint progress review, demo readiness, blocker resolution, and next-sprint task breakdown.

---

## Meeting Log Template (For upcoming sessions)

### [YYYY-MM-DD] - [Meeting Type: Mentor Review / Team Sync / Sprint Planning]
- **Attendees:** @Name1, @Name2
- **Objective / Agenda:**
  - Topic 1
  - Topic 2
- **Key Discussion Points:**
  - Progress updates and current blockers.
- **Decisions Made:**
  - Decision 1
- **Action Items:**
  - [ ] Task 1 (@Assignee)
  - [ ] Task 2 (@Assignee)

---

## Meeting Records

### [2026-10-21] - Initial Sprint Planning & Project Kickoff
- **Meeting Type:** Team Planning Sync
- **Objective:** Establish repository setup, technical alignment, and deliverables leading to the next team review.

#### 1. Organization & Setup
- **Trello Board:** Board initialized with sprint tasks assigned to individual team members.
- **GitHub Repository:** Base project initialized with backend and frontend scaffolding; enforced Git workflow (feature branches, PR reviews, merge protocol).
- **IDE Standardization:** Confirmed development environments:
  - **IntelliJ IDEA** for Java / Spring Boot backend.
  - **Visual Studio Code** for Angular frontend.
- **Database Architecture:** Agreed on PostgreSQL structure (Users, Announcements, Photos, Locations) containerized via Docker.
- **Quality Gate:** Established a minimum automated test coverage threshold of **70%** across pull requests.

#### 2. Development Deliverables (First Half of DEMO 1)
- **Announcement Creation:** End-to-end flow for posting lost and found pet reports with detailed attributes and photo attachments.
- **Announcement Feed:** Simple, clean list view displaying active announcements (newest first).
- **Poster Generation:** Initial prototype of downloadable PDF poster derived from announcement data.

#### 3. Documentation & QA
- **Documentation Deliverables:** Maintain continuous updates for project overview, local run instructions, database schema migrations, and Architecture Decision Records (ADRs).
- **Testing & QA:** QA to construct test scenarios covering announcement creation, photo attachments, and feed display.

---

## Mentor Check-in Ledger

### [2026-10-21] - Initial Mentor Sync
- **Attendees:** NineLives.exe Team, Course Mentor
- **Objective:** Scope validation and architectural sign-off for Demo 1.
- **Mentor Feedback:**
  - Approved tech stack (Spring Boot 4.1, Angular, PostgreSQL 17).
  - Recommended focusing initial demo on core announcement and photo workflows.
- **Action Items:**
  - [x] Set up repository, guidelines, and ADRs (@Tudor)
  - [x] Configure PostgreSQL and Flyway migrations (@Matei)
  - [x] Implement initial announcement endpoints (@Antonia)
  - [x] Implement photo handling service (@Patrick)
  - [x] Implement poster PDF export prototype (@Alexandru)

---

### [2026-11-04] - Mentor Check-in & Sprint 1 Review
- **Attendees:** NineLives.exe Team, Course Mentor
- **Objective:** Review completed backend deliverables (announcements, photos, posters), verify automated test coverage (backend 63 tests, frontend 26 tests), and align project documentation.
- **Key Discussion Points:**
  - Backend endpoints fully implemented and verified via automated test suites (95.7% instruction coverage on backend, 97.7% on frontend).
  - Poster generation prototype completed using DejaVu Sans for Romanian diacritics; gracefully renders without image when none is attached.
  - Frontend list connected to live backend; announcement creation form actively being built.
  - Completed thorough documentation audit to eliminate outdated references (mock data, adoptions, Jasmine/Karma) and accurately align with current schema and error handling.
- **Decisions Made:**
  - Prioritize frontend announcement form completion and photo upload component integration for the upcoming Demo 1 milestone.
  - Keep documentation updated in lockstep with feature PRs.
- **Action Items:**
  - [ ] Complete frontend announcement creation form (@Victor)
  - [ ] Integrate photo upload component in frontend (@Luca)
  - [ ] Add end-user usage guide with screenshots once form is merged (@Tudor)
  - [ ] Execute manual test scenarios for announcement creation and edge cases (@Jessica)
