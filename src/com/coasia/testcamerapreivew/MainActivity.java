package com.coasia.testcamerapreivew;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.FaceDetector;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.Menu;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

public class MainActivity extends Activity   implements Callback, PreviewCallback, OnClickListener{
    private Camera mCamera = null;
    private static final String TAG = "testcamerapreivew ";
    private static final int CAMERA_NUM = 0;
    private static final int MAX_FACES = 10;
    private static final float FACE_RECT_RATIO = 1.5f;
    private static final int FACE_RECT_COLOR = Color.RED;
    private SurfaceHolder mHolder;
    private SurfaceView mSurfaceView;
    private byte[] rgbBuffer;
    private SurfaceView mFaceView;
    float faceConfidence=0.5f;//default faceDetector return face with confidence > 0.4f which was set in framework
    int w =640;
    int h =480;
    float xScale =1f;
    float yScale =1f;
    private Parameters p;
    private FaceDetector.Face[] faces; 
    private int face_count;
    boolean debug =true;
    void __log(String str){
        if(debug)Log.i(TAG, str);    
    }
    boolean swFd=false;
    boolean hwFd=false;
    int orientation =0;
    byte []buffer1 = null; 
    byte []buffer2 = null;
    byte []buffer3 = null;
    Button btnCapture;
    CameraInfo cameraInfo = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(this.checkCameraHardware(this)) {
            mCamera =MainActivity.getCameraInstance();
        }
      
        mOrientationListener = new MyOrientationEventListener(this);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        cameraInfo =new CameraInfo();
        Camera.getCameraInfo(CAMERA_NUM, cameraInfo);
        setContentView(R.layout.activity_main);
        btnCapture = (Button) this.findViewById(R.id.button_capture);
        btnCapture.setOnClickListener(this);
        mSurfaceView = (SurfaceView) this.findViewById(R.id.surfaceView_camera);
        
        mFaceView =(SurfaceView)this.findViewById(R.id.face);        
        mFaceView.getHolder().addCallback(this);
        mFaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        mFaceView.setZOrderOnTop(true);
        
        
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
       // mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        p = mCamera.getParameters();
        List<Size> sizes = p.getSupportedPreviewSizes();
        __log("supported resolution============");
        for(Size size :sizes) {
            __log(size.width+"x"+size.height);         
        }
        //w=p.getPreferredPreviewSizeForVideo().width;
        //h=p.getPreferredPreviewSizeForVideo().height;
        p.setPreviewSize(w, h);
       // if(p.getSupportedFocusModes().contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
        //p.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE); will invalid due to preview not started yet 
        orientation =cameraInfo.orientation;
        mCamera.setParameters(p);
        mCamera.setPreviewCallback(this);
        
        PixelFormat pf = new PixelFormat();
        PixelFormat.getPixelFormatInfo(p.getPreviewFormat(),pf);
        int bufSize = (w*h*pf.bitsPerPixel)/8;
        __log("buffer============");
        //把buffer給preview callback備用
        
       //  buffer1 = new byte[bufSize];
         //buffer2 = new byte[bufSize];
        buffer3 = new byte[bufSize];
         rgbBuffer = new byte[w*h*2];
         
