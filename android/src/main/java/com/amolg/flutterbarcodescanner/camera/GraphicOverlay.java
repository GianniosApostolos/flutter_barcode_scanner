/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amolg.flutterbarcodescanner.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;


import com.amolg.flutterbarcodescanner.BarcodeCaptureActivity;
import com.amolg.flutterbarcodescanner.FlutterBarcodeScannerPlugin;
import com.amolg.flutterbarcodescanner.constants.AppConstants;
import com.amolg.flutterbarcodescanner.utils.AppUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;


public class GraphicOverlay<T extends GraphicOverlay.Graphic> extends View {
    private final Object mLock = new Object();
    private float mWidthScaleFactor = 1.0f, mHeightScaleFactor = 1.0f;

    private int mFacing = CameraSource.CAMERA_FACING_BACK;
    private Set<T> mGraphics = new HashSet<>();

    /**
     * Custom added values for overlay
     */
    private float left, top, endY;
    private int rectWidth, rectHeight, frames, lineColor, lineWidth;
    private boolean revAnimation;
    private String scanResult;


    public static abstract class Graphic {
        private GraphicOverlay mOverlay;

        public Graphic(GraphicOverlay overlay) {
            mOverlay = overlay;
        }

        public abstract void draw(Canvas canvas);

        public float scaleX(float horizontal) {
            return horizontal * mOverlay.mWidthScaleFactor;
        }

        public float scaleY(float vertical) {
            return vertical * mOverlay.mHeightScaleFactor;
        }

        public float translateX(float x) {
            if (mOverlay.mFacing == CameraSource.CAMERA_FACING_FRONT) {
                return mOverlay.getWidth() - scaleX(x);
            } else {
                return scaleX(x);
            }
        }

        public float translateY(float y) {
            return scaleY(y);
        }

        public void postInvalidate() {
            mOverlay.postInvalidate();
        }
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);

        rectWidth = AppConstants.BARCODE_RECT_WIDTH;
        rectHeight = BarcodeCaptureActivity.SCAN_MODE == BarcodeCaptureActivity.SCAN_MODE_ENUM.QR.ordinal()
                ? AppConstants.BARCODE_RECT_HEIGHT : (int) (AppConstants.BARCODE_RECT_HEIGHT / 1.5);

        lineColor = Color.parseColor(FlutterBarcodeScannerPlugin.lineColor);

        lineWidth = AppConstants.BARCODE_LINE_WIDTH;
        frames = AppConstants.BARCODE_FRAMES;
    }

    public void setScanResult(String scanResult) {

        this.scanResult = scanResult;
        
    }

    public void clear() {
        synchronized (mLock) {
            mGraphics.clear();
        }
        postInvalidate();
    }


    public void add(T graphic) {
        synchronized (mLock) {
            mGraphics.add(graphic);
        }
        postInvalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        left = (w - AppUtil.dpToPx(getContext(), rectWidth)) / 2;
        top = (h - AppUtil.dpToPx(getContext(), rectHeight)) / 2;
        endY = top;
        super.onSizeChanged(w, h, oldw, oldh);
    }


    public void remove(T graphic) {
        synchronized (mLock) {
            mGraphics.remove(graphic);
        }
        postInvalidate();
    }

    public List<T> getGraphics() {
        synchronized (mLock) {
            return new Vector(mGraphics);
        }
    }

    public float getWidthScaleFactor() {
        return mWidthScaleFactor;
    }

    public float getHeightScaleFactor() {
        return mHeightScaleFactor;
    }

    public void setCameraInfo(int previewWidth, int previewHeight, int facing) {
        synchronized (mLock) {
            mFacing = facing;
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // draw transparent rect
        int cornerRadius = 10;
        Paint eraser = new Paint();
        eraser.setAntiAlias(true);
        eraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        RectF rect = new RectF(left, top, AppUtil.dpToPx(getContext(), rectWidth) + left, AppUtil.dpToPx(getContext(), rectHeight) + top);
        canvas.drawRoundRect(rect, (float) cornerRadius, (float) cornerRadius, eraser);

        String scanResultText =this.scanResult;
        // draw result text
        if(scanResultText != null && !scanResultText.trim().isEmpty()) {
            //int alpha = (int) (255 * 0.8);

            String blackTransparentColor = String.format("#%02X000000", (int) (255 * 0.8));
            Paint backgroundColor = new Paint();
            backgroundColor.setColor(Color.parseColor(blackTransparentColor));

            // Create text
            Paint textPaint = new Paint();     
            float textWidth = textPaint.measureText(scanResultText);   
               
            float textSize = ((rectWidth * rectHeight) / (rectWidth + rectHeight)) / 1.5f; // Change to 2f 
            float textPosX = (canvas.getWidth()/2 - textWidth*2);
            float textPosY =  top + AppUtil.dpToPx(getContext(), rectHeight) + rectHeight;
            
            textPaint.setColor(Color.parseColor("#ffffff"));
            
            textPaint.setTextSize(textSize);

            // Create background for text
            float rectStartPosX = textPosX - textPosX/10;
            float rectStartPosY = textPosY - textPosY/12;
            float rectEndPosX = canvas.getWidth()/2 + textWidth*2+textPosX/10;
            float rectEndPosY = textPosY + textSize+10;

            RectF backgroundRectangle = new RectF(rectStartPosX, rectStartPosY, rectEndPosX, rectEndPosY);
            //RectF rectF = new RectF((textPosX-textPosX/10),(textPosY-textPosY/12),(canvas.getWidth()/2 + textWidth*2+textPosX/10),(textPosY+textSize+10));

            // Draw background rectangle and scan result text
            canvas.drawRoundRect(backgroundRectangle, 70f, 70f, backgroundColor);
            canvas.drawText(scanResultText, textPosX, textPosY, textPaint);
        }


        // draw horizontal line
        Paint line = new Paint();
        line.setColor(lineColor);
        line.setStrokeWidth(Float.valueOf(lineWidth));

        // draw the line to product animation
        if (endY >= top + AppUtil.dpToPx(getContext(), rectHeight) + frames) {
            revAnimation = true;
        } else if (endY == top + frames) {
            revAnimation = false;
        }

        // check if the line has reached to bottom
        if (revAnimation) {
            endY -= frames;
        } else {
            endY += frames;
        }
    
        canvas.drawLine(left, endY, left + AppUtil.dpToPx(getContext(), rectWidth), endY, line);
        invalidate();
    }
}