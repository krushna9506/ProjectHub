package com.projecthub.utils;

import com.projecthub.models.WorkflowRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WorkflowAutomationEngine {

    public static final String TRIGGER_TASK_ASSIGNED = "TASK_ASSIGNED";
    public static final String TRIGGER_TASK_STATUS_CHANGED = "TASK_STATUS_CHANGED";
    public static final String TRIGGER_ALL_SUBTASKS_COMPLETED = "ALL_SUBTASKS_COMPLETED";
    public static final String TRIGGER_MILESTONE_COMPLETED = "MILESTONE_COMPLETED";

    public static final String ACTION_NOTIFY_ASSIGNEE = "NOTIFY_ASSIGNEE";
    public static final String ACTION_NOTIFY_LEAD = "NOTIFY_LEAD";
    public static final String ACTION_POST_CHAT_UPDATE = "POST_CHAT_UPDATE";
    public static final String ACTION_SET_TASK_STATUS = "SET_TASK_STATUS";
    public static final String ACTION_SET_TASK_PHASE = "SET_TASK_PHASE";

    private WorkflowAutomationEngine() {}

    public static List<WorkflowRule> matchRules(List<WorkflowRule> rules, String triggerType, String triggerValue) {
        List<WorkflowRule> matched = new ArrayList<>();
        if (rules == null || triggerType == null) {
            return matched;
        }
        String normType = triggerType.trim().toUpperCase(Locale.ROOT);
        String normValue = triggerValue != null ? triggerValue.trim().toUpperCase(Locale.ROOT) : "";

        for (WorkflowRule rule : rules) {
            if (rule == null || !rule.isEnabled()) continue;
            String ruleType = rule.getTriggerType() != null ? rule.getTriggerType().trim().toUpperCase(Locale.ROOT) : "";
            if (!normType.equals(ruleType)) continue;

            String expectedValue = rule.getTriggerValue() != null ? rule.getTriggerValue().trim().toUpperCase(Locale.ROOT) : "";
            if (expectedValue.isEmpty() || expectedValue.equals(normValue)) {
                matched.add(rule);
            }
        }
        return matched;
    }

    public static String describeRule(WorkflowRule rule) {
        if (rule == null) return "";
        String trig = formatTriggerLabel(rule.getTriggerType(), rule.getTriggerValue());
        String act = formatActionLabel(rule.getActionType(), rule.getActionValue());
        return "When " + trig + " -> " + act;
    }

    public static String formatTriggerLabel(String triggerType, String triggerValue) {
        if (triggerType == null) return "Event";
        switch (triggerType) {
            case TRIGGER_TASK_ASSIGNED:
                return "Task is assigned";
            case TRIGGER_TASK_STATUS_CHANGED:
                return (triggerValue != null && !triggerValue.isEmpty())
                        ? "Status changes to " + triggerValue
                        : "Task status changes";
            case TRIGGER_ALL_SUBTASKS_COMPLETED:
                return "All subtasks completed";
            case TRIGGER_MILESTONE_COMPLETED:
                return "Milestone completed";
            default:
                return triggerType;
        }
    }

    public static String formatActionLabel(String actionType, String actionValue) {
        if (actionType == null) return "Run action";
        switch (actionType) {
            case ACTION_NOTIFY_ASSIGNEE:
                return "Notify assignee";
            case ACTION_NOTIFY_LEAD:
                return "Notify project lead";
            case ACTION_POST_CHAT_UPDATE:
                return "Post update in Team Chat";
            case ACTION_SET_TASK_STATUS:
                return "Move task to " + (actionValue != null && !actionValue.isEmpty() ? actionValue : "IN_REVIEW");
            case ACTION_SET_TASK_PHASE:
                return "Set phase to " + (actionValue != null && !actionValue.isEmpty() ? actionValue : "REVIEW");
            default:
                return actionType;
        }
    }
}
