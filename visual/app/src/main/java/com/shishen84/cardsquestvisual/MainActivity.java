package com.shishen84.cardsquestvisual;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

public class MainActivity extends Activity {
    private GameView gameView;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        gameView = new GameView(this);
        setContentView(gameView);
        hideSystemUI();
    }
    private void hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(5894 | 4096);
        }
    }
    @Override protected void onPause() { if (gameView != null) gameView.onHostPause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); if (gameView != null) gameView.onHostResume(); hideSystemUI(); }
    @Override protected void onDestroy() { if (gameView != null) gameView.releaseAudio(); super.onDestroy(); }
    @Override public void onWindowFocusChanged(boolean hasFocus) { super.onWindowFocusChanged(hasFocus); if (hasFocus) hideSystemUI(); }
    @Override public void onBackPressed() { if (!gameView.handleBack()) super.onBackPressed(); }
}
