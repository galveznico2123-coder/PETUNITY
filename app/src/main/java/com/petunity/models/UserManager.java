package com.petunity.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;

public class UserManager {
    private static UserManager instance;
    
    // LiveData for all profile fields to ensure real-time UI updates
    private final MutableLiveData<String> name = new MutableLiveData<>("");
    private final MutableLiveData<String> email = new MutableLiveData<>("");
    private final MutableLiveData<String> phone = new MutableLiveData<>("");
    private final MutableLiveData<String> location = new MutableLiveData<>("");
    private final MutableLiveData<String> bio = new MutableLiveData<>("");
    private final MutableLiveData<String> profileImageUrl = new MutableLiveData<>("");
    private final MutableLiveData<String> membershipType = new MutableLiveData<>("Citizen Member");
    private final MutableLiveData<Double> petsHelped = new MutableLiveData<>(0.0);
    
    private final List<PetProfile> myPets = new ArrayList<>();

    private UserManager() {}

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    // Getters for LiveData (to be observed by Fragments/Activities)
    public LiveData<String> getNameLiveData() { return name; }
    public LiveData<String> getUserNameLiveData() { return name; } // Added to fix "cannot find symbol" errors
    public LiveData<String> getEmailLiveData() { return email; }
    public LiveData<String> getPhoneLiveData() { return phone; }
    public LiveData<String> getLocationLiveData() { return location; }
    public LiveData<String> getBioLiveData() { return bio; }
    public LiveData<String> getProfileImageLiveData() { return profileImageUrl; }
    public LiveData<String> getMembershipTypeLiveData() { return membershipType; }
    public LiveData<Double> getPetsHelpedLiveData() { return petsHelped; }

    // Regular Getters (for backward compatibility or one-time checks)
    public String getName() { return name.getValue(); }
    public String getEmail() { return email.getValue(); }
    public String getPhone() { return phone.getValue(); }
    public String getLocation() { return location.getValue(); }
    public String getBio() { return bio.getValue(); }
    public String getProfileImageUrl() { return profileImageUrl.getValue(); }
    public String getMembershipType() { return membershipType.getValue(); }
    public double getPetsHelped() { return petsHelped.getValue() != null ? petsHelped.getValue() : 0.0; }

    // Setters (using postValue for thread-safe updates)
    public void setName(String val) { name.postValue(val != null ? val : ""); }
    public void setEmail(String val) { email.postValue(val != null ? val : ""); }
    public void setPhone(String val) { phone.postValue(val != null ? val : ""); }
    public void setLocation(String val) { location.postValue(val != null ? val : ""); }
    public void setBio(String val) { bio.postValue(val != null ? val : ""); }
    public void setProfileImageUrl(String val) { profileImageUrl.postValue(val != null ? val : ""); }
    public void setMembershipType(String val) { membershipType.postValue(val != null ? val : "Citizen Member"); }
    public void setPetsHelped(double val) { petsHelped.postValue(val); }
    
    public void addPetsHelped(double amount) { 
        Double current = petsHelped.getValue();
        setPetsHelped((current != null ? current : 0.0) + amount);
    }

    public String getMembershipLevelName() {
        double helped = getPetsHelped();
        int count = (int) Math.ceil(helped);
        if (count <= 5) return "Pet Starter 🐣";
        if (count <= 15) return "Pet Buddy 🐶";
        if (count <= 30) return "Pet Guardian 🛡️";
        if (count <= 50) return "Pet Champion 🏆";
        return "Pet Hero 🦸‍♀️🦸";
    }

    public List<PetProfile> getMyPets() { return myPets; }
    public void addPet(PetProfile pet) { myPets.add(pet); }

    public boolean isRescuer() {
        String type = membershipType.getValue();
        return "Rescuer Member".equals(type) || "rescuer".equalsIgnoreCase(type);
    }

    public void clear() {
        setName("");
        setEmail("");
        setPhone("");
        setLocation("");
        setBio("");
        setProfileImageUrl("");
        setMembershipType("Citizen Member");
        setPetsHelped(0.0);
        myPets.clear();
    }
}
