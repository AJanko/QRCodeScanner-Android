package com.example.artur.qrcodeapp;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;



public class Image {

    private Bitmap original;
    private Bitmap grayscaleBitmap;
    private Bitmap binaryBitmap;
    private Mat originalMat;
    private Mat grayscaleMat;
    private Mat binaryMat;
    private int bitmapWidth,bitmapHeight;

    private final static String TAG = "tag";



    public Image(Bitmap b)
    {
        original = b.copy(Bitmap.Config.ARGB_8888, true);
        bitmapHeight = original.getHeight();
        bitmapWidth = original.getWidth();


        originalMat = new Mat();
        grayscaleMat = new Mat();  //or 8uc1?
        //grayscaleMat = correctPerspective(grayscaleMat);
        binaryMat = new Mat();
        Utils.bitmapToMat(original, originalMat);

        //Imgproc.blur(originalMat, grayscaleMat, new Size(1, 1));      //!!gdybys usuwal zmien nizej grayscaleMat na originalMat
        Imgproc.cvtColor(originalMat, grayscaleMat, Imgproc.COLOR_BGR2GRAY); //skala szarosci
        //Imgproc.equalizeHist(grayscaleMat,grayscaleMat);
        //Imgproc.thre shold(grayscaleMat,binaryMat,200, 255, Imgproc.THRESH_BINARY);      //progowanie(binaryzacja), testowe
        Imgproc.threshold(grayscaleMat,binaryMat,0, 255, Imgproc.THRESH_OTSU | Imgproc.THRESH_BINARY);    //moze lepsze
        //Imgproc.medianBlur(binaryMat,binaryMat,3);
    }


    public Bitmap getOriginal() {
        return original;
    }

    public Bitmap getGrayscaleBitmap() {
        grayscaleBitmap = Bitmap.createBitmap(bitmapWidth,bitmapHeight, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(grayscaleMat,grayscaleBitmap);
        return grayscaleBitmap;
    }

    public Bitmap getBinaryBitmap() {
        binaryBitmap = Bitmap.createBitmap(bitmapWidth,bitmapHeight, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(binaryMat,binaryBitmap);
        return binaryBitmap;
    }

    public Mat getOriginalMat() {
        return originalMat;
    }

    public Mat getGrayscaleMat() {
        return grayscaleMat;
    }

    public Mat getBinaryMat() {
        return binaryMat;
    }

    }




