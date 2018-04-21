package com.example.artur.qrcodeapp;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Artur on 2017-05-05.
 */

public class QrCodeDetector {
    private Mat originalMat, binaryMat, grayscaleMat, blurMat, destMat, detectedEdgesMat, contourMat, grayContourMat;
    private int bitmapHeight, bitmapWidth;
    private int thresholdValue = 50;
    private int finderPatternLength;
    private String TAG = "tag";
    private Point[] qrCodeVertices;



    public QrCodeDetector(Mat originalMat,Mat binaryMat, Mat grayscaleMat, int height, int width)
    {
        this.bitmapHeight = height;
        this.bitmapWidth = width;
        this.binaryMat = new Mat();
        this.binaryMat = binaryMat;
        this.grayscaleMat = new Mat();
        this.grayscaleMat = grayscaleMat;
        this.originalMat = new Mat();
        this.originalMat = originalMat;
        blurMat = new Mat();
        //EdgeDetection();
        ContourDetection();

    }


    private void ContourDetection() {           //wziac wszystkie kontury wieksze niz minimalne i w ich srodku badac stosunek pixeli bialych i czarnych powinien byc 1:1:3:1:1
        contourMat = new Mat();
        originalMat.copyTo(contourMat);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();


        //Imgproc.Canny(grayContourMat, grayContourMat, 50, 150, 3,false);   //200 zamiast 300?
        Imgproc.findContours(binaryMat.clone(), contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);



        List<Point[]> finderPatterns = new ArrayList<Point[]>();    //lista do przechowywania koordynatow finder patternow
        Log.d(TAG,"ilosc konturow: "+contours.size());

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint2f mop2f = new MatOfPoint2f();
            Point points[] = new Point[4];
            contours.get(i).convertTo(mop2f,CvType.CV_32F);         //convert MatOfPoint to MatOfPoint2f
            RotatedRect rect = Imgproc.minAreaRect(mop2f);            //get the Points of rotatedrect
            rect.points(points);                                        //zapisanie punktow w tablicy
            //Log.d(TAG,points[0].x+" "+points[0].y);


            if (rect.boundingRect().height >10 && rect.boundingRect().width < contourMat.width()/2){
                for(int j = 0; j < 4; ++j){
                    Imgproc.line(contourMat, points[j], points[(j + 1) % 4], new Scalar(0, 0, 255)); //rysowanie linii
                }
                boolean boolResult = CheckIfFinderPattern(points);
                Log.d(TAG, "Czy znaleziono pattern? - " + boolResult);
                if(boolResult) {
                    String pointString = "";
                    for (int aa=0;aa<4;aa++) pointString+=points[aa]+"-";
                    Log.d(TAG,"Znalezione punkty: "+pointString);
                    finderPatterns.add(points);
                }
            }
        }
        Log.d(TAG,"Znaleziono "+finderPatterns.size()+" finder patternów");
        String message="Pierwsze:\n";//this and for ponizej
        /*for(int i=0;i<finderPatterns.size();i++)
            for(int j=0;j<4;j++)
                message+="P"+i+j+"("+(int)finderPatterns.get(i)[j].x+","+(int)finderPatterns.get(i)[j].y+")\n";
        Log.d(TAG,message);*/

