# ProjectHub – Smart Student Project Collaboration System

A collaborative project management platform for university teams, hackathons, capstones, and research labs—built with a native **Android (Java 17 + MVVM)** app, a **React + Vite Web Companion**, **Firebase** (Auth, Cloud Firestore, Cloud Storage, Cloud Messaging, Cloud Functions), and **GitHub** repository integration.

---

## Key Features

1. **Task & Board Management (`KanbanBoard`, `TaskDetailsActivity`)**
   - Assign tasks to real project members loaded from Firestore.
   - Full task creation, editing, deletion, due dates, priority/issue-type/search filters, and task dependencies/blockers (`dependsOnTaskId` / `dependsOnTaskKey`).
   - Interactive subtasks & checklists with real-time completion progress.
   - Custom workflow statuses (`TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`, or team-defined columns) with both **List** and multi-column **Board** views.
2. **Proactive Alerts & Background Push (`NotificationHelper`, `DeadlineReminderWorker`, `functions/`)**
   - In-app and FCM push alerts for task assignments, `@mentions`, comments, join requests, team invitations, workflow automations, and approaching 48h deadlines.
   - Deduplicated Android WorkManager periodic deadline reminders (`DeadlineReminderWorker`) + Firebase Cloud Functions (`sendPushOnNotificationCreate`, `scheduledDeadlineAlerts`).
3. **Controlled Files & Rich Team Chat**
   - Role-scoped resource sharing & Firebase Storage uploads (`MEMBERS_ONLY`, `LEAD_ONLY`, `PUBLIC`), inline Glide image previews, code snippets, and uploader/lead deletion.
   - Real-time group chat supporting quoted replies, `@mentions`, emoji reactions, and 1-click Jitsi Meet rooms.
4. **Calendar, Timeline & Workflow Automations**
   - Interactive Gantt progress timeline, Sprint/Milestone countdown tracker, and Calendar date schedule view.
   - Trigger-based workflow rules (`TASK_ASSIGNED`, `TASK_STATUS_CHANGED`, `ALL_SUBTASKS_COMPLETED`, `MILESTONE_COMPLETED` → notify assignee/lead, post chat update, update status/phase).
5. **GitHub Integration (`GitHubRepository`)**
   - Link any `owner/repo` (with optional personal access token), inspect Issues & Pull Requests side-by-side, and import them as ProjectHub tasks in one tap.
6. **Student & Faculty Campus Advantage**
   - Verified campus profiles (`.edu`, `.ac.*`, etc.), Student vs Faculty Mentor roles, skill tags, public project discovery, and Talent & Mentor directory with project invites.
   - Ready-made templates for **Capstone**, **SIH / Hackathon**, **Research Paper**, and **Blank** workspaces.
7. **Web Companion (`web/` & `docs/WEB_COMPANION_PLAN.md`)**
   - Desktop-first React + Vite SPA sharing the exact Firebase backend for multi-column planning, code snippets, GitHub triage, and calendar management.

---

## Repository Layout

- `app/` — Android application (Java 17, Material 3, MVVM, WorkManager, Firebase BoM 35.2.0)
- `functions/` — Firebase Cloud Functions v2 for background FCM push & scheduled deadline alerts
- `web/` — React + Vite Web Companion (`docs/WEB_COMPANION_PLAN.md`)
- `firestore.rules` & `storage.rules` — Hardened Firebase security rules
- `firebase.json` — Firebase Hosting, Functions, Firestore, and Storage configuration

---

## Build & Verify

### Android App & Unit Tests
```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME='C:\Users\Shridhar\AppData\Local\Android\Sdk'
.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon
```

### Web Companion
```powershell
cd web
copy .env.example .env   # Fill in your Firebase Web config
npm install
npm run build
```

### Firebase Rules & Functions
```powershell
node --check functions/index.js
firebase deploy --only firestore:rules,storage,functions,hosting
```
