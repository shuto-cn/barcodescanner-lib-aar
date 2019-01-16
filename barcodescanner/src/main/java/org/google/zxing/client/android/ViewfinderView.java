/*
 * Copyright (C) 2008 ZXing authors
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

package org.google.zxing.client.android;

import org.google.zxing.ResultPoint;
import org.google.zxing.client.android.camera.CameraManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import barcodescanner.xservices.nl.barcodescanner.R;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

  private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
  private static final long ANIMATION_DELAY = 80L;
  private static final int CURRENT_POINT_OPACITY = 0xA0;
  private static final int MAX_RESULT_POINTS = 20;
  private static final int POINT_SIZE = 6;

  private CameraManager cameraManager;
  private final Paint paint;
  private Bitmap resultBitmap;
  private final int maskColor;
  private final int resultColor;
  private final int laserColor;
  private final int hornColor;
  private final int borderColor;
  private final int resultPointColor;

    private final int scanLineStart;
    private final int scanLineMiddle;
    private final int scanLineEnd;

    private int scannerAlpha;


  private List<ResultPoint> possibleResultPoints;
  private List<ResultPoint> lastPossibleResultPoints;

  // This constructor is used when the class is built from an XML resource.
  public ViewfinderView(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Initialize these once for performance rather than calling them every time in onDraw().
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Resources resources = getResources();
    maskColor = resources.getColor(R.color.viewfinder_mask);
    resultColor = resources.getColor(R.color.result_view);
    hornColor = resources.getColor(R.color.viewfinder_horn);
    borderColor = resources.getColor(R.color.viewfinder_border);
    laserColor = resources.getColor(R.color.viewfinder_laser);
    resultPointColor = resources.getColor(R.color.possible_result_points);
      scanLineStart = resources.getColor(R.color.viewfinder_scanline_start);
      scanLineMiddle = resources.getColor(R.color.viewfinder_scanline_middle);
      scanLineEnd = resources.getColor(R.color.viewfinder_scanline_end);
    scannerAlpha = 0;
    possibleResultPoints = new ArrayList<>(5);
    lastPossibleResultPoints = null;
  }

  public void setCameraManager(CameraManager cameraManager) {
    this.cameraManager = cameraManager;
  }

  @SuppressLint("DrawAllocation")
  @Override
  public void onDraw(Canvas canvas){
      if (cameraManager == null) {
          return; // not ready yet, early draw before done configuring
      }
      Rect frame = cameraManager.getFramingRect();
      Rect previewFrame = cameraManager.getFramingRectInPreview();
      if (frame == null || previewFrame == null) {
          return;
      }
      int width = canvas.getWidth();
      int height = canvas.getHeight();
      //绘制背景
      paint.setColor(resultBitmap != null ? resultColor : maskColor);
      paint.setStyle(Paint.Style.FILL);
      canvas.drawRect(0, 0, width, height,paint);

      float rx=20,ry=20;
      //清空扫码区域
      paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
      canvas.drawRoundRect(new RectF(frame.left,frame.top,frame.right,frame.bottom),rx,ry,paint);
      paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

      //绘制四个角所在区域为一个圆角矩形
      paint.setColor(hornColor);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(10);// 设置“空心”的外框的宽度
      canvas.drawRoundRect(new RectF(frame.left,frame.top,frame.right,frame.bottom),rx,ry,paint);
      paint.setStyle(Paint.Style.FILL);
      //清除多余边框
      paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
      canvas.drawRect(frame.left+60,frame.top-5,frame.right-60,frame.bottom+5,paint);
      canvas.drawRect(frame.left-5,frame.top+60,frame.right+5,frame.bottom-60,paint);
      //绘制四角直线圆角头
      paint.setStyle(Paint.Style.FILL);
      paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
      //左上
      canvas.drawRoundRect(new RectF(frame.left+50, frame.top-5, frame.left + 70, frame.top+5),rx,ry, paint);//-
      canvas.drawRoundRect(new RectF(frame.left-5, frame.top+50, frame.left+5, frame.top + 70),rx,ry,  paint);//|
      //右上
      canvas.drawRoundRect(new RectF(frame.right - 70, frame.top-5, frame.right-50, frame.top+5),rx,ry, paint);//-
      canvas.drawRoundRect(new RectF(frame.right -5, frame.top + 50 , frame.right + 5, frame.top + 70),rx,ry, paint);//|
      //左下
      canvas.drawRoundRect(new RectF(frame.left + 50, frame.bottom - 5, frame.left + 70, frame.bottom + 5),rx,ry, paint);//-
      canvas.drawRoundRect(new RectF(frame.left - 5, frame.bottom - 70, frame.left + 5, frame.bottom - 50),rx,ry, paint);//|
      //右下
      canvas.drawRoundRect(new RectF(frame.right - 70, frame.bottom - 5, frame.right - 50, frame.bottom + 5),rx,ry, paint);//-
      canvas.drawRoundRect(new RectF(frame.right - 5, frame.bottom - 70 , frame.right + 5, frame.bottom - 50),rx,ry, paint);//|
      //绘制白色边线
      paint.setColor(borderColor);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(4);// 设置“空心”的外框的宽度
      paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
      canvas.drawRoundRect(new RectF(frame.left,frame.top,frame.right,frame.bottom),rx,ry,paint);
      paint.setStyle(Paint.Style.FILL);
      //填充边线空白部分
      paint.setColor(resultBitmap != null ? resultColor : maskColor);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(6);// 设置“空心”的外框的宽度
      paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
      canvas.drawRoundRect(new RectF(frame.left-3,frame.top-3,frame.right+2,frame.bottom+2),rx,ry,paint);

      //绘制扫描线
      int middle = frame.height() / 2 + frame.top;
      int laserLinePosition = middle;
      laserLinePosition = laserLinePosition + 5;
      if (laserLinePosition > frame.height()) {
          laserLinePosition = 0;
      }
      LinearGradient linearGradient = new LinearGradient(frame.left + 1, frame.top + laserLinePosition, frame.right - 1, frame.top + 10 + laserLinePosition, new int[]{
              scanLineStart,
              scanLineMiddle,
              scanLineEnd
      }, new float[]{
              0f,0.5f,1f
      }, Shader.TileMode.CLAMP);

      paint.setShader(linearGradient);
      paint.setStyle(Paint.Style.FILL);
      paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
      canvas.drawRect(frame.left + 1, middle-3, frame.right - 1, middle+4, paint);
      paint.setShader(null);
      float scaleX = frame.width() / (float)previewFrame.width();
      float scaleY = frame.height() / (float)previewFrame.height();
      List<ResultPoint> currentPossible = possibleResultPoints;
      List<ResultPoint> currentLast = lastPossibleResultPoints;
      int frameLeft = frame.left;
      int frameTop = frame.top;
      if (currentPossible.isEmpty()) {
          lastPossibleResultPoints = null;

      } else {
          possibleResultPoints = new ArrayList <> (5);
          lastPossibleResultPoints = currentPossible;
          paint.setAlpha(CURRENT_POINT_OPACITY);
          paint.setColor(resultPointColor);
          for (ResultPoint point: currentPossible) {
              canvas.drawCircle(frameLeft + (int)(point.getX() * scaleX),
                      frameTop + (int)(point.getY() * scaleY),
                      POINT_SIZE, paint);
          }
      }
      if (currentLast != null) {
          paint.setAlpha(CURRENT_POINT_OPACITY / 2);
          paint.setColor(resultPointColor);
          float radius = POINT_SIZE / 2.0f;
          for (ResultPoint point: currentLast) {
              canvas.drawCircle(frameLeft + (int)(point.getX() * scaleX),
                      frameTop + (int)(point.getY() * scaleY),
                      radius, paint);
          }
      }

      postInvalidateDelayed(16,
              frame.left,
              frame.top,
              frame.right,
              frame.bottom);
  }

  public void drawViewfinder() {
    Bitmap resultBitmap = this.resultBitmap;
    this.resultBitmap = null;
    if (resultBitmap != null) {
      resultBitmap.recycle();
    }
    invalidate();
  }

  /**
   * Draw a bitmap with the result points highlighted instead of the live scanning display.
   *
   * @param barcode An image of the decoded barcode.
   */
  public void drawResultBitmap(Bitmap barcode) {
    resultBitmap = barcode;
    invalidate();
  }

  public void addPossibleResultPoint(ResultPoint point) {
    List<ResultPoint> points = possibleResultPoints;
    synchronized (points) {
      points.add(point);
      int size = points.size();
      if (size > MAX_RESULT_POINTS) {
        // trim it
        points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
      }
    }
  }

}
