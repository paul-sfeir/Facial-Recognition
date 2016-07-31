package com.example.hyouka.pictureloaderrefactored;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.media.FaceDetector;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Hyouka on 4/20/2016.
 */
public class PictureManager {
    String PC = "PictureLoader";
    // for constructor
    private MainActivity activity;
    protected ImageView iv;
    // for ImageView drag and zoom effect
    protected int IMAGE_ACTION = 0;
    private static final int IMAGE_ZOOM = 1;
    private static final int IMAGE_DRAG = 2;
    protected float beforeSize;

    protected String ImageFilePath = null;
    public String getImageFilePath(){return ImageFilePath;}

    public int FILE_BROWSE_CODE = 233;

    private static final int IMAGEVIEW_WIDTH = 350;
    private static final int IMAGEVIEW_HEIGHT = 400;

    protected int width;
    protected int height;


    protected Matrix resetMatrix = new Matrix();

    protected FaceDetector.Face[] faces;
    protected int detected;

    protected DatabaseHelper helper;
    protected int sample = 1;

    public PictureManager(MainActivity activity, ImageView targetImageView){
        this.activity = activity;
        this.iv = targetImageView;
        helper = new DatabaseHelper(activity.getApplicationContext());
        setListViewOnItemClickListener();
    }

    private void setListViewOnItemClickListener(){
        ListView listView = (ListView) activity.findViewById(R.id.face_position);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(ImageFilePath.equals(null))return;
                BitmapDrawable drawable = (BitmapDrawable) iv.getDrawable();
                if(drawable==null)return;
                Bitmap bp = drawable.getBitmap();
                Bitmap tempbp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(tempbp);
                canvas.drawBitmap(bp, 0, 0, null);
                Paint paint = new Paint();
                paint.setColor(Color.YELLOW);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);
                TextView textView = (TextView) view.findViewById(R.id.face_lux);
                Float lux = Float.parseFloat((String) textView.getText());
                textView = (TextView) view.findViewById(R.id.face_luy);
                Float luy = Float.parseFloat((String) textView.getText());
                textView = (TextView) view.findViewById(R.id.face_rdx);
                Float rdx = Float.parseFloat((String) textView.getText());
                textView = (TextView) view.findViewById(R.id.face_rdy);
                Float rdy = Float.parseFloat((String) textView.getText());

