package com.projecthub.models;

public class WorkflowRule {
    private String ruleId;
    private String projectId;
    private String name;
    private String triggerType; // "TASK_ASSIGNED", "TASK_STATUS_CHANGED", "ALL_SUBTASKS_COMPLETED", "MILESTONE_COMPLETED"
    private String triggerValue; // Optional match value (e.g., "IN_REVIEW", "DONE", or empty for any)
    private String actionType;  // "NOTIFY_ASSIGNEE", "NOTIFY_LEAD", "POST_CHAT_UPDATE", "SET_TASK_STATUS", "SET_TASK_PHASE"
    private String actionValue; // Target status/phase or message suffix
    private boolean enabled;

    public WorkflowRule() {
        this.enabled = true;
    }

    public WorkflowRule(String ruleId, String projectId, String name, String triggerType,
                        String triggerValue, String actionType, String actionValue, boolean enabled) {
        this.ruleId = ruleId;
        this.projectId = projectId;
        this.name = name;
        this.triggerType = triggerType;
        this.triggerValue = triggerValue != null ? triggerValue : "";
        this.actionType = actionType;
        this.actionValue = actionValue != null ? actionValue : "";
        this.enabled = enabled;
    }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

    public String getTriggerValue() { return triggerValue != null ? triggerValue : ""; }
    public void setTriggerValue(String triggerValue) { this.triggerValue = triggerValue; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getActionValue() { return actionValue != null ? actionValue : ""; }
    public void setActionValue(String actionValue) { this.actionValue = actionValue; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
