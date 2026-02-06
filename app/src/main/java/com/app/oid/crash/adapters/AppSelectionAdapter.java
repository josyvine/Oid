package com.oid.crash.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.oid.crash.databinding.ItemTargetAppBinding;
import com.oid.crash.models.AppInfo;

import java.util.List;

/**
 * Adapter for the Target App Selection list.
 * Manages the display of app icons, names, and checkboxes.
 */
public class AppSelectionAdapter extends RecyclerView.Adapter<AppSelectionAdapter.AppViewHolder> {

    private List<AppInfo> appList;
    private final OnAppSelectionListener listener;

    /**
     * Interface to communicate selection changes back to the Fragment.
     */
    public interface OnAppSelectionListener {
        void onAppToggle(String packageName, boolean isSelected);
    }

    public AppSelectionAdapter(List<AppInfo> appList, OnAppSelectionListener listener) {
        this.appList = appList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use ViewBinding for the item layout
        ItemTargetAppBinding binding = ItemTargetAppBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new AppViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = appList.get(position);
        holder.bind(app, listener);
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    /**
     * Updates the data set and refreshes the UI. 
     * Used for initial load and search filtering.
     */
    public void updateList(List<AppInfo> newList) {
        this.appList = newList;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder class using ViewBinding to access UI elements efficiently.
     */
    static class AppViewHolder extends RecyclerView.ViewHolder {
        private final ItemTargetAppBinding binding;

        public AppViewHolder(ItemTargetAppBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(AppInfo app, OnAppSelectionListener listener) {
            binding.tvAppName.setText(app.getAppName());
            binding.tvPackageName.setText(app.getPackageName());
            binding.ivAppIcon.setImageDrawable(app.getIcon());

            // Set checkbox state without triggering the listener
            binding.cbTarget.setOnCheckedChangeListener(null);
            binding.cbTarget.setChecked(app.isSelected());

            // Handle checkbox toggle
            binding.cbTarget.setOnCheckedChangeListener((buttonView, isChecked) -> {
                app.setSelected(isChecked);
                if (listener != null) {
                    listener.onAppToggle(app.getPackageName(), isChecked);
                }
            });

            // Allow clicking the entire row to toggle the checkbox
            this.itemView.setOnClickListener(v -> {
                binding.cbTarget.toggle();
            });
        }
    }
}