package com.example.WalletSIOPM;

import android.util.Log;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

public abstract class SecureActivity extends AppCompatActivity {

    private static final String TAG = "SecureActivity";

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int obscuredFlags = MotionEvent.FLAG_WINDOW_IS_OBSCURED
                | MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED;
        if ((event.getFlags() & obscuredFlags) != 0) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                Log.w(TAG, "Rejected touch obscured by another window");
            }
            return false;
        }
        return super.dispatchTouchEvent(event);
    }
}
