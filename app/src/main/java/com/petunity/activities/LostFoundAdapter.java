package com.petunity.activities;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.petunity.databinding.ItemPetListingBinding;
import com.petunity.models.PetListing;

import java.util.List;

public class LostFoundAdapter extends RecyclerView.Adapter<LostFoundAdapter.PetViewHolder> {
    private final List<PetListing> pets;
    private final OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(PetListing pet);
    }

    public LostFoundAdapter(List<PetListing> pets, OnContactClickListener listener) {
        this.pets = pets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPetListingBinding binding = ItemPetListingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PetViewHolder(binding, listener);
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

        public PetViewHolder(@NonNull ItemPetListingBinding binding, OnContactClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(PetListing pet) {
            binding.petNameText.setText(pet.getName());
            binding.petBreedText.setText(pet.getBreed());
            binding.petLocationText.setText(pet.getLocation());
            binding.petTimeText.setText(pet.getTimeAgo());

            binding.contactButton.setOnClickListener(v -> listener.onContactClick(pet));
        }
    }
}