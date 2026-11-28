package com.projecthub.repository;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.projecthub.models.AppNotification;
import com.projecthub.models.ChatMessage;
import com.projecthub.models.FileReference;
import com.projecthub.models.JoinRequest;
import com.projecthub.models.Milestone;
import com.projecthub.models.Project;
import com.projecthub.models.Task;
import com.projecthub.models.TaskComment;
import com.projecthub.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OperationCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface StorageUploadCallback {
        void onProgress(int percent);
        void onSuccess(FileReference fileReference);
        void onError(String message);
    }

    public LiveData<List<Project>> getUserProjects(String userId) {
        MutableLiveData<List<Project>> projectsLiveData = new MutableLiveData<>();
        
        // Listen to projects where user is a member
        db.collection("projects")
            .whereGreaterThan("members." + userId, "")
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    projectsLiveData.setValue(new ArrayList<>());
                    return;
                }
                if (snapshots != null) {
                    List<Project> list = snapshots.toObjects(Project.class);
                    projectsLiveData.setValue(list);
                }
            });

        return projectsLiveData;
    }

    public LiveData<List<Project>> getPublicProjects() {
        MutableLiveData<List<Project>> projectsLiveData = new MutableLiveData<>();

        db.collection("projects")
            .whereEqualTo("public", true)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    projectsLiveData.setValue(new ArrayList<>());
                    return;
                }
                if (snapshots != null) {
                    List<Project> list = snapshots.toObjects(Project.class);
                    projectsLiveData.setValue(list);
                }
            });

        return projectsLiveData;
    }

    public void createProject(Project project, OperationCallback callback) {
        DocumentReference docRef = db.collection("projects").document();
        project.setProjectId(docRef.getId());

        docRef.set(project)
            .addOnSuccessListener(aVoid -> {
                // Also add to user's joined projects array
                db.collection("users").document(project.getLeadId())
                    .update("joinedProjectIds", FieldValue.arrayUnion(project.getProjectId()));
                callback.onSuccess();
            })
            .addOnFailureListener(e -> callback.onError("Failed to create project: " + e.getMessage()));
    }

    public void joinProjectWithCode(String userId, String joinCode, OperationCallback callback) {
        db.collection("projects")
            .whereEqualTo("joinCode", joinCode.trim().toUpperCase())
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot.isEmpty()) {
                    callback.onError("Invalid project code. Please check and try again.");
                    return;
                }

                DocumentReference projectRef = querySnapshot.getDocuments().get(0).getReference();
                String projectId = projectRef.getId();

                projectRef.update("members." + userId, "MEMBER")
                    .addOnSuccessListener(aVoid -> {
                        db.collection("users").document(userId)
                            .update("joinedProjectIds", FieldValue.arrayUnion(projectId));
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> callback.onError("Failed to join: " + e.getMessage()));
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
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

    public LiveData<List<JoinRequest>> getJoinRequestsForLead(String leadId) {
        MutableLiveData<List<JoinRequest>> requestsLiveData = new MutableLiveData<>();

        db.collection("join_requests")
            .whereEqualTo("leadId", leadId)
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    requestsLiveData.setValue(snapshots.toObjects(JoinRequest.class));
                }
            });

        return requestsLiveData;
    }

    public LiveData<List<JoinRequest>> getJoinRequestsForApplicant(String applicantId) {
        MutableLiveData<List<JoinRequest>> requestsLiveData = new MutableLiveData<>();

        db.collection("join_requests")
            .whereEqualTo("applicantId", applicantId)
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    requestsLiveData.setValue(snapshots.toObjects(JoinRequest.class));
                }
            });

        return requestsLiveData;
    }

    public void acceptJoinRequest(JoinRequest request, OperationCallback callback) {
        db.collection("join_requests").document(request.getRequestId())
            .update("status", "ACCEPTED")
            .addOnSuccessListener(aVoid -> {
                db.collection("projects").document(request.getProjectId())
                    .update("members." + request.getApplicantId(), "MEMBER")
                    .addOnSuccessListener(aVoid2 -> {
                        db.collection("users").document(request.getApplicantId())
                            .update("joinedProjectIds", FieldValue.arrayUnion(request.getProjectId()));
                        NotificationHelper.dispatchNotification(null, request.getApplicantId(),
                            "Join Request Accepted 🎉",
                            "You are now a member of " + (request.getProjectTitle() != null ? request.getProjectTitle() : "the project"),
                            "JOIN_ACCEPTED", request.getProjectId(), request.getProjectTitle());
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void rejectJoinRequest(JoinRequest request, OperationCallback callback) {
        db.collection("join_requests").document(request.getRequestId())
            .update("status", "REJECTED")
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public LiveData<Project> getProject(String projectId) {
        MutableLiveData<Project> projectLiveData = new MutableLiveData<>();
        db.collection("projects").document(projectId)
            .addSnapshotListener((snapshot, error) -> {
                if (snapshot != null && snapshot.exists()) {
                    projectLiveData.setValue(snapshot.toObject(Project.class));
                }
            });
        return projectLiveData;
    }

    public LiveData<List<Task>> getProjectTasks(String projectId) {
        MutableLiveData<List<Task>> tasksLiveData = new MutableLiveData<>();

        db.collection("projects").document(projectId).collection("tasks")
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    tasksLiveData.setValue(snapshots.toObjects(Task.class));
                }
            });

        return tasksLiveData;
    }

    public void addTask(String projectId, Task task, OperationCallback callback) {
        DocumentReference docRef = db.collection("projects").document(projectId)
            .collection("tasks").document();
        task.setTaskId(docRef.getId());
        task.setProjectId(projectId);
        if (task.getTaskKey() == null || task.getTaskKey().isEmpty()) {
            task.setTaskKey("TS-" + (100 + (int)(Math.random() * 900)));
        }

        docRef.set(task)
            .addOnSuccessListener(aVoid -> {
                recalculateProgress(projectId);
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
        db.collection("projects").document(projectId)
            .collection("tasks").document(taskId)
            .update("status", newStatus)
            .addOnSuccessListener(aVoid -> recalculateProgress(projectId));
    }

    public LiveData<List<TaskComment>> getTaskComments(String projectId, String taskId) {
        MutableLiveData<List<TaskComment>> commentsLiveData = new MutableLiveData<>();

        db.collection("projects").document(projectId)
            .collection("tasks").document(taskId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    commentsLiveData.setValue(snapshots.toObjects(TaskComment.class));
                }
            });

        return commentsLiveData;
    }

    public void addTaskComment(String projectId, String taskId, TaskComment comment, OperationCallback callback) {
        DocumentReference docRef = db.collection("projects").document(projectId)
            .collection("tasks").document(taskId)
            .collection("comments").document();
        comment.setCommentId(docRef.getId());
        comment.setTaskId(taskId);
        comment.setProjectId(projectId);

        docRef.set(comment)
            .addOnSuccessListener(aVoid -> {
                // Increment commentsCount on task
                db.collection("projects").document(projectId)
                    .collection("tasks").document(taskId)
                    .update("commentsCount", FieldValue.increment(1));
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public LiveData<List<Milestone>> getProjectMilestones(String projectId) {
        MutableLiveData<List<Milestone>> milestonesLiveData = new MutableLiveData<>();

        db.collection("projects").document(projectId).collection("milestones")
            .orderBy("dueDate", Query.Direction.ASCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    milestonesLiveData.setValue(snapshots.toObjects(Milestone.class));
                }
            });

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
            .update("status", newStatus);
    }

    public LiveData<List<FileReference>> getProjectFiles(String projectId) {
        MutableLiveData<List<FileReference>> filesLiveData = new MutableLiveData<>();

        db.collection("projects").document(projectId).collection("files")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    filesLiveData.setValue(snapshots.toObjects(FileReference.class));
                }
            });

        return filesLiveData;
    }

    public void addFileReference(String projectId, FileReference fileRef, OperationCallback callback) {
        DocumentReference docRef = db.collection("projects").document(projectId)
            .collection("files").document();
        fileRef.setFileId(docRef.getId());
        fileRef.setProjectId(projectId);

        docRef.set(fileRef)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public LiveData<List<ChatMessage>> getProjectChatMessages(String projectId) {
        MutableLiveData<List<ChatMessage>> messagesLiveData = new MutableLiveData<>();

        db.collection("projects").document(projectId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    messagesLiveData.setValue(snapshots.toObjects(ChatMessage.class));
                }
            });

        return messagesLiveData;
    }

    public void sendChatMessage(String projectId, ChatMessage message, OperationCallback callback) {
        DocumentReference docRef = db.collection("projects").document(projectId)
            .collection("messages").document();
        message.setMessageId(docRef.getId());
        message.setProjectId(projectId);

        docRef.set(message)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public void updateProjectGithubUrl(String projectId, String githubUrl) {
        db.collection("projects").document(projectId)
            .update("githubRepoUrl", githubUrl);
    }

    public LiveData<List<AppNotification>> getInAppNotifications(String userId) {
        MutableLiveData<List<AppNotification>> notificationsLiveData = new MutableLiveData<>();
        db.collection("users").document(userId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (snapshots != null) {
                    notificationsLiveData.setValue(snapshots.toObjects(AppNotification.class));
                }
            });
        return notificationsLiveData;
    }

    public void markNotificationAsRead(String userId, String notificationId) {
        db.collection("users").document(userId)
            .collection("notifications").document(notificationId)
            .update("read", true);
    }

    public void clearAllNotifications(String userId) {
        db.collection("users").document(userId)
            .collection("notifications").get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                }
            });
    }

    public void uploadProjectFile(String projectId, Uri fileUri, String fileName, 
                                  String uploadedByName, StorageUploadCallback callback) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        String uniqueName = System.currentTimeMillis() + "_" + (fileName != null ? fileName : "file");
        StorageReference storageRef = storage.getReference()
            .child("project_files")
            .child(projectId)
            .child(uniqueName);

        UploadTask uploadTask = storageRef.putFile(fileUri);
        uploadTask.addOnProgressListener(snapshot -> {
            if (snapshot.getTotalByteCount() > 0) {
                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                if (callback != null) callback.onProgress((int) progress);
            }
        }).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                String url = downloadUri.toString();
                String type = "DOCUMENT";
                String lower = fileName != null ? fileName.toLowerCase() : "";
                if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp")) {
                    type = "IMAGE";
                }
                FileReference fileRef = new FileReference(null, projectId, fileName, url, type, uploadedByName, System.currentTimeMillis());
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
