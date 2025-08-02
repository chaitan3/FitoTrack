package de.tadris.fitness.util.unit;

import de.tadris.fitness.R;

public class Kcal implements EnergyUnit {

    @Override
    public double getFromJoules(double energyInJoules) {
        return energyInJoules / 4186.8;
    }

    @Override
    public double toJoules(double energyInUnit) {
        return energyInUnit * 4186.8;
    }

    @Override
    public String getInternationalShortName() {
        return "kcal";
    }

    @Override
    public int getLongNameTitle() {
        return R.string.unitKcalLong;
    }
}
