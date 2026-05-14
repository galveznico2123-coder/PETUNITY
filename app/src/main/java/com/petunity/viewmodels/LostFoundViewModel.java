package com.petunity.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.petunity.models.PetListing;

import java.util.ArrayList;
import java.util.List;

public class LostFoundViewModel extends ViewModel {
    private final MutableLiveData<List<PetListing>> lostPets = new MutableLiveData<>();
    private final MutableLiveData<List<PetListing>> foundPets = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<PetListing>> getLostPets() { return lostPets; }
    public LiveData<List<PetListing>> getFoundPets() { return foundPets; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }

    public void fetchPets(String status) {
        isLoading.setValue(true);
        db.collection("pet_listing")
                .whereEqualTo("status", status)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, e) -> {
                    isLoading.setValue(false);
                    if (e != null) {
                        error.setValue("Failed to fetch listings: " + e.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<PetListing> list = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            PetListing pet = doc.toObject(PetListing.class);
                            if (pet != null) list.add(pet);
                        }
                        if ("lost".equals(status)) lostPets.setValue(list);
                        else foundPets.setValue(list);
                    }
                });
    }
}
