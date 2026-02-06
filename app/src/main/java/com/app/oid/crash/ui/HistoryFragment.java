package com.oid.crash.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.oid.crash.R;
import com.oid.crash.adapters.CrashHistoryAdapter;
import com.oid.crash.databinding.FragmentHistoryBinding;
import com.oid.crash.models.CrashFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fragment responsible for displaying the history of crash reports.
 * Allows users to view, open in external readers, and delete reports.
 */
public class HistoryFragment extends Fragment implements CrashHistoryAdapter.OnReportActionListener {

    private FragmentHistoryBinding binding;
    private CrashHistoryAdapter adapter;
    private List<CrashFile> crashFileList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        crashFileList = new ArrayList<>();
        setupRecyclerView();
        loadCrashReports();

        // Refresh listener (Swipe to refresh if needed, or simple button)
        binding.btnRefresh.setOnClickListener(v -> loadCrashReports());
    }

    private void setupRecyclerView() {
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CrashHistoryAdapter(crashFileList, this);
        binding.rvHistory.setAdapter(adapter);
    }

    /**
     * Scans the app's internal report directory recursively.
     * Path: /Android/data/com.oid.crash/files/reports/
     */
    private void loadCrashReports() {
        binding.progressBar.setVisibility(View.VISIBLE);
        crashFileList.clear();

        File reportsDir = new File(requireContext().getExternalFilesDir(null), "reports");
        
        if (reportsDir.exists() && reportsDir.isDirectory()) {
            File[] appFolders = reportsDir.listFiles();
            if (appFolders != null) {
                for (File folder : appFolders) {
                    if (folder.isDirectory()) {
                        File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt"));
                        if (files != null) {
                            for (File file : files) {
                                crashFileList.add(new CrashFile(file));
                            }
                        }
                    }
                }
            }
        }

        // Sort by timestamp: Newest crash first (as per instructions)
        Collections.sort(crashFileList, (f1, f2) -> Long.compare(f2.getTimestamp(), f1.getTimestamp()));

        binding.progressBar.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();

        // Show empty state if no reports found
        if (crashFileList.isEmpty()) {
            binding.tvNoReports.setVisibility(View.VISIBLE);
            binding.rvHistory.setVisibility(View.GONE);
        } else {
            binding.tvNoReports.setVisibility(View.GONE);
            binding.rvHistory.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Implementation of the Adapter Listener for clicking a report.
     */
    @Override
    public void onReportClicked(CrashFile crashFile) {
        // Open the In-App Viewer (we will create this Activity next)
        Intent intent = new Intent(requireContext(), LogViewerActivity.class);
        intent.putExtra("file_path", crashFile.getFilePath());
        intent.putExtra("app_name", crashFile.getAppName());
        startActivity(intent);
    }

    /**
     * Opens the report using an external text reader app.
     */
    @Override
    public void onOpenExternalClicked(CrashFile crashFile) {
        File file = new File(crashFile.getFilePath());
        Uri uri = FileProvider.getUriForFile(requireContext(), 
                requireContext().getPackageName() + ".fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/plain");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            startActivity(Intent.createChooser(intent, "Open Report With"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No text reader found", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles the deletion of a single report.
     */
    @Override
    public void onDeleteClicked(CrashFile crashFile) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_confirm)
                .setMessage(R.string.delete_msg)
                .setPositiveButton("Delete", (dialog, which) -> {
                    File file = new File(crashFile.getFilePath());
                    if (file.delete()) {
                        Toast.makeText(requireContext(), "Report deleted", Toast.LENGTH_SHORT).show();
                        loadCrashReports();
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}