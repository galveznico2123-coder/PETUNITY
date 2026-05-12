package com.petunity.models;

import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

public class UserManager {
    private static UserManager instance;
    private String name = "Demo User";
    private String membershipType = "Citizen Member";
    private double petsHelped = 0.0;
    private Uri profileImageUri;
    private List<PetProfile> myPets = new ArrayList<>();

    private UserManager() {
        // Updated mock pet with Phase 1 attributes
        PetProfile mockPet = new PetProfile("Max", "Golden Retriever", 3, "Large");
        mockPet.getVibeTags().add("High Energy");
        mockPet.getVibeTags().add("Playful");
        mockPet.setVaccinated(true);
        myPets.add(mockPet);
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMembershipType() { return membershipType; }
    public void setMembershipType(String membershipType) { this.membershipType = membershipType; }

    public double getPetsHelped() { return petsHelped; }
    public void setPetsHelped(double petsHelped) { this.petsHelped = petsHelped; }
    
    public void addPetsHelped(double amount) {
        this.petsHelped += amount;
    }

    public String getMembershipLevelName() {
        int count = (int) Math.ceil(petsHelped);
        if (count <= 5) return "Pet Starter 🐣";
        if (count <= 15) return "Pet Buddy 🐶";
        if (count <= 30) return "Pet Guardian 🛡️";
        if (count <= 50) return "Pet Champion 🏆";
        return "Pet Hero 🦸‍♀️🦸";
    }

    public Uri getProfileImageUri() { return profileImageUri; }
    public void setProfileImageUri(Uri uri) { this.profileImageUri = uri; }

    public List<PetProfile> getMyPets() { return myPets; }
    public void addPet(PetProfile pet) { myPets.add(pet); }

    public boolean isRescuer() {
        return "Rescuer Member".equals(membershipType);
    }
}
