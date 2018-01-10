package com.compass.camera;

/**
 * Created by kavya on 10/1/18.
 */

public interface FrameCallback {
    void onFrame(byte[] bytes, long time);
}
