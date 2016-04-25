package com.example.jess.drawingtest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jess.drawingtest.ColorPickerDialog.OnColorChangedListener;
import com.example.jess.drawingtest.activity.ScannerActivity;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.zxing.qrcode.QRCodeReader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

public class MainActivity extends AppCompatActivity implements OnClickListener, OnColorChangedListener, GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener, SensorEventListener {

    private SensorManager mSensorManager;
    Sensor accelerometer;
    Sensor magnetometer;
    private boolean sendOrientation = false;

    private static final String DEBUG_TAG = "Gestures";
    private GestureDetectorCompat mDetector;

    private static SocketIO socket;
    private static Socket mSocket;
    Boolean isConnected = false;
    private DrawingView drawView;
    private float smallBrush, mediumBrush, largeBrush;
    private Button drawBtn, eraseBtn, newBtn, saveBtn, galBtn, colBtn, grabBtn, panBtn, delBtn;
    private ImageButton currPaint;
    private Button buttonCheck;
    String ipAddress = null;
    private static int red, green, blue;
    private GridView gridviewGallery;
    private ArrayList<String> filePaths = new ArrayList<String>();// list of file paths
    File[] listFile;
    private static final int RESULT_LOAD_IMG = 1;
    private static final int RESULT_QRCODE = 2;
    private Paint mPaint;
    private AutoFitTextureView mTextureView;
    private File mFile;
    private static String TAG = "MainActivity";

    //Scanner
    private TextView barcodeInfo;
    private Button scan_btn;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private SurfaceView cameraView;

    private Button btn_rgb;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mMenuTitles;
    private ActionBarDrawerToggle mDrawerToggle;
    private SharedPreferences sessionIdPref;
    private SharedPreferences teamIdPref;

    private ArrayList<Boolean> colorSelectState;
    // Color Pallette
    private Button btn_color;
    private OnColorChangedListener mListener;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mBackgroundHandler.post(new ImageSaver(getApplicationContext(), reader.acquireNextImage(), mFile));
        }

    };

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;
    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;


    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    private static DisplayMetrics metrics;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private QRCodeReader mQrReader;
    private int[] colorArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mQrReader = new QRCodeReader();


        try {
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        } catch (Exception e) {
            Toast.makeText(this, "Hardware compatibility issue", Toast.LENGTH_LONG).show();
        }

        mDetector = new GestureDetectorCompat(this, this);
        mDetector.setOnDoubleTapListener(this);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        gridviewGallery = (GridView) findViewById(R.id.gridviewGallery);


        smallBrush = getResources().getInteger(R.integer.small_size);
        mediumBrush = getResources().getInteger(R.integer.medium_size);
        largeBrush = getResources().getInteger(R.integer.large_size);
        ipAddress = "https://stickie2-jaskiratr.c9users.io";
        mPaint = new Paint();

        galBtn = (Button) findViewById(R.id.gal_btn);
        colBtn = (Button) findViewById(R.id.col_btn);
        drawBtn = (Button) findViewById(R.id.draw_btn);
        eraseBtn = (Button) findViewById(R.id.erase_btn);
        newBtn = (Button) findViewById(R.id.new_btn);
        saveBtn = (Button) findViewById(R.id.save_btn);
        grabBtn = (Button) findViewById(R.id.grab_btn);
        panBtn = (Button) findViewById(R.id.pan_btn);
        delBtn = (Button) findViewById(R.id.del_btn);
        buttonCheck = (Button) findViewById(R.id.buttonCheck);

        //Camera View
        mTextureView = (AutoFitTextureView) findViewById(R.id.texture);

        //Scanner
        /*cameraView = (SurfaceView) findViewById(R.id.camera_view);*/



        barcodeInfo = (TextView) findViewById(R.id.tv_barcode);
        scan_btn = (Button) findViewById(R.id.scanner_btn);
//        btn_rgb = (Button) findViewById(R.id.rgb_btn);

        scan_btn.setOnClickListener(this);
//        btn_rgb.setOnClickListener(this);

        // Color Pallette
