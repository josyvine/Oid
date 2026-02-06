package com.oid.crash.models;

import android.graphics.drawable.Drawable;

/**
 * Data model representing an installed application on the device.
 * Used in the Recycler View to allow user selection.
 */
public class AppInfo implements Comparable<AppInfo> {

    private String appName;
    private String packageName;
    private Drawable icon;
    private boolean isSelected;

    public AppInfo(String appName, String packageName, Drawable icon, boolean isSelected) {
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
        this.isSelected = isSelected;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    // Sort apps alphabetically by name for better UX
    @Override
    public int compareTo(AppInfo other) {
        return this.appName.compareToIgnoreCase(other.appName);
    } 
}