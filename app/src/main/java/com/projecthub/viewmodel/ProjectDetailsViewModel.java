package com.projecthub.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.projecthub.models.ChatMessage;
import com.projecthub.models.FileReference;
import com.projecthub.models.Milestone;
import com.projecthub.models.Project;
import com.projecthub.models.Subtask;
import com.projecthub.models.Task;
import com.projecthub.models.TaskComment;
import com.projecthub.models.User;
import com.projecthub.models.WorkflowRule;
import com.projecthub.repository.ProjectRepository;

import java.util.ArrayList;
import java.util.List;

public class ProjectDetailsViewModel extends ViewModel {
    private final ProjectRepository repository = new ProjectRepository();
    private LiveData<Project> project;
    private LiveData<List<Task>> tasks;
    private LiveData<List<Milestone>> milestones;
    private final MutableLiveData<List<FileReference>> files = new MutableLiveData<>(new ArrayList<>());
    private LiveData<List<ChatMessage>> chatMessages;
    private LiveData<List<WorkflowRule>> automations;
    private final MutableLiveData<List<User>> projectMembers = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> actionMessage = new MutableLiveData<>();
    private boolean filesLoadedAsLead = false;
    private boolean filesInitialized = false;

    public LiveData<String> getActionMessage() { return actionMessage; }

    public void init(String projectId) {
        if (tasks == null && projectId != null && !projectId.isEmpty()) {
            project = repository.getProject(projectId);
            tasks = repository.getProjectTasks(projectId);
            milestones = repository.getProjectMilestones(projectId);
            chatMessages = repository.getProjectChatMessages(projectId);
            automations = repository.getProjectAutomations(projectId);
        }
    }

    public void ensureFilesLoadedForRole(String projectId, String userId, boolean isLead) {
        if (!filesInitialized || isLead != filesLoadedAsLead) {
            filesInitialized = true;
            filesLoadedAsLead = isLead;
            repository.loadProjectFilesForUser(projectId, userId, isLead, files);
        }
    }

    public LiveData<Project> getProject() { return project; }
    public LiveData<List<Task>> getTasks() { return tasks; }
    public LiveData<List<Milestone>> getMilestones() { return milestones; }
    public LiveData<List<FileReference>> getFiles() { return files; }
    public LiveData<List<ChatMessage>> getChatMessages() { return chatMessages; }
    public LiveData<List<WorkflowRule>> getAutomations() { return automations; }
    public LiveData<List<User>> getProjectMembers() { return projectMembers; }

    public void loadMembers(List<String> memberIds) {
        repository.loadProjectMembersInto(memberIds, projectMembers);
    }

    public LiveData<Task> getTask(String projectId, String taskId) {
        return repository.getTask(projectId, taskId);
    }

    public void sendChatMessage(String projectId, ChatMessage message) {
        repository.sendChatMessage(projectId, message, null, null);
    }

    public void sendChatMessage(String projectId, ChatMessage message, String projectTitle) {
        repository.sendChatMessage(projectId, message, projectTitle, null);
    }

    public void toggleChatReaction(String projectId, String messageId, String emoji, String userId) {
        repository.toggleChatReaction(projectId, messageId, emoji, userId);
    }

    public void updateProjectGithubUrl(String projectId, String githubUrl) {
        repository.updateProjectGithubUrl(projectId, githubUrl);
    }

