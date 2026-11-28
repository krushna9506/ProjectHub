package com.projecthub.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.projecthub.models.User;
import com.projecthub.utils.CampusVerifier;

import java.util.ArrayList;
import java.util.List;

public class AuthRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration usersListener;

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    public FirebaseUser getCurrentFirebaseUser() {
        return auth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public void registerUser(String email, String password, String name, String department,
                             String rollNumber, AuthCallback callback) {
        registerUser(email, password, name, department, rollNumber, "STUDENT", new ArrayList<>(), callback);
    }

    public void registerUser(String email, String password, String name, String department,
                             String rollNumber, String role, List<String> skills, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser fbUser = authResult.getUser();
                if (fbUser == null) {
                    callback.onError("Registration succeeded but user session is missing.");
                    return;
                }
                String uid = fbUser.getUid();

                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();
                fbUser.updateProfile(profileUpdates);
                fbUser.sendEmailVerification();

                User user = new User(uid, name, email, department, rollNumber);
                user.setRole(role != null && !role.isEmpty() ? role : "STUDENT");
                user.setSkills(skills != null ? skills : new ArrayList<>());
                String domain = CampusVerifier.extractDomain(email);
                user.setCollegeDomain(domain);
                user.setCampusVerified(CampusVerifier.isCampusVerified(fbUser.isEmailVerified(), email));

                db.collection("users").document(uid).set(user)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(user))
                    .addOnFailureListener(e -> callback.onError("Failed to save user profile: " + e.getMessage()));
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void loginUser(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser fbUser = authResult.getUser();
                if (fbUser == null) {
                    callback.onError("Login failed.");
                    return;
                }
                String uid = fbUser.getUid();
                getUserProfile(uid, new AuthCallback() {
                    @Override
                    public void onSuccess(User user) {
                        boolean verified = CampusVerifier.isCampusVerified(fbUser.isEmailVerified(), fbUser.getEmail());
                        if (verified != user.isCampusVerified()) {
                            user.setCampusVerified(verified);
                            db.collection("users").document(uid).update("campusVerified", verified);
                        }
                        callback.onSuccess(user);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getUserProfile(String uid, AuthCallback callback) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        if (user.getUserId() == null || user.getUserId().isEmpty()) {
                            user.setUserId(documentSnapshot.getId());
                        }
                        callback.onSuccess(user);
                    } else {
                        callback.onError("Failed to parse user profile.");
                    }
                } else {
                    callback.onError("User profile does not exist.");
                }
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateUserProfile(User user, AuthCallback callback) {
        if (user == null || user.getUserId() == null || user.getUserId().isEmpty()) {
            if (callback != null) callback.onError("Invalid user profile.");
            return;
        }
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser != null && user.getName() != null && !user.getName().isEmpty()) {
            fbUser.updateProfile(new UserProfileChangeRequest.Builder()
                .setDisplayName(user.getName())
                .build());
        }
        String domain = CampusVerifier.extractDomain(user.getEmail());
        if (user.getCollegeDomain() == null || user.getCollegeDomain().isEmpty()) {
            user.setCollegeDomain(domain);
        }
        if (fbUser != null && user.getUserId().equals(fbUser.getUid())) {
            user.setCampusVerified(CampusVerifier.isCampusVerified(fbUser.isEmailVerified(), fbUser.getEmail()));
        }

        db.collection("users").document(user.getUserId()).set(user)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) callback.onSuccess(user);
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public void sendCampusVerificationEmail(AuthCallback callback) {
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser == null) {
            if (callback != null) callback.onError("Not signed in.");
            return;
        }
        fbUser.sendEmailVerification()
            .addOnSuccessListener(aVoid -> getUserProfile(fbUser.getUid(), callback))
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError(e.getMessage());
            });
    }

    public void syncCampusVerificationStatus(AuthCallback callback) {
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser == null) {
            if (callback != null) callback.onError("Not signed in.");
            return;
        }
        fbUser.reload().addOnSuccessListener(aVoid -> {
            boolean verified = CampusVerifier.isCampusVerified(fbUser.isEmailVerified(), fbUser.getEmail());
            String domain = CampusVerifier.extractDomain(fbUser.getEmail());
            db.collection("users").document(fbUser.getUid())
                .update("campusVerified", verified, "collegeDomain", domain)
                .addOnSuccessListener(unused -> getUserProfile(fbUser.getUid(), callback))
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError(e.getMessage());
        });
    }

    public LiveData<List<User>> getAllUsers() {
        MutableLiveData<List<User>> usersLiveData = new MutableLiveData<>();
        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }
        usersListener = db.collection("users").addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) {
                usersLiveData.setValue(new ArrayList<>());
                return;
            }
            List<User> list = new ArrayList<>();
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                User u = doc.toObject(User.class);
                if (u != null) {
                    if (u.getUserId() == null || u.getUserId().isEmpty()) {
                        u.setUserId(doc.getId());
                    }
                    list.add(u);
                }
            }
            usersLiveData.setValue(list);
        });
        return usersLiveData;
    }

    public void clearListeners() {
        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }
    }

    public void logout() {
        clearListeners();
        auth.signOut();
    }
}
