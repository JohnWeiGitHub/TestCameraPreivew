package com.coasia.testcamerapreivew;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;



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
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.FaceDetector;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SimpleExpandableListAdapter;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity   implements Callback, PreviewCallback, OnClickListener, OnCheckedChangeListener{
    private Camera mCamera = null;
    Bitmap background_image;
    MediaPlayer _shootMP = null;//for shutter sound
    private boolean enableFaceDetect = true;//enable the function of face detection
    public boolean saveOrientationExif =false;//save the orientation in Exif, true will make frequently setParameter
    private static final String TAG = "testcamerapreivew ";
    private static final String FOLDER_NAME = TAG;///storage/emulated/legacy/Pictures/$FOLDER_NAME/
    private static final int CAMERA_NUM = 0;
    private static final int MAX_FACES = 10;
    private static final float FACE_RECT_RATIO = 1.5f;
    private static final int FACE_RECT_COLOR = Color.RED;
    private static final long FACE_DETECT_INTERVAL = 333;//  1/30 for 30 fps
    private SurfaceHolder mHolder;
    private SurfaceView mSurfaceView;
    private SurfaceView mFaceView;
    float faceConfidence=0.4f;//default faceDetector return face with confidence > 0.4f which was set in framework
    int w =640;//preview size
    int h =480;
    float xScale =1f;
    float yScale =1f;
    private Parameters p;
    private FaceDetector.Face[] faces; 
    private int face_count;
    boolean debug =true;
    boolean capture_in_progress =false;
    boolean faceDetectInProgress = false;
    
    int orientation =0;
    byte []buffer3 = null;
    Button btnCapture;
    ToggleButton btnConnect;
    CameraInfo cameraInfo = null;
    private Matrix mRotateMatrix = null;
    private static final int[] sOrientDegrees = { 90, 180, 270 };
    int count_capture=0;//for debug; save preview frame as a bmp
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private MyOrientationEventListener mOrientationListener;
    // The degrees of the device rotated clockwise from its natural orientation.
    private int mLastScreenOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private int mSensorOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    
    
    
    //BLE related field
    private Handler mHandler = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private boolean mScanning;
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private BluetoothDevice mHelloSensor;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    //

    void __log(String str){
        if(debug)Log.i(TAG, str); 
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(this.checkCameraHardware(this)) {
            if(mCamera == null)
                mCamera =MainActivity.getCameraInstance();
        }
      
        mOrientationListener = new MyOrientationEventListener(this);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        cameraInfo =new CameraInfo();
        Camera.getCameraInfo(CAMERA_NUM, cameraInfo);
        setContentView(R.layout.activity_main);
        btnCapture = (Button) this.findViewById(R.id.button_capture);
        btnConnect = (ToggleButton) this.findViewById(R.id.toggleButton_connect);
        btnConnect.setOnCheckedChangeListener( this);
        btnCapture.setOnClickListener(this);
        mSurfaceView = (SurfaceView) this.findViewById(R.id.surfaceView_camera);
        
        mFaceView =(SurfaceView)this.findViewById(R.id.face);        
        mFaceView.getHolder().addCallback(this);
        mFaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        mFaceView.setZOrderOnTop(true);
        
        
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
                
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        p = mCamera.getParameters();
        List<Size> sizes = p.getSupportedPreviewSizes();
        __log("supported resolution============");
        //for debug; show all the supported preview size
        /*
        for(Size size :sizes) {
            __log(size.width+"x"+size.height);         
        }*/
                
        p.setPreviewSize(w, h);

        //p.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE); will invalid due to preview not started yet 
        orientation =cameraInfo.orientation;
        mCamera.setParameters(p);
        mCamera.setPreviewCallback(this);
        
        PixelFormat pf = new PixelFormat();
        PixelFormat.getPixelFormatInfo(p.getPreviewFormat(),pf);
        int bufSize = (w*h*pf.bitsPerPixel)/8;
        
        buffer3 = new byte[bufSize];//buffer for preview call back
        
        mHandler = new Handler();
        enableBLE();
       
    }

    boolean enableBLE(){
      

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            //finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
           // finish();
            return false;
        }
        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mHelloSensor=null;
                __enableConnect(false);
                scanLeDevice(true);               
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
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
        startPreview(holder);

    }
    void startPreview(SurfaceHolder holder) {

        // start preview with new settings
        try {

            if(enableFaceDetect) {
                mCamera.addCallbackBuffer(buffer3 );
                mCamera.setPreviewCallbackWithBuffer(this);
                __log("setPreviewCallbackWithBuffer============");
            }
            
            
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
        startPreview(holder);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub

    }

   

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }
    
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            __log("device name=" + device.getName());
            if (device.getName().equals("Hello Sensor")) {
                mHelloSensor = device;
                __enableConnect(true);
                scanLeDevice(false);//get the desired device; stop redundant scan
            }
        }
    };
    
    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        mOrientationListener.disable();
        if(enableFaceDetect)
            mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        
        scanLeDevice(false);
        mHelloSensor =null;        
        unregisterReceiver(mGattUpdateReceiver);
        
    }
    
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        mOrientationListener.enable();
        if(mCamera == null) {
            mCamera = this.getCameraInstance();
            __log("onResume = null");
            startPreview(mHolder);
        }       
        
        
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        mHelloSensor =null;
        __enableConnect(false);
        scanLeDevice(true);
        
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            Log.e(TAG,">>>>>>>>BLE not enabled and something will malfunction<<<<<<<<<<");
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    
    
    
    
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // TODO Auto-generated method stub
        //__log("onPreviewFrame enter");
        
        
        if(!faceDetectInProgress) {        
            background_image = getBitmapImageFromYUV(data, w, h);
            new FaceDectecorAsyncTask().execute( new String());
        }

        if(enableFaceDetect)
            camera.addCallbackBuffer(data);
       
       // __log("onPreviewFrame exit");
    }   



    private void drawFaceRetangle(Canvas canvas, android.media.FaceDetector.Face[] faces, int face_count) {

        xScale = mFaceView.getWidth();
        yScale = mFaceView.getHeight();
        // the preivew are actually scaled after layout, we should take it into account when draw rect on face
        if (cameraInfo.orientation % 180 == 90) {
            xScale /= h;
            yScale /= w;
        } else {
            xScale /= w;
            yScale /= h;
        }

        Paint tmp_paint = new Paint();
        PointF tmp_point = new PointF();
        RectF r = new RectF();
        FaceDetector.Face face;
        float eyeDis = 0f;

        for (int i = 0; i < face_count; i++) {// draw the face rect according orientation
            face = faces[i];
            if (faceConfidence > face.confidence())
                return;
            eyeDis = face.eyesDistance() * FACE_RECT_RATIO;
            tmp_paint.setColor(FACE_RECT_COLOR);
            tmp_paint.setAlpha(50);
            face.getMidPoint(tmp_point);
            if (mSensorOrientation == 90) {
                r.set(new RectF(xScale * (tmp_point.x - eyeDis),
                        yScale * (tmp_point.y - eyeDis), xScale * (tmp_point.x + eyeDis), yScale * (tmp_point.y + eyeDis)));
            } else if (mSensorOrientation == 180) {
                r.set(new RectF((tmp_point.y - eyeDis) * xScale, mFaceView.getHeight() - (tmp_point.x + eyeDis) * yScale,
                        (tmp_point.y + eyeDis) * xScale, mFaceView.getHeight() - (tmp_point.x - eyeDis) * yScale));
            } else if (mSensorOrientation == 270) {
                r.set(new RectF(mFaceView.getWidth() - (tmp_point.x + eyeDis) * xScale, mFaceView.getHeight() - (tmp_point.y + eyeDis) * yScale,
                        mFaceView.getWidth() - (tmp_point.x - eyeDis) * xScale, mFaceView.getHeight() - (tmp_point.y - eyeDis) * yScale));
            } else {
                r.set(new RectF(mFaceView.getWidth() - (tmp_point.y + eyeDis) * xScale,
                        (tmp_point.x - eyeDis) * yScale, mFaceView.getWidth() - (tmp_point.y - eyeDis) * xScale, (tmp_point.x + eyeDis) * yScale));
            }

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

    @Override
    public void onClick(View v) {
        //count_capture=1;
        __takePicture();
    }

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
          return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                  Environment.DIRECTORY_PICTURES), FOLDER_NAME);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
    private PictureCallback mPicture = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            capture_in_progress = false;
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions" );
                return;
            }
            

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                MediaScannerConnection.scanFile(MainActivity.this,
                        new String[] {pictureFile.getAbsolutePath()}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };
    
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            if(mHelloSensor!= null) {
                mBluetoothLeService.connect(mHelloSensor.getAddress());
                __enableConnect(false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;            
        }
    };
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                __enableConnect(true);
                __log("connected");

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
               mConnected = false;
              
               __checkConnect(false);
                __enableConnect(false);//user need to rescan

                if(mBluetoothLeService != null) mBluetoothLeService.disconnect();
                __log("dis connect");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if(mBluetoothLeService != null)
                   
                    enableNotify(mBluetoothLeService.getSupportedGattService(UUID.fromString(SampleGattAttributes.UUID_SERVICE_HELLO)),
                        SampleGattAttributes.UUID_CHARACTERISTIC_HELLO_NOTIFY);
                
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                __takePicture ();//get a remote notify of click                
            }
        }
    };
    
    
    void __delay(long miniSec) {
        try {
            Thread.sleep(miniSec);//delay
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    void __takePicture (){
        if(!capture_in_progress) {
            mCamera.takePicture(null, null, mPicture);
            shootSound();
            capture_in_progress = true;
        } else {
            Toast.makeText(MainActivity.this, "capture in progress", Toast.LENGTH_SHORT).show();
            __log("previous capture in progress");
        }
    }
    
    
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // TODO Auto-generated method stub
        if(isChecked) {
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            if(mBluetoothLeService == null)
                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            else {
                if(mHelloSensor!= null)
                    mBluetoothLeService.connect(mHelloSensor.getAddress());
            }
                
        } else {
            if(mBluetoothLeService!=null)
                mBluetoothLeService.disconnect();
            __enableConnect(false);    
        }
        
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mBluetoothLeService !=null)
            unbindService(mServiceConnection);
        mBluetoothLeService = null;
           
    }
    
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    

    private boolean enableNotify(BluetoothGattService gattService, String uuid_char) {
        if (gattService == null) return false;
        __delay(500);
        BluetoothGattCharacteristic gattCharNotify = gattService.getCharacteristic(UUID.fromString(uuid_char));
        if (gattCharNotify == null) return false;
        __delay(500);
        mBluetoothLeService.setCharacteristicNotification(gattCharNotify, true);
        __log("register for notify!!!");
        return true;
             
       
    }
    void __enableConnect(final boolean enabled) {
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(btnConnect!=null) btnConnect.setEnabled(enabled);
            }
        });
        
    }
    void __checkConnect(final boolean checked) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(btnConnect!=null) btnConnect.setChecked(checked);
            }
        });
       
    }    
   
    public void shootSound()
    {
        AudioManager meng = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume( AudioManager.STREAM_NOTIFICATION);

        if (volume != 0)
        {
           
            if (_shootMP == null)
                _shootMP = MediaPlayer.create(this, Uri.parse("file:///system/media/audio/ui/Shutter.ogg"));
            if (_shootMP != null)
                _shootMP.start();
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
            if (orientation == ORIENTATION_UNKNOWN)
                return;
            mLastScreenOrientation = 0;
            for (int i : sOrientDegrees) {
                if (i - 45 < orientation && orientation <= i + 45) {
                    if (mLastScreenOrientation == i)
                        return;
                    else
                        mLastScreenOrientation = i;
                    break;
                }
            }

            if (mRotateMatrix == null)
                mRotateMatrix = new Matrix();
            mSensorOrientation = mLastScreenOrientation + cameraInfo.orientation;
            mRotateMatrix.setRotate(mSensorOrientation == 360 ? 0 : mSensorOrientation);

            p.setRotation(mSensorOrientation == 360 ? 0 : mSensorOrientation);
            if (saveOrientationExif)
                mCamera.setParameters(p);
        }
    }
    
    /*task for face detect*/
    class FaceDectecorAsyncTask extends AsyncTask<String, Integer, Integer>{



        @Override
        protected Integer doInBackground(String... param) {
            
            
            if(mRotateMatrix!=null)
                background_image = Bitmap.createBitmap(background_image, 0, 0, w, h, mRotateMatrix, true);
            
            FaceDetector face_detector = new FaceDetector( 
            background_image.getWidth(), background_image.getHeight(), 
            MAX_FACES); 

            faces = new FaceDetector.Face[MAX_FACES]; 
            // The bitmap must be in 565 format (for now). 
            face_count = face_detector.findFaces(background_image, faces); 
           // if(face_count>0)   __log(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>Face_Detection Count: " + String.valueOf(face_count)); 
           
            if(count_capture>0) {
                MainActivity.this.storeImage(background_image);
                count_capture=0;
            }
            background_image.recycle();
            background_image = null;
            
            
            final Surface surface = mFaceView.getHolder().getSurface();
            if (surface == null || !surface.isValid())
                Log.e(TAG, "surface is null or not valid");
            Canvas canvas = surface.lockCanvas(null);
            if (canvas == null) {
                Log.e(TAG, "Cannot draw onto the canvas as it's null");
            } else {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                if (face_count > 0)
                    drawFaceRetangle(canvas, faces, face_count);
                surface.unlockCanvasAndPost(canvas);
            }

            try {
                Thread.sleep(FACE_DETECT_INTERVAL);// 30 fps
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
          
            return null;
        }

        @Override
        protected void onPostExecute(Integer result) {
            faceDetectInProgress = false;
            super.onPostExecute(result);
            
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPreExecute() {
            faceDetectInProgress = true;
            super.onPreExecute();
        }

    }

}
