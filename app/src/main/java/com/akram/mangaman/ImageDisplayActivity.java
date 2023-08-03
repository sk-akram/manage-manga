package com.akram.mangaman;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ImageDisplayActivity extends AppCompatActivity {

    private List<File> imageFiles;
    private int currentIndex;
    private ViewPager viewPager;
    private ImagePagerAdapter imagePagerAdapter;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "ImageDisplayPrefs";
    private static final String PREFS_KEY_POSITION_PREFIX = "lastPosition_";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);

        viewPager = findViewById(R.id.viewPager);
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (getIntent().hasExtra("folder_path")) {
            String folderPath = getIntent().getStringExtra("folder_path");
            imageFiles = getImageFiles(folderPath);
            String folderIdentifier = getFolderIdentifier(folderPath);
            currentIndex = getLastPosition(folderIdentifier);
            setupViewPager();
            viewPager.setCurrentItem(currentIndex);
            viewPager.setRotation(180);
        } else {
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        String folderPath = getIntent().getStringExtra("folder_path");
        String folderIdentifier = getFolderIdentifier(folderPath);
        saveLastPosition(folderIdentifier, currentIndex);
    }

    private List<File> getImageFiles(String folderPath) {
        File folder = new File(folderPath);
        List<File> imageFiles = new ArrayList<>();

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();

            if (files != null) {
                // Sort the files based on their names
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File file1, File file2) {
                        return file1.getName().compareTo(file2.getName());
                    }
                });

                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".png")) {
                        imageFiles.add(file);
                    }
                }
            }
        }

        return imageFiles;
    }


    private void setupViewPager() {
        imagePagerAdapter = new ImagePagerAdapter(this, imageFiles);
        viewPager.setAdapter(imagePagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Not needed
            }

            @Override
            public void onPageSelected(int position) {
                currentIndex = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // Not needed
            }
        });
    }

    private void saveLastPosition(String folderIdentifier, int position) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String key = PREFS_KEY_POSITION_PREFIX + folderIdentifier;
        editor.putInt(key, position);
        editor.apply();
    }

    private int getLastPosition(String folderIdentifier) {
        String key = PREFS_KEY_POSITION_PREFIX + folderIdentifier;
        return sharedPreferences.getInt(key, 0);
    }

    private String getFolderIdentifier(String folderPath) {
        return folderPath.replace("/", "_");
    }
}