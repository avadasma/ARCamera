package com.compass.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.compass.camera.camera.AFilter;
import com.compass.camera.camera.GroupFilter;
import com.compass.camera.camera.NoFilter;
import com.compass.camera.camera.TextureFilter;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by kavya on 10/1/18.
 */

public class TextureController implements GLSurfaceView.Renderer {
    private Context mContext;
    private Object mSurface;
    private GLView mGLView;

    private MyRenderer mRenderer;
    // Filter effects processing
    private TextureFilter mEffectFilter;
    // Intermediate effects
    private GroupFilter mGroupFilter;
    // Filter used to render the output
    private AFilter mShowFilter;
    private Point mDataSize;
    private Point mWindowSize;
    private AtomicBoolean isParamSet = new AtomicBoolean(false);
    // Matrix used to draw callbacks
    private float[] callbackOM = new float[16];
    // Create an off-screen buffer for the final export of data
    private int[] mExportFrame = new int[1];
    private int[] mExportTexture = new int[1];
    // Transform matrix for drawing onto the screen
    private float[] SM = new float[16];
    // Output to the screen
    private int mShowType = MatrixUtils.TYPE_CENTER_CROP;
    private int mDirectionFlag = -1;
    // Video flag
    private boolean isRecord = false;
    // Once shot flag
    private boolean isShoot = false;  ///completed
    private boolean isNeedFrame = false;
    //Buffer for storing callback data
    private ByteBuffer[] outPutBuffer = new ByteBuffer[3];
    // Callback
    private FrameCallback mFrameCallback;//completed
    // Callback data width and height
    private int frameCallbackWidth, frameCallbackHeight;//completed
    // The buffer index used by the callback data
    private int indexOutput=0;
    //Filters and effects are applied to both preview and FrameCallback
    public static final int FRAME_CALLBACK_DEFAULT = 0;
    // Preview has a filter effect, FrameCallback no
    public static final int FRAME_CALLBACK_NO_FILTER = 1;
    // Preview no filter effect, FrameCallback there
    public static final int FRAME_CALLBACK_FILTER = 2;
    // Only preview, disable FrameCallback
    public static final int FRAME_CALLBACK_DISABLE = 3;
    // Disable preview, only FrameCallback
    public static final int FRAME_CALLBACK_ONLY = 4;

    private int mFrameCallbackType = FRAME_CALLBACK_DEFAULT;


    public TextureController(Context context) {
        this.mContext = context;
        init();
    }

    public void surfaceCreated(Object nativeWindow){
        this.mSurface = nativeWindow;
        mGLView.surfaceCreated(null);
    }

    public void surfaceChanged(int width, int height){
        this.mWindowSize.x = width;
        this.mWindowSize.y = height;
        mGLView.surfaceChanged(null, 0, width, height);
    }

    public void surfaceDestroyed(){
        mGLView.surfaceDestroyed(null);
    }


    private void init(){
        mGLView = new GLView(mContext);

        //Avoid GLView's attachToWindow and detachFromWindow crashes
        ViewGroup v = new ViewGroup(mContext) {
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {}
        };
        v.addView(mGLView);
        v.setVisibility(View.GONE);

        mEffectFilter = new TextureFilter(mContext.getResources());
        mShowFilter = new NoFilter(mContext.getResources());
        mGroupFilter = new GroupFilter(mContext.getResources());

        //Set the default DateSize, DataSize by AiyaProvider based on the data source image height
        // and height settings
        mDataSize = new Point(720,1280);
        mWindowSize = new Point(720,1280);
    }

    //Before creating Surface, Should be called
    public void setDataSize(int width, int height){
        mDataSize.x = width;
        mDataSize.y = height;
    }

    public SurfaceTexture getTexture(){
        return mEffectFilter.getTexture();
    }

    public void setImageDirection(int flag){
        this.mDirectionFlag = flag;
    }

    public void setRenderer(MyRenderer renderer){
        mRenderer = renderer;
    }


    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mEffectFilter.create();
        mGroupFilter.create();
        mShowFilter.create();

        if(!isParamSet.get()){
            if(mRenderer!=null){
                mRenderer.onSurfaceCreated(gl, config);
            }
            sdkParamSet();
        }
        //calculateCallbackOM();

        mEffectFilter.setFlag(mDirectionFlag);

