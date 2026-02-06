package com.oid.crash.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.oid.crash.R;
import com.oid.crash.databinding.FragmentHomeBinding;
import com.oid.crash.services.CrashMonitorService;
import com.oid.crash.utils.AppDatabaseHelper;

/**
 * The Dashboard Fragment.
 * Handles starting/stopping the monitor service and switching modes.
 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private AppDatabaseHelper db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabaseHelper.getInstance(requireContext());

        setupListeners();
        updateUIState();
    }

    /**
     * Initializes buttons and mode switches.
     */
    private void setupListeners() {
        // Mode Selection: Normal vs Power
        binding.radioGroupMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_normal) {
                db.setMonitorMode(0);
                binding.tvModeDescription.setText(R.string.info_normal_mode);
            } else if (checkedId == R.id.radio_power) {
                db.setMonitorMode(1);
                binding.tvModeDescription.setText(R.string.info_power_mode);
            }
        });

        // Start Monitoring Button
        binding.btnToggleService.setOnClickListener(v -> {
            if (isServiceRunning(CrashMonitorService.class)) {
                stopMonitoring();
            } else {
                startMonitoring();
            }
        });
    }

    /**
     * Refreshes the UI text and button colors based on the current service state.
     */
    private void updateUIState() {
        boolean running = isServiceRunning(CrashMonitorService.class);

        if (running) {
            binding.tvStatusValue.setText(R.string.status_active);
            binding.tvStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.oid_secondary));
            binding.btnToggleService.setText(R.string.btn_stop_service);
            binding.btnToggleService.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.oid_secondary));
            
            // Disable mode switching while active to prevent configuration errors
            binding.radioNormal.setEnabled(false);
            binding.radioPower.setEnabled(false);
        } else {
            binding.tvStatusValue.setText(R.string.status_inactive);
            binding.tvStatusValue.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            binding.btnToggleService.setText(R.string.btn_start_service);
            binding.btnToggleService.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.oid_primary));

            binding.radioNormal.setEnabled(true);
            binding.radioPower.setEnabled(true);
        }

        // Set Radio state based on saved mode
        int mode = db.getMonitorMode();
        if (mode == 0) {
            binding.radioNormal.setChecked(true);
            binding.tvModeDescription.setText(R.string.info_normal_mode);
        } else {
            binding.radioPower.setChecked(true);
            binding.tvModeDescription.setText(R.string.info_power_mode);
        }
        
        // Target app count update
        int count = db.getTargetPackages().size();
        binding.tvTargetCount.setText("Monitoring " + count + " target apps.");
    }

    private void startMonitoring() {
        Intent serviceIntent = new Intent(requireContext(), CrashMonitorService.class);
        ContextCompat.startForegroundService(requireContext(), serviceIntent);
        
        // Briefly delay to allow service to start before refreshing UI
        binding.getRoot().postDelayed(this::updateUIState, 500);
    }

    private void stopMonitoring() {
        Intent serviceIntent = new Intent(requireContext(), CrashMonitorService.class);
        requireContext().stopService(serviceIntent);
        
        binding.getRoot().postDelayed(this::updateUIState, 500);
    }

    /**
     * Utility to check if a service is currently active in the background.
     */
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUIState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}