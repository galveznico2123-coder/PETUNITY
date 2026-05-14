package com.petunity.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.petunity.R;
import com.petunity.databinding.ItemPetListingBinding;
import com.petunity.models.PetListing;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class LostFoundAdapter extends RecyclerView.Adapter<LostFoundAdapter.PetViewHolder> {
    private final List<PetListing> pets;
    private final OnContactClickListener listener;
    private final String currentUserId;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnContactClickListener {
        void onContactClick(PetListing pet);
    }

    public LostFoundAdapter(List<PetListing> pets, OnContactClickListener listener) {
        this.pets = pets;
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPetListingBinding binding = ItemPetListingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PetViewHolder(binding, listener, currentUserId);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        holder.bind(pets.get(position));
    }

    @Override
    public int getItemCount() {
        return pets.size();
    }

    static class PetViewHolder extends RecyclerView.ViewHolder {
        private final ItemPetListingBinding binding;
        private final OnContactClickListener listener;
        private final String currentUserId;

        public PetViewHolder(@NonNull ItemPetListingBinding binding, OnContactClickListener listener, String currentUserId) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.currentUserId = currentUserId;
        }

        void bind(PetListing pet) {
            binding.petNameText.setText(pet.getName());
            binding.petBreedText.setText(pet.getBreed());
            binding.petLocationText.setText(pet.getLocation());
            
            // Display actual date instead of "just now"
            if (pet.getTimestamp() != null) {
                binding.petTimeText.setText(dateFormat.format(pet.getTimestamp().toDate()));
            } else {
                binding.petTimeText.setText(pet.getTimeAgo());
            }

            // Load image via Glide from Cloudinary URL
            if (pet.getImageUrl() != null && !pet.getImageUrl().isEmpty()) {
                Glide.with(binding.petImageView.getContext())
                        .load(pet.getImageUrl())
                        .placeholder(R.drawable.ic_logo)
                        .error(R.drawable.ic_logo)
                        .into(binding.petImageView);
            } else {
                binding.petImageView.setImageResource(R.drawable.ic_logo);
            }

            // Logic to prevent contacting yourself
            if (pet.getUserId() != null && pet.getUserId().equals(currentUserId)) {
                binding.contactButton.setText("My Pet");
                binding.contactButton.setEnabled(false);
                binding.contactButton.setAlpha(0.5f);
            } else {
                binding.contactButton.setText("Contact Owner");
                binding.contactButton.setEnabled(true);
                binding.contactButton.setAlpha(1.0f);
                binding.contactButton.setOnClickListener(v -> listener.onContactClick(pet));
            }
        }
    }
}
