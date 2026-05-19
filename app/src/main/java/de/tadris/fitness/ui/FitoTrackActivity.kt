/*
 * Copyright (c) 2026 Jannis Scheibe <jannis@tadris.de>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     FitoTrack is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.tadris.fitness.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.AttrRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.util.PermissionUtils;
import de.tadris.fitness.util.ThemeUtils;

abstract public class FitoTrackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Instance.getInstance(this).themes.getDefaultTheme());
    }

    public int getThemePrimaryColor() {
        return getThemeColor(android.R.attr.colorPrimary);
    }

    protected int getThemePrimaryDarkColor() {
        return getThemeColor(android.R.attr.colorPrimaryDark);
    }

    public int getThemeTextColor() {
        return getThemeColor(android.R.attr.textColorPrimary);
    }

    protected int getThemeTextColorInverse() {
        return (0xffffffff - getThemeTextColor()) | 0xff000000;
    }

    protected int getThemeColor(@AttrRes int colorRes) {
        return ThemeUtils.resolveThemeColor(this, colorRes);
    }

    protected void showErrorDialog(Exception e, @StringRes int title, @StringRes int message) {
        showErrorDialog(e, title, message, null);
    }

    protected void showErrorDialog(Exception e, @StringRes int title, @StringRes int message, @Nullable Runnable listener) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(getString(message) + "\n\n" + e.getMessage())
                .setPositiveButton(R.string.okay, (dialog, which) -> {
                    if (listener != null) listener.run();
                })
                .setOnDismissListener(dialog -> {
                    if (listener != null) listener.run();
                })
                .create().show();
    }

    protected void requestStoragePermissions() {
        if (!hasStoragePermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10);
        }
    }

    protected boolean hasStoragePermission() {
        return PermissionUtils.checkStoragePermissions(this, true);
    }

    protected void requestKeyboard(View v) {
        v.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    protected void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
