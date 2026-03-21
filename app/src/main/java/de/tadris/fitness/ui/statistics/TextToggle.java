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

package de.tadris.fitness.ui.statistics;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import de.tadris.fitness.R;

/**
 * Implementation of App Widget functionality.
 */
public class TextToggle extends LinearLayout {

    public TextView currentTitle;
    public TextView swapTitle;
    private View toggleArrows;
    private boolean swapped;

    private IOnToggleListener onToggleListener;

    OnClickListener clickListener;

    public TextToggle(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.text_toggle, this);

        this.currentTitle = findViewById(R.id.currentTitle);
        this.swapTitle = findViewById(R.id.swapTitle);
        this.toggleArrows = findViewById(R.id.toggle_arrows);
        this.swapped = false;

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.TextToggle);
        currentTitle.setText(array.getText(R.styleable.TextToggle_currentText));
        swapTitle.setText(array.getText(R.styleable.TextToggle_swapText));
        array.recycle();

        clickListener = view -> {
            this.toggle();
        };

        currentTitle.setOnClickListener(clickListener);
        swapTitle.setOnClickListener(clickListener);
        toggleArrows.setOnClickListener(clickListener);
        findViewById(R.id.textToggleLayout).setOnClickListener(clickListener);
    }

    public void toggle() {
        CharSequence current = currentTitle.getText();
        currentTitle.setText(swapTitle.getText());
        swapTitle.setText(current);

        toggleArrows.clearAnimation();
        toggleArrows.setRotation(0f);
        toggleArrows.animate()
                .rotationBy(180f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        swapped = !swapped;

        if (onToggleListener != null) {
            onToggleListener.onToggle(currentTitle.getText());
        }
    }

    public void setOnToggleListener(IOnToggleListener onToggleListener) {
        this.onToggleListener = onToggleListener;
    }

    public boolean isSwapped() {
        return this.swapped;
    }
}