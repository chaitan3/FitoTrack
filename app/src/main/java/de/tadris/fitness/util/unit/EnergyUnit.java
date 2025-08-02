package de.tadris.fitness.util.unit;

import androidx.annotation.StringRes;

public interface EnergyUnit {

    double getFromJoules(double energyInJoules);

    double toJoules(double energyInUnit);

    String getInternationalShortName();

    @StringRes
    int getLongNameTitle();

}
