package com.example.cameraapplication;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

//import com.example.cameraapplication.adapter.RetrofitClient;
import com.example.cameraapplication.adapter.RetrofitClient;
import com.example.cameraapplication.adapter.RetrofitServices;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity{

    private ImageView imageView;
    private VideoView videoView;
    private static final int CAPTURE_REQUEST = 1;
    private static final int VIDEO_REQUEST = 2;
    private static final int SELECT_PHOTO = 3;
    private StorageReference storageRef;
    String token ="";
    Retrofit retrofit;
    RetrofitServices retrofitServices;
    String currentPhotoPath;
    Intent[] intentArray;
    String imageFileName;
    File image;
    String dUrl = "";
    private ProgressBar progressBar;
    private TextView progressText;
    ConstraintLayout progressLayout;
//    private EditText outputUrlText;
    String outputUrl;
    ImageUploader imageUploader;
//    Button saveToDevice;
    TouchImageView fullImageView;
    Bundle bundle;
    boolean dialogOpen = false;
    Dialog imageDialog;
    ImageButton dismissButton;
    ProgressBar fullScreenProgress;
//    String link = "";
    Boolean dialog = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.save_menu) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button videoButton = findViewById(R.id.video_button);
        Button cameraButton = findViewById(R.id.camera_button);
        Button pickButton = findViewById(R.id.pick_btn);
