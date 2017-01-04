package com.truedreamz.setprofilepicture;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static com.truedreamz.setprofilepicture.AndroidCameraUtils.APP_PATH;
import static com.truedreamz.setprofilepicture.AndroidCameraUtils.AV_TEXTURE_PATH;
import static com.truedreamz.setprofilepicture.R.id.imgViewProfile_pic;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    //popup action id
    public static final int ID_GALLERY     = 1;
    public static final int ID_CAMERA   = 2;

    public static final int CAMERA_PERMISSION_REQUEST_CODE = 3;

    public static final int TAKE_IMAGE_REQUEST = 4;
    public static final int PICK_IMAGE_REQUEST = 5;

    TDQuickPopup avatarPopup;
    private File fileUserPhoto;
    private ImageView imgProfilePic;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        imgProfilePic=(ImageView)findViewById(imgViewProfile_pic);

        loadPickImagePopup();
    }

    private void checkCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "SDK >= 23");
            if (this.checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Request permission");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST_CODE);
            }
            else {
                Log.d(TAG, "Permission granted: taking pic");
                openCamera();
            }
        }
        else {
            Log.d(TAG, "Android < 6.0");
            openCamera();
        }
    }

    private void openCamera(){
        // Invoking camera with full image output - EXTRA_OUTPUT
        Intent intent=new Intent(ProfileActivity.this,AndroidCameraUtils.class);
        startActivityForResult(intent,TAKE_IMAGE_REQUEST);
    }


    private void loadPickImagePopup(){
        TDActionItem galleryItem 	= new TDActionItem(ID_GALLERY, "Gallery",
                getResources().getDrawable(R.drawable.gallery_popup));
        TDActionItem cameraItem 	= new TDActionItem(ID_CAMERA, "Camera",
                getResources().getDrawable(R.drawable.camera_popup));

        //use setSticky(true) to disable QuickAction dialog being dismissed after an item is clicked
        galleryItem.setSticky(true);
        cameraItem.setSticky(true);

        //create QuickAction. Use QuickAction.VERTICAL or QuickAction.HORIZONTAL param to define layout
        //orientation
        avatarPopup= new TDQuickPopup(this, TDQuickPopup.VERTICAL);

        //add action items into QuickAction
        avatarPopup.addActionItem(galleryItem);
        avatarPopup.addActionItem(cameraItem);

        //Set listener for action item clicked
        avatarPopup.setOnActionItemClickListener(new TDQuickPopup.OnActionItemClickListener() {
            @Override
            public void onItemClick(TDQuickPopup source, int pos, int actionId) {
                TDActionItem actionItem = avatarPopup.getActionItem(pos);
                //here we can filter which action item was clicked with pos or actionId parameter
                if (actionId == ID_GALLERY) {
                    pickGalleryImage();
                } else if (actionId == ID_CAMERA) {
                    checkCameraPermission();
                }
                source.dismiss();
            }
        });
    }

    private void pickGalleryImage(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private Bitmap getGalleryImage(Uri selectedImage ){
        InputStream imageStream = null;
        try {
            imageStream = getContentResolver().openInputStream(
                    selectedImage);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Bitmap bmp = BitmapFactory.decodeStream(imageStream);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 50, stream);
        return bmp;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try{
                Uri selectedImage = data.getData();
                imgProfilePic.setImageBitmap(getGalleryImage(selectedImage));
            }
            catch (Exception ex){
                Log.e(TAG,"IOException:"+ex.getLocalizedMessage());
                finish();
            }
        }if (requestCode == TAKE_IMAGE_REQUEST && resultCode == RESULT_OK)
        {
            String fullPath = APP_PATH + AV_TEXTURE_PATH;
            File dir = new File(getExternalFilesDir(null), fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            fileUserPhoto = new File(dir.getAbsolutePath(), "dp.png");
            setUserPhoto(fileUserPhoto.getAbsolutePath());
        }
    }

    private void setUserPhoto(String path){
        Bitmap bitmap =rotatedBitmap(path);
        if(bitmap!=null){
            imgProfilePic.setImageBitmap(bitmap);
        }
        else{
            Toast.makeText(ProfileActivity.this,"Error : To take picture", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap rotatedBitmap(String path){
        try{
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            //opts.inJustDecodeBounds = true;
            Bitmap bm = BitmapFactory.decodeFile(path, opts);
            if(bm!=null){
                Matrix matrix = new Matrix();
                Bitmap rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true);
                return  rotatedBitmap;
            }else{
                return  null;
            }
        }catch (Exception ex){
            return  null;
        }
    }

    public void onSetPictureClick(View v){
        avatarPopup.show(v);
    }
}
