package com.projecthub.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.projecthub.models.User;
import com.projecthub.repository.AuthRepository;

public class AuthViewModel extends ViewModel {
    private final AuthRepository authRepository = new AuthRepository();
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
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

    public void logout() {
        authRepository.logout();
        currentUser.setValue(null);
    }
}
