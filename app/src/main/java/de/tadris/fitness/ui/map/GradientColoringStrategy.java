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

package de.tadris.fitness.ui.map;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import androidx.core.graphics.ColorUtils;


public class GradientColoringStrategy implements ColoringStrategy {

    // this color is based on https://colorbrewer2.org/#type=sequential&scheme=BuPu&n=9
    public final static String PATTERN_PURPLE = "#f7fcfd #e0ecf4 #bfd3e6 #9ebcda #8c96c6 #8c6bb1 #88419d #810f7c #4d004b";
    public final static String PATTERN_PINK = "#f7f4f9 #e7e1ef #d4b9da #c994c7 #df65b0 #e7298a #ce1256 #980043 #67001f";

    // other "nice" patterns
    public final static String PATTERN_MAP = "#ed5f53 #ede553 #53ede5 #1a1ee0 #e01a2e";
    public final static String PATTERN_BRIGHT = "#f0f2cd #c9f28c #a4f4ef #cfd6f7 #e4cbf4 #f4c1e7 #ddb8c2";
    public final static String PATTERN_YELLOW_RED_BLUE = "#f2f215 #f2f215 #800000 #000080";/* best used with no blending */
    // http://geoffair.org/fg/map-test2/colormap.html
    public final static String PATTERN_HEIGHT_MAP = "#000080 #c1ffff #d2ffff #e3ffff #c8ffed #c8ffc0 #c8ff9a #c8ff6a #c8ff14 #b1ff14 #9dff14 #86ff14 #60ff14 #23ff14 #00f200 #64e400 #00dc00 #8bde00 #c5f300 #ffff63 #fff85d #fff200 #ffe857 #ffd851 #ffc84b #ffb845 #ffa83f #ff9839 #ff8833 #ff782d #ff6827 #fa5821 #f2481b #ea3815 #e2280f #da1809 #d20803 #c80000 #bc0000 #b00000 #a40000 #980000 #8c0000 #800000 #740000 #680000 #5c0000 #500000 #440000 #380000 #2c0000 #200000 #200000";

    double value_min;
    double value_max;
    private final static int ITEMS_PER_GRADIENT = 10;

    int[] colorPalette;

    /**
     * Helper function to construct a coloring strategy based on a string representation of the color
     *
     * @param pattern a space delimited list of rgb values in the format of #rrggbb or #aarrbbgg
     * @param doBlend Wheter to blend from one value to the other or not
     * @return a new instance of ColoringStrategy
     */
    public static ColoringStrategy fromPattern(String pattern, boolean doBlend) {
        String[] colorStrings = pattern.split(" ");
        assert colorStrings.length > 1; // We expect at least two colors because we "blend" between colors ..
        int[] colors = new int[colorStrings.length];
        for (int i = 0; i < colorStrings.length; i++) {
            colors[i] = Color.parseColor(colorStrings[i]);
        }
        return new GradientColoringStrategy(colors, doBlend);
    }

    /**
     * Construct a new Gradient given the array of argb colors and a minimal max value to map
     * the color to.
     *
     * @param colors  The colors to map.
     * @param doBlend Wheter to blend from one value to the other or not. If blend is set to false
     *                the first color in the list will be discarded hence an additional bogus color
     *                is needed.
     */
    public GradientColoringStrategy(int[] colors, boolean doBlend) {
        assert colors.length > 1;

        int blend_count = colors.length - 1;// color -1 blends (from color n to color n+1
        int item_count = blend_count * ITEMS_PER_GRADIENT;//We are going to create 10 items per color
        // the strategy there is to precalculate the gradient so allow cheap lookup later
        colorPalette = new int[item_count];
        for (int i = 0; i < blend_count; i++) {
            for (int f = 0; f < ITEMS_PER_GRADIENT; f++) {
                int index = i * ITEMS_PER_GRADIENT + f;
                float ratio = (doBlend) ? (float) f / ITEMS_PER_GRADIENT :1;
                colorPalette[index] = ColorUtils.blendARGB(colors[i],colors[i+1],ratio);
            }
        }
    }

    /**
     * The GradientColoringStrategy has similarities to the Android gradient. It might be possible
     * to use the gradient here to display the gradient in the settings pane or similar
     * @return an Android gradient that is analog to this gradient
     */
    public GradientDrawable asGradientDrawable(){
        // We are returning the full colorPallete (as opposed to the initial colors given to the
        // class. The result .. should be very similar
        return new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colorPalette);
    }

    public int getColor(double value){
        //find out in what bin to map the value
        double offset = value - value_min ;
        double max_offset = value_max - value_min;
        int index = (int)((offset / max_offset) * colorPalette.length);

        // index can become negative or higher compared to the amount of control points
        // in such situation we take the edge value
        index = Math.min(index, colorPalette.length - 1);//if the scale is smaller than the actual speed -> use the largest
        index = Math.max(index, 0);//same for smaller scale
        return colorPalette[index];
    }

    @Override
    public void setMin(double value) {
        value_min = value;
    }

    @Override
    public void setMax(double value) {
        value_max = value;
    }
}
