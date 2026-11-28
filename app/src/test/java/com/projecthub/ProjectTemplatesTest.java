package com.projecthub;

import com.projecthub.utils.ProjectTemplates;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectTemplatesTest {

    @Test
    public void templates_containStarterMilestonesTasksAndRules() {
        ProjectTemplates.TemplateSpec capstone = ProjectTemplates.getTemplate(ProjectTemplates.TEMPLATE_CAPSTONE);
        assertEquals("Capstone", capstone.category);
        assertEquals(90, capstone.defaultDays);
        assertFalse(capstone.milestones.isEmpty());
        assertFalse(capstone.tasks.isEmpty());
        assertFalse(capstone.workflowRules.isEmpty());
        assertTrue(capstone.tasks.get(0).getTotalSubtasksCount() > 0);

        ProjectTemplates.TemplateSpec hackathon = ProjectTemplates.getTemplate(ProjectTemplates.TEMPLATE_HACKATHON);
        assertEquals("SIH Hackathon", hackathon.category);
        assertEquals(3, hackathon.defaultDays);
        assertFalse(hackathon.tasks.isEmpty());

        ProjectTemplates.TemplateSpec research = ProjectTemplates.getTemplate(ProjectTemplates.TEMPLATE_RESEARCH);
        assertEquals("Research", research.category);
        assertEquals(60, research.defaultDays);
        assertFalse(research.milestones.isEmpty());
    }
}