//        btn_color = (Button) findViewById(R.id.btn_color);
//        btn_color.setOnClickListener(this);
        //Color Palette State
        colorSelectState = new ArrayList<>();
        colorArray = this.getResources().getIntArray(R.array.demo_colors);
        for(int i = 0; i < colorArray.length; i++ ){
            if(i == 0){
                colorSelectState.add(true);
            }else{
                colorSelectState.add(false);
            }
        }
        /*barcodeDetector =
                new BarcodeDetector.Builder(this)
                        .setBarcodeFormats(Barcode.QR_CODE)
                        .build();

        cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                Log.d(TAG, "Surface Created");

                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException ie) {
                    Log.e("CAMERA SOURCE", ie.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                Log.d(TAG, "Surface Changed");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

                Log.d(TAG, "Surface Destroyed");

                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (barcodes.size() != 0) {
                    barcodeInfo.post(new Runnable() {    // Use the post method of the TextView
                        public void run() {
                            barcodeInfo.setText(    // Update the TextView
                                    barcodes.valueAt(0).displayValue
                            );
                        }
                    });
                }
            }
        });

        cameraSource.stop();
        cameraSource.release();
        barcodeDetector.release();*/


        // onClickListener
        galBtn.setOnClickListener(this);
        colBtn.setOnClickListener(this);
        drawBtn.setOnClickListener(this);
        eraseBtn.setOnClickListener(this);
        newBtn.setOnClickListener(this);
        saveBtn.setOnClickListener(this);
        grabBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Start
                        Log.d("Hold: ", "down");
                        mSocket.emit("grab", "pick");///////////////////
                        break;
                    case MotionEvent.ACTION_UP:
                        // End
                        Log.d("Hold: ", "up");
                        mSocket.emit("grab", "release");///////////////////
                        break;
                }
                return false;
            }
        });
        panBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Start
                        Log.d("Hold: ", "down");
                        //find current orientation
                        sendOrientation = true;
                        initValue = true;
                        yawOffset = 0;
                        pitchOffset = 0;
                        rollOffset = 0;
                        break;
                    case MotionEvent.ACTION_UP:
                        // End
                        Log.d("Hold: ", "up");
                        sendOrientation = false;
                        break;
                }
                return false;
            }
        });
        buttonCheck.setOnClickListener(this);
        scan_btn.setOnClickListener(this);

        drawView = (DrawingView) findViewById(R.id.drawing);
        LinearLayout paintLayout = (LinearLayout) findViewById(R.id.paint_colors);

        currPaint = (ImageButton) paintLayout.getChildAt(0);
        currPaint.setImageResource(R.drawable.paint_pressed);
        drawView.setBrushSize(smallBrush);
        metrics = getResources().getDisplayMetrics();

        // Grid view Set On Click Listener
        gridviewGallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                drawView.setBitmap(listFile[position].getAbsolutePath());

                gridviewGallery.setVisibility(View.GONE);

                captureStillPicture();

            }
        });

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleSendMultipleImages(intent); // Handle multiple images being sent
            }
        } else {
            // Handle other intents, such as being started from the home screen
        }

        //Creating directory location
        mFile = new File(getExternalFilesDir(null), "pic.jpg");

        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        try {
            mSocket = IO.socket("https://stickie2-jaskiratr.c9users.io:8081");
        } catch (URISyntaxException e) {

        }

        mSocket.connect();

        mSocket.on("findColor", onNewMessage);
        if (mSocket.connected()) {
            isConnected = true;
        } else {
            isConnected = false;
        }

        mMenuTitles = getResources().getStringArray(R.array.menu_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mTitle = mDrawerTitle = getTitle();
        mMenuTitles = getResources().getStringArray(R.array.menu_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mMenuTitles));

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("TAG", "selectItem: Click!");
                selectItem(position);

            }
        });
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup parent = (ViewGroup) findViewById(R.id.action_bar);
        View v = inflator.inflate(R.layout.actionbar_layout, parent, false);
        getSupportActionBar().setCustomView(v);

        v.setOnTouchListener(new Swipe_listener(getApplicationContext()) {

            public void onSwipeRight() {
//                Toast.makeText(MainActivity.this, "right", Toast.LENGTH_SHORT).show();
                postNote();
            }

            public void onSwipeLeft() {
//                Toast.makeText(MainActivity.this, "left", Toast.LENGTH_SHORT).show();
                postNote();
            }

            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });


        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
