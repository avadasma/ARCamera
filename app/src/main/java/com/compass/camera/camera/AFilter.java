package com.compass.camera.camera;

import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;
import android.util.SparseArray;

import com.compass.camera.MatrixUtils;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * Created by kavya on 10/1/18.
 */

public abstract class AFilter {
    private final static String TAG = AFilter.class.getSimpleName();
    public static boolean DEBUG = true;
    /**
     * Unit matrix
     */
    public static final float[] OM = MatrixUtils.getOriginalMatrix();
    /**
     * Program handle
     */
    protected int mProgram;
    /**
     *Vertex coordinate handle
     */
    protected int mHPosition;
    /**
     * Texture coordinate handle
     */
    protected int mHCoord;
    /**
     * Total transformation matrix handle
     */
    protected int mHMatrix;
    /**
     * The default texture map handle
     */
    protected int mHTexture;

    protected Resources mRes;

    /**
     * Vertex coordinates Buffer
     */
    protected FloatBuffer mVerBuffer;

    /**
     * Texture coordinates Buffer
     */
    protected FloatBuffer mTexBuffer;

    /**
     *Index coordinates Buffer
     */
    protected ShortBuffer mindexBuffer;

    protected int mFlag = 0;

    private float[] matrix = Arrays.copyOf(OM, 16);

    //Texture2D0 is used by default
    private int textureType = 0;
    private int textureId = 0;

    //Vertex coordinates
    private float pos[] = {
            -1.0f,  1.0f,
            -1.0f, -1.0f,
            1.0f, 1.0f,
            1.0f,  -1.0f,
    };

    //Texture coordinates
    private float[] coord={
            0.0f, 0.0f,
            0.0f,  1.0f,
            1.0f,  0.0f,
            1.0f, 1.0f,
    };

    private SparseArray<boolean[]> mBools;
    private SparseArray<int[]> mInts;
    private SparseArray<float[]> mFloats;

    public AFilter(Resources mRes){
        this.mRes = mRes;
        initBuffer();
    }

    public final void create(){
        onCreate();
    }

    public final void setSize(int width, int height){
        onSizeChanged(width, height);
    }

    public void draw(){
        onClear();
        onUseProgram();
        onSetExpandData();
        onBindTexture();
        onDraw();
    }

    public void setMatrix(float[] matrix){
        this.matrix = matrix;
    }

    public float[] getMatrix(){
        return matrix;
    }


    public final int getTextureType(){
        return textureType;
    }

    public final int getTextureId(){
        return textureId;
    }

    public final void setTextureId(int textureId){
        this.textureId = textureId;
    }

    public void setFlag(int flag){
        this.mFlag = flag;
    }

    public int getFlag(){
        return mFlag;
    }


    public void setInt(int type, int ... params){
        if(mInts == null){
            mInts = new SparseArray<>();
        }
        mInts.put(type, params);
    }



    public int getOutputTexture(){
        return -1;
    }

    /**
     * To complete the creation of the program, you can directly call createProgram to achieve
     */
    protected abstract void onCreate();
    protected abstract void onSizeChanged(int width, int height);

    protected final void createProgram(String vertex, String fragment){
        mProgram = uCreateGlProgram(vertex,fragment);
        mHPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mHCoord = GLES20.glGetAttribLocation(mProgram,"vCoord");
        mHMatrix = GLES20.glGetUniformLocation(mProgram,"vMatrix");
        mHTexture = GLES20.glGetUniformLocation(mProgram,"vTexture");
    }

    protected final void createProgramByAssetsFile(String vertex, String fragment){
        createProgram(uRes(mRes, vertex), uRes(mRes, fragment));
    }

    /**
     * Buffer initialization
     */
    protected void initBuffer(){
        ByteBuffer a = ByteBuffer.allocateDirect(32);
        a.order(ByteOrder.nativeOrder());
        mVerBuffer = a.asFloatBuffer();
        mVerBuffer.put(pos);
        mVerBuffer.position(0);
        ByteBuffer b = ByteBuffer.allocateDirect(32);
        b.order(ByteOrder.nativeOrder());
        mTexBuffer = b.asFloatBuffer();
        mTexBuffer.put(coord);
        mTexBuffer.position(0);
    }

    protected void onUseProgram(){
        GLES20.glUseProgram(mProgram);
    }

    /**
     * Enable vertex coordinates and texture coordinates for drawing
     */
    protected void onDraw(){
        GLES20.glEnableVertexAttribArray(mHPosition);
        GLES20.glVertexAttribPointer(mHPosition, 2, GLES20.GL_FLOAT, false, 0, mVerBuffer);
        GLES20.glEnableVertexAttribArray(mHCoord);
        GLES20.glVertexAttribPointer(mHCoord, 2, GLES20.GL_FLOAT, false, 0, mTexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mHPosition);
        GLES20.glDisableVertexAttribArray(mHCoord);
    }

    /**
     * Clear the canvas
     */
    protected void onClear(){
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Set other extended data
     */
    protected void onSetExpandData(){
        GLES20.glUniformMatrix4fv(mHMatrix, 1, false, matrix, 0);
    }

    /**
     * Bind the default texture
     */
    protected void onBindTexture(){
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureType);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getTextureId());
        GLES20.glUniform1i(mHTexture, textureType);
    }

    public static void glError(int code, Object index){
        if(DEBUG && code != 0){
            Log.e(TAG, "glError:" + code + "---" + index);
        }
    }

    //Load text content in Assets by path
    public static String uRes(Resources mRes, String path){
        StringBuilder result = new StringBuilder();
        try{
            InputStream is = mRes.getAssets().open(path);
            int ch;
            byte[] buffer = new byte[1024];
            while (-1 != (ch = is.read(buffer))){
                result.append(new String(buffer, 0, ch));
            }
        }catch (Exception e){
            return null;
        }
        return result.toString().replaceAll("\\r\\n", "\n");
    }

    //Create a GL program
    public static int uCreateGlProgram(String vertexSource, String fragmentSource){
        int vertex = uLoadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertex == 0) return 0;
        int fragment = uLoadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragment == 0) return 0;
        int program = GLES20.glCreateProgram();
        if (program !=0 ){
            GLES20.glAttachShader(program, vertex);
            GLES20.glAttachShader(program, fragment);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE){
                glError(1, "Could not link program:" +  GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    //Loading shader
    public static int uLoadShader(int shaderType, String source){
        int shader = GLES20.glCreateShader(shaderType);
        if(0 != shader){
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0){
                glError(1, "Could not compile shader:" + shaderType);
                glError(1, "GLES20 Error:" + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }
}
