package com.projecthub.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.projecthub.models.User;

public class AuthRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                String uid = authResult.getUser().getUid();
                User user = new User(uid, name, email, department, rollNumber);
                db.collection("users").document(uid).set(user)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(user))
                    .addOnFailureListener(e -> callback.onError("Failed to save user profile: " + e.getMessage()));
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void loginUser(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                String uid = authResult.getUser().getUid();
                getUserProfile(uid, callback);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getUserProfile(String uid, AuthCallback callback) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    callback.onSuccess(user);
                } else {
                    callback.onError("User profile does not exist.");
                }
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void logout() {
        auth.signOut();
    }
}
