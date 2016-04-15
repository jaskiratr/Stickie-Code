package com.example.jess.drawingtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.jess.drawingtest.singleton.Singleton;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;


/**
 * Created by jess on 11/27/15.
 */

public class DrawingView extends View{

    private static String TAG = "DrawingView";

    //drawing path
    private Path drawPath = new Path();
    //drawing and canvas paint
    private Paint drawPaint = new Paint(), canvasPaint;
    //initial color
    private int paintColor = 0xFFFF0A9E;
    //canvas
    private Canvas drawCanvas, paintCanvas;
    //canvas bitmap
    private Bitmap canvasBitmap, paintBitmap;
    private float brushSize, lastBrushSize;

    private int bitmapWidth, bitmapHeight;

    private boolean erase=false;

    public DrawingView(Context context, AttributeSet attrs){
        super(context, attrs);
        setupDrawing();
    }

    public DrawingView(Context context) {
        super(context);

    }

    private void setupDrawing(){
        drawPath = new Path();
        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(20);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        canvasPaint = new Paint(Paint.DITHER_FLAG);

        brushSize = getResources().getInteger(R.integer.medium_size);
        lastBrushSize = brushSize;
        drawPaint.setStrokeWidth(brushSize);

    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        Log.d(TAG, "onSizeChanged()");

        //view given size
        super.onSizeChanged(w, h, oldw, oldh);

        Singleton.INSTANCE.setWidth(w);
        Singleton.INSTANCE.setHeight(h);

        bitmapWidth = w;
        bitmapHeight = h;

        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        paintBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(paintBitmap);

    }

    @Override
    protected void onDraw(Canvas canvas) {

        Log.d(TAG, "onDraw()");

        //draw view
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawBitmap(paintBitmap, 0, 0, canvasPaint);
        canvas.drawPath(drawPath, drawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //detect user touch
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();
                break;
            default:
                return false;
        }

        invalidate();
        return true;
    }

    public void setColor(String newColor){
        //set color
        invalidate();
        paintColor = Color.parseColor(newColor);
        drawPaint.setColor(paintColor);
    }

    public void setColor(int col){

        invalidate();
        drawPaint.setColor(col);
    }

    public void setBrushSize(float newSize){
        //update size
        float pixelAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, getResources().getDisplayMetrics());
        brushSize=pixelAmount;
        drawPaint.setStrokeWidth(brushSize);
    }

    public void setLastBrushSize(float lastSize){
        lastBrushSize=lastSize;
    }
    public float getLastBrushSize(){
        return lastBrushSize;
    }

    public void setErase(boolean isErase) {
        //set erase true or false
        erase = isErase;
        if (erase) {
            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        } else {
            drawPaint.setXfermode(null);
        }
    }
    public void startNew(){
        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        canvasBitmap = Bitmap.createBitmap(Singleton.INSTANCE.getWidth(), Singleton.INSTANCE.getHeight(), Bitmap.Config.ARGB_8888);
        paintBitmap = Bitmap.createBitmap(Singleton.INSTANCE.getWidth(), Singleton.INSTANCE.getHeight(), Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(paintBitmap);
        invalidate();
    }

    // Draw Bitmap on Canvas
    public void setBitmap(String imageUrl){

        Bitmap myBitmap = BitmapFactory.decodeFile(imageUrl);

        Bitmap scaleBitmap = null;
        int bWidth = myBitmap.getWidth();
        int bHeight = myBitmap.getHeight();
        int diff = 0;

        Log.d(TAG, "bitmapWidth = " + bitmapWidth + " bitmapHeight = " + bitmapHeight);

        Log.d(TAG, "bWidth = " + bWidth + " bHeight = " + bHeight);

        if(bWidth >= bHeight){

            if(bWidth > bitmapWidth ){

                // landscape
                float ratio = (float) bWidth / bitmapWidth;
                diff = bWidth - bitmapWidth;
                bHeight = (int)(bHeight / ratio);
                scaleBitmap = Bitmap.createScaledBitmap(myBitmap, bWidth - diff, bHeight, false);
            }
            else{

                scaleBitmap = myBitmap;
            }
        }
        else{

            if(bHeight > bitmapHeight){

                float ratio = (float) bHeight / bitmapHeight;
                diff = bHeight - bitmapHeight;
                bWidth = (int)(bWidth / ratio);
                scaleBitmap = Bitmap.createScaledBitmap(myBitmap, bWidth, bHeight - diff, false);
            }
            else{

                scaleBitmap = myBitmap;
            }
        }



        canvasBitmap = scaleBitmap;
        invalidate();

    }

    public void setImageBitmap(Context ctx, String src){

        if(src != null && !src.isEmpty()){

            Picasso.with(ctx).load(src).into(target);

        }else{

            Toast.makeText(ctx, "Invalid Url", Toast.LENGTH_SHORT).show();
        }
    }

    private Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {

            Log.d("URLIMAGE", "onBitmapFailed");

            Bitmap scaleBitmap = null;
            int bWidth = bitmap.getWidth();
            int bHeight = bitmap.getHeight();
            int diff = 0;

            Log.d(TAG, "bitmapWidth = " + bitmapWidth + " bitmapHeight = " + bitmapHeight);

            Log.d(TAG, "bWidth = " + bWidth + " bHeight = " + bHeight);

            if(bWidth >= bHeight){

                if(bWidth > bitmapWidth ){

                    float ratio = (float) bWidth / bitmapWidth;
                    diff = bWidth - bitmapWidth;
                    bHeight = (int)(bHeight / ratio);
                    scaleBitmap = Bitmap.createScaledBitmap(bitmap, bWidth - diff, bHeight, false);
                }
                else{

                    scaleBitmap = bitmap;
                }
            }
            else{

                if(bHeight > bitmapHeight){

                    float ratio = (float) bHeight / bitmapHeight;
                    diff = bHeight - bitmapHeight;
                    bWidth = (int)(bWidth / ratio);
                    scaleBitmap = Bitmap.createScaledBitmap(bitmap, bWidth, bHeight - diff, false);
                }
                else{

                    scaleBitmap = bitmap;
                }
            }

            canvasBitmap = scaleBitmap;
            invalidate();

        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

            Log.d("URLIMAGE", "onBitmapFailed");

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

            Log.d("URLIMAGE", "onBitmapFailed");

        }


    };


}
