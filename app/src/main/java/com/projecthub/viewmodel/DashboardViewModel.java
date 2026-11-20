package com.projecthub.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.projecthub.models.Project;
import com.projecthub.repository.ProjectRepository;

import java.util.List;

public class DashboardViewModel extends ViewModel {
    private final ProjectRepository repository = new ProjectRepository();
    private LiveData<List<Project>> projects;
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

    public void createProject(Project project) {
        isLoading.setValue(true);
        repository.createProject(project, new ProjectRepository.OperationCallback() {
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
}