        deleteFrameBuffer();
        GLES20.glGenFramebuffers(1,mExportFrame,0);
        EasyGlUtils.genTexturesWithParameter(1,mExportTexture,0,GLES20.GL_RGBA,
                mDataSize.x, mDataSize.y);
    }

   private void deleteFrameBuffer() {
        GLES20.glDeleteFramebuffers(1, mExportFrame, 0);
        GLES20.glDeleteTextures(1, mExportTexture, 0);
    }

   @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        MatrixUtils.getMatrix(SM,mShowType, mDataSize.x, mDataSize.y, width, height);

        mShowFilter.setSize(width, height);
        mShowFilter.setMatrix(SM);

        mGroupFilter.setSize(mDataSize.x,mDataSize.y);

        mEffectFilter.setSize(mDataSize.x, mDataSize.y);
        mShowFilter.setSize(mDataSize.x,mDataSize.y);

        if(mRenderer != null){
            mRenderer.onSurfaceChanged(gl, width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if(isParamSet.get()){
            mEffectFilter.draw();
            mGroupFilter.setTextureId(mEffectFilter.getOutputTexture());
            mGroupFilter.draw();

            //Incoming textures are displayed on the screen
            GLES20.glViewport(0, 0, mWindowSize.x, mWindowSize.y);
            mShowFilter.setMatrix(SM);

            switch (mFrameCallbackType) {
                case FRAME_CALLBACK_DEFAULT:
                case FRAME_CALLBACK_NO_FILTER:
                    mShowFilter.setTextureId(mGroupFilter.getOutputTexture());
                    break;
                case FRAME_CALLBACK_FILTER:
                    mShowFilter.setTextureId(mEffectFilter.getOutputTexture());
                    break;
                case FRAME_CALLBACK_ONLY:
                    mShowFilter.setTextureId(0);
                    break;
            }

            mShowFilter.draw();

            if(mRenderer != null){
                mRenderer.onDrawFrame(gl);
            }
            callbackIfNeeded();
        }
    }


    public AFilter getLastFilter() {
        return mGroupFilter.getLastFilter();
    }

    public void takePhoto(){
        isShoot = true;
    }


    public void setFrameCallback(int width, int height, FrameCallback frameCallback){
        this.frameCallbackWidth = width;
        this.frameCallbackHeight = height;
        if (frameCallbackWidth > 0 && frameCallbackHeight > 0) {
            if(outPutBuffer != null){
                outPutBuffer = new ByteBuffer[3];
            }
         //   calculateCallbackOM();
            this.mFrameCallback = frameCallback;
        } else {
            this.mFrameCallback = null;
        }
    }

   private void calculateCallbackOM(){
        if(frameCallbackHeight > 0 && frameCallbackWidth > 0 && mDataSize.x > 0 && mDataSize.y > 0){
            //The output transformation matrix is calculated
            MatrixUtils.getMatrix(callbackOM, MatrixUtils.TYPE_CENTER_CROP, mDataSize.x, mDataSize.y,
                    frameCallbackWidth,
                    frameCallbackHeight);
            MatrixUtils.flip(callbackOM, false, true);
        }
    }



    private void sdkParamSet(){
        if(!isParamSet.get()&&mDataSize.x>0&&mDataSize.y>0) {
            isParamSet.set(true);
        }
    }



   //Need callback, then zoom to the specified size of the image, read the data and callback
    private void callbackIfNeeded() {
        if (mFrameCallback != null && (isRecord || isShoot || isNeedFrame)) {
            indexOutput = indexOutput++ >= 2 ? 0 : indexOutput;
            if (outPutBuffer[indexOutput] == null) {
                outPutBuffer[indexOutput] = ByteBuffer.allocate(frameCallbackWidth *
                        frameCallbackHeight*4);
            }
            GLES20.glViewport(0, 0, frameCallbackWidth, frameCallbackHeight);
            EasyGlUtils.bindFrameTexture(mExportFrame[0],mExportTexture[0]);

            switch (mFrameCallbackType) {
                case FRAME_CALLBACK_DEFAULT:
                case FRAME_CALLBACK_FILTER:
                case FRAME_CALLBACK_ONLY:
                    mShowFilter.setTextureId(mGroupFilter.getOutputTexture());
                    break;
                case FRAME_CALLBACK_NO_FILTER:
                    mShowFilter.setTextureId(mEffectFilter.getOutputTexture());
                    break;
            }

            mShowFilter.setMatrix(callbackOM);
            mShowFilter.draw();
            frameCallback();
            isShoot = false;
            EasyGlUtils.unBindFrameBuffer();
            mShowFilter.setMatrix(SM);
        }
    }

    //Read data and callback
    private void frameCallback(){
        GLES20.glReadPixels(0, 0, frameCallbackWidth, frameCallbackHeight,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, outPutBuffer[indexOutput]);
        ByteBuffer byteBuffer = outPutBuffer[indexOutput];
        if (byteBuffer != null) {
            mFrameCallback.onFrame(byteBuffer.array(), 0);
        }
    }

    public void requestRender(){
        mGLView.requestRender();
    }



    private class GLView extends GLSurfaceView{

        public GLView(Context context) {
            super(context);
            init();
        }

        private void init(){
            getHolder().addCallback(null);
            setEGLWindowSurfaceFactory(new GLSurfaceView.EGLWindowSurfaceFactory() {
                @Override
                public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig
                        config, Object window) {
                    return egl.eglCreateWindowSurface(display, config, mSurface, null);
                }

                @Override
                public void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface) {
                    egl.eglDestroySurface(display, surface);
                }
            });
            setEGLContextClientVersion(2);
            setRenderer(TextureController.this);
            setRenderMode(RENDERMODE_WHEN_DIRTY);
            setPreserveEGLContextOnPause(true);
        }


    }
}