                canvas.drawRect(
                       lux,luy,rdx,rdy,
                        paint);
                iv.setImageDrawable(new BitmapDrawable(activity.getResources(), tempbp));
            }
        });
    }

    public void enableZoomAndDrag(boolean enable){
        if(enable){
            final PointF pressPoint0 = new PointF();
            final Matrix transformMatrix = new Matrix();
            iv.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    // handle touch event
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:
                            pressPoint0.set(event.getX(), event.getY());
                            transformMatrix.set(iv.getImageMatrix());
                            IMAGE_ACTION = IMAGE_DRAG;
                            break;
                        case MotionEvent.ACTION_POINTER_DOWN:
                            beforeSize = getDistance(event);
                            IMAGE_ACTION = IMAGE_ZOOM;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            switch (IMAGE_ACTION) {
                                case IMAGE_DRAG:
                                    PointF press = new PointF(event.getX(), event.getY());
                                    transformMatrix.postTranslate
                                            ((press.x - pressPoint0.x), (press.y - pressPoint0.y));
                                    pressPoint0.set(press);
                                    break;
                                case IMAGE_ZOOM:
                                    float afterSize = getDistance(event);
                                    PointF middlePointF = getMiddlePointF(event);
                                    float zoomScale = afterSize / beforeSize;
                                    beforeSize = afterSize;
                                    iv.setScaleType(ImageView.ScaleType.MATRIX);
                                    transformMatrix.postScale(zoomScale,
                                            zoomScale, middlePointF.x, middlePointF.y);
                                    break;
                            }
                            break;
                        case MotionEvent.ACTION_POINTER_UP:
                            IMAGE_ACTION = 0;
                            break;
                        case MotionEvent.ACTION_UP:
                            IMAGE_ACTION = 0;
                            break;
                    }
                    iv.setImageMatrix(transformMatrix);
                    iv.invalidate();
                    return true;
                }
            });

        }else{
            iv.setOnTouchListener(null);
        }
    }

    public void selectPicture(){
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                // use FileBrowser.java to use the simple file browser to get selected file path
                Intent intent = new Intent(activity, FileBrowser.class);
                activity.startActivityForResult(intent, FILE_BROWSE_CODE);
            } else {
                Toast.makeText(activity.getApplicationContext(), "Storage not present", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPicture(String path){
        if(!Util.isImage(new File(path).getName()))return;
        Log.d(PC,"image loading from: "+path);
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opt);

        opt.inSampleSize = calculateInSampleSize(opt, IMAGEVIEW_WIDTH, IMAGEVIEW_HEIGHT);

        sample = opt.inSampleSize;

        opt.inJustDecodeBounds = false;
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap image = BitmapFactory.decodeFile(path, opt);
        iv.setImageBitmap(image);
        iv.setImageMatrix(resetMatrix);
        iv.invalidate();

        if(image==null)return;
        ImageFilePath = path;
        // get image property for property button to show
        width = image.getWidth();
        height = image.getHeight();
        Log.d(PC,"image loaded from: "+path);

        // set listview to null
        ListView lv = (ListView) activity.findViewById(R.id.face_position);
        lv.setAdapter(null);
    }

    public void showPictureProperty(){
        try {
            // calculate the values of the properties
            NumberFormat numformat = new DecimalFormat("#.00");
            Bitmap image = ((BitmapDrawable) iv.getDrawable()).getBitmap();
            int size = image.getRowBytes() * image.getHeight();
            File file = new File(ImageFilePath);
            long actualsize = file.length();

            // show the properties using a alterdialog
            AlertDialog.Builder box = new AlertDialog.Builder(activity);
            box.setTitle("Image Property");
            String message = "Width X Height:\t" + Integer.toString(width) + " X " + Integer.toString(height);
            message += "\nDisplay Size:\n\t" + numformat.format(size / 1024.00) + " KB(" + Integer.toString(size) + " Bytes)";
            message += "\nSize On Disk:\n\t" + numformat.format(actualsize / 1024.00) + " KB(" + Long.toString(actualsize) + "Bytes)";
            box.setMessage(message);
            box.create().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void detectFaces(int MAX_NUM_OF_FACE){
        if(MAX_NUM_OF_FACE<=0)MAX_NUM_OF_FACE=10;
        try {
            // if no image is loaded, do not run detection
            if (ImageFilePath.equals(null)) return;
            String md5 = Util.MD5(new File(ImageFilePath));
            Cursor file = helper.getFileRecords(md5);
            if(file==null || file.getCount()==0) {
                // prepare bitmap
                ImageView iv = (ImageView) activity.findViewById(R.id.face_image);
                BitmapDrawable drawable = (BitmapDrawable) iv.getDrawable();
                Bitmap bp = null;
                if(drawable==null)return;
                else bp = drawable.getBitmap();
                // prepare face detection
                faces = new FaceDetector.Face[MAX_NUM_OF_FACE];
                FaceDetector detector = new FaceDetector(width, height, MAX_NUM_OF_FACE);
                // get detected faces
                detected = detector.findFaces(bp, faces);
                // prepare to draw rectangle
                if (detected <= 0) return;
                float x[] = new float[detected];
                float y[] = new float[detected];
                float eye[] = new float[detected];
                for (int i = 0; i < detected; ++i) {
                    PointF middle = new PointF();
                    faces[i].getMidPoint(middle);
                    x[i] = middle.x;
                    y[i] = middle.y;
                    eye[i] = faces[i].eyesDistance();
                }
                DrawRectAndSetData(x, y, eye, detected);
                // insert data
                helper.addImageFileRecord(md5, Calendar.getInstance().toString(),ImageFilePath,detected);
                for(int i=0;i<detected;++i){
                    String eye_dis = Float.toString(faces[i].eyesDistance());
                    PointF mid = new PointF();
                    faces[i].getMidPoint(mid);
                    String mid_x = Float.toString(mid.x);
                    String mid_y = Float.toString(mid.y);
                    String confidence = Float.toString(faces[i].confidence());
                    String e_x = Float.toString(faces[i].pose(0));
                    String e_y = Float.toString(faces[i].pose(1));
                    String e_z = Float.toString(faces[i].pose(2));
                    helper.addFaceRecord(
                            md5,"face"+i,sample,
                            eye_dis,mid_x,mid_y,confidence,e_x,e_y,e_z
                    );
                }
            }else{
                file.moveToFirst();
                int face_num = file.getInt(3);
                float[] x = new float[face_num];
                float[] y = new float[face_num];
                float[] eye_dis = new float[face_num];
                Cursor face = helper.getFaceRecords(md5);
                face.moveToFirst();
                for(int i=0;i<face_num;++i){
                    eye_dis[i] = Float.valueOf(face.getString(3));
                    x[i] = Float.valueOf(face.getString(4));
                    y[i] = Float.valueOf(face.getString(5));
                    face.moveToNext();
                }
                DrawRectAndSetData(x,y,eye_dis,face_num);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void DrawRectAndSetData(float x[],float y[], float eye[],int len){
        // set number
        TextView num = (TextView) activity.findViewById(R.id.number_of_faces);
        num.setText(Integer.toString(len));
        // draw rectangle
        ImageView iv = (ImageView) activity.findViewById(R.id.face_image);
        BitmapDrawable tempDrawable = (BitmapDrawable) iv.getDrawable();
        Bitmap bp=null;
        if (tempDrawable!=null)bp = tempDrawable.getBitmap();
        else return;
        Bitmap tempbp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(tempbp);
        canvas.drawBitmap(bp, 0, 0, null);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        for (int i = 0; i < len; i++) {
            canvas.drawRect((int) (x[i] - eye[i]),
                    (int) (y[i] - eye[i]),
                    (int) (x[i] + eye[i]),
                    (int) (y[i] + eye[i]), paint);
        }
        iv.setImageDrawable(new BitmapDrawable(activity.getResources(), tempbp));
        // set data
        NumberFormat numformat = new DecimalFormat("#.00");
        ListView location = (ListView) activity.findViewById(R.id.face_position);
        List<Map<String, Object>> locationlist = new ArrayList<Map<String, Object>>();
        for(int i=0;i<len;++i){
            Map<String,Object> locationlistmap = new HashMap<String, Object>();

            locationlistmap.put("lux", numformat.format(x[i] - eye[i]));
            locationlistmap.put("luy", numformat.format(y[i] - eye[i]));
            locationlistmap.put("rdx", numformat.format(x[i] + eye[i]));
            locationlistmap.put("rdy", numformat.format(y[i] + eye[i]));
            locationlist.add(locationlistmap);
        }
        SimpleAdapter sa = new SimpleAdapter(activity, locationlist, R.layout.face_position_listview,
                new String[]{"lux","luy","rdx","rdy"},
                new int[]{R.id.face_lux,R.id.face_luy,R.id.face_rdx,R.id.face_rdy});
        location.setAdapter(sa);
    }

    /*
    *  Other help methods are written below
    */
    // used only for zoom
    protected float getDistance(MotionEvent event){
        float xDistance = event.getX(0) - event.getX(1);
        float yDistance = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(xDistance*xDistance+yDistance*yDistance);
    }
    // used only for zoom
    protected PointF getMiddlePointF(MotionEvent event){
        PointF result = new PointF();
        result.set((event.getX(0)+event.getX(1))/2,(event.getY(0)+event.getY(1))/2);
        return result;
    }
    /* code from android developer web page http://developer.android.com/training/displaying-bitmaps/load-bitmap.html */
    protected static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
