package com.akram.mangaman;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class ImageFolderAdapter extends BaseAdapter {

    private Context context;
    private List<File> imageFolders;

    public ImageFolderAdapter(Context context, List<File> imageFolders) {
        this.context = context;
        this.imageFolders = imageFolders;
    }

    public void setImageFolders(List<File> imageFolders) {
        this.imageFolders = imageFolders;
    }

    @Override
    public int getCount() {
        return imageFolders.size();
    }

    @Override
    public Object getItem(int position) {
        return imageFolders.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.grid_item_card, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.imageView = convertView.findViewById(R.id.imageView);
            viewHolder.textView = convertView.findViewById(R.id.folderNameTextView);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        File imageFolder = imageFolders.get(position);
        String folderName = imageFolder.getName();
        viewHolder.textView.setText(folderName);

        // Load "page_001.png" as the thumbnail
        File thumbnailFile = new File(imageFolder, "page_001.png");
        if (thumbnailFile.exists()) {
            Glide.with(context)
                    .load(thumbnailFile)
                    .centerCrop()
                    .into(viewHolder.imageView);
        } else {
            // Show a placeholder image if "page_001.png" doesn't exist
            viewHolder.imageView.setImageResource(R.drawable.placeholder_image);
        }

        convertView.setOnClickListener(v -> {
            // Start ImageDisplayActivity and pass the selected folder's path as an extra
            Intent intent = new Intent(context, ImageDisplayActivity.class);
            String folderPath = imageFolders.get(position).getPath();
            intent.putExtra("folder_path", folderPath);
            context.startActivity(intent);
        });

        convertView.setOnLongClickListener(v -> {
            showOptionsDialog(position);
            return true;
        });

        return convertView;
    }

    private void showOptionsDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setItems(new CharSequence[]{"Rename", "Delete"}, (dialog, option) -> {
            if (option == 0) {
                showRenameDialog(position);
            } else if (option == 1) {
                showDeleteConfirmationDialog(position);
            }
        });
        builder.show();
    }

    private void showDeleteConfirmationDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Folder");
        builder.setMessage("Are you sure you want to delete this folder?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteFolder(position);
        });

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void deleteFolder(int position) {
        File imageFolder = imageFolders.get(position);
        boolean deleted = deleteRecursive(imageFolder);

        if (deleted) {
            imageFolders.remove(position);
            notifyDataSetChanged();
        }
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return file.delete();
    }


    private void showRenameDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Rename Folder");

        // Create an EditText input field
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            renameFolder(position, newName);
        });

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void renameFolder(int position, String newName) {
        File imageFolder = imageFolders.get(position);
        File newFolder = new File(imageFolder.getParentFile(), newName);
        boolean renamed = imageFolder.renameTo(newFolder);

        if (renamed) {
            imageFolders.set(position, newFolder);
            notifyDataSetChanged();
        }
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView textView;
    }
}