    public void updateProjectCustomStatuses(String projectId, List<String> statuses) {
        repository.updateProjectCustomStatuses(projectId, statuses, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                actionMessage.setValue("Workflow statuses updated!");
            }

            @Override
            public void onError(String message) {
                actionMessage.setValue("Failed to update statuses: " + message);
            }
        });
    }

    public void addTask(String projectId, Task task) {
        repository.addTask(projectId, task, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                actionMessage.setValue("Task added!");
            }

            @Override
            public void onError(String message) {
                actionMessage.setValue("Failed to add task: " + message);
            }
        });
    }

    public void updateTask(String projectId, Task task, String prevAssigneeId, String prevStatus) {
        repository.updateTask(projectId, task, prevAssigneeId, prevStatus, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                actionMessage.setValue("Task updated!");
            }

            @Override
            public void onError(String message) {
                actionMessage.setValue("Failed to update task: " + message);
            }
        });
    }

    public void deleteTask(String projectId, String taskId, ProjectRepository.OperationCallback callback) {
        repository.deleteTask(projectId, taskId, callback);
    }

    public void updateTaskSubtasks(String projectId, String taskId, List<Subtask> subtasks) {
        repository.updateTaskSubtasks(projectId, taskId, subtasks, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {}

            @Override
            public void onError(String message) {
                actionMessage.setValue("Failed to update checklist: " + message);
            }
        });
    }

    public void updateTaskStatus(String projectId, String taskId, String newStatus) {
        repository.updateTaskStatus(projectId, taskId, newStatus);
    }

    public void logTaskWorkTime(String projectId, String taskId, double hours) {
        repository.logTaskWorkTime(projectId, taskId, hours, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                actionMessage.setValue("Logged " + hours + "h to task timesheet!");
            }

            @Override
            public void onError(String message) {
                actionMessage.setValue("Failed to log time: " + message);
            }
        });
    }

    public void updateMilestoneStatus(String projectId, String milestoneId, String newStatus) {
        repository.updateMilestoneStatus(projectId, milestoneId, newStatus);
    }

    public LiveData<List<TaskComment>> getTaskComments(String projectId, String taskId) {
        return repository.getTaskComments(projectId, taskId);
    }

    public void addTaskComment(String projectId, String taskId, TaskComment comment) {
        addTaskCommentWithMentions(projectId, taskId, comment, null, null, null);
    }

    public void addTaskCommentWithMentions(String projectId, String taskId, TaskComment comment,
                                           Task taskContext, String projectTitle, List<String> mentionedUserIds) {
        repository.addTaskCommentWithMentions(projectId, taskId, comment, taskContext, projectTitle, mentionedUserIds,
            new ProjectRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    actionMessage.setValue("Comment posted");
                }

                @Override
                public void onError(String message) {
                    actionMessage.setValue("Failed to post comment: " + message);
                }
            });
    }

    public void addMilestone(String projectId, Milestone milestone) {
        repository.addMilestone(projectId, milestone, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                actionMessage.setValue("Milestone added!");
            }

            @Override
            public void onError(String message) {
                actionMessage.setValue("Failed to add milestone: " + message);
            }
        });
    }

    public void addFileReference(String projectId, FileReference fileRef) {
        repository.addFileReference(projectId, fileRef, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                actionMessage.setValue("File reference added!");
            }

            @Override
            public void onError(String message) {
                actionMessage.setValue("Failed to add file: " + message);
            }
        });
    }

    public void deleteFileReference(String projectId, FileReference fileRef) {
        repository.deleteFileReference(projectId, fileRef, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                actionMessage.setValue("File deleted");
            }

            @Override
            public void onError(String message) {
                actionMessage.setValue("Failed to delete file: " + message);
            }
        });
    }

    public void uploadProjectFile(String projectId, android.net.Uri fileUri, String fileName,
                                  String uploadedByName, ProjectRepository.StorageUploadCallback callback) {
        repository.uploadProjectFile(projectId, fileUri, fileName, uploadedByName, callback);
    }

    public void uploadProjectFile(String projectId, android.net.Uri fileUri, String fileName,
                                  String uploadedById, String uploadedByName, String accessLevel,
                                  long fileSizeBytes, ProjectRepository.StorageUploadCallback callback) {
        repository.uploadProjectFile(projectId, fileUri, fileName, uploadedById, uploadedByName, accessLevel, fileSizeBytes, callback);
    }

    public void addAutomationRule(String projectId, WorkflowRule rule) {
        repository.addAutomationRule(projectId, rule, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                actionMessage.setValue("Automation rule added!");
            }

            @Override
            public void onError(String message) {
                actionMessage.setValue("Failed to add automation: " + message);
            }
        });
    }

    public void toggleAutomationRule(String projectId, String ruleId, boolean enabled) {
        repository.toggleAutomationRule(projectId, ruleId, enabled);
    }

    public void deleteAutomationRule(String projectId, String ruleId) {
        repository.deleteAutomationRule(projectId, ruleId, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                actionMessage.setValue("Automation rule deleted");
            }

            @Override
            public void onError(String message) {
                actionMessage.setValue("Failed to delete automation: " + message);
            }
        });
    }

    public void updateProjectLocation(String projectId, String venue, double lat, double lng) {
        repository.updateProjectLocation(projectId, venue, lat, lng, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                actionMessage.setValue("Meetup venue updated!");
            }

            @Override
            public void onError(String message) {
                actionMessage.setValue("Failed to update venue: " + message);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.clearListeners();
    }
}
