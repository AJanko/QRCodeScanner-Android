package com.example.artur.qrcodeapp;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Artur on 2017-05-08.
 */

public class RotateMat {                            //do wyprostowania qrKodu gdy juz znalezione zostana wszystkie finder patterny

    private double angle;           //kat w radianach
    private Point[] points;
    private Mat binaryMatToRotate, binaryDestMat,transformed,finalMat;



    public RotateMat(Mat constructorMat, Point[] points){       //height=width bo qr kod jest kwadratem
        Log.d("TAG","RotateMat create");
        this.points = new Point[4];
        for(int i=0;i<4;i++)
            this.points[i]=points[i];

        binaryMatToRotate = new Mat();
        binaryMatToRotate = constructorMat;

        rotateMat();
    }


    private void rotateMat(){
        MatOfPoint matOfPoint= new MatOfPoint(points);
        //najpierw sortuj punkty wedlug tego ktory jest najbardzie po lewej stronie
        Point[] leftPoints = new Point[2];
        int min=0, secondMin=1;
        if(points[min].x>points[secondMin].x){
            min=1; secondMin=0;
        }

        for(int i=2; i<4; i++){
            if(points[i].x<=points[min].x){
                secondMin=min;
                min=i;
            }
            else if(points[i].x<=points[secondMin].x) secondMin=i;
        }
        //po znalezieniu punktów z lewej strony obliczamy dlugosc boku
        double matWidth= CalculateLineLength(points[min],points[secondMin]);

        Rect rectangle = Imgproc.boundingRect(matOfPoint);              //aby wyznaczyc punkt z ktorego zaczac wyznaczac pozycje qr kodu(lewy gorny rog)
        Point[] boundingRectPoints = new Point[4];                              //punkty przechowujace koordynaty boundingRectu
        boundingRectPoints[0] = new Point(rectangle.x,rectangle.y);
        boundingRectPoints[1] = new Point(rectangle.x+matWidth,rectangle.y);
        boundingRectPoints[2] = new Point(rectangle.x,rectangle.y+matWidth);
        boundingRectPoints[3] = new Point(rectangle.x+matWidth,rectangle.y+matWidth);


        MatOfPoint boundingRectMatOfPoint = new MatOfPoint(boundingRectPoints);             //tworzenie macierzy punktow z punktow

        Mat matFromMatOfPoint = new Mat();                                              //konwertowanie matofpoint to mat
        Mat matFromBoundingRectMatOfPoint = new Mat();
        matOfPoint.convertTo(matFromMatOfPoint, CvType.CV_32F);
        boundingRectMatOfPoint.convertTo(matFromBoundingRectMatOfPoint, CvType.CV_32F);

        Mat transmtx = Imgproc.getPerspectiveTransform(matFromMatOfPoint,matFromBoundingRectMatOfPoint);
        transformed = Mat.zeros(binaryMatToRotate.height(), binaryMatToRotate.width(), CvType.CV_8UC1);
        Imgproc.warpPerspective(binaryMatToRotate, transformed, transmtx, binaryMatToRotate.size());

        //copy from transformed to new Mat with transformed Mat size

        binaryDestMat = new Mat(transformed,rectangle);
        Log.d("tag",binaryDestMat.rows()+"="+rectangle.height+"? and "+binaryDestMat.cols()+"="+rectangle.width+"?");

        //some little correction to delete white pixels around qrcode
        byte[] temp = new byte[binaryDestMat.channels()];
        int delup=0,delleft=0,delbottom=0,delright=0,dell=0,delu=0,delr=0,delb=0;
        for (int a=0;a<5;a++){
            for(int b=0;b<20;b++){
                binaryDestMat.get(b,a,temp);
                if(temp[0]<0) dell++;
                binaryDestMat.get(a,b,temp);
                if(temp[0]<0) delu++;
                binaryDestMat.get(b,binaryDestMat.cols()-a-1,temp);
                if(temp[0]<0) delr++;
                binaryDestMat.get(binaryDestMat.rows()-a-1,b,temp);
                if(temp[0]<0) delb++;
            }
            //Log.d("dell",""+dell);
            //Log.d("delu",""+delu);
            //Log.d("delr",""+delr);
            //Log.d("delb",""+delb);
            if(dell>10) delleft++;
            if(delu>10) delup++;
            if(delr>10) delright++;
            if(delb>10) delbottom++;
            dell=0;delu=0;delr=0;delb=0;
        }
        Point[] correctRectanglePoints = new Point[4];
        correctRectanglePoints[0] = new Point(delleft,delup);
        correctRectanglePoints[1] = new Point(binaryDestMat.cols()-delright-1,delup);
        correctRectanglePoints[2] = new Point(binaryDestMat.cols()-delright-1,binaryDestMat.rows()-delbottom-1);
        correctRectanglePoints[3] = new Point(delleft,binaryDestMat.rows()-delbottom-1);

        MatOfPoint correctRectangleMatOfPoint = new MatOfPoint(correctRectanglePoints);
        Rect correctRectangle = Imgproc.boundingRect(correctRectangleMatOfPoint);

        finalMat = new Mat(binaryDestMat,correctRectangle);

    }



    public Bitmap getQrCodeBitmap(){
        Bitmap bitmap = Bitmap.createBitmap(finalMat.width(), finalMat.height(), Bitmap.Config.ARGB_8888);   //zmien wysokosc i szer
        Utils.matToBitmap(finalMat,bitmap);        //transformed
        return bitmap;
    }

    public Mat getQrCodeMat(){      //binaryDestMat
        return finalMat;
    }

    private int CalculateLineLength(Point pointA, Point pointB){
        double x1,x2,y1,y2,result;
        x1 = pointA.x;
        x2 = pointB.x;
        y1 = pointA.y;
        y2 = pointB.y;
        if((int)x1==(int)x2) result=Math.abs(y2-y1);
        else if ((int)y1==(int)y2) result=Math.abs(x2-x1);
        else result = Math.sqrt(Math.pow(x2-x1,2)+Math.pow(y2-y1,2));
        return (int)result;
    }

}
