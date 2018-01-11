package com.compass.camera.ui.activities;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.compass.camera.contaract.ARCamContract;
import com.compass.camera.presenter.ARCamPresenter;
import com.compass.camera.utils.Accelerometer;
import com.compass.camera.gl.Camera1Renderer;
import com.compass.camera.gl.CameraTrackRenderer;
import com.compass.camera.ui.custum.CircularProgressView;
import com.compass.camera.ui.custum.CustomBottomSheet;
import com.compass.camera.model.DynamicPoint;
import com.compass.camera.iCallBacks.FrameCallback;
import com.compass.camera.gl.My3DRenderer;
import com.compass.camera.iCallBacks.MyRenderer;
import com.compass.camera.model.Ornament;
import com.compass.camera.ui.adapter.OrnamentAdapter;
import com.compass.camera.model.OrnamentFactory;
import com.compass.camera.utils.PermissionUtils;
import com.compass.camera.R;
import com.compass.camera.gl.TextureController;
import com.compass.camera.camera.AFilter;
import com.compass.camera.camera.LandmarkFilter;
import com.sensetime.stmobileapi.STMobileFaceAction;

import org.rajawali3d.renderer.ISurfaceRenderer;
import org.rajawali3d.view.ISurface;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ARCamContract.View,FrameCallback {

    private final static String TAG = MainActivity.class.getSimpleName();
    // Photo Size
    private final static int IMAGE_WIDTH = 720;
    private final static int IMAGE_HEIGHT = 1280;


    // SenseTime The maximum supported face detection is 640 x 480
    private final static int PREVIEW_WIDTH = 640;
    private final static int PREVIEW_HEIGHT = 480;

    private final static int TYPE_NONE = -1;
    private final static int TYPE_PHOTO = 0;
    private final static int TYPE_RECORD = 1;

    private Context mContext;
    private ARCamPresenter mPresenter;

    // Used to render the camera preview
    private SurfaceView mSurfaceView;

    //Used to display face detection parameters
    private TextView mTrackText, mActionText;

    // Processing filter logic
    protected TextureController mController;
    private MyRenderer mRenderer;

    // Camera Id, rear set to 0, front set to 1
    private int cameraId = 1;

    //The default camera filter id
    protected int mCurrentFilterId = R.id.menu_camera_default;

    // Accelerometer tools seem to be used for face detection
    private static Accelerometer mAccelerometer;

    //Because you can not write filters that display 3D models,
    // With the help of OpenGL ES engine Rajawali for now.
    // Used to render 3D models
    private ISurface mRenderSurface;
    private ISurfaceRenderer mISurfaceRenderer;
    //Because the SurfaceView that renders the camera and the 3D model is separate,
    // Photo / video can only take two data separately, and then merged
    // Take pictures
    private Bitmap mRajawaliBitmap = null;

    // Camera / video button
    private CircularProgressView mCapture;

    // Duration of video recording
    private long time;

    // The flag 0 for processing frame data and 1 for recording
    private int mFrameType = TYPE_NONE;

    // Decorations list
    private CustomBottomSheet mOrnamentSheet;
    private RecyclerView mRvOrnament;
    private OrnamentAdapter mOrnamentAdapter;
    private List<Ornament> mOrnaments;

    private boolean mIsNeedSkinColor = false;
    // private PointF mSamplePoint = null;

    private boolean mIsNeedFrameCallback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = MainActivity.this;
        mPresenter = new ARCamPresenter(this);
        // For face detection
        mAccelerometer = new Accelerometer(this);
        mAccelerometer.start();

        PermissionUtils.askPermission(this, new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10, initViewRunnable);
    }

    protected void setContentView(){
        setContentView(R.layout.activity_ar_cam);
        initSurfaceView();
        initRajawaliSurface();
        initCaptureButton();
        initMenuButton();
        initCommonView();
        initOrnamentSheet();

    }

    private void initSurfaceView() {
        mSurfaceView = (SurfaceView) findViewById(R.id.camera_surface);
        // Hold down the screen to cancel the filter, let go of the recovery filter,
        // in order to contrast
   /*     mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                   *//* case MotionEvent.ACTION_DOWN:
                        mController.clearFilter();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mController.addFilter(FilterFactory.getFilter(getResources(),
                                mCurrentFilterId));
                        break;*//*
                }
                return true;
            }
        });*/
    }

    private void initRajawaliSurface() {
        mRenderSurface = (org.rajawali3d.view.SurfaceView) findViewById(R.id.rajwali_surface);
        // Make the background of Rajawali's SurfaceView transparent
        ((org.rajawali3d.view.SurfaceView) mRenderSurface).setTransparent(true);

        mISurfaceRenderer = new My3DRenderer(this);
        ((My3DRenderer) mISurfaceRenderer).setScreenW(IMAGE_WIDTH);
        ((My3DRenderer) mISurfaceRenderer).setScreenH(IMAGE_HEIGHT);
        mRenderSurface.setSurfaceRenderer(mISurfaceRenderer);


        //When taking pictures, take the frame data of Rajawali first,turn Bitmap stand-by;
        // Then take the camera preview frame data, the final synthesis
        ((org.rajawali3d.view.SurfaceView) mRenderSurface).setOnTakeScreenshotListener
                (new org.rajawali3d.view.SurfaceView.OnTakeScreenshotListener() {
            @Override
            public void onTakeScreenshot(Bitmap bitmap) {
                Log.e(TAG, "onTakeScreenshot(Bitmap bitmap)");
                mRajawaliBitmap = bitmap;
                mController.takePhoto();
            }
        });

    }

    private void initCaptureButton() {
        mCapture = (CircularProgressView) findViewById(R.id.btn_capture);

        // Photo / video button logic
        mCapture.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:

                        time = System.currentTimeMillis();
                        break;
                    case MotionEvent.ACTION_UP:
                        //Short press to take picture
                        if(System.currentTimeMillis() - time < 500){
                            mFrameType = TYPE_PHOTO;
                            mController.setFrameCallback(IMAGE_WIDTH, IMAGE_HEIGHT,
                                    MainActivity.this);
                            // When taking pictures, take Rajawali's frame data first
                            ((org.rajawali3d.view.SurfaceView) mRenderSurface).takeScreenshot();
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void initMenuButton() {
        ImageView ivOrnament = (ImageView) findViewById(R.id.iv_ornament);

        ivOrnament.setColorFilter(Color.WHITE);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.iv_ornament:
                        mOrnamentSheet.show();
                        break;
                }
            }
        };
        ivOrnament.setOnClickListener(onClickListener);

    }


    private void initCommonView() {
        //mLayoutRoot = (RelativeLayout) findViewById(R.id.layout_root);
        mTrackText = (TextView) findViewById(R.id.tv_track);
        mActionText = (TextView) findViewById(R.id.tv_action);
    }



    private void initOrnamentSheet() {
        mOrnaments = new ArrayList<>();
        mOrnamentAdapter = new OrnamentAdapter(mContext, mOrnaments);
        mOrnamentAdapter.setOnItemClickListener(new OrnamentAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                mOrnamentSheet.dismiss();
                Ornament ornament = mOrnaments.get(position);
                if (position == 0)
                    ornament = null;
                ((My3DRenderer) mISurfaceRenderer).setOrnamentModel(ornament);
                ((My3DRenderer) mISurfaceRenderer).setIsNeedUpdateOrnament(true);
            }
        });

        View sheetView = LayoutInflater.from(mContext)
                .inflate(R.layout.layout_bottom_sheet, null);
        mRvOrnament = (RecyclerView) sheetView.findViewById(R.id.rv_gallery);
        mRvOrnament.setAdapter(mOrnamentAdapter);
        mRvOrnament.setLayoutManager(new GridLayoutManager(mContext, 4));
        mOrnamentSheet = new CustomBottomSheet(mContext);
        mOrnamentSheet.setContentView(sheetView);
        mOrnamentSheet.getWindow().findViewById(R.id.design_bottom_sheet)
                .setBackgroundResource(android.R.color.transparent);

        mOrnaments.addAll(OrnamentFactory.getPresetOrnament());
        // mOrnaments.addAll(OrnamentFactory.getPresetMask());
        mOrnamentAdapter.notifyDataSetChanged();
    }



    // Initialized Runnable
    private Runnable initViewRunnable = new Runnable() {
        @Override
        public void run() {
            //mExecutor = Executors.newSingleThreadExecutor();
            mController = new TextureController(mContext);
            // Set the data source
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mRenderer = new CameraTrackRenderer(mContext, (CameraManager)getSystemService
                        (CAMERA_SERVICE), mController, cameraId);
                // Face detection of the callback
                ((CameraTrackRenderer) mRenderer).setTrackCallBackListener(new CameraTrackRenderer.
                        TrackCallBackListener() {
                    @Override
                    public void onTrackDetected(STMobileFaceAction[] faceActions,
                                                final int orientation, final int value,
                                                final float pitch, final float roll,
                                                final float yaw, final int eye_dist,
                                                final int id, final int eyeBlink,
                                                final int mouthAh, final int headYaw,
                                                final int headPitch, final int browJump) {
                        onTrackDetectedCallback(faceActions, orientation, value,
                                pitch, roll, yaw, eye_dist, id, eyeBlink, mouthAh, headYaw,
                                headPitch, browJump);
                    }
                });

            }else{
                // Camera1 temporarily did not write face detection
                mRenderer = new Camera1Renderer(mController, cameraId);
            }

            setContentView();

            mController.setFrameCallback(IMAGE_WIDTH, IMAGE_HEIGHT, MainActivity.this);
            mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    mController.surfaceCreated(holder);
                    mController.setRenderer(mRenderer);
                }
                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
                {
                    mController.surfaceChanged(width, height);
                }
                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mController.surfaceDestroyed();
                }
            });
        }
    };


    @Override
    public void onFrame(final byte[] bytes, long time) {

        if (mFrameType == TYPE_PHOTO) {  // Deal with pictures
            mFrameType = TYPE_NONE;
            handlePhotoFrame(bytes);
        }
    }

    @Override
    public void onSavePhotoSuccess(final String fileName) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Saved successfully\n" + fileName,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSavePhotoFailed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Unable to save the photo\n",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }



    @Override
    public void onGet3dModelRotation(float pitch, float roll, float yaw) {
        ((My3DRenderer) mISurfaceRenderer).setAccelerometerValues(roll, yaw, pitch);


    }

    @Override
    public void onGet3dModelTransition(float x, float y, float z) {
        ((My3DRenderer) mISurfaceRenderer).setTransition(x, y, z);
    }

    @Override
    public void onGetFaceLandmark(float[] landmarkX, float[] landmarkY, int isMouthOpen) {
        if (mIsNeedSkinColor) {
            float x = landmarkX[44] * IMAGE_WIDTH;
            float y = landmarkY[44] * IMAGE_HEIGHT;
            //mSamplePoint = new PointF(x, y);
        }

        AFilter aFilter = mController.getLastFilter();
        if(aFilter != null && aFilter instanceof LandmarkFilter) {
            ((LandmarkFilter) aFilter).setLandmarks(landmarkX, landmarkY);
            ((LandmarkFilter) aFilter).setMouthOpen(isMouthOpen);
        }

        float[] copyLandmarkX = new float[landmarkX.length];
        float[] copyLandmarkY = new float[landmarkY.length];
        System.arraycopy(landmarkX, 0, copyLandmarkX, 0, landmarkX.length);
        System.arraycopy(landmarkY, 0, copyLandmarkY, 0, landmarkY.length);
        mPresenter.handleChangeModel(copyLandmarkX, copyLandmarkY);
    }

    @Override
    public void onGetChangePoint(List<DynamicPoint> mDynamicPoints) {
        ((My3DRenderer) mISurfaceRenderer).setDynamicPoints(mDynamicPoints);
    }


    private void handlePhotoFrame(final byte[] bytes) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mPresenter.handlePhotoFrame(bytes, mRajawaliBitmap, IMAGE_WIDTH, IMAGE_HEIGHT);
            }
        }).start();
    }

    private void onTrackDetectedCallback(STMobileFaceAction[] faceActions, final int orientation,
                                         final int value, final float pitch, final float roll,
                                         final float yaw, final int eye_dist, final int id,
                                         final int eyeBlink, final int mouthAh, final int headYaw,
                                         final int headPitch, final int browJump) {
        // Handle 3D model rotation
        mPresenter.handle3dModelRotation(pitch, roll, yaw);
        //Handle 3D model translation
        mPresenter.handle3dModelTransition(faceActions, orientation, eye_dist, yaw, PREVIEW_WIDTH,
                PREVIEW_HEIGHT);
        // Handle the key point of the face
        mPresenter.handleFaceLandmark(faceActions, orientation, mouthAh, PREVIEW_WIDTH,
                PREVIEW_HEIGHT);
        // Display face detection parameters
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTrackText.setText("TRACK: " + value + " MS"
                        + "\nPITCH: " + pitch + "\nROLL: " + roll + "\nYAW: " + yaw
                        + "\nEYE_DIST:" + eye_dist);
                mActionText.setText("ID:" + id + "\nEYE_BLINK:" + eyeBlink + "\nMOUTH_AH:"
                        + mouthAh + "\nHEAD_YAW:" + headYaw + "\nHEAD_PITCH:" + headPitch
                        + "\nBROW_JUMP:" + browJump);
            }
        });
    }


}
