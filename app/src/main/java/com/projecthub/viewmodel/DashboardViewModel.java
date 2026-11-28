package com.projecthub.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.projecthub.models.AppNotification;
import com.projecthub.models.JoinRequest;
import com.projecthub.models.Project;
import com.projecthub.models.User;
import com.projecthub.repository.ProjectRepository;

import java.util.List;

public class DashboardViewModel extends ViewModel {
    private final ProjectRepository repository = new ProjectRepository();
    private LiveData<List<Project>> projects;
    private LiveData<List<Project>> publicProjects;
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public LiveData<List<Project>> getProjects(String userId) {
        if (projects == null) {
            projects = repository.getUserProjects(userId);
        }
        return projects;
    }

    public LiveData<List<Project>> getPublicProjects() {
        if (publicProjects == null) {
            publicProjects = repository.getPublicProjects();
        }
        return publicProjects;
    }

    public void createProject(Project project) {
        createProjectWithTemplate(project, project.getTemplateType());
    }

    public void createProjectWithTemplate(Project project, String templateType) {
        isLoading.setValue(true);
        repository.createProjectWithTemplate(project, templateType, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                statusMessage.setValue("Project created successfully!");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                statusMessage.setValue("Error: " + message);
            }
        });
    }

    public void sendTeamOrMentorInvite(User targetUser, Project project, String inviterName, String inviteRole) {
        isLoading.setValue(true);
        repository.sendTeamOrMentorInvite(targetUser, project, inviterName, inviteRole, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                statusMessage.setValue("Invitation sent to " + targetUser.getName() + "!");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                statusMessage.setValue("Error: " + message);
            }
        });
    }

    public void joinProject(String userId, String code) {
        isLoading.setValue(true);
        repository.joinProjectWithCode(userId, code, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                statusMessage.setValue("Joined project successfully!");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                statusMessage.setValue("Error: " + message);
            }
        });
    }

    public void sendJoinRequest(JoinRequest request) {
        isLoading.setValue(true);
        repository.sendJoinRequest(request, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                statusMessage.setValue("Join request sent successfully!");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                statusMessage.setValue("Error: " + message);
            }
        });
    }

    public LiveData<List<JoinRequest>> getJoinRequestsForLead(String leadId) {
        return repository.getJoinRequestsForLead(leadId);
    }

    public LiveData<List<JoinRequest>> getJoinRequestsForApplicant(String applicantId) {
        return repository.getJoinRequestsForApplicant(applicantId);
    }

    public void acceptJoinRequest(JoinRequest request) {
        isLoading.setValue(true);
        repository.acceptJoinRequest(request, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                statusMessage.setValue("Applicant accepted into project!");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                statusMessage.setValue("Error: " + message);
            }
        });
    }

    public void rejectJoinRequest(JoinRequest request) {
        isLoading.setValue(true);
        repository.rejectJoinRequest(request, new ProjectRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                statusMessage.setValue("Request rejected.");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                statusMessage.setValue("Error: " + message);
            }
        });
    }

    public LiveData<List<AppNotification>> getInAppNotifications(String userId) {
        return repository.getInAppNotifications(userId);
    }

    public void markNotificationAsRead(String userId, String notificationId) {
        repository.markNotificationAsRead(userId, notificationId);
    }

    public void clearAllNotifications(String userId) {
        repository.clearAllNotifications(userId);
    }

    public void saveFcmToken(String userId, String token) {
        repository.saveUserFcmToken(userId, token);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.clearListeners();
    }
}

