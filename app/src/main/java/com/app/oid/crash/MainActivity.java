package com.oid.crash;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.oid.crash.databinding.ActivityMainBinding;
import com.oid.crash.utils.AppDatabaseHelper;

/**
 * Main Activity for Oid Crash.
 * Manages the top-level navigation, toolbar menu, and core permissions.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private AppDatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewBinding (Matches template style)
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabaseHelper.getInstance(this);

        // Setup Toolbar
        setSupportActionBar(binding.toolbar);

        // Setup Navigation Component
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Define Top-Level Destinations
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_home, 
                    R.id.nav_targets, 
                    R.id.nav_history)
                    .build();

            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(binding.bottomNav, navController);
        }

        // Check for essential monitoring permissions on startup
        checkRequiredPermissions();
    }

    /**
     * Creates the 3-dot menu in the top right.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Handles clicks on the 3-dot menu items.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            navController.navigate(R.id.nav_settings);
            return true;
        } else if (id == R.id.action_help) {
            showHelpDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Verifies that the app has the permissions needed to monitor other apps.
     */
    private void checkRequiredPermissions() {
        // 1. Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // 2. Usage Stats Permission (Crucial for tracking foreground app)
        if (!hasUsageStatsPermission()) {
            showPermissionDialog("Usage Access Required", 
                    "Oid Crash needs Usage Access to detect when target apps are running.",
                    new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, 
                Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void showPermissionDialog(String title, String message, Intent intent) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Grant", (dialog, which) -> startActivity(intent))
                .setNegativeButton("Later", null)
                .show();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Oid Crash Help")
                .setMessage("1. Select apps to monitor in 'Target Apps'.\n\n" +
                           "2. Enable Normal Mode for basic tracking.\n\n" +
                           "3. Use Power Mode with Wireless ADB for full stack traces without a PC.\n\n" +
                           "No Root required.")
                .setPositiveButton("Got it", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}