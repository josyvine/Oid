package com.oid.crash.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.oid.crash.R;
import com.oid.crash.databinding.ActivityLogViewerBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Dedicated Activity to view the content of a crash report.
 * Provides features to read the full stacktrace and share the file.
 */
public class LogViewerActivity extends AppCompatActivity {

    private static final String TAG = "LogViewerActivity";
    private ActivityLogViewerBinding binding;
    private String filePath;
    private String appName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize ViewBinding
        binding = ActivityLogViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Toolbar with Back Button
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Retrieve data from Intent
        filePath = getIntent().getStringExtra("file_path");
        appName = getIntent().getStringExtra("app_name");

        if (appName != null) {
            getSupportActionBar().setTitle("Report: " + appName);
        }

        // Load the file content into the UI
        if (filePath != null) {
            loadLogFile(filePath);
        } else {
            Toast.makeText(this, "Error: File path not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Reads the text content of the .txt file line by line.
     */
    private void loadLogFile(String path) {
        File file = new File(path);
        StringBuilder text = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            binding.tvLogContent.setText(text.toString());
        } catch (IOException e) {
            Log.e(TAG, "Failed to read log file", e);
            binding.tvLogContent.setText("Error loading log file content.");
        }
    }

    /**
     * Creates the options menu for the viewer (Share action).
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.log_viewer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_share) {
            shareLogFile();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Uses FileProvider to share the .txt file with other applications.
     */
    private void shareLogFile() {
        File file = new File(filePath);
        if (!file.exists()) return;

        Uri uri = FileProvider.getUriForFile(this, 
                getPackageName() + ".fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Crash Report: " + appName);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "Share Crash Report via"));
    }
}