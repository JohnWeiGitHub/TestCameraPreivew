package com.coasia.testcamerapreivew;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.FaceDetector;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.TextureView;

public class MainActivity extends Activity   implements Callback, PreviewCallback{
    private Camera mCamera = null;
    private static final String TAG = "testcamerapreivew ";
    private static final int CAMERA_NUM = 0;
    private static final int MAX_FACES = 10;
    private SurfaceHolder mHolder;
    private SurfaceView mSurfaceView;
    private byte[] rgbBuffer;
   
    int w =640;
    int h =480;
    private Parameters p;
    private FaceDetector.Face[] faces; 
    private int face_count;
    boolean debug =true;
    void __log(String str){
        if(debug)Log.i(TAG, str);    
    }
    boolean swFd=false;
    boolean hwFd=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(this.checkCameraHardware(this)) {
            mCamera =MainActivity.getCameraInstance();
        }
            
        
        CameraInfo cameraInfo =new CameraInfo();
        Camera.getCameraInfo(CAMERA_NUM, cameraInfo);
        setContentView(R.layout.activity_main);
        mSurfaceView = (SurfaceView) this.findViewById(R.id.surfaceView_camera);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
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
        mCamera.setDisplayOrientation(cameraInfo.orientation);
        mCamera.setParameters(p);
        mCamera.setPreviewCallback(this);
        
        PixelFormat pf = new PixelFormat();
        PixelFormat.getPixelFormatInfo(p.getPreviewFormat(),pf);
        int bufSize = (w*h*pf.bitsPerPixel)/8;
        __log("buffer============");
        //把buffer給preview callback備用
        
         byte []buffer1 = new byte[bufSize];
        byte []buffer2 = new byte[bufSize];
        byte []buffer3 = new byte[bufSize];
         rgbBuffer = new byte[w*h*2];
         
        /*mCamera.addCallbackBuffer(buffer1 );
        mCamera.addCallbackBuffer(buffer2 );
       mCamera.addCallbackBuffer(buffer3 );
       mCamera.setPreviewCallbackWithBuffer(this);*/
       
         __log("setPreviewCallbackWithBuffer============");
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
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
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
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(holder);
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
        try {
           // mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(holder);
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
        mCamera.stopPreview();
        //mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
        
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if(mCamera == null)
            mCamera = this.getCameraInstance();
        //mCamera.addCallbackBuffer(buffer1 );
        //mCamera.addCallbackBuffer(buffer2 );
        //mCamera.addCallbackBuffer(buffer3 );

       mCamera.setPreviewCallback(this);
    }

    int count;
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // TODO Auto-generated method stub
        //Log.i(TAG, "onPreviewFrame");
        
        toRGB565(data,w,h,rgbBuffer);
        
        camera.addCallbackBuffer(data);
        camera.setPreviewCallbackWithBuffer(this); 
        
       // Log.i(TAG, "onPreviewFrame2");  
        
        
        
        Bitmap background_image = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        ByteBuffer buf = ByteBuffer.wrap(rgbBuffer); // data is my array
        background_image.copyPixelsFromBuffer(buf);
        
        FaceDetector face_detector = new FaceDetector( 
        background_image.getWidth(), background_image.getHeight(), 
        MAX_FACES); 

        faces = new FaceDetector.Face[MAX_FACES]; 
        // The bitmap must be in 565 format (for now). 
        face_count = face_detector.findFaces(background_image, faces); 
        if(face_count>0)    Log.d(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>Face_Detection", "Face Count: " + String.valueOf(face_count)); 
        
        background_image.recycle();
        background_image = null;
      /*
        
        Canvas canvas = mHolder.lockCanvas();
        if (canvas == null) {
            Log.e(TAG, "Cannot draw onto the canvas as it's null");
        } else {
            drawFaceRetangle(canvas,faces,face_count);
            mHolder.unlockCanvasAndPost(canvas);
        }
        */
        
        //camera.addCallbackBuffer(data);
       // camera.setPreviewCallbackWithBuffer(this);        
        
       
    }   

    private void drawFaceRetangle(Canvas canvas, android.media.FaceDetector.Face[] faces, int face_count) {
        
        Paint tmp_paint = new Paint();
         PointF tmp_point = new PointF(); 
        for (int i = 0; i < face_count; i++) { 
            FaceDetector.Face face = faces[i]; 
            
            tmp_paint.setColor(Color.RED); 
            tmp_paint.setAlpha(100); 
            face.getMidPoint(tmp_point); 
            canvas.drawCircle(tmp_point.x, tmp_point.y, face.eyesDistance(), 
            tmp_paint); 
            } 
        // TODO Auto-generated method stub
     
    }

    private void toRGB565(byte[] yuvs, int width, int height, byte[] rgbs) {
        //the end of the luminance data
        final int lumEnd = width * height;
        //points to the next luminance value pair
        int lumPtr = 0;
        //points to the next chromiance value pair
        int chrPtr = lumEnd;
        //points to the next byte output pair of RGB565 value
        int outPtr = 0;
        //the end of the current luminance scanline
        int lineEnd = width;

        while (true) {

            //skip back to the start of the chromiance values when necessary
            if (lumPtr == lineEnd) {
                if (lumPtr == lumEnd) break; //we've reached the end
                //division here is a bit expensive, but's only done once per scanline
                chrPtr = lumEnd + ((lumPtr  >> 1) / width) * width;
                lineEnd += width;
            }

            //read the luminance and chromiance values
            final int Y1 = yuvs[lumPtr++] & 0xff; 
            final int Y2 = yuvs[lumPtr++] & 0xff; 
            final int Cr = (yuvs[chrPtr++] & 0xff) - 128; 
            final int Cb = (yuvs[chrPtr++] & 0xff) - 128;
            int R, G, B;

            //generate first RGB components
            B = Y1 + ((454 * Cb) >> 8);
            if(B < 0) B = 0; else if(B > 255) B = 255; 
            G = Y1 - ((88 * Cb + 183 * Cr) >> 8); 
            if(G < 0) G = 0; else if(G > 255) G = 255; 
            R = Y1 + ((359 * Cr) >> 8); 
            if(R < 0) R = 0; else if(R > 255) R = 255; 
            //NOTE: this assume little-endian encoding
            rgbs[outPtr++]  = (byte) (((G & 0x3c) << 3) | (B >> 3));
            rgbs[outPtr++]  = (byte) ((R & 0xf8) | (G >> 5));

            //generate second RGB components
            B = Y2 + ((454 * Cb) >> 8);
            if(B < 0) B = 0; else if(B > 255) B = 255; 
            G = Y2 - ((88 * Cb + 183 * Cr) >> 8); 
            if(G < 0) G = 0; else if(G > 255) G = 255; 
            R = Y2 + ((359 * Cr) >> 8); 
            if(R < 0) R = 0; else if(R > 255) R = 255; 
            //NOTE: this assume little-endian encoding
            rgbs[outPtr++]  = (byte) (((G & 0x3c) << 3) | (B >> 3));
            rgbs[outPtr++]  = (byte) ((R & 0xf8) | (G >> 5));
        }
    }
    

}
