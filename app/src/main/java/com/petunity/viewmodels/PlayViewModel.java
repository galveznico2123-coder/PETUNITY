package com.petunity.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.petunity.models.PetProfile;

import java.util.ArrayList;
import java.util.List;

public class PlayViewModel extends ViewModel {
    private final MutableLiveData<List<PetProfile>> potentialMatches = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<PetProfile> myPet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public LiveData<List<PetProfile>> getPotentialMatches() { return potentialMatches; }
    public LiveData<PetProfile> getMyPet() { return myPet; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }

    public void loadData() {
        if (auth.getUid() == null) return;
        isLoading.setValue(true);

        db.collection("pet_profiles")
                .whereEqualTo("ownerId", auth.getUid())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        myPet.setValue(queryDocumentSnapshots.getDocuments().get(0).toObject(PetProfile.class));
                        fetchOtherPets();
                    } else {
                        isLoading.setValue(false);
                        error.setValue("No pet persona found. Please create one first!");
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    error.setValue("Failed to load your pet: " + e.getMessage());
                });
    }

    private void fetchOtherPets() {
        db.collection("pet_profiles")
                .whereNotEqualTo("ownerId", auth.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<PetProfile> matches = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        PetProfile other = doc.toObject(PetProfile.class);
                        other.setId(doc.getId());
                        matches.add(other);
                    }
                    potentialMatches.setValue(matches);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    error.setValue("Failed to fetch matches: " + e.getMessage());
                });
    }
}
