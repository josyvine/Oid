package com.oid.crash.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.oid.crash.databinding.ItemCrashReportBinding;
import com.oid.crash.models.CrashFile;

import java.util.List;

/**
 * Adapter for displaying the list of generated crash reports.
 * Manages binding report metadata to the history list items.
 */
public class CrashHistoryAdapter extends RecyclerView.Adapter<CrashHistoryAdapter.CrashViewHolder> {

    private final List<CrashFile> crashFiles;
    private final OnReportActionListener listener;

    /**
     * Interface for handling interactions with crash reports in the list.
     */
    public interface OnReportActionListener {
        void onReportClicked(CrashFile crashFile);
        void onDeleteClicked(CrashFile crashFile);
        void onOpenExternalClicked(CrashFile crashFile);
    }

    public CrashHistoryAdapter(List<CrashFile> crashFiles, OnReportActionListener listener) {
        this.crashFiles = crashFiles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CrashViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use ViewBinding for the item layout (standard in your template style)
        ItemCrashReportBinding binding = ItemCrashReportBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CrashViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CrashViewHolder holder, int position) {
        CrashFile file = crashFiles.get(position);
        holder.bind(file, listener);
    }

    @Override
    public int getItemCount() {
        return crashFiles.size();
    }

    /**
     * ViewHolder class that handles the binding of data to the XML views.
     */
    static class CrashViewHolder extends RecyclerView.ViewHolder {
        private final ItemCrashReportBinding binding;

        public CrashViewHolder(ItemCrashReportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CrashFile file, OnReportActionListener listener) {
            // Display formatted metadata from the model
            binding.tvAppName.setText(file.getAppName());
            binding.tvCrashDate.setText(file.getFormattedDate());
            binding.tvFileSize.setText(file.getReadableFileSize());

            // 1. Single Tap: Open in-app log viewer
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReportClicked(file);
                }
            });

            // 2. Long Press: Open in external text reader (as per requirements)
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onOpenExternalClicked(file);
                }
                return true;
            });

            // 3. Delete Icon Click: Trigger single delete logic
            binding.btnDeleteReport.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClicked(file);
                }
            });
        }
    }
}