# ProjectHub Web Companion Plan (Beyond Android)

## 1. Purpose & Platform Split
ProjectHub's Android client excels at quick field updates, on-the-go task status taps, campus meetup check-ins, and real-time push alerts. For longer planning sessions, code-heavy reviews, multi-column sprint organization, and GitHub issue triage on laptops, the **Web Companion** shares the exact same Firebase backend:

| Capability | Android App | Web Companion (`web/`) |
| :--- | :--- | :--- |
| Primary Use Case | Mobile alerts, quick status updates, live chat, meetup GPS | Multi-column Kanban board, GitHub issue/PR side-by-side triage, calendar & Gantt planning, code snippet inspection |
| Authentication | Firebase Auth (Email/Password + campus domain verification) | Shared Firebase Auth session (`getAuth`) |
| Database | Cloud Firestore real-time snapshot listeners | Shared Cloud Firestore JS SDK (`onSnapshot`) |
| Files & Storage | Firebase Storage upload & Glide preview | Shared Firebase Storage upload/download with role-aware `accessLevel` queries |
| Notifications | FCM Android push (`ProjectHubMessagingService`) + WorkManager deadline checks | Shared `users/{uid}/notifications` inbox + optional FCM Web Push (`firebase/messaging` + service worker) |

---

## 2. Shared Data Contract & Security Alignment
Both clients read and write identically structured Firestore documents validated by `firestore.rules`:
- **`users/{uid}`**: `role` (`STUDENT` / `FACULTY_MENTOR`), `skills`, `collegeDomain`, `campusVerified`, `bio`, `githubUsername`.
- **`projects/{pid}`**: `customStatuses`, `templateType`, `mentorId`, `mentorName`, `githubRepoUrl`, `members` map (`LEAD`, `MEMBER`, `FACULTY`), `isPublic`/`public`.
- **`projects/{pid}/tasks/{tid}`**: `subtasks` array, `dependsOnTaskId`/`dependsOnTaskKey`, `phase`, `dueDate`, `githubIssueNumber`/`githubIssueUrl`.
- **`projects/{pid}/messages/{mid}`**: `replyToMessageId`/`replyToSenderName`/`replyToPreview`, `reactions` map (`emoji -> uid[]`), `mentionedUserIds`.
- **`projects/{pid}/files/{fid}`**: `accessLevel` (`MEMBERS_ONLY`, `LEAD_ONLY`, `PUBLIC`), `storagePath`, `fileSizeBytes`, `uploadedById`.
- **`projects/{pid}/automations/{rid}`**: trigger/action workflow rules.

> **Important Rule Alignment:** Non-lead members query `files` with `where("accessLevel", "in", ["MEMBERS_ONLY", "PUBLIC"])` plus `where("uploadedById", "==", uid)` so `LEAD_ONLY` files stay confidential at the Firestore rule layer.

---

## 3. Desktop-First UX Priorities Implemented in `web/`
1. **Multi-Column Kanban Board (`KanbanBoard.jsx`)**:
   - Displays all custom workflow columns side by side with search, priority filter, and "My Tasks" filter.
   - Supports full task create/edit/delete, real project member assignment dropdown, due date input, blocker selection, and interactive subtask checklists.
2. **Team Chat (`TeamChat.jsx`)**:
   - Real-time thread with quoted replies, `@mention` insertion + notification dispatch, and click-to-toggle emoji reactions.
3. **Calendar, Milestones & Automations (`CalendarRoadmap.jsx`)**:
   - Chronological deadline schedule for tasks & milestones, milestone status cycling, and interactive workflow automation rule builder.
4. **GitHub Integration (`GitHubPanel.jsx`)**:
   - Connects any `owner/repo` (with optional personal access token in browser memory/localStorage) and lets teams inspect Issues & PRs and import them as ProjectHub tasks in one click.
5. **Files & Campus Discovery (`FilesPanel.jsx`, `ExploreCampus.jsx`)**:
   - Role-scoped files & code snippet previewer, plus public project search and verified campus talent/faculty mentor directory.

---

## 4. Phased Rollout & Deployment
- **Phase 1 (Implemented in `web/`):** Core React + Vite + Firebase SPA covering Auth, Templates, Multi-column Kanban, Subtasks, Chat (replies/mentions/reactions), Calendar/Milestones/Automations, GitHub Issues/PRs, Controlled Files, and Campus Explore.
- **Phase 2 (Hosting & Push):**
  - Build production bundle: `cd web && npm install && npm run build`
  - Deploy rules, functions, and static hosting via `firebase.json`:
    `firebase deploy --only firestore:rules,storage,functions,hosting`
- **Phase 3 (Web Push & OAuth):** Add `firebase-messaging-sw.js` with Web Push VAPID key and GitHub OAuth app login for private repository sync without manual PAT entry.
