package com.projecthub.repository;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.projecthub.models.AppNotification;
import com.projecthub.models.ChatMessage;
import com.projecthub.models.FileReference;
import com.projecthub.models.JoinRequest;
import com.projecthub.models.Milestone;
import com.projecthub.models.Project;
import com.projecthub.models.Subtask;
import com.projecthub.models.Task;
import com.projecthub.models.TaskComment;
import com.projecthub.models.User;
import com.projecthub.models.WorkflowRule;
import com.projecthub.utils.NotificationHelper;
import com.projecthub.utils.ProjectTemplates;
import com.projecthub.utils.WorkflowAutomationEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<ListenerRegistration> activeListeners = new ArrayList<>();
    private final List<ListenerRegistration> fileListeners = new ArrayList<>();

    public interface OperationCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface StorageUploadCallback {
        void onProgress(int percent);
        void onSuccess(FileReference fileReference);
        void onError(String message);
    }

    public void clearListeners() {
        for (ListenerRegistration reg : activeListeners) {
            if (reg != null) {
                reg.remove();
            }
        }
        activeListeners.clear();
        clearFileListeners();
    }

    private void clearFileListeners() {
        for (ListenerRegistration reg : fileListeners) {
            if (reg != null) {
                reg.remove();
            }
        }
        fileListeners.clear();
    }

    private void trackListener(ListenerRegistration reg) {
        if (reg != null) {
            activeListeners.add(reg);
        }
    }

    private String getCurrentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "";
    }

    public LiveData<List<Project>> getUserProjects(String userId) {
        MutableLiveData<List<Project>> projectsLiveData = new MutableLiveData<>();

        ListenerRegistration reg = db.collection("projects")
            .whereGreaterThan("members." + userId, "")
            .addSnapshotListener((snapshots, error) -> {
                if (error != null || snapshots == null) {
                    projectsLiveData.setValue(new ArrayList<>());
                    return;
                }
                List<Project> list = new ArrayList<>();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    Project p = doc.toObject(Project.class);
                    if (p != null) {
                        if (p.getProjectId() == null || p.getProjectId().isEmpty()) {
                            p.setProjectId(doc.getId());
                        }
                        list.add(p);
                    }
                }
                projectsLiveData.setValue(list);
            });
        trackListener(reg);
        return projectsLiveData;
    }

    public LiveData<List<Project>> getPublicProjects() {
        MutableLiveData<List<Project>> projectsLiveData = new MutableLiveData<>();

        ListenerRegistration reg = db.collection("projects")
            .whereEqualTo("isPublic", true)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null || snapshots == null) {
                    projectsLiveData.setValue(new ArrayList<>());
                    return;
                }
                List<Project> list = new ArrayList<>();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    Project p = doc.toObject(Project.class);
                    if (p != null) {
                        if (p.getProjectId() == null || p.getProjectId().isEmpty()) {
                            p.setProjectId(doc.getId());
                        }
                        list.add(p);
                    }
                }
                projectsLiveData.setValue(list);
            });
        trackListener(reg);
        return projectsLiveData;
    }

    public void createProject(Project project, OperationCallback callback) {
        createProjectWithTemplate(project, project.getTemplateType(), callback);
    }

    public void createProjectWithTemplate(Project project, String templateType, OperationCallback callback) {
        DocumentReference docRef = db.collection("projects").document();
        String projectId = docRef.getId();
        project.setProjectId(projectId);
        if (project.getJoinCode() != null) {
            project.setJoinCode(project.getJoinCode().trim().toUpperCase());
        }
        String effectiveTemplate = (templateType != null && !templateType.isEmpty())
                ? templateType : ProjectTemplates.TEMPLATE_BLANK;
        project.setTemplateType(effectiveTemplate);

        docRef.set(project)
            .addOnSuccessListener(aVoid -> {
                try {
                    db.collection("users").document(project.getLeadId())
                        .update("joinedProjectIds", FieldValue.arrayUnion(projectId));
                } catch (Exception ignored) {}

                ProjectTemplates.TemplateSpec spec = ProjectTemplates.getTemplate(effectiveTemplate);
                WriteBatch batch = db.batch();

                String cleanCode = project.getJoinCode() != null ? project.getJoinCode().trim().toUpperCase() : "";
                if (cleanCode.length() >= 6) {
                    Map<String, Object> codeDoc = new HashMap<>();
                    codeDoc.put("joinCode", cleanCode);
                    codeDoc.put("projectId", projectId);
                    codeDoc.put("leadId", project.getLeadId());
                    codeDoc.put("createdAt", project.getCreatedAt() > 0 ? project.getCreatedAt() : System.currentTimeMillis());
                    batch.set(db.collection("join_codes").document(cleanCode), codeDoc);
                }

                for (Milestone m : spec.milestones) {
                    DocumentReference mRef = docRef.collection("milestones").document();
                    m.setMilestoneId(mRef.getId());
                    m.setProjectId(projectId);
                    batch.set(mRef, m);
                }

                for (Task t : spec.tasks) {
                    DocumentReference tRef = docRef.collection("tasks").document();
                    t.setTaskId(tRef.getId());
                    t.setProjectId(projectId);
                    batch.set(tRef, t);
                }

                for (WorkflowRule rule : spec.workflowRules) {
                    DocumentReference rRef = docRef.collection("automations").document();
                    rule.setRuleId(rRef.getId());
                    rule.setProjectId(projectId);
                    batch.set(rRef, rule);
                }

                batch.commit()
                    .addOnSuccessListener(unused -> {
                        if (callback != null) callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onSuccess();
                    });
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError("Failed to create project: " + e.getMessage());
            });
    }

    public void joinProjectWithCode(String userId, String joinCode, OperationCallback callback) {
        String cleanCode = joinCode != null ? joinCode.trim() : "";
        if (cleanCode.isEmpty() || userId == null || userId.isEmpty()) {
            if (callback != null) callback.onError("Please enter a valid project join code.");
            return;
        }

        String upperCode = cleanCode.toUpperCase();
        db.collection("join_codes").document(upperCode).get()
            .addOnSuccessListener(codeSnap -> {
                if (codeSnap != null && codeSnap.exists()) {
                    String projectId = codeSnap.getString("projectId");
                    String resolvedCode = codeSnap.getString("joinCode");
                    if (projectId != null && !projectId.isEmpty()) {
                        processJoinByIdAndCode(
                            db.collection("projects").document(projectId),
                            resolvedCode != null ? resolvedCode : upperCode,
                            userId,
                            callback
                        );
                        return;
                    }
                }
                // Fallback for legacy public projects created before join_codes existed
                db.collection("projects")
                    .whereEqualTo("isPublic", true)
                    .whereEqualTo("joinCode", upperCode)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap.isEmpty()) {
                            if (callback != null) callback.onError("No project found with code: " + upperCode);
                        } else {
                            DocumentSnapshot projDoc = snap.getDocuments().get(0);
                            processJoinByIdAndCode(projDoc.getReference(), upperCode, userId, callback);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onError("Failed to find project: " + e.getMessage());
                    });
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError("Failed to find project: " + e.getMessage());
            });
    }

    private void processJoinByIdAndCode(DocumentReference projectRef, String codeUsed, String userId, OperationCallback callback) {
        String projectId = projectRef.getId();
        Map<String, Object> updates = new HashMap<>();
        updates.put("members." + userId, "MEMBER");
        updates.put("lastJoinCodeUsed", codeUsed + ":" + userId);

        projectRef.update(updates)
            .addOnSuccessListener(aVoid -> {
                try {
                    db.collection("users").document(userId)
                        .update("joinedProjectIds", FieldValue.arrayUnion(projectId));
                } catch (Exception ignored) {}
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError("Failed to join: " + e.getMessage());
            });
    }

    public void sendJoinRequest(JoinRequest request, OperationCallback callback) {
        DocumentReference docRef = db.collection("join_requests").document();
        request.setRequestId(docRef.getId());

        docRef.set(request)
            .addOnSuccessListener(aVoid -> {
                NotificationHelper.dispatchNotification(null, request.getLeadId(),
                    "New Team Join Request",
                    request.getApplicantName() + " requested to join " + (request.getProjectTitle() != null ? request.getProjectTitle() : "your project"),
                    "JOIN_REQUEST", request.getProjectId(), request.getProjectTitle());
                callback.onSuccess();
            })
            .addOnFailureListener(e -> callback.onError("Failed to send request: " + e.getMessage()));
    }

    public void sendTeamOrMentorInvite(User targetUser, Project project, String inviterName, String inviteRole, OperationCallback callback) {
        if (targetUser == null || project == null) {
            if (callback != null) callback.onError("Invalid user or project.");
            return;
        }
        String roleLabel = "FACULTY".equalsIgnoreCase(inviteRole) ? "Faculty Mentor" : "Teammate";
        String title = "Project Invitation (" + roleLabel + ")";
        String message = inviterName + " invited you to join \"" + project.getTitle()
                + "\". Use Join Code: " + project.getJoinCode();

        NotificationHelper.dispatchNotification(
            null,
            targetUser.getUserId(),
            title,
            message,
            "TEAM_INVITE",
            project.getProjectId(),
            project.getTitle()
        );
        if (callback != null) callback.onSuccess();
    }

    public LiveData<List<JoinRequest>> getJoinRequestsForLead(String leadId) {
        MutableLiveData<List<JoinRequest>> requestsLiveData = new MutableLiveData<>();

        ListenerRegistration reg = db.collection("join_requests")
            .whereEqualTo("leadId", leadId)
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    List<JoinRequest> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        JoinRequest r = doc.toObject(JoinRequest.class);
                        if (r != null) {
                            if (r.getRequestId() == null || r.getRequestId().isEmpty()) {
                                r.setRequestId(doc.getId());
                            }
                            list.add(r);
                        }
                    }
                    requestsLiveData.setValue(list);
                }
            });
        trackListener(reg);
        return requestsLiveData;
    }

    public LiveData<List<JoinRequest>> getJoinRequestsForApplicant(String applicantId) {
        MutableLiveData<List<JoinRequest>> requestsLiveData = new MutableLiveData<>();

        ListenerRegistration reg = db.collection("join_requests")
            .whereEqualTo("applicantId", applicantId)
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    List<JoinRequest> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        JoinRequest r = doc.toObject(JoinRequest.class);
                        if (r != null) {
                            if (r.getRequestId() == null || r.getRequestId().isEmpty()) {
                                r.setRequestId(doc.getId());
                            }
                            list.add(r);
                        }
                    }
                    requestsLiveData.setValue(list);
                }
            });
        trackListener(reg);
        return requestsLiveData;
    }

    public void acceptJoinRequest(JoinRequest request, OperationCallback callback) {
        if (request == null || request.getRequestId() == null || request.getRequestId().isEmpty()) {
            if (callback != null) callback.onError("Invalid join request ID.");
            return;
        }
        db.collection("join_requests").document(request.getRequestId())
            .update("status", "ACCEPTED")
            .addOnSuccessListener(aVoid -> {
                db.collection("projects").document(request.getProjectId())
                    .update("members." + request.getApplicantId(), "MEMBER")
                    .addOnSuccessListener(aVoid2 -> {
                        NotificationHelper.dispatchNotification(null, request.getApplicantId(),
                            "Join Request Accepted",
                            "You are now a member of " + (request.getProjectTitle() != null ? request.getProjectTitle() : "the project"),
                            "JOIN_ACCEPTED", request.getProjectId(), request.getProjectTitle());
                        if (callback != null) callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onError("Failed to add member: " + e.getMessage());
                    });
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError("Failed to accept request: " + e.getMessage());
            });
    }

    public void rejectJoinRequest(JoinRequest request, OperationCallback callback) {
        if (request == null || request.getRequestId() == null || request.getRequestId().isEmpty()) {
            if (callback != null) callback.onError("Invalid join request ID.");
            return;
        }
        db.collection("join_requests").document(request.getRequestId())
            .update("status", "REJECTED")
            .addOnSuccessListener(aVoid -> {
                NotificationHelper.dispatchNotification(null, request.getApplicantId(),
                    "Join Request Update",
                    "Your request to join " + (request.getProjectTitle() != null ? request.getProjectTitle() : "the project") + " was not accepted.",
                    "JOIN_REJECTED", request.getProjectId(), request.getProjectTitle());
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public LiveData<Project> getProject(String projectId) {
        MutableLiveData<Project> projectLiveData = new MutableLiveData<>();
        ListenerRegistration reg = db.collection("projects").document(projectId)
            .addSnapshotListener((snapshot, error) -> {
                if (snapshot != null && snapshot.exists()) {
                    Project p = snapshot.toObject(Project.class);
                    if (p != null && (p.getProjectId() == null || p.getProjectId().isEmpty())) {
                        p.setProjectId(snapshot.getId());
                    }
                    projectLiveData.setValue(p);
                }
            });
        trackListener(reg);
        return projectLiveData;
    }

    public LiveData<List<User>> getProjectMembers(List<String> memberIds) {
        MutableLiveData<List<User>> membersLiveData = new MutableLiveData<>(new ArrayList<>());
        loadProjectMembersInto(memberIds, membersLiveData);
        return membersLiveData;
    }

    public void loadProjectMembersInto(List<String> memberIds, MutableLiveData<List<User>> target) {
        if (memberIds == null || memberIds.isEmpty()) {
            target.setValue(new ArrayList<>());
            return;
        }
        List<com.google.android.gms.tasks.Task<DocumentSnapshot>> fetches = new ArrayList<>();
        for (String uid : memberIds) {
            if (uid != null && !uid.isEmpty()) {
                fetches.add(db.collection("users").document(uid).get());
            }
        }
        if (fetches.isEmpty()) {
            target.setValue(new ArrayList<>());
            return;
        }
        Tasks.whenAllSuccess(fetches)
            .addOnSuccessListener(results -> {
                List<User> members = new ArrayList<>();
                for (Object obj : results) {
                    if (obj instanceof DocumentSnapshot) {
                        DocumentSnapshot snap = (DocumentSnapshot) obj;
                        if (snap.exists()) {
                            User u = snap.toObject(User.class);
                            if (u != null) {
                                if (u.getUserId() == null || u.getUserId().isEmpty()) {
                                    u.setUserId(snap.getId());
                                }
                                members.add(u);
                            }
                        }
                    }
                }
                target.setValue(members);
            })
            .addOnFailureListener(e -> target.setValue(new ArrayList<>()));
    }

    public void updateProjectCustomStatuses(String projectId, List<String> statuses, OperationCallback callback) {
        if (statuses == null || statuses.isEmpty()) {
            if (callback != null) callback.onError("At least one status is required.");
            return;
        }
        db.collection("projects").document(projectId)
            .update("customStatuses", statuses)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public LiveData<List<Task>> getProjectTasks(String projectId) {
        MutableLiveData<List<Task>> tasksLiveData = new MutableLiveData<>();

        ListenerRegistration reg = db.collection("projects").document(projectId).collection("tasks")
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    List<Task> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Task t = doc.toObject(Task.class);
                        if (t != null) {
                            if (t.getTaskId() == null || t.getTaskId().isEmpty()) {
                                t.setTaskId(doc.getId());
                            }
                            list.add(t);
                        }
                    }
                    tasksLiveData.setValue(list);
                }
            });
        trackListener(reg);
        return tasksLiveData;
    }

    public LiveData<Task> getTask(String projectId, String taskId) {
        MutableLiveData<Task> taskLiveData = new MutableLiveData<>();
        ListenerRegistration reg = db.collection("projects").document(projectId)
            .collection("tasks").document(taskId)
            .addSnapshotListener((snapshot, error) -> {
                if (snapshot != null && snapshot.exists()) {
                    Task t = snapshot.toObject(Task.class);
                    if (t != null && (t.getTaskId() == null || t.getTaskId().isEmpty())) {
                        t.setTaskId(snapshot.getId());
                    }
                    taskLiveData.setValue(t);
                }
            });
        trackListener(reg);
        return taskLiveData;
    }

    public void addTask(String projectId, Task task, OperationCallback callback) {
        DocumentReference docRef = db.collection("projects").document(projectId)
            .collection("tasks").document();
        task.setTaskId(docRef.getId());
        task.setProjectId(projectId);
        if (task.getTaskKey() == null || task.getTaskKey().isEmpty()) {
            task.setTaskKey("TS-" + (100 + (int) (Math.random() * 900)));
        }

        docRef.set(task)
            .addOnSuccessListener(aVoid -> {
                recalculateProgress(projectId);
                if (task.getAssignedTo() != null && !task.getAssignedTo().isEmpty()) {
                    runAutomationsWithFallbackAssignment(projectId, task, true);
                }
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public void updateTask(String projectId, Task task, String previousAssigneeId, String previousStatus, OperationCallback callback) {
        if (task == null || task.getTaskId() == null || task.getTaskId().isEmpty()) {
            if (callback != null) callback.onError("Invalid task ID.");
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", task.getTitle());
        updates.put("description", task.getDescription());
        updates.put("status", task.getStatus());
        updates.put("priority", task.getPriority());
        updates.put("issueType", task.getIssueType());
        updates.put("phase", task.getPhase());
        updates.put("assignedTo", task.getAssignedTo());
        updates.put("assignedToName", task.getAssignedToName());
        updates.put("dueDate", task.getDueDate());
        updates.put("dependsOnTaskId", task.getDependsOnTaskId());
        updates.put("dependsOnTaskKey", task.getDependsOnTaskKey());
        updates.put("tags", task.getTags());

        db.collection("projects").document(projectId)
            .collection("tasks").document(task.getTaskId())
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                recalculateProgress(projectId);

                String newAssignee = task.getAssignedTo() != null ? task.getAssignedTo() : "";
                String prevAssignee = previousAssigneeId != null ? previousAssigneeId : "";
                if (!newAssignee.isEmpty() && !newAssignee.equals(prevAssignee)) {
                    runAutomationsWithFallbackAssignment(projectId, task, true);
                }

                String newStatus = task.getStatus() != null ? task.getStatus() : "";
                String prevStat = previousStatus != null ? previousStatus : "";
                if (!newStatus.isEmpty() && !newStatus.equalsIgnoreCase(prevStat)) {
                    runAutomations(projectId, WorkflowAutomationEngine.TRIGGER_TASK_STATUS_CHANGED, newStatus, task, null);
                }

                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public void deleteTask(String projectId, String taskId, OperationCallback callback) {
        if (projectId == null || projectId.isEmpty() || taskId == null || taskId.isEmpty()) {
            if (callback != null) callback.onError("Invalid task ID.");
            return;
        }
        db.collection("projects").document(projectId)
            .collection("tasks").document(taskId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                recalculateProgress(projectId);
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public void updateTaskSubtasks(String projectId, String taskId, List<Subtask> subtasks, OperationCallback callback) {
        List<Subtask> safeList = subtasks != null ? subtasks : new ArrayList<>();
        db.collection("projects").document(projectId)
            .collection("tasks").document(taskId)
            .update("subtasks", safeList)
            .addOnSuccessListener(aVoid -> {
                boolean allDone = !safeList.isEmpty();
                for (Subtask s : safeList) {
                    if (s == null || !s.isCompleted()) {
                        allDone = false;
                        break;
                    }
                }
                if (allDone) {
                    db.collection("projects").document(projectId)
                        .collection("tasks").document(taskId).get()
                        .addOnSuccessListener(snap -> {
                            Task t = snap.toObject(Task.class);
                            if (t != null) {
                                if (t.getTaskId() == null) t.setTaskId(snap.getId());
                                runAutomations(projectId, WorkflowAutomationEngine.TRIGGER_ALL_SUBTASKS_COMPLETED, "", t, null);
                            }
                        });
                }
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public void logTaskWorkTime(String projectId, String taskId, double hours, OperationCallback callback) {
        db.collection("projects").document(projectId)
            .collection("tasks").document(taskId)
            .update("timeSpentHours", FieldValue.increment(hours))
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public void updateTaskStatus(String projectId, String taskId, String newStatus) {
        updateTaskStatus(projectId, taskId, newStatus, false);
    }

    public void updateTaskStatus(String projectId, String taskId, String newStatus, boolean fromAutomation) {
        db.collection("projects").document(projectId)
            .collection("tasks").document(taskId)
            .update("status", newStatus)
            .addOnSuccessListener(aVoid -> {
                recalculateProgress(projectId);
                if (!fromAutomation) {
                    db.collection("projects").document(projectId)
                        .collection("tasks").document(taskId).get()
                        .addOnSuccessListener(snap -> {
                            Task t = snap.toObject(Task.class);
                            if (t != null) {
                                if (t.getTaskId() == null) t.setTaskId(snap.getId());
                                runAutomations(projectId, WorkflowAutomationEngine.TRIGGER_TASK_STATUS_CHANGED, newStatus, t, null);
                            }
                        });
                }
            });
    }

    public LiveData<List<TaskComment>> getTaskComments(String projectId, String taskId) {
        MutableLiveData<List<TaskComment>> commentsLiveData = new MutableLiveData<>();

        ListenerRegistration reg = db.collection("projects").document(projectId)
            .collection("tasks").document(taskId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    List<TaskComment> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        TaskComment c = doc.toObject(TaskComment.class);
                        if (c != null) {
                            if (c.getCommentId() == null || c.getCommentId().isEmpty()) {
                                c.setCommentId(doc.getId());
                            }
                            list.add(c);
                        }
                    }
                    commentsLiveData.setValue(list);
                }
            });
        trackListener(reg);
        return commentsLiveData;
    }

    public void addTaskComment(String projectId, String taskId, TaskComment comment, OperationCallback callback) {
        addTaskCommentWithMentions(projectId, taskId, comment, null, null, null, callback);
    }

    public void addTaskCommentWithMentions(String projectId, String taskId, TaskComment comment,
                                           Task taskContext, String projectTitle,
                                           List<String> mentionedUserIds, OperationCallback callback) {
        DocumentReference docRef = db.collection("projects").document(projectId)
            .collection("tasks").document(taskId)
            .collection("comments").document();
        comment.setCommentId(docRef.getId());
        comment.setTaskId(taskId);
        comment.setProjectId(projectId);

        docRef.set(comment)
            .addOnSuccessListener(aVoid -> {
                db.collection("projects").document(projectId)
                    .collection("tasks").document(taskId)
                    .update("commentsCount", FieldValue.increment(1));

                Set<String> notified = new HashSet<>();
                String authorId = comment.getAuthorId() != null ? comment.getAuthorId() : "";
                String authorName = comment.getAuthorName() != null ? comment.getAuthorName() : "Teammate";
                String taskName = taskContext != null && taskContext.getTitle() != null ? taskContext.getTitle() : "a task";

                if (mentionedUserIds != null) {
                    for (String uid : mentionedUserIds) {
                        if (uid != null && !uid.isEmpty() && !uid.equals(authorId) && !notified.contains(uid)) {
                            notified.add(uid);
                            NotificationHelper.dispatchNotification(
                                null,
                                uid,
                                "Mentioned in " + taskName,
                                authorName + ": " + comment.getMessage(),
                                "MENTION",
                                projectId,
                                projectTitle
                            );
                        }
                    }
                }

                if (taskContext != null && taskContext.getAssignedTo() != null) {
                    String assigneeId = taskContext.getAssignedTo();
                    if (!assigneeId.isEmpty() && !assigneeId.equals(authorId) && !notified.contains(assigneeId)) {
                        NotificationHelper.dispatchNotification(
                            null,
                            assigneeId,
                            "New Comment on " + taskName,
                            authorName + ": " + comment.getMessage(),
                            "COMMENT",
                            projectId,
                            projectTitle
                        );
                    }
                }

                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public LiveData<List<Milestone>> getProjectMilestones(String projectId) {
        MutableLiveData<List<Milestone>> milestonesLiveData = new MutableLiveData<>();

        ListenerRegistration reg = db.collection("projects").document(projectId).collection("milestones")
            .orderBy("dueDate", Query.Direction.ASCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    List<Milestone> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Milestone m = doc.toObject(Milestone.class);
                        if (m != null) {
                            if (m.getMilestoneId() == null || m.getMilestoneId().isEmpty()) {
                                m.setMilestoneId(doc.getId());
                            }
                            list.add(m);
                        }
                    }
                    milestonesLiveData.setValue(list);
                }
            });
        trackListener(reg);
        return milestonesLiveData;
    }

    public void addMilestone(String projectId, Milestone milestone, OperationCallback callback) {
        DocumentReference docRef = db.collection("projects").document(projectId)
            .collection("milestones").document();
        milestone.setMilestoneId(docRef.getId());
        milestone.setProjectId(projectId);

        docRef.set(milestone)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public void updateMilestoneStatus(String projectId, String milestoneId, String newStatus) {
        db.collection("projects").document(projectId)
            .collection("milestones").document(milestoneId)
            .update("status", newStatus)
            .addOnSuccessListener(aVoid -> {
                if ("COMPLETED".equalsIgnoreCase(newStatus) || "DONE".equalsIgnoreCase(newStatus)) {
                    db.collection("projects").document(projectId)
                        .collection("milestones").document(milestoneId).get()
                        .addOnSuccessListener(snap -> {
                            Milestone ms = snap.toObject(Milestone.class);
                            String title = ms != null && ms.getTitle() != null ? ms.getTitle() : "Milestone";
                            runAutomations(projectId, WorkflowAutomationEngine.TRIGGER_MILESTONE_COMPLETED, newStatus, null, title);
                        });
                }
            });
    }

    public LiveData<List<FileReference>> getProjectFiles(String projectId) {
        MutableLiveData<List<FileReference>> filesLiveData = new MutableLiveData<>();
        loadProjectFilesForUser(projectId, getCurrentUid(), false, filesLiveData);
        return filesLiveData;
    }

    public void loadProjectFilesForUser(String projectId, String userId, boolean isLead,
                                        MutableLiveData<List<FileReference>> target) {
        clearFileListeners();
        if (isLead) {
            ListenerRegistration reg = db.collection("projects").document(projectId).collection("files")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (snapshots != null) {
                        List<FileReference> list = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            FileReference f = doc.toObject(FileReference.class);
                            if (f != null) {
                                if (f.getFileId() == null || f.getFileId().isEmpty()) {
                                    f.setFileId(doc.getId());
                                }
                                list.add(f);
                            }
                        }
                        target.setValue(list);
                    }
                });
            fileListeners.add(reg);
            return;
        }

        final Map<String, FileReference> sharedMap = new LinkedHashMap<>();
        final Map<String, FileReference> ownMap = new LinkedHashMap<>();

        Runnable emitMerged = () -> {
            Map<String, FileReference> combined = new LinkedHashMap<>(sharedMap);
            combined.putAll(ownMap);
            List<FileReference> merged = new ArrayList<>(combined.values());
            Collections.sort(merged, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            target.setValue(merged);
        };

        ListenerRegistration regShared = db.collection("projects").document(projectId).collection("files")
            .whereIn("accessLevel", Arrays.asList("MEMBERS_ONLY", "PUBLIC"))
            .addSnapshotListener((snapshots, error) -> {
                sharedMap.clear();
                if (snapshots != null) {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        FileReference f = doc.toObject(FileReference.class);
                        if (f != null) {
                            if (f.getFileId() == null || f.getFileId().isEmpty()) f.setFileId(doc.getId());
                            sharedMap.put(f.getFileId(), f);
                        }
                    }
                }
                emitMerged.run();
            });
        fileListeners.add(regShared);

        if (userId != null && !userId.isEmpty()) {
            ListenerRegistration regOwn = db.collection("projects").document(projectId).collection("files")
                .whereEqualTo("uploadedById", userId)
                .addSnapshotListener((snapshots, error) -> {
                    ownMap.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            FileReference f = doc.toObject(FileReference.class);
                            if (f != null) {
                                if (f.getFileId() == null || f.getFileId().isEmpty()) f.setFileId(doc.getId());
                                ownMap.put(f.getFileId(), f);
                            }
                        }
                    }
                    emitMerged.run();
                });
            fileListeners.add(regOwn);
        }
    }

    public void addFileReference(String projectId, FileReference fileRef, OperationCallback callback) {
        DocumentReference docRef = db.collection("projects").document(projectId)
            .collection("files").document();
        fileRef.setFileId(docRef.getId());
        fileRef.setProjectId(projectId);
        if (fileRef.getUploadedById() == null || fileRef.getUploadedById().isEmpty()) {
            fileRef.setUploadedById(getCurrentUid());
        }

        docRef.set(fileRef)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public void deleteFileReference(String projectId, FileReference fileRef, OperationCallback callback) {
        if (fileRef == null || fileRef.getFileId() == null) {
            if (callback != null) callback.onError("Invalid file reference.");
            return;
        }
        Runnable deleteFirestore = () -> db.collection("projects").document(projectId)
            .collection("files").document(fileRef.getFileId())
            .delete()
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });

        if (fileRef.getStoragePath() != null && !fileRef.getStoragePath().isEmpty()) {
            FirebaseStorage.getInstance().getReference().child(fileRef.getStoragePath())
                .delete()
                .addOnSuccessListener(aVoid -> deleteFirestore.run())
                .addOnFailureListener(e -> deleteFirestore.run());
        } else {
            deleteFirestore.run();
        }
    }

    public LiveData<List<ChatMessage>> getProjectChatMessages(String projectId) {
        MutableLiveData<List<ChatMessage>> messagesLiveData = new MutableLiveData<>();

        ListenerRegistration reg = db.collection("projects").document(projectId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    List<ChatMessage> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        ChatMessage m = doc.toObject(ChatMessage.class);
                        if (m != null) {
                            if (m.getMessageId() == null || m.getMessageId().isEmpty()) {
                                m.setMessageId(doc.getId());
                            }
                            list.add(m);
                        }
                    }
                    messagesLiveData.setValue(list);
                }
            });
        trackListener(reg);
        return messagesLiveData;
    }

    public void sendChatMessage(String projectId, ChatMessage message, OperationCallback callback) {
        sendChatMessage(projectId, message, null, callback);
    }

    public void sendChatMessage(String projectId, ChatMessage message, String projectTitle, OperationCallback callback) {
        DocumentReference docRef = db.collection("projects").document(projectId)
            .collection("messages").document();
        message.setMessageId(docRef.getId());
        message.setProjectId(projectId);

        docRef.set(message)
            .addOnSuccessListener(aVoid -> {
                List<String> mentions = message.getMentionedUserIds();
                if (mentions != null && !mentions.isEmpty()) {
                    String senderId = message.getSenderId() != null ? message.getSenderId() : "";
                    String senderName = message.getSenderName() != null ? message.getSenderName() : "Teammate";
                    for (String uid : mentions) {
                        if (uid != null && !uid.isEmpty() && !uid.equals(senderId)) {
                            NotificationHelper.dispatchNotification(
                                null,
                                uid,
                                senderName + " mentioned you in chat",
                                message.getMessage(),
                                "MENTION",
                                projectId,
                                projectTitle
                            );
                        }
                    }
                }
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public void toggleChatReaction(String projectId, String messageId, String emoji, String userId) {
        if (projectId == null || messageId == null || emoji == null || userId == null || userId.isEmpty()) return;
        DocumentReference msgRef = db.collection("projects").document(projectId)
            .collection("messages").document(messageId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(msgRef);
            ChatMessage msg = snapshot.toObject(ChatMessage.class);
            Map<String, List<String>> reactions = (msg != null && msg.getReactions() != null)
                    ? new HashMap<>(msg.getReactions())
                    : new HashMap<>();

            List<String> users = reactions.get(emoji) != null
                    ? new ArrayList<>(reactions.get(emoji))
                    : new ArrayList<>();

            if (users.contains(userId)) {
                users.remove(userId);
            } else {
                users.add(userId);
            }

            if (users.isEmpty()) {
                reactions.remove(emoji);
            } else {
                reactions.put(emoji, users);
            }
            transaction.update(msgRef, "reactions", reactions);
            return null;
        });
    }

    public void updateProjectGithubUrl(String projectId, String githubUrl) {
        db.collection("projects").document(projectId)
            .update("githubRepoUrl", githubUrl);
    }

    public LiveData<List<WorkflowRule>> getProjectAutomations(String projectId) {
        MutableLiveData<List<WorkflowRule>> rulesLiveData = new MutableLiveData<>();
        ListenerRegistration reg = db.collection("projects").document(projectId)
            .collection("automations")
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    List<WorkflowRule> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        WorkflowRule r = doc.toObject(WorkflowRule.class);
                        if (r != null) {
                            if (r.getRuleId() == null || r.getRuleId().isEmpty()) {
                                r.setRuleId(doc.getId());
                            }
                            list.add(r);
                        }
                    }
                    rulesLiveData.setValue(list);
                }
            });
        trackListener(reg);
        return rulesLiveData;
    }

    public void addAutomationRule(String projectId, WorkflowRule rule, OperationCallback callback) {
        DocumentReference docRef = db.collection("projects").document(projectId)
            .collection("automations").document();
        rule.setRuleId(docRef.getId());
        rule.setProjectId(projectId);
        docRef.set(rule)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public void toggleAutomationRule(String projectId, String ruleId, boolean enabled) {
        if (projectId == null || projectId.isEmpty() || ruleId == null || ruleId.isEmpty()) return;
        db.collection("projects").document(projectId)
            .collection("automations").document(ruleId)
            .update("enabled", enabled);
    }

    public void deleteAutomationRule(String projectId, String ruleId, OperationCallback callback) {
        if (projectId == null || projectId.isEmpty() || ruleId == null || ruleId.isEmpty()) {
            if (callback != null) callback.onError("Invalid automation rule ID.");
            return;
        }
        db.collection("projects").document(projectId)
            .collection("automations").document(ruleId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    private void runAutomationsWithFallbackAssignment(String projectId, Task task, boolean notifyOnFallback) {
        db.collection("projects").document(projectId).collection("automations").get()
            .addOnSuccessListener(ruleSnaps -> {
                List<WorkflowRule> allRules = ruleSnaps != null ? ruleSnaps.toObjects(WorkflowRule.class) : Collections.emptyList();
                List<WorkflowRule> matched = WorkflowAutomationEngine.matchRules(
                    allRules, WorkflowAutomationEngine.TRIGGER_TASK_ASSIGNED, "");
                boolean handledAssigneeNotification = false;
                for (WorkflowRule rule : matched) {
                    if (WorkflowAutomationEngine.ACTION_NOTIFY_ASSIGNEE.equals(rule.getActionType())) {
                        handledAssigneeNotification = true;
                    }
                }
                executeMatchedAutomations(projectId, matched, task, null);

                if (notifyOnFallback && !handledAssigneeNotification
                        && task.getAssignedTo() != null && !task.getAssignedTo().isEmpty()) {
                    String currentUid = getCurrentUid();
                    if (!task.getAssignedTo().equals(currentUid)) {
                        db.collection("projects").document(projectId).get()
                            .addOnSuccessListener(projSnap -> {
                                Project proj = projSnap.toObject(Project.class);
                                String pTitle = proj != null && proj.getTitle() != null ? proj.getTitle() : "Project";
                                NotificationHelper.dispatchNotification(
                                    null,
                                    task.getAssignedTo(),
                                    "Task Assigned: " + task.getTitle(),
                                    "You were assigned a task in " + pTitle,
                                    "TASK_ASSIGNED",
                                    projectId,
                                    pTitle
                                );
                            });
                    }
                }
            });
    }

    private void runAutomations(String projectId, String triggerType, String triggerValue, Task contextTask, String milestoneTitle) {
        db.collection("projects").document(projectId).collection("automations").get()
            .addOnSuccessListener(ruleSnaps -> {
                if (ruleSnaps == null || ruleSnaps.isEmpty()) return;
                List<WorkflowRule> allRules = ruleSnaps.toObjects(WorkflowRule.class);
                List<WorkflowRule> matched = WorkflowAutomationEngine.matchRules(allRules, triggerType, triggerValue);
                if (!matched.isEmpty()) {
                    executeMatchedAutomations(projectId, matched, contextTask, milestoneTitle);
                }
            });
    }

    private void executeMatchedAutomations(String projectId, List<WorkflowRule> matched, Task contextTask, String milestoneTitle) {
        if (matched == null || matched.isEmpty()) return;
        db.collection("projects").document(projectId).get()
            .addOnSuccessListener(projSnap -> {
                Project project = projSnap.toObject(Project.class);
                String projectTitle = project != null && project.getTitle() != null ? project.getTitle() : "Project";
                String leadId = project != null ? project.getLeadId() : "";
                String subjectName = contextTask != null && contextTask.getTitle() != null
                        ? contextTask.getTitle()
                        : (milestoneTitle != null ? milestoneTitle : "Workspace item");

                for (WorkflowRule rule : matched) {
                    String action = rule.getActionType();
                    String val = rule.getActionValue();

                    if (WorkflowAutomationEngine.ACTION_NOTIFY_ASSIGNEE.equals(action)) {
                        if (contextTask != null && contextTask.getAssignedTo() != null && !contextTask.getAssignedTo().isEmpty()) {
                            NotificationHelper.dispatchNotification(
                                null,
                                contextTask.getAssignedTo(),
                                "Task Assigned: " + subjectName,
                                "Assigned to you in " + projectTitle,
                                "TASK_ASSIGNED",
                                projectId,
                                projectTitle
                            );
                        }
                    } else if (WorkflowAutomationEngine.ACTION_NOTIFY_LEAD.equals(action)) {
                        if (leadId != null && !leadId.isEmpty()) {
                            NotificationHelper.dispatchNotification(
                                null,
                                leadId,
                                "Automation: " + rule.getName(),
                                subjectName + " triggered \"" + rule.getName() + "\" in " + projectTitle,
                                "AUTOMATION",
                                projectId,
                                projectTitle
                            );
                        }
                    } else if (WorkflowAutomationEngine.ACTION_POST_CHAT_UPDATE.equals(action)) {
                        String suffix = (val != null && !val.isEmpty()) ? " — " + val : "";
                        ChatMessage botMsg = new ChatMessage(
                            null,
                            projectId,
                            "automation_bot",
                            "Workflow Bot",
                            "",
                            "[" + rule.getName() + "] " + subjectName + suffix,
                            "AUTOMATION",
                            System.currentTimeMillis()
                        );
                        sendChatMessage(projectId, botMsg, projectTitle, null);
                    } else if (WorkflowAutomationEngine.ACTION_SET_TASK_STATUS.equals(action)) {
                        if (contextTask != null && contextTask.getTaskId() != null && val != null && !val.isEmpty()
                                && !val.equalsIgnoreCase(contextTask.getStatus())) {
                            updateTaskStatus(projectId, contextTask.getTaskId(), val, true);
                        }
                    } else if (WorkflowAutomationEngine.ACTION_SET_TASK_PHASE.equals(action)) {
                        if (contextTask != null && contextTask.getTaskId() != null && val != null && !val.isEmpty()) {
                            db.collection("projects").document(projectId)
                                .collection("tasks").document(contextTask.getTaskId())
                                .update("phase", val);
                        }
                    }
                }
            });
    }

    public LiveData<List<AppNotification>> getInAppNotifications(String userId) {
        MutableLiveData<List<AppNotification>> notificationsLiveData = new MutableLiveData<>();
        ListenerRegistration reg = db.collection("users").document(userId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    List<AppNotification> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        AppNotification n = doc.toObject(AppNotification.class);
                        if (n != null) {
                            if (n.getNotificationId() == null || n.getNotificationId().isEmpty()) {
                                n.setNotificationId(doc.getId());
                            }
                            list.add(n);
                        }
                    }
                    notificationsLiveData.setValue(list);
                }
            });
        trackListener(reg);
        return notificationsLiveData;
    }

    public void markNotificationAsRead(String userId, String notificationId) {
        if (userId == null || userId.isEmpty() || notificationId == null || notificationId.isEmpty()) return;
        db.collection("users").document(userId)
            .collection("notifications").document(notificationId)
            .update("read", true);
    }

    public void clearAllNotifications(String userId) {
        db.collection("users").document(userId)
            .collection("notifications").get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot != null) {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                }
            });
    }

    public void uploadProjectFile(String projectId, Uri fileUri, String fileName,
                                  String uploadedByName, StorageUploadCallback callback) {
        uploadProjectFile(projectId, fileUri, fileName, getCurrentUid(), uploadedByName, "MEMBERS_ONLY", 0L, callback);
    }

    public void uploadProjectFile(String projectId, Uri fileUri, String fileName,
                                  String uploadedById, String uploadedByName, String accessLevel,
                                  long fileSizeBytes, StorageUploadCallback callback) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        String safeName = fileName != null ? fileName.replaceAll("[^a-zA-Z0-9._-]", "_") : "file";
        String uniqueName = System.currentTimeMillis() + "_" + safeName;
        String storagePath = "project_files/" + projectId + "/" + uniqueName;
        StorageReference storageRef = storage.getReference().child(storagePath);

        StorageMetadata metadata = new StorageMetadata.Builder()
            .setCustomMetadata("uploadedBy", uploadedById != null ? uploadedById : "")
            .setCustomMetadata("accessLevel", accessLevel != null ? accessLevel : "MEMBERS_ONLY")
            .build();

        UploadTask uploadTask = storageRef.putFile(fileUri, metadata);
        uploadTask.addOnProgressListener(snapshot -> {
            if (snapshot.getTotalByteCount() > 0) {
                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                if (callback != null) callback.onProgress((int) progress);
            }
        }).addOnSuccessListener(taskSnapshot -> {
            long actualSize = taskSnapshot.getTotalByteCount() > 0 ? taskSnapshot.getTotalByteCount() : fileSizeBytes;
            storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                String url = downloadUri.toString();
                String type = "DOCUMENT";
                String lower = fileName != null ? fileName.toLowerCase() : "";
                if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith(".gif")) {
                    type = "IMAGE";
                }
                FileReference fileRef = new FileReference(null, projectId, fileName, url, type, uploadedByName, System.currentTimeMillis());
                fileRef.setUploadedById(uploadedById);
                fileRef.setStoragePath(storagePath);
                fileRef.setFileSizeBytes(actualSize);
                fileRef.setAccessLevel(accessLevel != null ? accessLevel : "MEMBERS_ONLY");

                addFileReference(projectId, fileRef, new OperationCallback() {
                    @Override
                    public void onSuccess() {
                        if (callback != null) callback.onSuccess(fileRef);
                    }
                    @Override
                    public void onError(String message) {
                        if (callback != null) callback.onError(message);
                    }
                });
            }).addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError(e.getMessage());
        });
    }

    public void updateProjectLocation(String projectId, String venue, double lat, double lng, OperationCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("venueLocation", venue != null ? venue : "");
        updates.put("latitude", lat);
        updates.put("longitude", lng);

        db.collection("projects").document(projectId).update(updates)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public void saveUserFcmToken(String userId, String token) {
        if (userId != null && !userId.isEmpty() && token != null) {
            db.collection("users").document(userId)
                .update("fcmToken", token);
        }
    }

    private void recalculateProgress(String projectId) {
        db.collection("projects").document(projectId).collection("tasks").get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot == null || querySnapshot.isEmpty()) {
                    db.collection("projects").document(projectId).update("progressPercentage", 0);
                    return;
                }
                int total = querySnapshot.size();
                int done = 0;
                for (Task t : querySnapshot.toObjects(Task.class)) {
                    if ("DONE".equalsIgnoreCase(t.getStatus())) {
                        done++;
                    }
                }
                int percentage = (int) (((double) done / total) * 100);
                db.collection("projects").document(projectId).update("progressPercentage", percentage);
            });
    }
}
