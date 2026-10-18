package com.shishen84.cardsquestvisual;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

public class MainActivity extends Activity {
    private GameView gameView;
    private HomeView homeView;
    private GameAudio audio;
    private boolean inGame = false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        audio = new GameAudio(this);
        showHome();
        hideSystemUI();
    }

    private void showHome() {
        inGame = false;
        gameView = null;
        homeView = new HomeView(this, new HomeView.Listener() {
            @Override public void onContinue() {
                audio.tap();
                startGame(false);
            }

            @Override public void onNewGame() {
                audio.tap();
                startGame(true);
            }

            @Override public void onAudioChanged() {
                audio.reload();
                audio.tap();
            }
        });
        setContentView(homeView);
        audio.start();
        hideSystemUI();
    }

    private void startGame(boolean newGame) {
        if (newGame) {
            getSharedPreferences("game", MODE_PRIVATE).edit().clear().apply();
        }
        inGame = true;
        homeView = null;
        gameView = new GameView(this);
        gameView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getX() < 86f && event.getY() < 84f) {
                    audio.tap();
                    showHome();
                    return true;
                }
                audio.tap();
            }
            return false;
        });
        setContentView(gameView);
        audio.start();
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

    @Override protected void onResume() {
        super.onResume();
        if (audio != null) audio.start();
    }

    @Override protected void onPause() {
        if (audio != null) audio.pause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (audio != null) audio.release();
        super.onDestroy();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    @Override public void onBackPressed() {
        if (inGame && gameView != null) {
            if (!gameView.handleBack()) showHome();
            return;
        }
        if (homeView != null && homeView.handleBack()) return;
        super.onBackPressed();
    }
}
