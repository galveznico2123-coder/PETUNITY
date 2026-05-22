package com.petunity.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.adapters.LostFoundAdapter;
import com.petunity.models.PetListing;

import java.util.ArrayList;
import java.util.List;

public class MyPetsActivity extends AppCompatActivity {
    private static final String TAG = "MyPetsActivity";
    private LostFoundAdapter adapter;
    private List<PetListing> myPetsList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private View loadingOverlay;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_posts); // Reusing layout since it fits our needs

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Registered Pets");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        loadingOverlay = findViewById(R.id.loadingOverlay);
        emptyText = findViewById(R.id.emptyText);
        RecyclerView recyclerView = findViewById(R.id.myPostsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Use the same adapter as FindFragment for consistency
        adapter = new LostFoundAdapter(myPetsList, pet -> {
            Toast.makeText(this, "Pet ID: " + pet.getName(), Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);

        fetchMyPets();
    }

    private void fetchMyPets() {
        String currentUserId = mAuth.getUid();
        if (currentUserId == null) return;

        setLoading(true);

        db.collection("pet_listing")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    setLoading(false);
                    
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        myPetsList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            PetListing pet = doc.toObject(PetListing.class);
                            if (pet != null) {
                                myPetsList.add(pet);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        
                        if (emptyText != null) {
                            emptyText.setText("No pets registered yet");
                            emptyText.setVisibility(myPetsList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }
                });
    }

    private void setLoading(boolean loading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }
}
