package com.oid.crash.ui;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.oid.crash.adapters.AppSelectionAdapter;
import com.oid.crash.databinding.FragmentTargetAppsBinding;
import com.oid.crash.models.AppInfo;
import com.oid.crash.utils.AppDatabaseHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Screen for Target App Selection.
 * Allows users to choose which installed apps Oid Crash should monitor.
 * Logic: Fetches installed apps -> Filters based on user search -> Saves selection to DB.
 */
public class TargetAppsFragment extends Fragment implements AppSelectionAdapter.OnAppSelectionListener {

    private FragmentTargetAppsBinding binding;
    private AppSelectionAdapter adapter;
    private List<AppInfo> fullAppList;
    private AppDatabaseHelper db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTargetAppsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        db = AppDatabaseHelper.getInstance(requireContext());
        fullAppList = new ArrayList<>();
        
        setupRecyclerView();
        setupSearch();
        
        // Load apps in a background thread to keep UI smooth
        loadAppsInBackground();
    }

    private void setupRecyclerView() {
        binding.rvApps.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AppSelectionAdapter(new ArrayList<>(), this);
        binding.rvApps.setAdapter(adapter);
    }

    /**
     * Filters the list as the user types in the search bar.
     */
    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Retrieves all apps installed on the device that have a launcher icon.
     */
    private void loadAppsInBackground() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        executor.execute(() -> {
            PackageManager pm = requireContext().getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<AppInfo> tempInfoList = new ArrayList<>();
            
            Set<String> savedTargets = db.getTargetPackages();

            for (ApplicationInfo app : packages) {
                // Filter: Only show apps that the user can actually launch
                if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                    String name = app.loadLabel(pm).toString();
                    Drawable icon = app.loadIcon(pm);
                    boolean isTargeted = savedTargets.contains(app.packageName);
                    
                    tempInfoList.add(new AppInfo(name, app.packageName, icon, isTargeted));
                }
            }

            // Sort Alphabetically
            Collections.sort(tempInfoList);

            // Update UI on main thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    fullAppList = tempInfoList;
                    adapter.updateList(fullAppList);
                    binding.progressBar.setVisibility(View.GONE);
                    
                    if (fullAppList.isEmpty()) {
                        binding.tvNoApps.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void filterApps(String query) {
        List<AppInfo> filtered = new ArrayList<>();
        for (AppInfo info : fullAppList) {
            if (info.getAppName().toLowerCase().contains(query.toLowerCase()) ||
                info.getPackageName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(info);
            }
        }
        adapter.updateList(filtered);
    }

    /**
     * Interface callback: Triggered whenever a checkbox is toggled.
     * Logic: Updates the local database set immediately.
     */
    @Override
    public void onAppToggle(String packageName, boolean isSelected) {
        Set<String> targets = new HashSet<>(db.getTargetPackages());
        
        if (isSelected) {
            targets.add(packageName);
        } else {
            targets.remove(packageName);
        }
        
        db.saveTargetPackages(targets);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}