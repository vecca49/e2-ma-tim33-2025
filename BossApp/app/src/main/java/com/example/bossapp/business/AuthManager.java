package com.example.bossapp.business;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.bossapp.data.local.SharedPrefsManager;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.UserRepository;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private FirebaseAuth auth;
    private UserRepository userRepository;
    private SharedPrefsManager prefsManager;

    public AuthManager(Context context) {
        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        prefsManager = new SharedPrefsManager(context);
        Log.d(TAG, "AuthManager inicijalizovan");
    }

    public interface OnRegistrationListener {
        void onSuccess();
        void onUsernameExists();
        void onError(String message);
    }

    public interface OnLoginListener {
        void onSuccess(User user);
        void onEmailNotVerified();
        void onError(String message);
    }

    public void registerUser(String email, String password, String username,
                             int avatarIndex, OnRegistrationListener listener) {

        Log.d(TAG, "Početak registracije za: " + email);

        // Prvo proveri da li username već postoji
        userRepository.checkUsernameExists(username, new UserRepository.OnUserCheckListener() {
            @Override
            public void onResult(boolean exists) {
                Log.d(TAG, "Username check rezultat: " + exists);
                if (exists) {
                    listener.onUsernameExists();
                    return;
                }

                // Username ne postoji, kreiraj Firebase Auth nalog
                createFirebaseAccount(email, password, username, avatarIndex, listener);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Greška pri proveri username-a", e);
                listener.onError("Greška pri proveri korisničkog imena: " + e.getMessage());
            }
        });
    }

    private void createFirebaseAccount(String email, String password, String username,
                                       int avatarIndex, OnRegistrationListener listener) {
        Log.d(TAG, "Kreiranje Firebase naloga");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        Log.d(TAG, "Firebase nalog kreiran: " + firebaseUser.getUid());
                        // Pošalji email verifikaciju
                        sendVerificationEmail(firebaseUser, username, avatarIndex, listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Greška pri kreiranju Firebase naloga", e);
                    String errorMsg = parseFirebaseError(e.getMessage());
                    listener.onError(errorMsg);
                });
    }

    private void sendVerificationEmail(FirebaseUser firebaseUser, String username,
                                       int avatarIndex, OnRegistrationListener listener) {
        Log.d(TAG, "Slanje verifikacionog emaila");

        firebaseUser.sendEmailVerification()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Email verifikacija poslata ✅");
                    // Kreiraj User objekat i sačuvaj u Firestore
                    User user = new User(
                            firebaseUser.getUid(),
                            firebaseUser.getEmail(),
                            username,
                            avatarIndex
                    );
                    saveUserToFirestore(user, listener);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Greška pri slanju verifikacionog emaila ❌", e);
                    firebaseUser.delete();
                    listener.onError("Greška pri slanju verifikacionog emaila: " + e.getMessage());
                });
    }


    private void saveUserToFirestore(User user, OnRegistrationListener listener) {
        Log.d(TAG, "Čuvanje korisnika u Firestore");

        userRepository.saveUser(user, new UserRepository.OnUserSaveListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Korisnik sačuvan u Firestore");
                // Odjavi korisnika dok ne verifikuje email
                auth.signOut();
                listener.onSuccess();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Greška pri čuvanju u Firestore", e);
                listener.onError("Greška pri čuvanju podataka: " + e.getMessage());
            }
        });
    }

    public void loginUser(String email, String password, OnLoginListener listener) {
        Log.d(TAG, "Pokušaj prijave za: " + email);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        // Osveži podatke o korisniku
                        firebaseUser.reload().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Proveri da li je email verifikovan
                                if (firebaseUser.isEmailVerified()) {
                                    Log.d(TAG, "Email je verifikovan ✅");
                                    // Učitaj podatke iz Firestore
                                    loadUserData(firebaseUser.getUid(), listener);
                                } else {
                                    Log.d(TAG, "Email nije verifikovan ❌");
                                    auth.signOut();
                                    listener.onEmailNotVerified();
                                }
                            } else {
                                Log.e(TAG, "Greška pri osvežavanju korisnika");
                                auth.signOut();
                                listener.onError("Greška pri proveri naloga");
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Greška pri prijavi", e);
                    String errorMsg = parseFirebaseError(e.getMessage());
                    listener.onError(errorMsg);
                });
    }

    private void loadUserData(String userId, OnLoginListener listener) {
        userRepository.getUserById(userId, new UserRepository.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG, "Podaci korisnika učitani");
                // Sačuvaj sesiju
                prefsManager.saveUserId(userId);
                listener.onSuccess(user);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Greška pri učitavanju podataka", e);
                auth.signOut();
                listener.onError("Greška pri učitavanju podataka korisnika");
            }
        });
    }

    private String parseFirebaseError(String errorMessage) {
        if (errorMessage.contains("email address is already in use")) {
            return "Email adresa je već u upotrebi";
        } else if (errorMessage.contains("password")) {
            return "Lozinka mora imati najmanje 6 karaktera";
        } else if (errorMessage.contains("email address is badly formatted")) {
            return "Neispravan format email adrese";
        }
        return "Greška pri registraciji: " + errorMessage;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }


    public boolean isUserLoggedIn() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    public void signOut() {
        auth.signOut();
        prefsManager.clearSession();
        Log.d(TAG, "Korisnik odjavljen");
    }
}