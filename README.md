# ProjectHub – Smart Student Project Collaboration System

A modern native Android application built in **Java** utilizing **Firebase** (Authentication & Cloud Firestore) under the **MVVM (Model-View-ViewModel)** architectural pattern. Designed specifically for university students, capstone teams, and project mentors to coordinate tasks, milestones, deliverables, and track real-time progress.

---

## 🌟 Key Features

1. **Authentication & Student Directory:**
   - Email & Password authentication with Firebase Auth.
   - Profile tracking with Student Name, Department, and Roll Number.
2. **Project Collaboration & Workspaces:**
   - Create projects with custom deadlines and goals.
   - Unique 6-character alphanumeric **Join Code** for teammates to join instantly.
3. **Interactive Task Lifecycle (Kanban Workflow):**
   - Quick one-tap task status cycling: `TODO` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `DONE`.
   - Task prioritization: `LOW`, `MEDIUM`, `HIGH`.
   - Team member assignments and due dates.
4. **Milestone Engine:**
   - Multi-phase project delivery tracking with due dates and real-time status.
5. **Resource & File References:**
   - Track GitHub repositories, Figma wireframes, design docs, and research papers.
   - Quick "Open" action launches external links directly in browser.
6. **Real-time Team Progress Calculation:**
   - Automatically computes and updates project completion percentage whenever tasks are updated:
     $$\text{Progress \%} = \left(\frac{\text{Completed Tasks}}{\text{Total Tasks}}\right) \times 100$$

---

## 📂 Project Architecture & Package Structure

```
com.projecthub
├── models/
│   ├── User.java              # Student profile model
│   ├── Project.java           # Project workspace & membership model
│   ├── Task.java              # Kanban task with status & priorities
│   ├── Milestone.java         # Project phases & target dates
│   └── FileReference.java     # Documentation & links
├── repository/
│   ├── AuthRepository.java    # Firebase Auth & Firestore user records
│   └── ProjectRepository.java # Firestore CRUD & Real-time Snapshot listeners
├── viewmodel/
│   ├── AuthViewModel.java
│   ├── DashboardViewModel.java
│   └── ProjectDetailsViewModel.java
├── adapters/
│   ├── ProjectAdapter.java    # Project card with progress bar
│   ├── TaskAdapter.java       # Interactive status transition task items
│   ├── MilestoneAdapter.java  # Milestone lists with dates
│   └── FileAdapter.java       # Resource reference cards
└── ui/
    ├── SplashActivity.java    # Auth gatekeeper
    ├── LoginActivity.java     # User sign-in
    ├── RegisterActivity.java  # Student registration
    ├── MainActivity.java      # Dashboard with project lists & actions
    ├── CreateProjectActivity.java # New project & code generation
    └── ProjectDetailsActivity.java # 3-tab hub (Tasks, Milestones, Files)
```

---

## ⚙️ Getting Started & Firebase Configuration

### 1. Configure Firebase
1. Head over to the [Firebase Console](https://console.firebase.google.com/).
2. Create a project named `ProjectHub`.
3. Add an Android app with package name:
   ```
   com.projecthub
   ```
4. Download your `google-services.json` and replace the placeholder at:
   ```
   app/google-services.json
   ```
5. In Firebase Console:
   - **Authentication:** Enable `Email/Password` under Sign-in methods.
   - **Cloud Firestore:** Create a database in test mode or with security rules.

### 2. Building & Running in Antigravity IDE / Android Studio
To compile via Gradle wrapper:
```bash
cd D:\PROJECTS\ProjectHub
.\gradlew assembleDebug
```
To install on a connected device or running emulator:
```bash
.\gradlew installDebug
```
Or open `D:\PROJECTS\ProjectHub` in Antigravity / Android Studio directly.
