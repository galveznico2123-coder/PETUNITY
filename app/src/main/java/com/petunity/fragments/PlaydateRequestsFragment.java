package com.petunity.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.petunity.R;
import com.petunity.activities.ChatActivity;
import com.petunity.adapters.PlaydateRequestsAdapter;
import com.petunity.models.PlaydateRequest;
import com.petunity.models.Post;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlaydateRequestsFragment extends Fragment {
    private RecyclerView recyclerView;
    private PlaydateRequestsAdapter adapter;
    private List<PlaydateRequest> requestsList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playdate_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        recyclerView = view.findViewById(R.id.requestsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new PlaydateRequestsAdapter(requestsList, mAuth.getUid(), new PlaydateRequestsAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(PlaydateRequest request) {
                updateRequestStatus(request, "Accepted");
                showMatchSuccessDialog(request);
            }

            @Override
            public void onDecline(PlaydateRequest request) {
                updateRequestStatus(request, "Declined");
            }

            @Override
            public void onChat(PlaydateRequest request) {
                openDirectChat(request);
            }

            @Override
            public void onComplete(PlaydateRequest request) {
                completePlaydate(request);
            }
        });
        
        recyclerView.setAdapter(adapter);
        view.findViewById(R.id.backButton).setOnClickListener(v -> getParentFragmentManager().popBackStack());
        
        loadRequests();
    }

    private void showMatchSuccessDialog(PlaydateRequest request) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_playdate_request, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(dialogView).create();
        
        TextView title = dialogView.findViewById(R.id.dialogTitle);
        title.setText("It's a Match! 🐾");
        
        if (dialogView.findViewById(R.id.timeInput) != null) dialogView.findViewById(R.id.timeInput).setVisibility(View.GONE);
        if (dialogView.findViewById(R.id.locationInput) != null) dialogView.findViewById(R.id.locationInput).setVisibility(View.GONE);
        
        TextView message = new TextView(getContext());
        message.setText("You and " + (request.getReceiverId().equals(mAuth.getUid()) ? request.getSenderPetName() : request.getReceiverPetName()) + " are going on a playdate! Start a chat to finalize details.");
        message.setPadding(60, 20, 60, 20);
        ((ViewGroup)dialogView.findViewById(R.id.dialogTitle).getParent()).addView(message, 1);

        View sendBtn = dialogView.findViewById(R.id.sendRequestButton);
        if (sendBtn instanceof com.google.android.material.button.MaterialButton) {
            ((com.google.android.material.button.MaterialButton)sendBtn).setText("Chat Now");
            sendBtn.setOnClickListener(v -> {
                dialog.dismiss();
                openDirectChat(request);
            });
        }
        
        dialog.show();
    }

    private void openDirectChat(PlaydateRequest request) {
        String otherUserId = request.getSenderId().equals(mAuth.getUid()) ? request.getReceiverId() : request.getSenderId();
        String otherPetName = request.getSenderId().equals(mAuth.getUid()) ? request.getReceiverPetName() : request.getSenderPetName();
        
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra("other_user_id", otherUserId);
        intent.putExtra("user_name", otherPetName + "'s Owner");
        startActivity(intent);
    }

    private void completePlaydate(PlaydateRequest request) {
        db.collection("playdate_requests").document(request.getId())
                .update("status", "Done")
                .addOnSuccessListener(aVoid -> {
                    addToReportsLog(request);
                    Toast.makeText(getContext(), "Playdate marked as Done! Added to your private reports log.", Toast.LENGTH_LONG).show();
                });
    }

    private void addToReportsLog(PlaydateRequest request) {
        String myPet = request.getReceiverId().equals(mAuth.getUid()) ? request.getReceiverPetName() : request.getSenderPetName();
        String otherPet = request.getReceiverId().equals(mAuth.getUid()) ? request.getSenderPetName() : request.getReceiverPetName();
        
        Post report = new Post();
        report.setUserId(mAuth.getUid());
        report.setUserName(com.petunity.models.UserManager.getInstance().getName());
        report.setTitle("Playdate Completed!");
        report.setContent("Successfully completed a playdate with " + otherPet + "! " + myPet + " had a great time at " + request.getSuggestedLocation() + ".");
        report.setTimestamp(Timestamp.now());
        report.setUrgent(false);
        report.setPrivate(true); 
        report.setParticipants(Arrays.asList(request.getSenderId(), request.getReceiverId()));

        db.collection("posts").add(report);
    }

    private void loadRequests() {
        String uid = mAuth.getUid();
        db.collection("playdate_requests")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    requestsList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            PlaydateRequest req = doc.toObject(PlaydateRequest.class);
                            req.setId(doc.getId());
                            if (req.getSenderId().equals(uid) || req.getReceiverId().equals(uid)) {
                                requestsList.add(req);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void updateRequestStatus(PlaydateRequest request, String newStatus) {
        db.collection("playdate_requests").document(request.getId())
                .update("status", newStatus);
    }
}
