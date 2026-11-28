package com.projecthub;

import com.projecthub.models.WorkflowRule;
import com.projecthub.utils.WorkflowAutomationEngine;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class WorkflowAutomationEngineTest {

    @Test
    public void matchRules_filtersByTriggerValueAndEnabledState() {
        WorkflowRule r1 = new WorkflowRule("1", "p1", "Notify on assign",
            WorkflowAutomationEngine.TRIGGER_TASK_ASSIGNED, "",
            WorkflowAutomationEngine.ACTION_NOTIFY_ASSIGNEE, "", true);
        WorkflowRule r2 = new WorkflowRule("2", "p1", "Post on done",
            WorkflowAutomationEngine.TRIGGER_TASK_STATUS_CHANGED, "DONE",
            WorkflowAutomationEngine.ACTION_POST_CHAT_UPDATE, "Done!", true);
        WorkflowRule r3 = new WorkflowRule("3", "p1", "Disabled rule",
            WorkflowAutomationEngine.TRIGGER_TASK_STATUS_CHANGED, "DONE",
            WorkflowAutomationEngine.ACTION_NOTIFY_LEAD, "", false);
        WorkflowRule r4 = new WorkflowRule("4", "p1", "Auto move subtasks",
            WorkflowAutomationEngine.TRIGGER_ALL_SUBTASKS_COMPLETED, "",
            WorkflowAutomationEngine.ACTION_SET_TASK_STATUS, "IN_REVIEW", true);

        List<WorkflowRule> rules = Arrays.asList(r1, r2, r3, r4);

        assertEquals(1, WorkflowAutomationEngine.matchRules(
            rules, WorkflowAutomationEngine.TRIGGER_TASK_ASSIGNED, "").size());
        assertEquals(1, WorkflowAutomationEngine.matchRules(
            rules, WorkflowAutomationEngine.TRIGGER_TASK_STATUS_CHANGED, "DONE").size());
        assertEquals(0, WorkflowAutomationEngine.matchRules(
            rules, WorkflowAutomationEngine.TRIGGER_TASK_STATUS_CHANGED, "IN_PROGRESS").size());
        assertEquals(1, WorkflowAutomationEngine.matchRules(
            rules, WorkflowAutomationEngine.TRIGGER_ALL_SUBTASKS_COMPLETED, "").size());
    }
}