//                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                mDrawerList.bringToFront();
                mDrawerLayout.requestLayout();
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        switch (item.getItemId()) {
            case R.id.action_websearch:
                // create intent to perform web search for this planet
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                intent.putExtra(SearchManager.QUERY, getSupportActionBar().getTitle());
                // catch event that there's no activity to handle intent
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(this, R.string.app_not_available, Toast.LENGTH_LONG).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {

        Log.d(TAG, "selectItem:" + mMenuTitles[position]);


        // update the main content by replacing fragments
        switch (mMenuTitles[position]) {
            case "Connect":
//                connectSessionDialog();
                Intent scanIntent = new Intent(MainActivity.this, ScannerActivity.class);
                startActivityForResult(scanIntent, RESULT_QRCODE);
                mDrawerLayout.closeDrawer(mDrawerList);
                break;
            case "End Session":
                mSocket.emit("endSession", "true");
                mDrawerLayout.closeDrawer(mDrawerList);
                break;
            case "Recenter":
                mSocket.emit("recenter", "true");
                mDrawerLayout.closeDrawer(mDrawerList);
                break;
            case "Invite":
                SharedPreferences settings = getSharedPreferences("UserInfo", 0);
                Intent intent2 = new Intent(); intent2.setAction(Intent.ACTION_SEND);
                intent2.setType("text/plain");
                intent2.putExtra(Intent.EXTRA_TEXT,
                        settings.getString("team_id", "")+
                        " invited to join a Stickie session. "+
                        "\n"+
                        "Follow the link to join: "+"\n"+
                        "https://stickie2-jaskiratr.c9users.io/"+ settings.getString("session_id", ""));
                startActivity(Intent.createChooser(intent2, "Share via"));

                mDrawerLayout.closeDrawer(mDrawerList);
                break;
            default:
                mDrawerLayout.closeDrawer(mDrawerList);
                break;
        }

    }

    void connectSessionDialog() {
        LayoutInflater linf = LayoutInflater.from(this);
        final View inflator = linf.inflate(R.layout.ip_address, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Connect to session");
        alert.setView(inflator);

        final EditText et1 = (EditText) inflator.findViewById(R.id.server_address);
        final EditText et2 = (EditText) inflator.findViewById(R.id.team_name);

        alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String s1 = et1.getText().toString();
                String s2 = et2.getText().toString();
                socketConnect(s1, s2);
                //do operations using s1 and s2 here...
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });

        alert.show();
    }

    public void socketConnect(String session, String team) {
        JSONObject id = new JSONObject();
        try {
            id.put("team_id", team);
            id.put("session_id", session);
            id.put("kind", "phone");
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        Log.e("TAG", "SEND ID");
        mSocket.emit("id", id);///////////////////

        SharedPreferences settings = getSharedPreferences("UserInfo", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("session_id", session);
        editor.putString("team_id", team);
        editor.commit();
    }


    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    float[] mGravity;
    float[] mGeomagnetic;
    boolean initValue = true; // First value to detect phone orientation offset
    float pitchOffset, yawOffset, rollOffset;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (sendOrientation) {

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mGravity = event.values;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = event.values;
            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);

                    float pitch = (float) (orientation[0] * 57.2958) - pitchOffset;
                    float roll = (float) (orientation[1] * 57.2958) - rollOffset;
                    float yaw = (float) (orientation[2] * 57.2958) - yawOffset;
                    if (initValue) {
                        pitchOffset = pitch;
                        rollOffset = roll;
                        yawOffset = yaw;
                        initValue = false;
                    }
//                    if(isConnected){
                    JSONObject phoneOrientation = new JSONObject();
                    try {
                        phoneOrientation.put("x", yaw);
                        phoneOrientation.put("y", roll);
                        phoneOrientation.put("z", pitch);
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                    mSocket.emit("phoneOrientation", phoneOrientation);
//                    }
                }
            }
        }
    }


    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            Toast.makeText(getApplicationContext(), " TEXT Received", Toast.LENGTH_SHORT).show();
            // Update UI to reflect text being shared
        }
    }

    void handleSendImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            Toast.makeText(getApplicationContext(), " IMAGE Received", Toast.LENGTH_SHORT).show();
            Log.d("Image URI", imageUri.toString());
            drawView.setImageBitmap(getApplicationContext(), imageUri.toString());
        }
    }

    void handleSendMultipleImages(Intent intent) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
            Toast.makeText(getApplicationContext(), " MULTI-IMAGE Received", Toast.LENGTH_SHORT).show();
            // Update UI to reflect multiple images being shared
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        mSensorManager.unregisterListener(this);
//        isConnected = false;
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        startBackgroundThread();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        SharedPreferences settings = getSharedPreferences("UserInfo", 0);
        socketConnect( settings.getString("session_id", ""),settings.getString("team_id", ""));
        Toast.makeText(this, "Session: " + settings.getString("session_id", "")+ "  Team: " + settings.getString("team_id", ""), Toast.LENGTH_LONG).show();
