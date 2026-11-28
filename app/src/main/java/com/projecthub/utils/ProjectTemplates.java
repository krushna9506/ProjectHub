package com.projecthub.utils;

import com.projecthub.models.Milestone;
import com.projecthub.models.Subtask;
import com.projecthub.models.Task;
import com.projecthub.models.WorkflowRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ProjectTemplates {

    public static final String TEMPLATE_BLANK = "BLANK";
    public static final String TEMPLATE_CAPSTONE = "CAPSTONE";
    public static final String TEMPLATE_HACKATHON = "HACKATHON";
    public static final String TEMPLATE_RESEARCH = "RESEARCH";

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private ProjectTemplates() {}

    public static class TemplateSpec {
        public final String templateType;
        public final String displayName;
        public final String category;
        public final int defaultDays;
        public final String lookingFor;
        public final List<Milestone> milestones;
        public final List<Task> tasks;
        public final List<WorkflowRule> workflowRules;

        public TemplateSpec(String templateType, String displayName, String category,
                            int defaultDays, String lookingFor,
                            List<Milestone> milestones, List<Task> tasks,
                            List<WorkflowRule> workflowRules) {
            this.templateType = templateType;
            this.displayName = displayName;
            this.category = category;
            this.defaultDays = defaultDays;
            this.lookingFor = lookingFor;
            this.milestones = milestones;
            this.tasks = tasks;
            this.workflowRules = workflowRules;
        }
    }

    public static TemplateSpec getTemplate(String templateType) {
        String key = templateType != null ? templateType.trim().toUpperCase(Locale.ROOT) : TEMPLATE_BLANK;
        long now = System.currentTimeMillis();

        if (TEMPLATE_CAPSTONE.equals(key)) {
            List<Milestone> milestones = Arrays.asList(
                new Milestone(null, null, "Proposal & Literature Review", "PENDING", now + 14 * DAY_MS),
                new Milestone(null, null, "System Architecture & Prototype", "PENDING", now + 45 * DAY_MS),
                new Milestone(null, null, "Testing, Thesis & Final Demo", "PENDING", now + 90 * DAY_MS)
            );
            List<Task> tasks = new ArrayList<>();
            Task t1 = createStarterTask("TS-101", "Draft Problem Statement & Abstract", "Define scope, objectives, and success metrics.", "DOCUMENTATION", "HIGH", "PLANNING", now + 7 * DAY_MS,
                Arrays.asList("Identify 5 reference papers", "Get faculty mentor sign-off"));
            Task t2 = createStarterTask("TS-102", "Design Database & API Architecture", "Draft ER diagram, Firestore schema, and API endpoints.", "FEATURE", "HIGH", "DESIGN", now + 21 * DAY_MS,
                Arrays.asList("Create system architecture diagram", "Review data security model"));
            Task t3 = createStarterTask("TS-103", "Build Core MVP & Integration Tests", "Implement primary user flows and automated test suite.", "TASK", "MEDIUM", "DEVELOPMENT", now + 40 * DAY_MS,
                Arrays.asList("Implement core modules", "Run end-to-end verification"));
            Task t4 = createStarterTask("TS-104", "Prepare Final Report & Defense Slides", "Compile evaluation benchmarks and final demo deck.", "DOCUMENTATION", "MEDIUM", "REVIEW", now + 80 * DAY_MS,
                Arrays.asList("Document benchmark results", "Rehearse 10-minute presentation"));
            tasks.add(t1);
            tasks.add(t2);
            tasks.add(t3);
            tasks.add(t4);

            return new TemplateSpec(
                TEMPLATE_CAPSTONE,
                "Capstone Project",
                "Capstone",
                90,
                "Backend Dev, Frontend/Mobile Dev, Research & Docs",
                milestones,
                tasks,
                defaultStarterRules()
            );
        }

        if (TEMPLATE_HACKATHON.equals(key)) {
            List<Milestone> milestones = Arrays.asList(
                new Milestone(null, null, "Ideation & Wireframes", "PENDING", now + 1 * DAY_MS),
                new Milestone(null, null, "Working MVP Build", "PENDING", now + 2 * DAY_MS),
                new Milestone(null, null, "Pitch Deck & Live Demo", "PENDING", now + 3 * DAY_MS)
            );
            List<Task> tasks = new ArrayList<>();
            tasks.add(createStarterTask("TS-101", "Lock Problem Statement & User Flow", "Finalize hackathon track criteria and demo flow.", "TASK", "URGENT", "PLANNING", now + 1 * DAY_MS,
                Arrays.asList("Define winning pitch angle", "Split frontend & backend tracks")));
            tasks.add(createStarterTask("TS-102", "Design High-Impact UI Screens", "Build clickable Figma/UI views for core demo.", "FEATURE", "HIGH", "DESIGN", now + 1 * DAY_MS,
                Arrays.asList("Hero screen & onboarding", "Main interactive workflow")));
            tasks.add(createStarterTask("TS-103", "Ship End-to-End Working Prototype", "Wire real backend/API calls with sample data.", "FEATURE", "URGENT", "DEVELOPMENT", now + 2 * DAY_MS,
                Arrays.asList("Core feature happy path", "Handle edge cases for live demo")));
            tasks.add(createStarterTask("TS-104", "Record Backup Demo Video & Pitch Deck", "Prepare 3-minute pitch slides and fallback recording.", "DOCUMENTATION", "HIGH", "REVIEW", now + 3 * DAY_MS,
                Arrays.asList("Create 8-slide deck", "Record 90s backup video")));

            return new TemplateSpec(
                TEMPLATE_HACKATHON,
                "SIH / Hackathon Sprint",
                "SIH Hackathon",
                3,
                "Fullstack Dev, UI/UX Designer, Pitch & ML",
                milestones,
                tasks,
                defaultStarterRules()
            );
        }

        if (TEMPLATE_RESEARCH.equals(key)) {
            List<Milestone> milestones = Arrays.asList(
                new Milestone(null, null, "Literature Survey & Dataset Prep", "PENDING", now + 15 * DAY_MS),
                new Milestone(null, null, "Experiments & Baseline Benchmarks", "PENDING", now + 40 * DAY_MS),
                new Milestone(null, null, "Paper Manuscript & Peer Review", "PENDING", now + 60 * DAY_MS)
            );
            List<Task> tasks = new ArrayList<>();
            tasks.add(createStarterTask("TS-101", "Curate Related Work Matrix", "Summarize 15+ peer-reviewed papers and baselines.", "DOCUMENTATION", "HIGH", "PLANNING", now + 10 * DAY_MS,
                Arrays.asList("Collect IEEE/ACM/arXiv citations", "Identify research gap")));
            tasks.add(createStarterTask("TS-102", "Preprocess Dataset & Reproducibility Script", "Clean dataset and track train/val/test splits.", "TASK", "HIGH", "DESIGN", now + 20 * DAY_MS,
                Arrays.asList("Verify data licensing", "Create deterministic split script")));
            tasks.add(createStarterTask("TS-103", "Run Ablation Studies & Metrics", "Evaluate proposed method against baselines.", "FEATURE", "HIGH", "DEVELOPMENT", now + 38 * DAY_MS,
                Arrays.asList("Run baseline models", "Log statistical significance")));
            tasks.add(createStarterTask("TS-104", "Draft LaTeX Manuscript & Figures", "Write methodology, results, and submit for mentor review.", "DOCUMENTATION", "MEDIUM", "REVIEW", now + 55 * DAY_MS,
                Arrays.asList("Plot comparison charts", "Run advisor proofreading pass")));

            return new TemplateSpec(
                TEMPLATE_RESEARCH,
                "Research Paper",
                "Research",
                60,
                "ML Researcher, Data Engineer, Technical Writer",
                milestones,
                tasks,
                defaultStarterRules()
            );
        }

        return new TemplateSpec(
            TEMPLATE_BLANK,
            "Blank Workspace",
            "General",
            30,
            "",
            new ArrayList<>(),
            new ArrayList<>(),
            defaultStarterRules()
        );
    }

    private static Task createStarterTask(String key, String title, String desc, String issueType,
                                          String priority, String phase, long dueDate, List<String> checklist) {
        Task task = new Task(null, null, null, title, desc, "TODO", priority, issueType, null, "", dueDate);
        task.setTaskKey(key);
        task.setPhase(phase);
        List<Subtask> subtasks = new ArrayList<>();
        if (checklist != null) {
            for (String item : checklist) {
                subtasks.add(new Subtask(null, item, false));
            }
        }
        task.setSubtasks(subtasks);
        return task;
    }

    public static List<WorkflowRule> defaultStarterRules() {
        List<WorkflowRule> rules = new ArrayList<>();
        rules.add(new WorkflowRule(
            null, null,
            "Notify assignee when task assigned",
            WorkflowAutomationEngine.TRIGGER_TASK_ASSIGNED,
            "",
            WorkflowAutomationEngine.ACTION_NOTIFY_ASSIGNEE,
            "",
            true
        ));
        rules.add(new WorkflowRule(
            null, null,
            "Move to Review when all subtasks done",
            WorkflowAutomationEngine.TRIGGER_ALL_SUBTASKS_COMPLETED,
            "",
            WorkflowAutomationEngine.ACTION_SET_TASK_STATUS,
            "IN_REVIEW",
            true
        ));
        rules.add(new WorkflowRule(
            null, null,
            "Announce completed tasks in Team Chat",
            WorkflowAutomationEngine.TRIGGER_TASK_STATUS_CHANGED,
            "DONE",
            WorkflowAutomationEngine.ACTION_POST_CHAT_UPDATE,
            "Task completed!",
            true
        ));
        return rules;
    }
}
