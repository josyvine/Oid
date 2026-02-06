package com.oid.crash.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.oid.crash.R;
import com.oid.crash.databinding.FragmentSettingsBinding;
import com.oid.crash.utils.AppDatabaseHelper;

/**
 * Settings Screen for Wireless ADB Configuration.
 * Required for Power Mode to capture full system crash logs.
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private AppDatabaseHelper db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabaseHelper.getInstance(requireContext());

        loadSavedSettings();

        // Pair Button Click Logic
        binding.btnPairDevice.setOnClickListener(v -> performPairing());
    }

    private void loadSavedSettings() {
        String savedIp = db.getAdbIp();
        int savedPort = db.getAdbPort();

        if (!TextUtils.isEmpty(savedIp)) {
            binding.etAdbIp.setText(savedIp);
        }
        binding.etAdbPort.setText(String.valueOf(savedPort));
    }

    /**
     * Logic to validate inputs and save pairing details.
     * In a full implementation, this would trigger the actual ADB pairing handshake.
     */
    private void performPairing() {
        String ip = binding.etAdbIp.getText().toString().trim();
        String portStr = binding.etAdbPort.getText().toString().trim();
        String pairingCode = binding.etPairingCode.getText().toString().trim();

        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(portStr) || TextUtils.isEmpty(pairingCode)) {
            Toast.makeText(requireContext(), "All fields are required for pairing", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            
            // Show loading state
            binding.btnPairDevice.setEnabled(false);
            binding.btnPairDevice.setText("Pairing...");

            // Logic Simulation: Save the session details
            db.saveAdbConnection(ip, port);

            // Simulate a successful pairing result
            binding.getRoot().postDelayed(() -> {
                if (isAdded()) {
                    binding.btnPairDevice.setEnabled(true);
                    binding.btnPairDevice.setText(R.string.btn_pair);
                    Toast.makeText(requireContext(), R.string.pairing_success, Toast.LENGTH_LONG).show();
                    
                    // Automatically switch to Power Mode after successful pairing
                    db.setMonitorMode(1); 
                }
            }, 2000);

        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid Port number", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}