//        Toast.makeText(getApplicationContext(), " RESUMED!", Toast.LENGTH_SHORT).show();

    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };


    /**
     * Starts tckground thread and its {@link Handler}.
     */
    private void startBackgroundThread() {

        Log.v(TAG, "startBackgroundThread()");
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Paint Clicking Function
    public void paintClicked(View view) {
        //use chosen color
        drawView.setErase(false);
        drawView.setBrushSize(drawView.getLastBrushSize());
        if (view != currPaint) {
            //update color
            ImageButton imgView = (ImageButton) view;
            String color = view.getTag().toString();

            Log.d("COLOR", "color = " + color);
            Log.d("COLOR", "color paint = " + Color.parseColor(color));

            drawView.setColor(color);
            /*imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));*/

            imgView.setImageResource(R.drawable.paint_pressed);
            currPaint.setImageResource(R.drawable.paint);

            currPaint = (ImageButton) view;
        }
    }

    /**
     * Opens the camera specified.
     */
    private void openCamera(int width, int height) {

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.M) {
            if (getApplicationContext().checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission();
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission();
                return;
            }
        }

        Log.v(TAG, "openCamera()");

        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    // REQUEST CAMERA PERMISSION
    private void requestCameraPermission() {

        Log.v(TAG, "requestCameraPermission()");
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            /*new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);*/
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    // Getting Color from Dialog
    @Override
    public void colorChanged(int color) {

        Log.d("ColorPickerDialog", "COLOR = " + color);

        drawView.setErase(false);
        drawView.setBrushSize(drawView.getLastBrushSize());
        drawView.setColor(color);


    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            Log.v(TAG, "confirmationDialog class onCreateDialog()");

            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(getActivity(),
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }


    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutputs(int width, int height) {

        Log.v(TAG, "setUpCameraOutputs()");
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        width, height, largest);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            /*ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);*/
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow

            Log.v(TAG, "CompareSizeByArea class Constructor");

            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        private Context context;

        public ImageSaver(Context ctxt, Image image, File file) {

            Log.v(TAG, "ImageSaver class constructor");

            context = ctxt;
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {

            Log.v(TAG, "run()");


            final Image.Plane[] planes = mImage.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            buffer.rewind();
            final byte[] data = new byte[buffer.capacity()];
            buffer.get(data);

            final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (bitmap == null)
                Log.e(TAG, "bitmap is null");
            else
                Log.e(TAG, "bitmap = " + bitmap);

            int redColors = 0;
            int greenColors = 0;
            int blueColors = 0;
            int pixelCount = 0;

            Bitmap b = Bitmap.createScaledBitmap(bitmap, 120, 120, false);

            for (int y = b.getHeight() / 2 - 10; y < b.getHeight() / 2 + 10; y++) {
                for (int x = b.getWidth() / 2 - 10; x < b.getWidth() / 2 + 10; x++) {
                    int c = b.getPixel(x, y);
                    pixelCount++;
                    redColors += Color.red(c);
                    greenColors += Color.green(c);
                    blueColors += Color.blue(c);
                }
            }
            // calculate average of bitmap r,g,b values
            red = (redColors / pixelCount);
            green = (greenColors / pixelCount);
            blue = (blueColors / pixelCount);

            Log.d(TAG, " r1 == " + red);
            Log.d(TAG, " g1 == " + green);
            Log.d(TAG, " b1 == " + blue);

//            Toast.makeText(context, "Red="+red+" Green="+green+" Blue="+blue, Toast.LENGTH_SHORT).show();

            JSONObject avgColor = new JSONObject();
            try {
                avgColor.put("red", red);
                avgColor.put("green", green);
                avgColor.put("blue", blue);
            } catch (JSONException ex) {
                ex.printStackTrace();
            }

            Log.e("TAG", "SEND AVG COLOR");
            mSocket.emit("color", avgColor);///////////////////
        }

    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface

        Log.v(TAG, "chooseOptimalSize()");

        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }


    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {

            Log.v(TAG, "ErrorDialog Class Constructor");

            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            Log.v(TAG, "ErrorDialog class onCreateDialog()");

            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {

        Log.v(TAG, "configureTransform()");

        if (null == mTextureView || null == mPreviewSize || null == getApplicationContext()) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.

            Log.v(TAG, "CameraDevice.StateCallback onOpened()");

            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {

            Log.v(TAG, "CameraDevice.StateCallback onDisconnected()");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {

            Log.v(TAG, "CameraDevice.StateCallback onError()");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            if (null != getApplicationContext()) {
                finish();
            }
        }

    };

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {

        Log.v(TAG, "createCameraPreviewSession()");

        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            Log.v(TAG, "createCaptureSession() onConfigured()");
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {

                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                /*ToDo*/

                                // Flash is automatically enabled when necessary.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {

                            Log.v(TAG, "onConfigureFailed()");

                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
//                    Log.v(TAG, "CameraCaptureSession STATE_PREVIEW");
                    break;
                }
                case STATE_WAITING_LOCK: {

                    Log.v(TAG, "CameraCaptureSession STATE_WAITING_LOCK");
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices

                    Log.v(TAG, "CameraCaptureSession STATE_WAITING_PRECAPTURE");
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_LOCKED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Log.v(TAG, "CameraCaptureSession STATE_WAITING");
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
//            Log.v(TAG, "OnCaptureProgressed()");
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
//            Log.v(TAG, "onCaptureCompleted()");
            process(result);
        }

    };

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {

        Log.v(TAG, "showToast()");
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {

        Log.v(TAG, "captureStillPicture()");

        try {

            if (null == getApplicationContext() || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            /*captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);*/

            // OFF Auto EXposure and Auto Auto Focus

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);

            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_STATE_LOCKED);
            captureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);


//            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);


            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
//                    showToast("Saved: " + mFile);
                    Log.d(TAG, mFile.toString());
                    unlockFocus();

                    //calling function to display RGB Values
                    /*displayRGBValues(mFile);*/
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {

        Log.v(TAG, "runPrecaptureSequence()");

        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {

        Log.v(TAG, "unlockFocus()");

        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_OFF);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    // Button Clicks
    @Override
    public void onClick(View view) {
        //respond to clicks
        switch (view.getId()) {
            case R.id.gal_btn:
                // Create intent to Open Image applications like Gallery, Google Photos
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                // Start the Intent
                startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
                break;

            case R.id.col_btn:
                // on button click for colors
//                new ColorPickerDialog(this, this, mPaint.getColor()).show();
                showColorDialog(colorArray, colorSelectState);
                /*drawView.setColor("#6449b0");*/
                /*drawView.setColor(Color.parseColor("#31698a"));*/

                Log.d(TAG, "mPaint COLOR =  " + Color.parseColor("#31698a"));

                break;

            case R.id.draw_btn:

                //draw button clicked
                final Dialog brushDialog = new Dialog(this);
                brushDialog.setTitle("Brush size:");
                brushDialog.setContentView(R.layout.brush_chooser);

                ImageButton smallBtn = (ImageButton) brushDialog.findViewById(R.id.small_brush);
                smallBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        drawView.setBrushSize(smallBrush);
                        drawView.setLastBrushSize(smallBrush);
                        drawView.setErase(false);
                        brushDialog.dismiss();
                    }
                });
                ImageButton mediumBtn = (ImageButton) brushDialog.findViewById(R.id.medium_brush);
                mediumBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        drawView.setBrushSize(mediumBrush);
                        drawView.setLastBrushSize(mediumBrush);
                        drawView.setErase(false);
                        brushDialog.dismiss();
                    }
                });
                ImageButton largeBtn = (ImageButton) brushDialog.findViewById(R.id.large_brush);
                largeBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        drawView.setBrushSize(largeBrush);
                        drawView.setLastBrushSize(largeBrush);
                        drawView.setErase(false);
                        brushDialog.dismiss();
                    }
                });
                brushDialog.show();

                break;

            case R.id.erase_btn:
                //switch to erase - choose size
                final Dialog brushDialogg = new Dialog(this);
                brushDialogg.setTitle("Eraser size:");
                brushDialogg.setContentView(R.layout.brush_chooser);

                ImageButton sBtn = (ImageButton) brushDialogg.findViewById(R.id.small_brush);
                sBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        drawView.setErase(true);
                        drawView.setBrushSize(smallBrush);
                        brushDialogg.dismiss();
                    }
                });
                ImageButton mBtn = (ImageButton) brushDialogg.findViewById(R.id.medium_brush);
                mBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        drawView.setErase(true);
                        drawView.setBrushSize(mediumBrush);
                        brushDialogg.dismiss();
                    }
                });
                ImageButton lBtn = (ImageButton) brushDialogg.findViewById(R.id.large_brush);
                lBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        drawView.setErase(true);
                        drawView.setBrushSize(largeBrush);
                        brushDialogg.dismiss();
                    }
                });
                brushDialogg.show();

                break;

            case R.id.new_btn:

                //new button
                AlertDialog.Builder newDialog = new AlertDialog.Builder(this);
                newDialog.setTitle("New drawing");
                newDialog.setMessage("Start new drawing (you will lose the current drawing)?");
                newDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        drawView.startNew();
                        dialog.dismiss();
                    }
                });
                newDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                newDialog.show();

                break;

            case R.id.save_btn:

                drawView.setDrawingCacheEnabled(true);
                Bitmap bitmap = drawView.getDrawingCache();
                Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, 150, 200, false);
                String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                try {

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    newBitmap.compress(Bitmap.CompressFormat.PNG, 2, baos);
                    byte[] b = baos.toByteArray();
                    String imageDataString = encodeImage(b);


                    Log.e("TAG", "SEND IMAGE");
                    mSocket.emit("image", imageDataString);
                    Toast.makeText(getApplicationContext(), "Keep Still", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
                }

                drawView.destroyDrawingCache();
                drawView.startNew();

                break;

            case R.id.buttonCheck:

                takePicture();

                break;

            // Scanner Button Click
            case R.id.scanner_btn:

                Intent scanIntent = new Intent(MainActivity.this, ScannerActivity.class);
                startActivityForResult(scanIntent, RESULT_QRCODE);

                /*try {
                    mCaptureSession.stopRepeating();
                    mCameraDevice.close();
                } catch (CameraAccessException e) {

                    Toast.makeText(getApplicationContext(), " CameraAccessException", Toast.LENGTH_SHORT).show();
                }*/
                /*findViewById(R.id.ll_rgb_camera).setVisibility(View.GONE);

                findViewById(R.id.ll_qr_camera).setVisibility(View.VISIBLE);

                try {
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }*/

                break;

//            case R.id.rgb_btn:



                /*cameraSource.stop();
                cameraSource.release();
                barcodeDetector.release();

                findViewById(R.id.ll_qr_camera).setVisibility(View.GONE);

                findViewById(R.id.ll_rgb_camera).setVisibility(View.VISIBLE);*/

                /*captureStillPicture();*/

//                break;
        }
    }

    // Color Palette Dialog View
    private void showColorDialog(int[] colorArr, final ArrayList<Boolean> colorSelectState)  {
        final Dialog dealDialog = new Dialog(MainActivity.this);
        dealDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dealDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dealDialog.setContentView(R.layout.color_palette);
        dealDialog.setCancelable(true);
        /*int[] colorArray = {R.color.black, R.color.aliceblue, R.color.aqua, R.color.aquamarine, R.color.azure,
                R.color.bisque, R.color.brown, R.color.blueviolet, R.color.red, R.color.khaki};*/
        /*Log.d(TAG, "colorArray length = " + colorArray.length);
        Log.d(TAG, "colorArray[0]");
        Log.d(TAG, "colorArray[0] = " + colorArray[0]);*/
        for(int i = 0; i < colorSelectState.size(); i++ ){
            if(colorSelectState.get(i)){
                colorChanged(colorArr[i]);
                break;
            }
        }
        GridView gvColorPalette = (GridView) dealDialog.findViewById(R.id.gv_color);
        final PaletteAdapter paletteAdapter = new PaletteAdapter(colorArr, colorSelectState, getApplicationContext());
        gvColorPalette.setAdapter(paletteAdapter);
        gvColorPalette.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                colorChanged(colorArray[position]);
                /*colorSelectState.remove(position);
                colorSelectState.add(position, true);*/
                int size = colorSelectState.size();
                colorSelectState.clear();
                for(int i = 0; i < size; i++){
                    if(i == position){
                        colorSelectState.add(true);
                    }else{
                        colorSelectState.add(false);
                    }
                }
                paletteAdapter.notifyDataSetChanged();
                dealDialog.dismiss();
            }
        });
        dealDialog.show();
        /*new SpectrumDialog.Builder(getApplicationContext())
                .setColors(R.array.demo_colors)
                .setSelectedColorRes(R.color.seashell)
                .setDismissOnColorSelected(true)
                .setOnColorSelectedListener(new SpectrumDialog.OnColorSelectedListener() {
                    @Override public void onColorSelected(boolean positiveResult, @ColorInt int color) {
                        if(positiveResult) {
                            Toast.makeText(getApplicationContext(), "Color selected: #" + Integer.toHexString(color).toUpperCase(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Dialog cancelled", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).build();*/
    }
    // Color Palette Gridview Adapter
    public class PaletteAdapter extends BaseAdapter {
        Context mContext;
        int[] colorsArray;
        ArrayList<Boolean> colorStateList;
        public PaletteAdapter(int[] colorArray, ArrayList<Boolean> stateList ,Context context) {
            mContext = context;
            colorsArray = colorArray;
            colorStateList = new ArrayList<>();
            colorStateList.addAll(stateList);
        }
        @Override
        public int getCount() {
            return 8;
        }
        @Override
        public Object getItem(int position) {
            return null;
        }
        @Override
        public long getItemId(int position) {
            return 0;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MyViewHolder viewHolder = new MyViewHolder();
            LayoutInflater inflater = LayoutInflater.from(mContext);
            if(convertView == null){
                convertView = inflater.inflate(R.layout.color_cell, null);
                viewHolder = new MyViewHolder();
                viewHolder.imageViewColor = (ImageView)convertView.findViewById(R.id.iv_color);
                viewHolder.selectImage = (ImageView) convertView.findViewById(R.id.iv_selectImage);
                viewHolder.defaultOverlay = (ImageView) convertView.findViewById(R.id.iv_overlayDefault);
                viewHolder.selectOverlay= (ImageView) convertView.findViewById(R.id.iv_overlaySelected);
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (MyViewHolder)convertView.getTag();
            }
            Log.d(TAG, "Setting color");
            viewHolder.imageViewColor.setBackgroundColor(colorsArray[position]);
            if(colorStateList.get(position)){
                viewHolder.selectImage.setVisibility(View.VISIBLE);
                viewHolder.selectOverlay.setVisibility(View.VISIBLE);
                viewHolder.defaultOverlay.setVisibility(View.GONE);
//                viewHolder.selectOverlay.setBackground("@drawable/overlay1");
//                android:background="@drawable/overlay2"/>
//                iv_background.setImageBitmap(bmp);
            }else{
                viewHolder.selectImage.setVisibility(View.GONE);
                viewHolder.selectOverlay.setVisibility(View.GONE);
                viewHolder.defaultOverlay.setVisibility(View.VISIBLE
                );
            }
            return convertView;
        }
        public class MyViewHolder{
            ImageView imageViewColor;
            ImageView selectImage;
            ImageView selectOverlay;
            ImageView defaultOverlay;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case RESULT_LOAD_IMG:

                try {
                    // When an Image is picked
                    if (resultCode == RESULT_OK && null != data) {
                        // Get the Image from data
                        Toast.makeText(this, "IMAGE PICKED",
                                Toast.LENGTH_LONG).show();
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};

                        // Get the cursor
                        Cursor cursor = getContentResolver().query(selectedImage,
                                filePathColumn, null, null, null);
                        // Move to first row
                        cursor.moveToFirst();

                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String imgDecodableString = cursor.getString(columnIndex);
                        cursor.close();

                        drawView.setBitmap(imgDecodableString);

                        captureStillPicture();


                    } else {
                        Toast.makeText(this, "You haven't picked Image",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
                            .show();
                }

                break;


            case RESULT_QRCODE:

                Log.d(TAG, "QR Code");

                try {
                    if (resultCode == RESULT_OK && null != data) {

                        String qrCode = data.getStringExtra("result");

                        Log.d(TAG, "QR Code = " + qrCode);
                        //String processing

                        String[] qrParam = qrCode.split(",");

                        // Send Socket Connect
                        socketConnect(qrParam[0],qrParam[1]);

                        barcodeInfo.setText(qrCode);

                        captureStillPicture();


                    } else {
                        Toast.makeText(this, "You haven't picked Image",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
                            .show();
                }

                break;


        }

    }


    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
        lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {

        Log.v(TAG, "lockFocus()");

        /*try {*/
        // This is how to tell the camera to lock focus.
            /*mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);*/

        // Tell #mCaptureCallback to wait for the lock.
            /*mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);*/

        captureStillPicture();


        /*} catch (CameraAccessException e) {
            e.printStackTrace();
        }*/
    }


    public static String encodeImage(byte[] imageByteArray) {
        return Base64.encodeToString(imageByteArray, Base64.DEFAULT);
    }

    // Clicking Uploading Button
    public void ipAdd(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.ip_address, null))
                .setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Dialog f = (Dialog) dialog;
                        EditText URI;
                        URI = (EditText) f.findViewById(R.id.server_address);
                        ipAddress = URI.getText().toString();
                        connectSocket();
                        URI.setText(String.valueOf(ipAddress));
                    }
                })
                .setNegativeButton(R.string.disconnect, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        disconnectSocket();
                        dialog.dismiss();
                    }
                });
        builder.create().show();

    }

    public void connectSocket() {


        try {
            socket = new SocketIO(ipAddress);
            socket.connect(new IOCallback() {
                @Override
                public void onMessage(JSONObject json, IOAcknowledge ack) {
                    try {
                        System.out.println("Server said:" + json.toString(2));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d("JSON ERROR", "OCCURERED");
                    }
                }

                @Override
                public void onMessage(String data, IOAcknowledge ack) {
                    System.out.println("Server said: " + data);


                }

                @Override
                public void onError(SocketIOException socketIOException) {
                    System.out.println("an Error occured");
                    socketIOException.printStackTrace();
                }

                @Override
                public void onDisconnect() {
                    System.out.println("Connection terminated.");
                    isConnected = false;
                }

                @Override
                public void onConnect() {
                    System.out.println("Connection established");
//                    socket.emit("device_id","phoneA");
                    isConnected = true;
                }

                @Override
                public void on(String event, IOAcknowledge ack, Object... args) {
                    System.out.println("Server triggered event '" + event + "'");
                    if (event.equals("findColor")) {
                        Log.d("SOCKET ", " FIND COLOR ");
//                        takePicture();// error
                        //http://stackoverflow.com/questions/5161951/android-only-the-original-thread-that-created-a-view-hierarchy-can-touch-its-vi
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                takePicture();
                            }
                        });
                    }
                    if (event.equals("helo")) {
                        socket.emit("HELO ", "BACK");
                    }
                }
            });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void disconnectSocket() {
        socket.disconnect();
    }

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
//            JSONObject data = (JSONObject) args[0];
            String id = (String) args[0];