        if (finderPatterns.size()==3) {
            finderPatternLength=CalculateFinderPatternLength(finderPatterns);

            qrCodeVertices = new Point[4];
            qrCodeVertices = GetQrCodeVertices(finderPatterns);     //skopiowanie tablicy wierzcholkow qrkodu
        }


    }

    private int CalculateFinderPatternLength(List<Point[]> listOfPoints){
        int maxL=0,length;
        for(int i=0;i<listOfPoints.size();i++){
            length=CalculateLineLength(listOfPoints.get(i)[0],listOfPoints.get(i)[1]);
            if(length>maxL) maxL=length;
        }
        return maxL;
    }



    public Point[] GetQrCode(){
        return qrCodeVertices;
    }

    private Point[] GetQrCodeVertices(List<Point[]> finderPatterns){
        /*String message="Pierwsze:\n";//this and for ponizej
        for(int i=0;i<finderPatterns.size();i++)
            for(int j=0;j<4;j++)
                message+="P"+i+j+"("+(int)finderPatterns.get(i)[j].x+","+(int)finderPatterns.get(i)[j].y+")\n";
        Log.d(TAG,message);*/

        Point[] qrKod = new Point[4];               //tablica do przechowania koncowych wierzcholkow qr kodu
        Point[] finder1,finder2,finder3;
        finder1 = new Point[4];
        finder2 = new Point[4];
        finder3 = new Point[4];
        for(int i=0;i<4;i++){
            finder1[i] = new Point(finderPatterns.get(0)[i].x,finderPatterns.get(0)[i].y);
            finder2[i] = new Point(finderPatterns.get(1)[i].x,finderPatterns.get(1)[i].y);
            finder3[i] = new Point(finderPatterns.get(2)[i].x,finderPatterns.get(2)[i].y);
        }


        int[] distance = new int[3];                        //1-2, 2-3, 1-3
        distance[0] = CalculateLineLength(finder1[0],finder2[0]);
        distance[1] = CalculateLineLength(finder2[0],finder3[0]);
        distance[2] = CalculateLineLength(finder1[0],finder3[0]);



        Log.d(TAG,"P1("+(int)finder1[0].x+","+(int)finder1[0].y+")-P2("+(int)finder2[0].x+","+(int)finder2[0].y+") distance: "+distance[0]+">"+distance[1]+">"+distance[2]);

        int iMax=0;
        for(int i=0; i<3 ;i++){
            if(distance[i]>distance[iMax]) iMax=i;
        }

        int middleRect=0;                         //indeks finder Patternu, ktory nie ma brata po przekatnej

        switch(iMax){
            case 0: middleRect=2;    break;       //3 jest lewym gornym
            case 1: middleRect=0;    break;       //1 jest lewym gornym
            case 2: middleRect=1;    break;       //2 jest lewym gornym
        }

        Point[] maxTriangle= new Point[3];              //szukajac trojkata z najwiekszym polem znajdziemy najbardziej zewnetrzne punkty
        Point p1,p2,p3;
        double maxField=0,tempField=0,p1p2,p1p3,p2p3,angle;

        for(int i=0;i<4;i++)
            for(int j=0;j<4;j++)
                for(int k=0;k<4;k++){
                    p1=finder1[i];
                    p2=finder2[j];
                    p3=finder3[k];

                    p1p2=CalculateLineLength(p1,p2);
                    p1p3=CalculateLineLength(p1,p3);
                    p2p3=CalculateLineLength(p2,p3);

                    if(p1p2==0 || p1p3==0 || p2p3==0) continue;


                    angle=Math.acos((Math.pow(p1p2,2)+Math.pow(p1p3,2)-Math.pow(p2p3,2))/(2*p1p2*p1p3));
                    tempField=0.5*p1p2*p1p3*Math.sin(angle);
                    //Log.d(TAG,"p1: "+p1+" p2: "+p2+" p3: "+p3+"p1p2: "+p1p2+" p1p3: "+p1p3+" angle: "+angle+" Math.sin: "+Math.sin(angle)+" tempField: "+tempField);

                    if(tempField>maxField){
                        maxField=tempField;
                        maxTriangle[0]=p1;
                        maxTriangle[1]=p2;
                        maxTriangle[2]=p3;
                    }
                }

        //maxTriangle[] - 3 punkty qrkodu, znalezc 4 nalezy
        int x,y;            //koordynaty 4 punktu
        //Log.d(TAG,"("+maxTriangle[0].x+","+maxTriangle[0].y+")"+"("+maxTriangle[1].x+","+maxTriangle[1].y+")"+"("+maxTriangle[2].x+","+maxTriangle[2].y+")");
        p1 = null;
        p2 = null;
        p3 = null;

        for(int i=0;i<3;i++){
            if(i==middleRect){
                p1 = maxTriangle[middleRect];
                Log.d(TAG,"p1 initialize");
            }

            else if (p2 == null){
                 p2 = maxTriangle[i];
                Log.d(TAG,"p2 initialize");
            }
                else{
                p3 = maxTriangle[i];
                Log.d(TAG,"p3 initialize");
            }
        }

        //determinants
        double line1,line2;
        Point[] D1,D2;
        D1 = new Point[2];
        D2 = new Point[2];
        D1[0] = p2;
        D2[0] = p3;
        D1[1] = null;
        D2[1] = null;

        for(int i=0;i<4;i++){
            if( D1[0].x==finder1[i].x &&  D1[0].y==finder1[i].y)
                D1[1]=findPointForDeterminantsCalculating(finder1,p2,p1);
            else if( D1[0].x==finder2[i].x &&  D1[0].y==finder2[i].y)
                D1[1]=findPointForDeterminantsCalculating(finder2,p2,p1);
            else if( D1[0].x==finder3[i].x &&  D1[0].y==finder3[i].y)
                D1[1]=findPointForDeterminantsCalculating(finder3,p2,p1);

            if( D2[0].x==finder1[i].x &&  D2[0].y==finder1[i].y)
                D2[1]=findPointForDeterminantsCalculating(finder1,p3,p1);
            else if( D2[0].x==finder2[i].x &&  D2[0].y==finder2[i].y)
                D2[1]=findPointForDeterminantsCalculating(finder2,p3,p1);
            else if( D2[0].x==finder3[i].x &&  D2[0].y==finder3[i].y)
                D2[1]=findPointForDeterminantsCalculating(finder3,p3,p1);
        }

        //determinants calculating
        double x1,x2,x3,x4,y1,y2,y3,y4;
        x1=D1[0].x; x2=D1[1].x; x3=D2[0].x; x4=D2[1].x;
        y1=D1[0].y; y2=D1[1].y; y3=D2[0].y; y4=D2[1].y;
        x=roundNumber(((((x1*y2)-(y1*x2))*(x3-x4))-((x1-x2)*((x3*y4)-(y3*x4))))/
                (((x1-x2)*(y3-y4))-((y1-y2)*(x3-x4))));
        y=roundNumber(((((x1*y2)-(y1*x2))*(y3-y4))-((y1-y2)*((x3*y4)-(y3*x4))))/
                (((x1-x2)*(y3-y4))-((y1-y2)*(x3-x4))));



        /*if (p3.x>p1.x && p2.x>p1.x)
            x = roundNumber((p2.x + Math.abs(p3.x-p1.x)));
        else if (p2.x>p3.x)
            x = roundNumber(p2.x - Math.abs(p3.x-p1.x));
        else
            x = roundNumber(p3.x - Math.abs(p2.x-p1.x));

        if(p3.y>p1.y && p2.y>p1.y)
            y = roundNumber(p2.y + Math.abs(p3.y-p1.y));
        else if(p2.y>p3.y)
            y = roundNumber(p2.y - Math.abs(p3.y-p1.y));
        else
            y = roundNumber(p3.y - Math.abs(p2.y-p1.y));*/


        qrKod[0] = p1;
        if(p2.x>p3.x){
            qrKod[1] = p2;
            qrKod[2] = p3;
        }
        else {
            qrKod[1] = p3;
            qrKod[2] = p2;
        }
        qrKod[3] = new Point(x,y);
        Log.d(TAG,"qrcodedetector end code");
        return qrKod;
    }

    private int roundNumber(double number){
        if(number-(int)number>=0.5)
            return (int)number+1;
        else return (int)number;
    }

    //zastanow sie czy nie sprawdzac finder pattenu pionowo i poziomo( zeby jedno bylo spelnione) na wypadek gdyby jakies pixele uciekly
    private boolean CheckIfFinderPattern(Point[] points)     //sprawdza czy kwadrat to finder pattern, pamietaj aby rzutowac na int obliczone koordynaty
    {
        Log.d(TAG,"CheckIFFinder start"+points[0].y);
        Point pointA,pointB;

        List<Point> listOfPoints= new ArrayList<Point>();
        List<Point> listOfPointsFirst= new ArrayList<Point>();
        List<Point> listOfPointsSecond= new ArrayList<Point>();

        bhm_line((int)points[0].x,(int)points[0].y,(int)points[1].x,(int)points[1].y,listOfPointsFirst);
        bhm_line((int)points[2].x,(int)points[2].y,(int)points[3].x,(int)points[3].y,listOfPointsSecond);

        pointA = listOfPointsFirst.get((int)listOfPointsFirst.size()/2);
        pointB = listOfPointsSecond.get((int)listOfPointsSecond.size()/2);

        if (pointA.x > pointB.x){
            Point temp = pointA.clone();
            pointA = pointB.clone();
            pointB = temp.clone();
        }


        bhm_line((int)pointA.x,(int)pointA.y,(int)pointB.x,(int)pointB.y,listOfPoints);         //stworzenie listy punktow tworzacych linie pomiedzy 2 przekazanymi punktami


        /*String logi="";
        for (int ii=0;ii<40;ii++){                                                              //testowanie algorytmu bresenhama
            logi+="x: "+(long)listOfPoints.get(listOfPoints.size()-ii-1).x+" y: "+(long)listOfPoints.get(listOfPoints.size()-ii-1).y+"\n";
        }
        logi+=listOfPoints.size()+"="+lineLength;
            Log.d(TAG,logi);*/                                                                    //testowanie algorytmu bresenhama


        int currentState=0;
        int[] stateCount = new int[5];              //tablica do wyliczania liczby pikseli tego samego koloru

        byte[] pixelValuePrev, pixelValue;
        pixelValuePrev = new byte[binaryMat.channels()];
        pixelValue = new byte[binaryMat.channels()];
        Point tempPoint;
        int iter =0;

        while(iter<listOfPoints.size()){
            tempPoint = listOfPoints.get(iter).clone();
            binaryMat.get((int)tempPoint.y,(int)tempPoint.x,pixelValuePrev);                    //pobranie koloru pikselu Punktu tempPoint do tablicy byte[]
            if(pixelValuePrev[0]==0) break;
            else iter++;
        }

        stateCount[0]++;
        //Log.d(TAG,"tutaj2: "+pixelValuePrev[0]+"   "+pixelValuePrev.length);
        //napisac funkcje bioraca ta liste punktow i na macierzy binarnej sprawdzajaca stan pikseli(czy pasuja do finder patternu)
        for (int j=iter; j< listOfPoints.size();j++){
            if (currentState>4){
                Log.d(TAG, "Something went wrong :( there is too much pixels colors on your line");
                break;
                //return false;
            }
            tempPoint = listOfPoints.get(j).clone();
            binaryMat.get((int)tempPoint.y,(int)tempPoint.x,pixelValue);

            //if(pixelValue[0]==pixelValuePrev[0]){
            if((pixelValue[0]<0 && pixelValuePrev[0]<0) || (pixelValue[0]>-1 && pixelValuePrev[0]>-1)){
                stateCount[currentState]++;
            }
            else{
                if(currentState==4) break;
                stateCount[currentState+1]++;
                currentState++;
            }

            pixelValuePrev[0] = pixelValue[0];
        }

        Log.d(TAG,"wartość: "+stateCount[0]+" | "+stateCount[1]+" | "+stateCount[2]+" | "+stateCount[3]+" | "+stateCount[4]);

        int stateCountSumUp=stateCount[0]+stateCount[1]+stateCount[2]+stateCount[3]+stateCount[4];

        if (stateCountSumUp>7 && Math.abs(stateCount[0]-stateCount[1])<2 && Math.abs(stateCount[3]-stateCount[4])<2 && Math.abs(stateCount[0]-stateCount[3])<2 && (Math.abs(stateCount[2]-3*stateCount[1])<3 || Math.abs(stateCount[2]-3*stateCount[3])<3)){
            return true;
        }
        else return false;
    }


    private void EdgeDetection()                //chyba sie nie przyda
    {
        Imgproc.blur(grayscaleMat, blurMat, new Size(3, 3));                                    //blurowanie macierzy
        detectedEdgesMat = new Mat(bitmapHeight,bitmapWidth,CvType.CV_8UC1);
        Imgproc.Canny(blurMat, detectedEdgesMat, thresholdValue, thresholdValue * 3, 3, false);          //szukanie krawedzi
        destMat = new Mat(bitmapHeight, bitmapWidth, CvType.CV_8UC4);
        Core.add(destMat, Scalar.all(255), destMat);                                                      //wypelnienie macierzy na czarno
        originalMat.copyTo(destMat, detectedEdgesMat);                                                   // nalozenie krawedzi z obrazu zrodlowego na obraz destMat
    }



    public Bitmap GetContourBitmap()
    {
        Bitmap contourBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(contourMat,contourBitmap);
        return contourBitmap;
    }


    //P1 is the angle which is calculated (|<P2P1P3|), return -1 if error
    private double calculateAngleFromPoints(Point p1, Point p2, Point p3){
        double angle,p1p2,p1p3,p2p3;
        p1p2=CalculateLineLength(p1,p2);
        p1p3=CalculateLineLength(p1,p3);
        p2p3=CalculateLineLength(p2,p3);

        if(p1p2!=0 && p1p3!=0 & p2p3!=0)
            angle = Math.acos((Math.pow(p1p2,2)+Math.pow(p1p3,2)-Math.pow(p2p3,2))/(2*p1p2*p1p3));
        else
            angle = -1;
        return angle;
    }


    //function for some calculations, p1 angle is calculated
    private Point findPointForDeterminantsCalculating(Point[] points, Point p1, Point p2){
        //p2.x==finder1[i].x && p2.y==finder1[i].y
        Log.d("TAG","determinantsPoint");
        Point finalPoint=null;
        for(int i=0;i<4;i++) {
            if (points[i].x==p1.x && points[i].y==p1.y) continue;
            double angle=calculateAngleFromPoints(p1,points[i],p2);
            Log.d("TAG","angle: "+angle);
            if (Math.abs(angle-(Math.PI/2))<Math.PI/36) finalPoint=points[i];
        }
        return finalPoint;
    }



    public int getFinderPatternLength(){
        return finderPatternLength;
    }


    public Bitmap GetEdgesBitmap()
    {
        Bitmap edgeBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(destMat,edgeBitmap);
        return edgeBitmap;
    }


    //Bresenham algorithm implementation
    private void bhm_line(int x1, int y1, int x2, int y2, List<Point> list){
        int kx,ky,dx,dy,e;
        if(x1<x2) kx = 1;
        else kx=-1;
        if(y1<y2) ky = 1;
        else ky = -1;

        dx = Math.abs(x2-x1);
        dy = Math.abs(y2-y1);
        list.add(new Point(x1,y1));

        if(!(dx<dy)) {
            e = dx/2;
            for(int i =0; i<dx; i++){
                x1=x1+kx;
                e=e-dy;
                if(e<0){
                    y1=y1+ky;
                    e=e+dx;
                }
                list.add(new Point(x1,y1));
            }
        }
        else{
            e=dy/2;
            for(int i=0; i<dy;i++){
                y1=y1+ky;
                e=e-dx;
                if(e<0){
                    x1=x1+kx;
                    e=e+dy;
                }
                list.add(new Point(x1,y1));
            }
        }
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
