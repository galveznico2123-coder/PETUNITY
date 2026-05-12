package com.petunity.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.petunity.R;
import com.petunity.fragments.PlaydateRequestsFragment;

public class PlayFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // This is a proxy class to ensure the navigation in MainActivity works
        // while the logic resides in the fragments package.
        return inflater.inflate(R.layout.fragment_play, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Immediately delegate to the logic-heavy fragment
        getParentFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, new com.petunity.fragments.PlayFragment())
                .commit();
    }
}
