package de.tadris.fitness.ui.dialog;

import android.app.Activity;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.recording.autostart.AutoStartWorkout;

public class ChooseAutoStartModeDialog extends NumberPickerDialog<AutoStartWorkout.Mode> {
    private final AutoStartModeSelectListener listener;
    private final AutoStartWorkout.Mode initialMode;

    /**
     * @param context       The context this dialog should be shown in
     * @param listener      The listener that is called when the user selects a delay
     * @param initialMode Initially selected auto start delay in seconds
     */
    public ChooseAutoStartModeDialog(Activity context,
                                     AutoStartModeSelectListener listener,
                                     AutoStartWorkout.Mode initialMode) {
        super(context, context.getString(R.string.pref_auto_start_mode_title));
        this.listener = listener;
        this.initialMode = initialMode;
    }

    /**
     * Initialize dialog with default delay from preferences
     * @param context       The context this dialog should be shown in
     * @param listener      The listener that is called when the user selects a delay
     */
    public ChooseAutoStartModeDialog(Activity context, AutoStartModeSelectListener listener) {
        super(context, context.getString(R.string.pref_auto_start_mode_title));
        this.listener = listener;
        this.initialMode = Instance.getInstance(context).userPreferences.getAutoStartMode();
    }

    @Override
    protected int getOptionCount() {
        return AutoStartWorkout.Mode.values().length;
    }

    @Override
    protected AutoStartWorkout.Mode getInitOption() {
        return initialMode;
    }

    @Override
    protected String format(AutoStartWorkout.Mode mode) {
        switch (mode) {
            case INSTANT:
                return context.getString(R.string.pref_auto_start_mode_instant);
            case ON_MOVE:
                return context.getString(R.string.pref_auto_start_mode_on_move);
            case WAIT_FOR_GPS:
                return context.getString(R.string.pref_auto_start_mode_wait_for_gps);
            default:
                return context.getString(R.string.pref_auto_start_mode_unknown);
        }
    }

    @Override
    protected int toOptionNum(AutoStartWorkout.Mode mode) {
        // this is okay here, no ones going to change the order of mode enums while the dialog is
        // shown ;-)
        return mode.ordinal();
    }

    @Override
    protected AutoStartWorkout.Mode fromOptionNum(int optionNum) {
        return AutoStartWorkout.Mode.values()[optionNum];
    }

    @Override
    protected void onSelectOption(AutoStartWorkout.Mode mode) {
        listener.onSelectAutoStartMode(mode);
    }

    public interface AutoStartModeSelectListener {
        /**
         * @param mode Selected auto start mode
         */
        void onSelectAutoStartMode(AutoStartWorkout.Mode mode);
    }
}
