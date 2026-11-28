package com.projecthub;

import com.projecthub.models.Subtask;
import com.projecthub.models.Task;
import com.projecthub.utils.CampusVerifier;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CampusVerifierAndTaskTest {

    @Test
    public void campusVerifier_checksAcademicDomainsAndVerificationFlag() {
        assertEquals("stanford.edu", CampusVerifier.extractDomain("Student@Stanford.EDU"));
        assertTrue(CampusVerifier.isAcademicDomain("stanford.edu"));
        assertTrue(CampusVerifier.isAcademicDomain("iitb.ac.in"));
        assertFalse(CampusVerifier.isAcademicDomain("gmail.com"));

        assertTrue(CampusVerifier.isCampusVerified(true, "a@mit.edu"));
        assertFalse(CampusVerifier.isCampusVerified(false, "a@mit.edu"));
        assertFalse(CampusVerifier.isCampusVerified(true, "a@gmail.com"));
    }

    @Test
    public void taskSubtasks_tracksCompletionCountsCorrectly() {
        Task task = new Task();
        assertFalse(task.areAllSubtasksCompleted());

        task.setSubtasks(Arrays.asList(
            new Subtask("1", "Step 1", true),
            new Subtask("2", "Step 2", false)
        ));
        assertEquals(2, task.getTotalSubtasksCount());
        assertEquals(1, task.getCompletedSubtasksCount());
        assertFalse(task.areAllSubtasksCompleted());

        task.getSubtasks().get(1).setCompleted(true);
        assertEquals(2, task.getCompletedSubtasksCount());
        assertTrue(task.areAllSubtasksCompleted());
    }
}