//            try{
//                String msg = data.getString("msg");
            if (id.equals("findColor")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("TAG", "TAKE PICTURE");
                        takePicture();
                    }
                });
            }
//            }

//            Log.e("TAG",data.toString());

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    takePicture();
//                }
//            });
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) {
//        Log.d(DEBUG_TAG,"onDown: " + event.toString());
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        Log.d(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString());
        drawView.setDrawingCacheEnabled(true);
        Bitmap bitmap = drawView.getDrawingCache();
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, 150, 200, false);
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        try {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            newBitmap.compress(Bitmap.CompressFormat.PNG, 2, baos);
            byte[] b = baos.toByteArray();
            String imageDataString = encodeImage(b);


            Log.e("TAG", "SEND IMAGE");
            mSocket.emit("image", imageDataString);
            Toast.makeText(getApplicationContext(), "Keep Still", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
        }

        drawView.destroyDrawingCache();
        drawView.startNew();
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
//        Log.d(DEBUG_TAG, "onLongPress: " + event.toString());
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
//        Log.d(DEBUG_TAG, "onScroll: " + e1.toString()+e2.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
//        Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
//        Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
//        Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
//        Log.d(DEBUG_TAG, "onDoubleTapEvent: " + event.toString());
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
//        Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
        return true;
    }

    public void postNote() {
        drawView.setDrawingCacheEnabled(true);
        Bitmap bitmap = drawView.getDrawingCache();
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, 150, 200, false);
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        try {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            newBitmap.compress(Bitmap.CompressFormat.PNG, 2, baos);
            byte[] b = baos.toByteArray();
            String imageDataString = encodeImage(b);


            Log.e("TAG", "SEND IMAGE");
            mSocket.emit("image", imageDataString);
            Toast.makeText(getApplicationContext(), "Keep Still", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
        }

        drawView.destroyDrawingCache();
        drawView.startNew();
    }
}