//        saveToDevice = findViewById(R.id.save_button);
        imageView = findViewById(R.id.image_view);
        imageView.setVisibility(View.INVISIBLE);
        videoView = findViewById(R.id.video_view);
        videoView.setVisibility(View.INVISIBLE);
        storageRef = FirebaseStorage.getInstance().getReference();
        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);
        progressLayout = findViewById(R.id.progress_layout);
        progressLayout.setVisibility(View.INVISIBLE);


        imageDialog = new Dialog(MainActivity.this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        imageDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//                            imageDialog.setCancelable(false);
        imageDialog.setContentView(R.layout.full_screen_image);

        dismissButton = imageDialog.findViewById(R.id.image_dismiss_button);
        fullImageView = imageDialog.findViewById(R.id.full_image_view);
        fullScreenProgress = imageDialog.findViewById(R.id.full_image_progress_bar);

//        saveToDevice.setActivated(false);

        cameraButton.setOnClickListener(view->{
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra("android.intent.extra.quickCapture",true);

            File file = null;
//            Uri uri = null;
            try {
                file = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(file != null){
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.cameraapplication",
                        file);
                Log.d("URI", photoURI.toString());
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            }

            if(cameraIntent.resolveActivity(getPackageManager()) != null){
                if(cameraIntent != null){
//                    intentArray = new Intent[]{cameraIntent};
                    startActivityForResult(cameraIntent, CAPTURE_REQUEST);
                }
                else{
//                    intentArray = new Intent[0];
                }
            }


        });

        videoButton.setOnClickListener(view -> {
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra("android.intent.extra.quickCapture",true);
            if(intent.resolveActivity(getPackageManager()) != null){
                startActivityForResult(intent, VIDEO_REQUEST);
            }
        });


        pickButton.setOnClickListener(view -> pickFile());


//        if(savedInstanceState != null){
//            dUrl = savedInstanceState.getString("link");
//            dialog = savedInstanceState.getBoolean("dialogOpen");
//
//            if(dialog){
//                dialog(dUrl);
//            }else{
//                setImage(dUrl);
//            }
//        }


    }




    private void pickFile(){
        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        photoPickerIntent.setType("*/*");
        if(photoPickerIntent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(photoPickerIntent, SELECT_PHOTO);
        }

    }


    private void upload(Uri u){
        progressLayout.bringToFront();
//        progressBar.setVisibility(View.VISIBLE);
        progressText.setText(getResources().getString(R.string.uploading_image));
//        progressText.setVisibility(View.VISIBLE);
        progressLayout.setVisibility(View.VISIBLE);

        Log.d("upload file uri", u.toString());
        StorageReference ref = storageRef.child("images/"+u.getLastPathSegment());

        ref.putFile(u)
                .addOnSuccessListener(taskSnapshot -> {
                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        dUrl = uri.toString();
                        Toast.makeText(MainActivity.this, "Url Stored.", Toast.LENGTH_LONG).show();
                        Log.d("URL", dUrl);
//                        useRetrofit(dUrl);
                        setImage(dUrl);
                        imageView.setOnClickListener(view -> {
                            dialog(dUrl);

                        });

                    })
                            .addOnFailureListener(e -> Log.d("download url error", e.getMessage()));

                    progressLayout.setVisibility(View.INVISIBLE);
                    Toast.makeText(MainActivity.this, "Uploaded to Firebase.", Toast.LENGTH_LONG).show();

                })
                .addOnFailureListener(e -> {
                    Log.d("Firebase upload error", e.getMessage());
                    progressLayout.setVisibility(View.INVISIBLE);
                });
    }

    private void dialog(String dUrl){
        fullScreenProgress.setVisibility(View.VISIBLE);
        fullScreenProgress.bringToFront();

        try{
            Log.d("picasso","trying picasso");
            Picasso.with(MainActivity.this).load(dUrl).into(fullImageView);

            Log.d("picasso", "picasso done");
            fullScreenProgress.setVisibility(View.INVISIBLE);
        }
        catch (Exception e){
            Log.d("picasso 1", "no download");
            Log.d("picasso 2", e.getMessage());
            fullScreenProgress.setVisibility(View.INVISIBLE);
        }
        dismissButton.setOnClickListener(view1 -> imageDialog.dismiss());

        try{
            imageDialog.show();
            dialogOpen = true;
        }catch(Exception e){e.printStackTrace();}
    }

    private void setImage(String dUrl){
        videoView.setVisibility(View.INVISIBLE);
        progressText.setText(getResources().getString(R.string.downloading_image));
        progressLayout.setVisibility(View.VISIBLE);
        progressLayout.bringToFront();

        Log.d("picasso", "inside setImage");
        try{
            Log.d("picasso","trying picasso");
            Picasso.with(this).load(dUrl).into(imageView);
            imageView.setVisibility(View.VISIBLE);
            Log.d("picasso", "picssso done");
            progressLayout.setVisibility(View.INVISIBLE);

        }
        catch (Exception e){
            Log.d("picasso 1", "no download");
            Log.d("picasso 2", e.getMessage());
            progressLayout.setVisibility(View.INVISIBLE);
        }


    }

    private void saveToDevice(File file){

//        File SDCardRoot = Environment.getExternalStorageDirectory();
//        //create a new file, specifying the path, and the filename which we want to save the file as.
//        File newfile = new File(SDCardRoot,"image.jpg");
//        //this will be used to write the downloaded data into the file we created
//        FileOutputStream fileOutput = null;
//        try {
//            fileOutput = new FileOutputStream(newfile);

//            InputStream inputStream = newfile.getInputStream();

//            int totalSize = urlConnection.getContentLength();

//            int downloadedSize = 0;

//            byte[] buffer = new byte[1024];

//            int bufferLength = 0; //used to store a temporary size of the buffer

//            while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
//                //add the data in the buffer to the file in the file output stream (the file on the sd card
//                fileOutput.write(buffer, 0, bufferLength);
//                //add up the size so we know how much is downloaded
//                downloadedSize += bufferLength;
//                //this is where you would do something to report the prgress, like this maybe
//                //updateProgress(downloadedSize, totalSize);
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

//        file.renameTo(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "CameraApp"));

        File destination = new File(Environment.getExternalStorageDirectory() + File.separator + "CameraApp");

        if (!destination.exists()) {
            Log.d("mkdirs", "not exist");
            try{
                destination.mkdirs();
                Log.d("mkdir try", "done");
                Log.d("mkdir boolean", destination.exists()+"");
            }catch(Exception e){
                Log.d("mkdir catch", e.getMessage());
            }
        }

//        File newFile = new File(destination, "saved_file");
        File newFile = new File(destination.getAbsolutePath() + File.separator + file.getName());

        if(!newFile.exists()){
            try {
                newFile.createNewFile();
                Log.d("mkdir newfile boolean", newFile.exists()+"");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.d("newfile absolute", newFile.getAbsolutePath());
        FileChannel outputChannel = null;
        FileChannel inputChannel = null;
        try {
            outputChannel = new FileOutputStream(newFile).getChannel();
            inputChannel = new FileInputStream(file).getChannel();
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
            inputChannel.close();
            Toast.makeText(this, "File Saved", Toast.LENGTH_LONG).show();
            file.delete();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Not Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Not Saved", Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (inputChannel != null) {
                        inputChannel.close();
                }
                if (outputChannel != null) outputChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    private void setImageFromStorage(Uri pickUri){
//        videoView.setVisibility(View.INVISIBLE);
//        progressLayout.bringToFront();
//        progressBar.setVisibility(View.VISIBLE);
//        progressText.setText(getResources().getString(R.string.downloading_image));
//        progressText.setVisibility(View.VISIBLE);
//
//        File pickFile = new File(pickUri.getPath());
//        try {
//            Picasso.with(this).load(pickFile).into(imageView);
//            imageView.setVisibility(View.VISIBLE);
//            progressText.setVisibility(View.INVISIBLE);
//            progressBar.setVisibility(View.INVISIBLE);
//        }
//        catch (Exception e){
//            Log.d("picasso storage", "failed");
//            Log.d("picasso storage", e.getMessage());
//            progressText.setVisibility(View.INVISIBLE);
//            progressBar.setVisibility(View.INVISIBLE);
//        }
//
//    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//
        if (requestCode == CAPTURE_REQUEST) {
            Log.d("request Code", "correct");
            if(resultCode == RESULT_OK){
                Log.d("result Code", "correct");

//                Log.d("on activity Uri", mUri.toString());
                Uri mUri = Uri.fromFile(image);

                try {
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(mUri);
                    this.sendBroadcast(mediaScanIntent);
                    Log.d("gallery try", "added");
                }
                catch (Exception e){
                    Log.d("gallery", "not added");
                    e.printStackTrace();
                }

//                saveToDevice.setActivated(true);
//                saveToDevice.setOnClickListener(view -> saveToDevice(image));

                upload(mUri);


//                galleryAdd(mUri);
            }
            else Log.d("result code", "wrong");
        }
        else{
            Log.d("request code", "wrong");
        }

        if(requestCode == SELECT_PHOTO){
            if(resultCode == RESULT_OK){
                if(data != null){
                    Uri pickUri = data.getData();
                    if(pickUri.toString().contains("image")){
                        upload(pickUri);
//                        setImageFromStorage(pickUri);
                    }
                    else if(pickUri.toString().contains("video")){
                        setVideo(pickUri);
                        uploadVideo(pickUri);
                    }



                }
                else Log.d("pick data", "null");
            }
            else Log.d("pick result", "wrong");
        }
        else Log.d("pick request", "wrong");




        if (requestCode == VIDEO_REQUEST && resultCode == RESULT_OK && data != null) {
            if(resultCode == RESULT_OK ){
                if(data != null){
                    Uri videoUri = data.getData();
                    Log.d("video uri", videoUri.toString());
                    setVideo(videoUri);
                    uploadVideo(videoUri);
                }
                else Log.d("video data", "null");
            }
            else Log.d("video result", "wrong");
        }
        else Log.d("video request", "wrong");
    }

    private void setVideo(Uri videoUri){
        imageView.setVisibility(View.INVISIBLE);
        videoView.setVideoURI(videoUri);
        videoView.setFocusable(true);
        videoView.seekTo(1);
        MediaController mc = new MediaController(this);
        videoView.setMediaController(mc);

        videoView.setVisibility(View.VISIBLE);
//        saveToDevice.setActivated(true);
    }

    private void uploadVideo(Uri videoUri){
        progressLayout.bringToFront();
        progressText.setText(getResources().getString(R.string.uploading_video));
        progressLayout.setVisibility(View.VISIBLE);

        Log.d("video uri", videoUri.toString());

        StorageReference videoRef = storageRef.child("videos/"+videoUri.getLastPathSegment());

        videoRef.putFile(videoUri)
                .addOnSuccessListener(taskSnapshot ->{
                    videoRef.getDownloadUrl()
                            .addOnSuccessListener(url->{
                                Log.d("video URL", url.toString());
                                String videoURL = url.toString();
                                Toast.makeText(this, "Video Uploaded", Toast.LENGTH_LONG).show();
                                Toast.makeText(this, "URL stored.", Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> Log.d("video url", "failed"));
                    progressLayout.setVisibility(View.INVISIBLE);
                })
                .addOnFailureListener(e -> {
                    Log.d("video upload", "failed");
                    progressLayout.setVisibility(View.INVISIBLE);
                });

    }



    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imageFileName = "SI_" + timeStamp+"_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        Log.d("AbsolutePath", currentPhotoPath);

        return image;
    }

    private void galleryAdd(Uri contentUri) {
        try {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//        File f = new File(currentPhotoPath);
//        Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
        }
        catch (Exception e){
            Log.d("gallery", "not added");
        }

    }



    private void useRetrofit(String inputUrl){

        retrofit = RetrofitClient.getRetrofit(token);
        retrofitServices = retrofit.create(RetrofitServices.class);

        imageUploader = new ImageUploader();
        imageUploader.setImageId(null);
        imageUploader.setInputUrl(inputUrl);
        imageUploader.setOutputUrl(null);
        Call<ImageUploader> imageUploaderCall = retrofitServices.uploadImage(imageUploader);

        imageUploaderCall.enqueue(new Callback<ImageUploader>() {
            @Override
            public void onResponse(Call<ImageUploader> call, Response<ImageUploader> response) {
                ImageUploader imageUploader = response.body();
                if(imageUploader != null){
                    outputUrl = imageUploader.getOutputUrl();
                    Log.d("output url", outputUrl);
                    setImage(outputUrl);
//                    setOutputUrl(outputUrl);


                }
                else Log.d("retrofit response", "no output url");
            }

            @Override
            public void onFailure(Call<ImageUploader> call, Throwable t) {
                Log.d("retrofit", "failed");
                Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                Log.d("retrofit", t.getMessage());
            }
        });
    }

}