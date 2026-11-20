package com.projecthub.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.projecthub.models.FileReference;
import com.projecthub.models.Milestone;
import com.projecthub.models.Task;
import com.projecthub.repository.ProjectRepository;

import java.util.List;

public class ProjectDetailsViewModel extends ViewModel {
    private final ProjectRepository repository = new ProjectRepository();
    private LiveData<List<Task>> tasks;
    private LiveData<List<Milestone>> milestones;
    private LiveData<List<FileReference>> files;
    private final MutableLiveData<String> actionMessage = new MutableLiveData<>();

    public LiveData<String> getActionMessage() { return actionMessage; }

    public void init(String projectId) {
        if (tasks == null) {
            tasks = repository.getProjectTasks(projectId);
            milestones = repository.getProjectMilestones(projectId);
            files = repository.getProjectFiles(projectId);
        }
    }

    public LiveData<List<Task>> getTasks() { return tasks; }
    public LiveData<List<Milestone>> getMilestones() { return milestones; }
    public LiveData<List<FileReference>> getFiles() { return files; }

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

    public void updateTaskStatus(String projectId, String taskId, String newStatus) {
        repository.updateTaskStatus(projectId, taskId, newStatus);
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
}
