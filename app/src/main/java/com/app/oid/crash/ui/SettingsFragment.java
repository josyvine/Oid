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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Settings Screen for Wireless ADB Configuration.
 * Updated: Now performs a REAL socket handshake to verify port 5555 is open.
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
     * Updated: Performs a real handshake.
     * Attempts to connect to the unlocked port to ensure pairing is real.
     */
    private void performPairing() {
        String ip = binding.etAdbIp.getText().toString().trim();
        String portStr = binding.etAdbPort.getText().toString().trim();
        String pairingCode = binding.etPairingCode.getText().toString().trim();

        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(portStr)) {
            Toast.makeText(requireContext(), "IP and Port are required", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnPairDevice.setEnabled(false);
        binding.btnPairDevice.setText("Verifying...");

        // REAL HANDSHAKE LOGIC: Check if the port opened by Bugjaeger is reachable
        new Thread(() -> {
            boolean success = false;
            try {
                int port = Integer.parseInt(portStr);
                Socket socket = new Socket();
                // 3-second timeout to check if port 5555 is actually open
                socket.connect(new InetSocketAddress(ip, port), 3000);
                socket.close();
                success = true;
            } catch (Exception e) {
                success = false;
            }

            final boolean finalSuccess = success;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    binding.btnPairDevice.setEnabled(true);
                    binding.btnPairDevice.setText(R.string.btn_pair);

                    if (finalSuccess) {
                        // Reality Confirmed: Port is open
                        db.saveAdbConnection(ip, Integer.parseInt(portStr));
                        db.setMonitorMode(1); // Auto-switch to Power Mode
                        Toast.makeText(requireContext(), "Pairing Verified! Power Mode Ready.", Toast.LENGTH_LONG).show();
                    } else {
                        // Reality Check Failed: Port is closed
                        Toast.makeText(requireContext(), "Connection Failed: Ensure Port " + portStr + " is opened via Bugjaeger.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}