package de.tadris.fitness.util.unit;

import de.tadris.fitness.R;

public class KJoule implements EnergyUnit {

    @Override
    public double getFromJoules(double energyInJoules) {
        return energyInJoules / 1000;
    }

    @Override
    public double toJoules(double energyInUnit) {
        return energyInUnit * 1000;
    }

    @Override
    public String getInternationalShortName() {
        return "kJ";
    }

    @Override
    public int getLongNameTitle() {
        return R.string.unitKJoule;
    }
}
