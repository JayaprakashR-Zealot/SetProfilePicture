package com.truedreamz.setprofilepicture;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;




public class AndroidCameraUtils extends AppCompatActivity implements SurfaceHolder.Callback{
    private static final String TAG= "AndroidCamera";
    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    boolean previewing = false;
    LayoutInflater controlInflater = null;

    ImageButton imgBtnCameraFlash;

    public final static String APP_PATH = "/SetDP/";
    public final static String AV_TEXTURE_PATH = "temp";
    private File fileUserPhoto;

    boolean isFrontCamera=true;
    boolean isBackCameraClicked=false;
    boolean isFlashON=false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getSupportActionBar().hide();
        setContentView(R.layout.activity_android_camera);

        getWindow().setFormat(PixelFormat.RGBA_8888);
        surfaceView = (SurfaceView)findViewById(R.id.camerapreview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        controlInflater = LayoutInflater.from(getBaseContext());
        View viewControl = controlInflater.inflate(R.layout.camera_control, null);
        LinearLayout.LayoutParams layoutParamsControl
                = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.FILL_PARENT);
        this.addContentView(viewControl, layoutParamsControl);

        imgBtnCameraFlash= (ImageButton)viewControl.findViewById(R.id.imgBtnFlash);

    }

    Camera.PictureCallback myPictureCallback_JPG = new Camera.PictureCallback(){

        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub
            if(arg0!=null){
                saveCameraImage(arg0);
            }
        }};

    private void saveCameraImage(byte[] imageByte) {
            String fullPath = APP_PATH + AV_TEXTURE_PATH;
            File dir = new File(getExternalFilesDir(null), fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            fileUserPhoto = new File(dir.getAbsolutePath(), "dp.png");
            try {
                fileUserPhoto.createNewFile();
            } catch (IOException ex) {
                Log.e(TAG, "IOException:" + ex.getLocalizedMessage());
            }

            try {
                FileOutputStream fos = new FileOutputStream(fileUserPhoto);

                if(isFrontCamera){
                    Bitmap realImage =makeSquare(imageByte,Camera.CameraInfo.CAMERA_FACING_FRONT);
                    boolean isSaved = realImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    Log.d(TAG, "saveCameraImage:" + isSaved);
                    fos.close();
                }else{
                    Bitmap realImage =makeSquare(imageByte,Camera.CameraInfo.CAMERA_FACING_BACK);
                    boolean isSaved = realImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    Log.d(TAG, "saveCameraImage:" + isSaved);
                    fos.close();
                }

                setResult(RESULT_OK);
                finish();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

    private Bitmap makeSquare(byte[] data, int cameraID) {
        int width;
        int height;
        Matrix matrix = new Matrix();
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraID, info);
        // Convert ByteArray to Bitmap
        Bitmap bitPic = BitmapFactory.decodeByteArray(data, 0, data.length);
        width = bitPic.getWidth();
        height = bitPic.getHeight();

        // Perform matrix rotations/mirrors depending on camera that took the photo
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            float[] mirrorY = { -1, 0, 0, 0, 1, 0, 0, 0, 1};
            Matrix matrixMirrorY = new Matrix();
            matrixMirrorY.setValues(mirrorY);

            matrix.postConcat(matrixMirrorY);
        }

        matrix.postRotate(90);

        // Create new Bitmap out of the old one
        Bitmap bitPicFinal = Bitmap.createBitmap(bitPic, 0, 0, width, height,matrix, true);
        bitPic.recycle();
        int desWidth;
        int desHeight;
        desWidth = bitPicFinal.getWidth();
        desHeight = desWidth;
        Bitmap croppedBitmap = Bitmap.createBitmap(bitPicFinal, 0,bitPicFinal.getHeight() / 2 - bitPicFinal.getWidth() / 2,desWidth, desHeight);
        croppedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 528, 528, true);
        return croppedBitmap;
    }

    public void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub
        openFrontCamera();
    }

    private void openFrontCamera(){
        if(previewing){
            //camera.stopFaceDetection();
            camera.stopPreview();
            previewing = false;
        }

        if(isBackCameraClicked){
            camera.release();
            camera = null;
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }

        if (camera != null){
            try {
                setCameraDisplayOrientation(AndroidCameraUtils.this, Camera.CameraInfo.CAMERA_FACING_FRONT, camera);
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();

                /*prompt.setText(String.valueOf(
                        "Max Face: " + camera.getParameters().getMaxNumDetectedFaces()));*/
                //camera.startFaceDetection();
                previewing = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void openBackCamera(){
        if(previewing){
            //camera.stopFaceDetection();
            camera.stopPreview();
            camera.release();
            camera = null;
            previewing = false;
        }

        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);

        isBackCameraClicked=true;

        if (camera != null){
            try {
                setCameraDisplayOrientation(AndroidCameraUtils.this, Camera.CameraInfo.CAMERA_FACING_BACK, camera);
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                /*prompt.setText(String.valueOf(
                        "Max Face: " + camera.getParameters().getMaxNumDetectedFaces()));*/
                //camera.startFaceDetection();
                previewing = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        //camera.setFaceDetectionListener(faceDetectionListener);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        //camera.stopFaceDetection();
        camera.stopPreview();
        camera.release();
        camera = null;
        previewing = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(camera!=null){
            camera.release();
        }
    }

    public void onFlashClick(View v){
        if(!isFlashON){
            isFlashON=true;
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(parameters);
        }else {
            isFlashON=false;
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(parameters);
        }
    }

    public void onTakePictureClick(View v){
        camera.takePicture(null,null, myPictureCallback_JPG);
    }


    public void onBackCameraClick(View v){
        if(isFrontCamera){
           openBackCamera();
            isFrontCamera=false;
            imgBtnCameraFlash.setVisibility(View.VISIBLE);
        }else{
            openFrontCamera();
            isFrontCamera=true;
            imgBtnCameraFlash.setVisibility(View.GONE);
        }
    }

    public void onHeaderBackClick(View v){
        finish();
    }
}