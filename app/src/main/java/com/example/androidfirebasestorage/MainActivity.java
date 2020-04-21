package com.example.androidfirebasestorage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.shashank.sony.fancytoastlib.FancyToast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.UUID;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int RESULT_LOAD_IMG = 1000; // Upload Image
    private static final int RESULT_SELECT_LOCAL_FILE = 2000; // Upload Local File
    private static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 2100 ;

    private Uri selectedFileUri;


    ImageView mImageView;
    Button btnUpload , btnSelectLocalFile;
    EditText edtFileName, edtFileDownloadLink;
    TextView textUploading , textUUIDFileName;
    ProgressBar mProgressBar;

    private String ImageExtension , mSelectedImageName;

    // Firebase Storage Ref.
    private StorageReference mStorageRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.imageView);
        mImageView.setOnClickListener(MainActivity.this);

        btnUpload = findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(MainActivity.this);
        btnUpload.setClickable(false); // if Image Is Not Selected Not Allow User To Press Upload Button

        btnSelectLocalFile = findViewById(R.id.btnSelectLocalFile);
        btnSelectLocalFile.setOnClickListener(MainActivity.this);

        edtFileName = findViewById(R.id.edtImageName);
        edtFileName.setText("Please Select Your Target Image First !");

        edtFileDownloadLink = findViewById(R.id.edtImageDownloadLink);

        textUploading = findViewById(R.id.textView3);
        textUUIDFileName = findViewById(R.id.textUUIDImageName);
        textUUIDFileName.setOnClickListener(MainActivity.this);
        textUUIDFileName.setClickable(false);


        mProgressBar = findViewById(R.id.progressBar);

        makeInfoToast(MainActivity.this , "Please Select Image or File First !");

        // Firebase Storage Ref.
        mStorageRef = FirebaseStorage.getInstance().getReference();

    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.imageView) {
            getImageLocation();
        }

        if (v.getId() == R.id.btnUpload) {

            // Show Progress Bar
            textUploading.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);

            // If Upload Image
            if (mImageView.isClickable() && !btnSelectLocalFile.isClickable()) {
                Log.i("btn Upload : " , "Upload Image");
                // Upload Photo Form ImageView To Firebase Storage
                mImageView.setDrawingCacheEnabled(true);
                mImageView.buildDrawingCache();
                Bitmap bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG , 100 , outputStream);
                byte[] data = outputStream.toByteArray();

                final StorageReference imageRef = mStorageRef.child(edtFileName.getText().toString()); // Create Firebase Storage Ref. To Upload To It.
                StorageMetadata imageMetaData = new StorageMetadata.Builder() // Get Image / File Meta Data If It Have or Your Want !
                        .setContentType("image/jpg")
                        .build();

                if (NetworkInfo.getNetworkStatus(MainActivity.this) == 1 && NetworkInfo.getNetworkStatus(MainActivity.this) == 2) { // Check User Network Connection
                    UploadTask uploadTask = imageRef.putBytes(data , imageMetaData);
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Show Error Toast Notification
                            makeErrorToast(MainActivity.this , "Your Image Was Upload UnSuccessful Please Try Again !");
                            makeErrorAlertDialog(MainActivity.this);
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Show Success Toast Notification
                            makeSuccessToast(MainActivity.this , "Your Image Upload Was Successful !");
                            // Update Download Uri To User
                            taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    edtFileDownloadLink.setText(task.getResult().toString());
                                    makeSuccessAlertDialog(MainActivity.this);
                                }
                            });
                        }
                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            mProgressBar.setProgress((int) progress);
                        }
                    }).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            // On Complete
                        }
                    });
                } else { // Make Error Toast Notification If User No Internet Connection
                    makeErrorToast(MainActivity.this , "Upload Cancelled Your Don't Have Network Connection");
                }
            }

                // If Upload Local File
                if (!mImageView.isClickable() && btnSelectLocalFile.isClickable()) {
                    Log.i("btn Upload : " , "Upload File");
                    StorageReference fileRef = mStorageRef.child("Local_File/" + edtFileName.getText().toString());
                    StorageMetadata fileMetaData = new StorageMetadata.Builder() // Get Image / File Meta Data If It Have or Your Want !
                            .setContentType("*/*")
                            .build();
                    UploadTask uploadTask = fileRef.putFile(selectedFileUri, fileMetaData);
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Show Error Toast Notification
                            makeErrorToast(MainActivity.this , "Your File Was Upload UnSuccessful Please Try Again !");
                            makeErrorAlertDialog(MainActivity.this);
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Show Success Toast Notification
                            makeSuccessToast(MainActivity.this , "Your File Upload Was Successful !");
                            // Update Download Uri To User
                            taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    edtFileDownloadLink.setText(task.getResult().toString());
                                    makeSuccessAlertDialog(MainActivity.this);
                                }
                            });
                        }
                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            mProgressBar.setProgress((int) progress);
                        }
                    });
                } else { // Make Error Toast Notification If User No Internet Connection
                    makeErrorToast(MainActivity.this , "Upload Cancelled Your Don't Have Network Connection");
                }
        }

        if (v.getId() == R.id.btnSelectLocalFile) {
            // Check User Permission Before Access Storage
            if (!EasyPermissions.hasPermissions(MainActivity.this , Manifest.permission.READ_EXTERNAL_STORAGE)) {
                EasyPermissions.requestPermissions(this, getString(R.string.permission_read_external_storage),
                        EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE, Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent = Intent.createChooser(intent , "Select Your File To Upload");
            startActivityForResult(intent , RESULT_SELECT_LOCAL_FILE);
        }

        if (v.getId() == R.id.textUUIDImageName) {
            edtFileName.setText(generateUUID() + "." + ImageExtension);
        }

    }

    private void getImageLocation() {

        try {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK && data != null) { // Get Selected Image And Show On ImageView || Update TextView Image Name
            makeInfoToast(MainActivity.this , "Your Image Is Selected !");
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                File file = new File(String.valueOf(imageUri));
                mSelectedImageName = file.getName(); // Get Selected Image Name Form Uri
                ImageExtension = getContentResolver().getType(imageUri);
                ImageExtension = ImageExtension.substring(ImageExtension.lastIndexOf("/") + 1); // SubString File Ext. Only . MIME Type
                mImageView.setImageBitmap(selectedImage); // Set Selected Image On ImageView
                edtFileName.setText(mSelectedImageName + "." + ImageExtension); // Set Current Image Name Appere To User
                btnSelectLocalFile.setClickable(false); // Not Allow User To Select File When Selected Photo
                textUUIDFileName.setClickable(true); // Allow User Can Click After Selected Image
                btnUpload.setClickable(true); // Allow User Can Click After Selected Image
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (requestCode == RESULT_SELECT_LOCAL_FILE && resultCode == RESULT_OK && data != null) {
            makeInfoToast(MainActivity.this , "Your Local File Is Selected !");
            selectedFileUri = data.getData();
            File file = new File(selectedFileUri.getPath());
            edtFileName.setText(file.getName()); // Set Selected File Name On Edittext
            btnSelectLocalFile.setText("File : " + edtFileName.getText().toString()); // Set Selected Local File Name On Button
            mImageView.setClickable(false); // Not Allow User To Select Photo When Selected File
            textUUIDFileName.setClickable(true); // Allow User Can Click After Selected Image
            btnUpload.setClickable(true); // Allow User Can Click After Selected Image
        }
    }

    private void makeInfoToast(Context context , String text) { // Call This Method To Show Info Toast
        FancyToast.makeText(context ,text ,FancyToast.LENGTH_LONG , FancyToast.INFO ,false).show();
    }

    private void makeErrorToast(Context context , String text) { // Call This Method To Show Error Toast
        FancyToast.makeText(context , text , Toast.LENGTH_LONG , FancyToast.ERROR , false).show();
    }

    private void makeSuccessToast(Context context , String text) { // Call This Method To Show Success Toast
        FancyToast.makeText(context , text , Toast.LENGTH_LONG , FancyToast.SUCCESS , false).show();
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

    private String getFileExtension(Uri uri) { // Not Use In This Project But Create For Example Propuse
        String uriToString = getContentResolver().getType(uri);
        uriToString = uriToString.substring(uriToString.lastIndexOf("/") + 1);
        return uriToString;
    }

    private void makeSuccessAlertDialog(Context context) { // Call This Method To Show Success Dialog
        AlertDialog dialog = new AlertDialog.Builder(context).create();
        dialog.setTitle("Your Image / File Upload Is Successfully !");
        dialog.setMessage("Click View To View Your Uploaded Image / File In Browser");
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "View", new DialogInterface.OnClickListener() { // Open Uploaded Image On Browser
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(edtFileDownloadLink.getText().toString()));
                startActivity(intent);
            }
        });
        dialog.show();
    }

    private void makeErrorAlertDialog(Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context).create();
        dialog.setTitle("Error To Upload Your Image / File !");
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Dismiss" , new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

}
