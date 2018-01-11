package com.compass.camera.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by kavya on 10/1/18.
 */

public class Accelerometer {
    public enum CLOCKWISE_ANGLE {
        Deg0(0), Deg90(1), Deg180(2), Deg270(3);
        private int value;
        private CLOCKWISE_ANGLE(int value){
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }

    private SensorManager sensorManager = null;

    private boolean hasStarted = false;

    private static CLOCKWISE_ANGLE rotation;

    /**
     *
     * @param ctx
     *Initialize the sensor with Activity
     */
    public Accelerometer(Context ctx) {
        sensorManager = (SensorManager) ctx
                .getSystemService(Context.SENSOR_SERVICE);
        rotation = CLOCKWISE_ANGLE.Deg0;
    }

    /**
     * Start monitoring the sensor
     */
    public void start() {
        if (hasStarted) return;
        hasStarted = true;
        rotation = CLOCKWISE_ANGLE.Deg0;
        sensorManager.registerListener(accListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     *
     * @return
     * Return to the current phone steering
     */
    static public int getDirection() {
        return rotation.getValue();
    }

    /**
     * The logic between the sensor and handset steering
     */
    private SensorEventListener accListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1) {
        }

        @Override
        public void onSensorChanged(SensorEvent arg0) {
            if (arg0.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x = arg0.values[0];
                float y = arg0.values[1];
                float z = arg0.values[2];
                if (Math.abs(x)>3 || Math.abs(y)>3) {
                    if (Math.abs(x)>Math.abs(y)) {
                        if (x > 0) {
                            rotation = CLOCKWISE_ANGLE.Deg0;
                            //Log.d("ROTATION","CLOCKWISE_ANGLE: Deg0");
                        } else {
                            rotation = CLOCKWISE_ANGLE.Deg180;
                            //Log.d("ROTATION","CLOCKWISE_ANGLE: Deg180");
                        }
                    } else {
                        if (y > 0) {
                            rotation = CLOCKWISE_ANGLE.Deg90;
                            //Log.d("ROTATION","CLOCKWISE_ANGLE: Deg90");
                        } else {
                            rotation = CLOCKWISE_ANGLE.Deg270;
                            //Log.d("ROTATION","CLOCKWISE_ANGLE: Deg270");
                        }
                    }
                }
            }
        }
    };
}
