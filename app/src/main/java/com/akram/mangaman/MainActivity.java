package com.akram.mangaman;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView myTextView;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myTextView = findViewById(R.id.main_icon);
        handler = new Handler();

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.pop_up_animation);
        myTextView.setVisibility(View.VISIBLE);
        myTextView.startAnimation(animation);

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Animation start
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Animation end, delay and start new activity
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(MainActivity.this, HomeActivity.class));
                        finish(); // Optional: Close current activity if needed
                    }
                }, 2000); // Delay in milliseconds (2 seconds)
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Animation repeat
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // Remove any pending delayed executions
    }
}
