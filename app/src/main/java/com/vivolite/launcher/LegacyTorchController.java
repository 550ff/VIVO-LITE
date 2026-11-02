package com.vivolite.launcher;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.List;

/** API 21/22 torch controller. The tiny preview surface is attached to the Activity decor. */
final class LegacyTorchController extends SurfaceView implements SurfaceHolder.Callback {
    interface Listener { void onTorchChanged(boolean enabled); }
    private final Activity activity;
    private final Listener listener;
    private Camera camera;
    private boolean enabled;
    private boolean permissionPending;

    LegacyTorchController(Activity activity,Listener listener) {
        super(activity);this.activity=activity;this.listener=listener;
        getHolder().addCallback(this);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setAlpha(0.01f);
    }

    void toggle() {
        if(enabled){turnOff();return;}
        if(Build.VERSION.SDK_INT>=23&&activity.checkSelfPermission(android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) {
            permissionPending=true;activity.requestPermissions(new String[]{android.Manifest.permission.CAMERA},MainActivity.REQUEST_CAMERA_PERMISSION);return;
        }
        turnOn();
    }

    void onPermissionResult(boolean granted) {
        if(!permissionPending)return;permissionPending=false;
        if(granted)turnOn();else android.widget.Toast.makeText(activity,"Camera permission is needed for the flashlight",android.widget.Toast.LENGTH_SHORT).show();
    }

    private void turnOn() {
        if(!getHolder().getSurface().isValid())return;
        try {
            camera=openBackCamera();
            if(camera==null)throw new IllegalStateException("No rear torch");
            Camera.Parameters parameters=camera.getParameters();
            List<Camera.Size> sizes=parameters.getSupportedPreviewSizes();
            if(sizes!=null&&!sizes.isEmpty()) {
                Camera.Size smallest=sizes.get(0);
                for(Camera.Size size:sizes)if((long)size.width*size.height<(long)smallest.width*smallest.height)smallest=size;
                parameters.setPreviewSize(smallest.width,smallest.height);
                getHolder().setFixedSize(smallest.width,smallest.height);
            }
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(parameters);
            camera.setPreviewDisplay(getHolder());
            camera.startPreview();
            enabled=true;listener.onTorchChanged(true);
        } catch(Exception e) { releaseCamera();android.widget.Toast.makeText(activity,"Flashlight unavailable while the camera is in use",android.widget.Toast.LENGTH_SHORT).show(); }
    }

    private Camera openBackCamera() {
        int count=Camera.getNumberOfCameras();
        for(int i=0;i<count;i++) {
            Camera.CameraInfo info=new Camera.CameraInfo();Camera.getCameraInfo(i,info);
            if(info.facing!=Camera.CameraInfo.CAMERA_FACING_BACK)continue;
            Camera candidate=null;
            try {
                candidate=Camera.open(i);
                List<String> modes=candidate.getParameters().getSupportedFlashModes();
                if(modes!=null&&modes.contains(Camera.Parameters.FLASH_MODE_TORCH))return candidate;
            } catch(Exception ignored) { }
            if(candidate!=null)try{candidate.release();}catch(Exception ignored){}
        }
        return null;
    }

    void turnOff() {
        if(camera!=null) {
            try {Camera.Parameters parameters=camera.getParameters();parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);camera.setParameters(parameters);}catch(Exception ignored){}
        }
        releaseCamera();
        if(enabled){enabled=false;listener.onTorchChanged(false);}
    }

    private void releaseCamera() {
        if(camera!=null){try{camera.stopPreview();}catch(Exception ignored){}try{camera.release();}catch(Exception ignored){}camera=null;}
    }

    boolean isEnabled(){return enabled;}
    @Override public void surfaceCreated(SurfaceHolder holder) { }
    @Override public void surfaceChanged(SurfaceHolder holder,int format,int width,int height) { }
    @Override public void surfaceDestroyed(SurfaceHolder holder) { turnOff(); }
}
