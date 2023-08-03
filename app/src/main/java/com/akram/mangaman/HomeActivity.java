package com.akram.mangaman;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class HomeActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int PICK_PDF_REQUEST_CODE = 200;

    private TextView titleTextView;
    private EditText searchBar;
    private ImageButton searchButton, addButton;

    private Handler handler;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickPDFLauncher;
    private GridView imageGridView;
    private ImageFolderAdapter imageFolderAdapter;
    private ThreadPoolExecutor executor;
    private int processedPageCount;
    private TextView progressTextView;
    private TextView pleaseWaitTextView;

    private List<File> allImageFolders;
    private List<File> filteredImageFolders;


    private boolean isSearchBarVisible = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        handler = new Handler(Looper.getMainLooper());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        titleTextView = findViewById(R.id.titleTextView);
        searchBar = findViewById(R.id.searchBar);
        addButton = findViewById(R.id.addButton);
        searchButton = findViewById(R.id.searchButton);
        progressTextView = findViewById(R.id.progressTextView);
        pleaseWaitTextView = findViewById(R.id.pleaseWaitTextView);


        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openPDFPicker();
                    } else {
                        // Permission not granted. Show a dialog or navigate to app settings.
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                });

        pickPDFLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (result.getData() != null) {
                            Uri pdfUri = result.getData().getData();
                            if (pdfUri != null) {
                                convertPDFToImages(pdfUri);
                            }
                        }
                    }
                });

        searchButton.setOnClickListener(v -> {
            if (isSearchBarVisible) {
                hideSearchBar();
            } else {
                showSearchBar();
            }
        });

        addButton.setOnClickListener(v -> checkPermissionAndOpenPDFPicker());
        imageGridView = findViewById(R.id.imageGridView);
        imageFolderAdapter = new ImageFolderAdapter(this, getImageFolders());

        imageGridView.setAdapter(imageFolderAdapter);
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterImageFolders(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

    }

    private void showSearchBar() {
        titleTextView.setVisibility(View.GONE);
        searchBar.setVisibility(View.VISIBLE);
        searchBar.requestFocus();
        isSearchBarVisible = true;
    }

    private void hideSearchBar() {
        titleTextView.setVisibility(View.VISIBLE);
        searchBar.setVisibility(View.GONE);
        searchBar.setText("");
        isSearchBarVisible = false;
        filterImageFolders("");
    }

    private void filterImageFolders(String query) {
        filteredImageFolders = new ArrayList<>();

        for (File imageFolder : allImageFolders) {
            if (imageFolder.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredImageFolders.add(imageFolder);
            }
        }

        imageFolderAdapter.setImageFolders(filteredImageFolders);
        imageFolderAdapter.notifyDataSetChanged();
    }

    private List<File> getImageFolders() {
        List<File> imageFolders = new ArrayList<>();

        File folder = new File(getFilesDir(), "files");
        if (folder.exists() && folder.isDirectory()) {
            File[] folders = folder.listFiles();
            if (folders != null) {
                Collections.addAll(imageFolders, folders);
            }
        }

        allImageFolders = imageFolders;
        filteredImageFolders = imageFolders;

        return imageFolders;
    }

    private void checkPermissionAndOpenPDFPicker() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            openPDFPicker();
        }
    }

    private void openPDFPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickPDFLauncher.launch(intent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_PDF_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri pdfUri = data.getData();
                if (pdfUri != null) {
                    convertPDFToImages(pdfUri);
                }
            }
        }
    }

    private void convertPDFToImages(Uri pdfUri) {
        String pdfFileName = getFileNameFromUri(pdfUri);

        // Create folder if it doesn't exist
        File folder = new File(getFilesDir(), "files");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Create a folder with the same name as the PDF file
        File pdfFolder = new File(folder, pdfFileName);
        if (!pdfFolder.exists()) {
            pdfFolder.mkdirs();
        }

        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

        int pageCount = getPdfPageCount(pdfUri);
        processedPageCount = 0;

        TextView progressTextView = findViewById(R.id.progressTextView);
        TextView pleaseWaitTextView = findViewById(R.id.pleaseWaitTextView);

        progressTextView.setVisibility(View.GONE);
        pleaseWaitTextView.setVisibility(View.GONE);

        if (pageCount > 0) {
            progressTextView.setVisibility(View.VISIBLE);
            pleaseWaitTextView.setVisibility(View.VISIBLE);
        }

        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            final int currentPageIndex = pageIndex;

            executor.submit(() -> {
                try {
                    ParcelFileDescriptor fileDescriptor = getContentResolver().openFileDescriptor(pdfUri, "r");
                    PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);

                    try {
                        PdfRenderer.Page page = pdfRenderer.openPage(currentPageIndex);
                        Rect rect = new Rect(0, 0, page.getWidth(), page.getHeight());
                        Bitmap bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
                        page.render(bitmap, rect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                        String imageName = "page_" + String.format("%03d", (currentPageIndex + 1)) + ".png";
                        File imageFile = new File(pdfFolder, imageName);
                        if (!imageFile.exists()) {
                            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                fos.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        page.close();
                        processedPageCount++;

                        runOnUiThread(() -> {
                            int progress = (int) (((float) processedPageCount / pageCount) * 100);
                            progressBar.setProgress(progress);
                            progressTextView.setText(progress + "%");
                        });

                        // Check if all pages are processed
                        if (processedPageCount == pageCount) {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                progressTextView.setVisibility(View.GONE);
                                pleaseWaitTextView.setVisibility(View.GONE);

                                List<File> imageFolders = getImageFolders();
                                imageFolderAdapter.setImageFolders(imageFolders);
                                imageFolderAdapter.notifyDataSetChanged();
                            });
                        }
                    } finally {
                        pdfRenderer.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private int getPdfPageCount(Uri pdfUri) {
        int pageCount = 0;
        try {
            ParcelFileDescriptor fileDescriptor = getContentResolver().openFileDescriptor(pdfUri, "r");
            PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);
            pageCount = pdfRenderer.getPageCount();
            pdfRenderer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pageCount;
    }



    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (displayNameIndex != -1) {
                    fileName = cursor.getString(displayNameIndex);
                }
            }
        }
        return fileName;
    }

    private void showToast(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openPDFPicker();
            } else {
                // Permission not granted. Show a dialog or navigate to app settings.
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }
    }
}