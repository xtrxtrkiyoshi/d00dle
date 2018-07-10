package com.example.aikee.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import flexjson.JSONSerializer;

public class PaintView extends View {
    String TAG = "taggy";
    public static int BRUSH_SIZE = 10;
    public static final int DEFAULT_COLOR = Color.RED;
    public static final int DEFAULT_BG_COLOR = Color.WHITE;
    private float mX, mY;
    private float startX, startY;
    private int height = 0;
    private int width = 0;

    private int canvasHeight = 0;
    private int canvasWidth = 0;

    private static final float TOUCH_TOLERANCE = 4;
    private Paint mPaint;

    private ArrayList<Path> paths = new ArrayList<>();


    private ArrayList<PointF> currentPathPoints = new ArrayList<>();

    private int currentColor;
    private boolean isInitialized;
    private int backgroundColor = DEFAULT_BG_COLOR;
    private int strokeWidth;
//    private ArrayList<Stroke> strokes;
    private enum StrokeState { DRAWN, UNDRAWN}
    private HashMap<Stroke,StrokeState> strokeMap;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);
    private Path mPath;

    public boolean isInitialized() {
        return isInitialized;
    }

    public ArrayList<PointF> getCurrentPathPoints() {
        return currentPathPoints;
    }

    public Stroke getCurrentStroke() {
        return currentStroke;
    }

    //return own height
    public int getScreenHeight() {
        return height;
    }

    //return own width
    public int getScreenWidth() {
        return width;
    }

    public void setCanvasDimensions(int height, int width){
        canvasHeight = height;
        canvasWidth = width;
    }



    private Stroke currentStroke;

    public PaintView(Context context) {
        super(context);
        isInitialized = false;
    }

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        isInitialized = false;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(DEFAULT_COLOR);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setXfermode(null);
        mPaint.setAlpha(0xff);
    }

    public void init(int height, int width)
    {
        isInitialized = true;
        currentPathPoints = new ArrayList<>();
        currentStroke = new Stroke();
        strokeMap = new HashMap<>();
        this.height = height;
        this.width = width;
        Log.d(TAG,"height - " + height + " width - " + width);
        mBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);


        currentColor = DEFAULT_COLOR;
        strokeWidth = BRUSH_SIZE;
    }

    public enum DrawingMode {DRAW, VIEW_ONLY}
    DrawingMode mode = DrawingMode.DRAW;

    public void setMode(DrawingMode mode){
        this.mode = mode;
        invalidate();
    }

    public void drawPainting(ArrayList<Stroke> strokes){
        if(mode == DrawingMode.VIEW_ONLY) {
            for(Stroke stroke : strokes){
                if(!strokeMap.containsKey(stroke)){
                    strokeMap.put(stroke,StrokeState.UNDRAWN);
                }
            }
            invalidate();
        }
    }


    public void clear()
    {
        backgroundColor = DEFAULT_BG_COLOR;
//        paths.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        if(isInitialized) {
            canvas.save();
            mCanvas.drawColor(backgroundColor);
            mPaint.setColor(DEFAULT_COLOR);
            mPaint.setStrokeWidth(strokeWidth);
            mPaint.setMaskFilter(null);
            switch (mode) {
                case DRAW:
                    for (Path path : paths) {
                        mCanvas.drawPath(path, mPaint);
                    }
                    break;
                case VIEW_ONLY:
                    if (strokeMap != null ){
                        Iterator it = strokeMap.entrySet().iterator();
                        while (it.hasNext()){
//                        for (Stroke stroke : strokeMap) {
                            HashMap.Entry entry = (HashMap.Entry) it.next();
                            Stroke stroke = (Stroke)entry.getKey();
                            StrokeState strokeState = (StrokeState) entry.getValue();
                            if (stroke != null) {
                                Path path = new Path();
                                ArrayList<PointF> paths = stroke.getPathPoints();
                                if (paths.size() > 1) {

                                    Iterator itr = paths.iterator();

                                    PointF p0 = (PointF) itr.next();
                                    if(strokeState.equals(StrokeState.UNDRAWN)){
                                        p0.x = (p0.x / canvasWidth) * width;
                                        p0.y = (p0.y / canvasHeight) * height;
                                    }
                                    path.moveTo(p0.x, p0.y);
                                    PointF M = new PointF();
                                    while (itr.hasNext()) {
                                        PointF p = (PointF) itr.next();
                                        if(strokeState.equals(StrokeState.UNDRAWN)) {
                                            p.x = (p.x / canvasWidth) * width;
                                            p.y = (p.y / canvasHeight) * height;
                                        }
                                        M = p;

                                        path.quadTo(p.x, p.y, (p.x + M.x) / 2, (p.y + M.y) / 2);
                                    }

                                    mCanvas.drawPath(path, mPaint);
                                }
                                strokeMap.put(stroke,StrokeState.DRAWN);
                            } else
                                Log.d(TAG, "stroke is null");
                        }
                    }
                    break;
            }
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            canvas.restore();
        }
    }


    protected void touchStart(float x, float y)
    {
        if(mode == DrawingMode.DRAW) {
            mPath = new Path();
            currentStroke = new Stroke();
//        FingerPath fp = new FingerPath(currentColor,strokeWidth,mPath);
            paths.add(mPath);

            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            startX = x;
            mY = y;
            startY = y;
        }
    }
    protected void touchMove(float x, float y)
    {
        if(mode == DrawingMode.DRAW) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);

            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                mX = x;
                mY = y;
            }
        }
    }
    protected void touchUp()
    {
        if(mode == DrawingMode.DRAW) {
            if (startX == mX && startY == mY)
                mPath.lineTo(mX + 1, mY);
            else
                mPath.lineTo(mX, mY);

            currentStroke.setPaths(new ArrayList<PointF>(currentPathPoints));
            currentStroke.setStrokeProperties(currentColor, strokeWidth);

            currentPathPoints.clear();
        }
    }

}
