package com.projecthub.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.projecthub.models.User;
import com.projecthub.repository.AuthRepository;

import java.util.List;

public class AuthViewModel extends ViewModel {
    private final AuthRepository authRepository = new AuthRepository();
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private LiveData<List<User>> campusUsers;

    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public boolean isUserLoggedIn() {
        return authRepository.isUserLoggedIn();
    }

    public String getCurrentUserId() {
        return authRepository.getCurrentFirebaseUser() != null ? 
               authRepository.getCurrentFirebaseUser().getUid() : null;
    }

    public void register(String email, String password, String name, String department, String rollNumber) {
        isLoading.setValue(true);
        authRepository.registerUser(email, password, name, department, rollNumber, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.setValue(false);
                currentUser.setValue(user);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void register(String email, String password, String name, String department,
                         String rollNumber, String role, List<String> skills) {
        isLoading.setValue(true);
        authRepository.registerUser(email, password, name, department, rollNumber, role, skills, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.setValue(false);
                currentUser.setValue(user);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void login(String email, String password) {
        isLoading.setValue(true);
        authRepository.loginUser(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.setValue(false);
                currentUser.setValue(user);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void fetchCurrentUserProfile() {
        if (getCurrentUserId() != null) {
            authRepository.getUserProfile(getCurrentUserId(), new AuthRepository.AuthCallback() {
                @Override
                public void onSuccess(User user) {
                    currentUser.setValue(user);
                }

                @Override
                public void onError(String message) {
                    errorMessage.setValue(message);
                }
            });
        }
    }

    public void updateProfile(User user) {
        isLoading.setValue(true);
        authRepository.updateUserProfile(user, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User updated) {
                isLoading.setValue(false);
                currentUser.setValue(updated);
                statusMessage.setValue("Profile updated!");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void sendVerificationEmail() {
        isLoading.setValue(true);
        authRepository.sendCampusVerificationEmail(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.setValue(false);
                currentUser.setValue(user);
                statusMessage.setValue("Verification email sent! Check your campus inbox.");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void syncVerificationStatus() {
        isLoading.setValue(true);
        authRepository.syncCampusVerificationStatus(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.setValue(false);
                currentUser.setValue(user);
                statusMessage.setValue(user.isCampusVerified()
                        ? "Campus profile verified!"
                        : "Email not yet verified or domain is not an academic domain.");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public LiveData<List<User>> getCampusUsers() {
        if (campusUsers == null) {
            campusUsers = authRepository.getAllUsers();
        }
        return campusUsers;
    }

    public void logout() {
        authRepository.logout();
        currentUser.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        authRepository.clearListeners();
    }
}