        /*mCamera.addCallbackBuffer(buffer1 );
        mCamera.addCallbackBuffer(buffer2 );
       mCamera.addCallbackBuffer(buffer3 );
       mCamera.setPreviewCallbackWithBuffer(this);
       
         __log("setPreviewCallbackWithBuffer============");*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(CAMERA_NUM); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
    
     Bitmap getBitmapImageFromYUV(byte[] data, int width, int height) {
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 80, baos);
        byte[] jdata = baos.toByteArray();
        BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
        bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length, bitmapFatoryOptions);
        return bmp;
 }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        if(holder!=mHolder)return;
        __log("surfaceChanged " +width+"x"+height);
        if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        // stop preview before making change
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
           //mCamera.setPreviewCallbackWithBuffer(this); 
        //    mCamera.setPreviewCallback(this);
            mCamera.addCallbackBuffer(buffer3 );
            mCamera.setPreviewCallbackWithBuffer(this);
            
              __log("setPreviewCallbackWithBuffer============");
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(orientation);
            mCamera.startPreview();
            p.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(p);
        } catch (Exception e){
            __log("Error starting camera preview: " + e.getMessage());
        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        if(holder!=mHolder)return;
        try {
           // mCamera.setPreviewCallbackWithBuffer(this);
           // mCamera.setPreviewCallback(this);
            mCamera.addCallbackBuffer(buffer3 );
            mCamera.setPreviewCallbackWithBuffer(this);
            
              __log("setPreviewCallbackWithBuffer============");
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(orientation);
            mCamera.startPreview();
            __log("surfaceCreated started preview");
            p.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(p);
        } catch (IOException e) {
            __log("Error setting camera preview: " + e.getMessage());
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        mOrientationListener.disable();
        mCamera.stopPreview();
        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
        
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        mOrientationListener.enable();
        if(mCamera == null)
            mCamera = this.getCameraInstance();
        //mCamera.addCallbackBuffer(buffer1 );
        //mCamera.addCallbackBuffer(buffer2 );
        //mCamera.addCallbackBuffer(buffer3 );

       //mCamera.setPreviewCallback(this);
    }

    
    private MyOrientationEventListener mOrientationListener;
    // The degrees of the device rotated clockwise from its natural orientation.
    private int mLastScreenOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private int mSensorOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // TODO Auto-generated method stub
        //__log("onPreviewFrame enter");
        
        //toRGB565(data,w,h,rgbBuffer);
        
       // camera.addCallbackBuffer(data);
       // camera.setPreviewCallbackWithBuffer(this); 
        
       // Log.i(TAG, "onPreviewFrame2");  
        
        
        
        Bitmap background_image = getBitmapImageFromYUV(data, w, h);
        if(mRotateMatrix!=null)
            background_image = Bitmap.createBitmap(background_image, 0, 0, w, h, mRotateMatrix, true);
        
        FaceDetector face_detector = new FaceDetector( 
        background_image.getWidth(), background_image.getHeight(), 
        MAX_FACES); 

        faces = new FaceDetector.Face[MAX_FACES]; 
        // The bitmap must be in 565 format (for now). 
        face_count = face_detector.findFaces(background_image, faces); 
        if(face_count>0)   __log(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>Face_Detection Count: " + String.valueOf(face_count)); 
       
        if(count_capture>0) {
            this.storeImage(background_image);
            count_capture=0;
        }
        background_image.recycle();
        background_image = null;
        
        
            final Surface surface = mFaceView.getHolder().getSurface();
            if(surface == null|| !surface.isValid())  Log.e(TAG, "surface is null or not valid");
            Canvas canvas = surface.lockCanvas(null);
            if (canvas == null) {
                Log.e(TAG, "Cannot draw onto the canvas as it's null");
            } else {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                if(face_count>0) drawFaceRetangle(canvas,faces,face_count);
                surface.unlockCanvasAndPost(canvas);
            }
        
        
        

        //camera.setPreviewCallbackWithBuffer(this);
        camera.addCallbackBuffer(data);
        
       // __log("onPreviewFrame exit");
    }   

private void storeImage(Bitmap image) {
    File pictureFile = getOutputMediaFile();
    if (pictureFile == null) {
        Log.d(TAG,
                "Error creating media file, check storage permissions: ");// e.getMessage());
        return;
    } 
    try {
        FileOutputStream fos = new FileOutputStream(pictureFile);
        image.compress(Bitmap.CompressFormat.PNG, 90, fos);
        fos.close();
    } catch (FileNotFoundException e) {
        Log.d(TAG, "File not found: " + e.getMessage());
    } catch (IOException e) {
        Log.d(TAG, "Error accessing file: " + e.getMessage());
    }  
}

    private void drawFaceRetangle(Canvas canvas, android.media.FaceDetector.Face[] faces, int face_count) {

      
        xScale =mFaceView.getWidth();       
        yScale =mFaceView.getHeight();
        
        if(cameraInfo.orientation%180==90){
            xScale /=h;
            yScale /=w;
        } else {
            xScale /=w;
            yScale /=h;
        }
        
        Paint tmp_paint = new Paint();
         PointF tmp_point = new PointF();
         RectF r= new RectF();
         FaceDetector.Face face;
         float eyeDis=0f;
         //face.eyesDistance();
        for (int i = 0; i < face_count; i++) { 
             face = faces[i];              
            if(faceConfidence>face.confidence())return;
            eyeDis=face.eyesDistance()*FACE_RECT_RATIO;
            tmp_paint.setColor(FACE_RECT_COLOR); 
            tmp_paint.setAlpha(200); 
            face.getMidPoint(tmp_point);
            if(mSensorOrientation == 90) {
            r.set( new RectF(xScale*(tmp_point.x-eyeDis),
                    yScale*(tmp_point.y-eyeDis),
                    xScale*(tmp_point.x+eyeDis),
                    yScale*(tmp_point.y+eyeDis)));
            } else if(mSensorOrientation == 180) {
                r.set( new RectF((tmp_point.y-eyeDis)*xScale,
                        mFaceView.getHeight()-(tmp_point.x+eyeDis)*yScale,
                        (tmp_point.y+eyeDis)*xScale,
                        mFaceView.getHeight()-(tmp_point.x-eyeDis)*yScale));
            } else if(mSensorOrientation == 270) {
                r.set( new RectF(mFaceView.getWidth()-(tmp_point.x+eyeDis)*xScale,
                        mFaceView.getHeight()-(tmp_point.y+eyeDis)*yScale,
                        mFaceView.getWidth()-(tmp_point.x-eyeDis)*xScale,
                        mFaceView.getHeight()-(tmp_point.y-eyeDis)*yScale));
            }  else {
                r.set( new RectF(mFaceView.getWidth()-(tmp_point.y+eyeDis)*xScale,
                        (tmp_point.x-eyeDis)*yScale,
                        mFaceView.getWidth()-(tmp_point.y-eyeDis)*xScale,
                        (tmp_point.x+eyeDis)*yScale
                        ));
            }
            
           // canvas.drawCircle(tmp_point.x, tmp_point.y, face.eyesDistance(),  tmp_paint); 
            
           
            canvas.drawRect(r, tmp_paint);
           
            } 
        // TODO Auto-generated method stub
     
    }
    /** Create a File for saving an image or video */
    private  File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this. 
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + getApplicationContext().getPackageName()
                + "/Files"); 

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        } 
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
            String mImageName="MI_"+ timeStamp +".jpg";
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);  
        return mediaFile;
    } 

    
    private class MyOrientationEventListener
    extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

    
    @Override
    
    public void onOrientationChanged(int orientation) {
        // We keep the last known orientation. So if the user first orient
        // the camera then point the camera to floor or sky, we still have
        // the correct orientation.
        if (orientation == ORIENTATION_UNKNOWN) return;
        mLastScreenOrientation=0;
        for (int i : sOrientDegrees) {
            if (i - 45 <orientation && orientation <= i + 45) {
                if(mLastScreenOrientation == i) return;
                else mLastScreenOrientation = i;
                break;
            }
        }
        
       // mLastRawOrientation = orientation;
        if(mRotateMatrix == null) mRotateMatrix= new Matrix();
        mSensorOrientation = mLastScreenOrientation+cameraInfo.orientation;
        mRotateMatrix.setRotate(mSensorOrientation==360 ?0 :mSensorOrientation);
        //mCurrentModule.onOrientationChanged(orientation);
        }
        
    }
    private Matrix mRotateMatrix = null;
    private static final int[] sOrientDegrees = { 90, 180, 270 };
    int count_capture;
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        count_capture=1;
    }
    

}
