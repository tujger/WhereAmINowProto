package ru.wtg.whereaminow.helpers;

/**
 * Created by tujger on 7/15/16.
 */
import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar.SnackbarLayout;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.view.View;

public class LinearLayoutCompatBehavior extends CoordinatorLayout.Behavior<LinearLayoutCompat> {

    public LinearLayoutCompatBehavior(Context context, AttributeSet attrs) {
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, LinearLayoutCompat child, View dependency) {
        return dependency instanceof SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, LinearLayoutCompat child, View dependency) {
        float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight());
        child.setTranslationY(translationY);
        return true;
    }

    @Override
    public void onDependentViewRemoved(CoordinatorLayout parent, LinearLayoutCompat child, View dependency) {
        float translationY = Math.min(0, parent.getBottom() - child.getBottom());
        child.setTranslationY(translationY);
    }
}