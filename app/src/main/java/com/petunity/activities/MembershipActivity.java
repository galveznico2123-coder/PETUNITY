package com.petunity.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.petunity.R;
import com.petunity.models.UserManager;

import java.util.Locale;

public class MembershipActivity extends AppCompatActivity {

    private TextView membershipLevelText, petsHelpedCount, nextLevelText;
    private LinearProgressIndicator membershipProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_membership);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Membership");
        }

        membershipLevelText = findViewById(R.id.membershipLevelText);
        petsHelpedCount = findViewById(R.id.petsHelpedCount);
        membershipProgress = findViewById(R.id.membershipProgress);
        nextLevelText = findViewById(R.id.nextLevelText);

        updateUI();

        // Setup mock task actions
        findViewById(R.id.tasksContainer).setOnClickListener(v -> {
            // This is just to demonstrate interactivity
        });
        
        // Example logic for task buttons
        setupTaskButtons();
    }

    private void updateUI() {
        UserManager user = UserManager.getInstance();
        double helped = user.getPetsHelped();
        String level = user.getMembershipLevelName();

        membershipLevelText.setText(level);
        petsHelpedCount.setText(String.format(Locale.getDefault(), "%.1f Pets Helped", helped));

        // Calculate progress to next level
        int currentPoints = (int) Math.ceil(helped);
        int nextThreshold = 0;
        String nextRank = "";

        if (currentPoints <= 5) {
            nextThreshold = 6;
            nextRank = "Pet Buddy 🐶";
        } else if (currentPoints <= 15) {
            nextThreshold = 16;
            nextRank = "Pet Guardian 🛡️";
        } else if (currentPoints <= 30) {
            nextThreshold = 31;
            nextRank = "Pet Champion 🏆";
        } else if (currentPoints <= 50) {
            nextThreshold = 51;
            nextRank = "Pet Hero 🦸‍♀️🦸";
        }

        if (nextThreshold > 0) {
            int progress = (int) ((helped / nextThreshold) * 100);
            membershipProgress.setProgress(progress);
            nextLevelText.setText(String.format(Locale.getDefault(), "%d more pets to reach %s", (nextThreshold - currentPoints), nextRank));
        } else {
            membershipProgress.setProgress(100);
            nextLevelText.setText("You are at the maximum rank!");
        }
    }

    private void setupTaskButtons() {
        // Since we didn't give IDs to the individual buttons in the XML template yet, 
        // in a real app you'd find them and set listeners.
        // For now, let's just make it look functional.
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
