package com.example.artur.qrcodeapp;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class QrCodeDecoder {
	
    Mat qrCodeMat,qrCodeMatFromBlockArray;
    int blockSize,finderPatternLength,height,width,blockArrayWidth,blockArrayHeight;
    int[][] qrCodeArray,blockArray;

	
    public QrCodeDecoder(Mat qrCodeMat, int finderPatternLength){
        this.qrCodeMat = qrCodeMat;
        width = qrCodeMat.cols();
        height = qrCodeMat.rows();
        this.finderPatternLength = finderPatternLength;
        matToArray();
        arrayToBlockArray();
        blockArrayToMat();
        String msg="";
        for(int i=0;i<blockArrayWidth;i++){
            for(int j=0;j<blockArrayHeight;j++)
                msg+=blockArray[i][j]+" ";
            msg+="\n";
        }
        Log.d("tag",msg);
        int qrKodSize = blockArrayHeight;
        decodeMessage(blockArray, qrKodSize);
    }

	
    private void decodeMessage(int[][] qrArray, int qrSize){        //operowanie na lokalnej tablicy nie globalnej
        int maskPattern, errorCorrectionLevel, formatInformation;   //maska, level korekcji bledow, informacja o formacie(byte, alfanumeric, numeric, kanji)
    }

	
    private int[][] doMasking(int[][] qrArray, int qrSize, int maskPattern){

        if(maskPattern==0){
            for(int i=0;i<qrSize;i++)
                for(int j=0;j<qrSize;j++)
                    if((i+j)%2==0)
                        if(qrArray[i][j]==0) qrArray[i][j]=1;
                        else qrArray[i][j]= 0;
        }
        else if(maskPattern==1){
            for(int i=0;i<qrSize;i++)
                for(int j=0;j<qrSize;j++)
                    if(i%2==0)
                        if(qrArray[i][j]==0) qrArray[i][j]=1;
                        else qrArray[i][j]= 0;
        }
        else if(maskPattern==2){
            for(int i=0;i<qrSize;i++)
                for(int j=0;j<qrSize;j++)
                    if(j%3==0)
                        if(qrArray[i][j]==0) qrArray[i][j]=1;
                        else qrArray[i][j]= 0;
        }
        else if(maskPattern==3){
            for(int i=0;i<qrSize;i++)
                for(int j=0;j<qrSize;j++)
                    if((i+j)%3==0)
                        if(qrArray[i][j]==0) qrArray[i][j]=1;
                        else qrArray[i][j]= 0;
        }
        else if(maskPattern==4){
            for(int i=0;i<qrSize;i++)
                for(int j=0;j<qrSize;j++)
                    if(((int)(i/2)+(int)(j/3))%2==0)
                        if(qrArray[i][j]==0) qrArray[i][j]=1;
                        else qrArray[i][j]= 0;
        }
        else if(maskPattern==5){
            for(int i=0;i<qrSize;i++)
                for(int j=0;j<qrSize;j++)
                    if(((i*j)%2)+((i*j)%3)==0)
                        if(qrArray[i][j]==0) qrArray[i][j]=1;
                        else qrArray[i][j]= 0;
        }
        else if(maskPattern==6){
            for(int i=0;i<qrSize;i++)
                for(int j=0;j<qrSize;j++)
                    if(((((i*j)%2)+((i*j)%3))%2)==0)
                        if(qrArray[i][j]==0)qrArray[i][j]=1;
                        else qrArray[i][j]= 0;
        }
        else if(maskPattern==7){
            for(int i=0;i<qrSize;i++)
                for(int j=0;j<qrSize;j++)
                    if(((((i+j)%2)+((i+j)%3))%2)==0)
                        if(qrArray[i][j]==0) qrArray[i][j]=1;
                        else qrArray[i][j]= 0;
        }
        return qrArray;
    }


    private void matToArray(){
        qrCodeArray = new int[height][width];
        for(int i=0;i<height;i++)
            for(int j=0;j<width;j++){
                byte[] pixelValue=new byte[qrCodeMat.channels()];
                qrCodeMat.get(i,j,pixelValue);
                qrCodeArray[i][j]=pixelValue[0];
            }
    }


    private void arrayToBlockArray(){   //sprawdza kolor jedynie srodkowych pikseli kazdego bloku
        blockSize=CalculateBlockSize(finderPatternLength,width,height);
        Log.d("tag","block: "+blockSize+"qrKODSize: "+blockArrayHeight);

        blockArray = new int[blockArrayHeight][blockArrayWidth];
        Log.d("size","blocksize: "+blockSize+" mat size: "+width+"/"+height+" qrSize: "+blockArrayHeight+"/"+blockArrayWidth);
        int white=0,black=0,itr,itc,skipPixelR=height-1,skipPixelC=width-1;

        for (int i=0;i<blockArrayHeight;i++){                           //rozkmin czy nie mylisz wierszy i kolumn, jesli qr kod bedzie za maly to nie zadziala
            for (int j=0;j<blockArrayWidth;j++){
                for(int k=1;k<blockSize-1;k++) {
                    for (int l = 1; l < blockSize-1; l++) {
                        itr=i*blockSize+k;
                        itc=j*blockSize+l;

                        if(itc>=width || itr>=height) continue;
                        if(blockSize>6 && (k==1 || l==1 || k==blockSize-2 || l==blockSize-2)) continue;    //pomija kolejne piksele przy sprawdzaniu koloru bloku, jesli blok jest wiekszy niz 6
                        if(qrCodeArray[itr][itc]<0) white++;
                        else black++;
                    }
                }
                Log.d("tag","white: "+white+"black: "+black+"\n");
                if(black<white) blockArray[i][j]=1;            //white
                else blockArray[i][j]=0;                        //black
                white=0;
                black=0;
            }
        }
        Log.d("tag","imageWidth: "+width+" imageHeight: "+height);
        Log.d("tag","width: "+blockArrayWidth+" height: "+blockArrayHeight);
        String msg="";
        for(int a=0;a<blockArrayWidth;a++){
            byte[] temp = new byte[qrCodeMat.channels()];
            qrCodeMat.get(a,a,temp);
            msg+=temp[0]+" ";
        }

        Log.d("tag",msg);

    }


    private void blockArrayToMat(){
        qrCodeMatFromBlockArray = new Mat(blockArrayHeight*blockSize,blockArrayHeight*blockSize, CvType.CV_8UC1);
        byte[] color = new byte[1];
        for (int i=0;i<blockArrayHeight;i++)                         //rozkmin czy nie mylisz wierszy i kolumn, jesli qr kod bedzie za maly to nie zadziala
            for (int j=0;j<blockArrayWidth;j++){
                if(blockArray[i][j]==0) color[0]=(byte)1;
                else color[0]=(byte)-1;

                for(int k=0;k<blockSize;k++)
                    for (int l = 0; l < blockSize; l++)
                        qrCodeMatFromBlockArray.put(i*blockSize+k,j*blockSize+l,color);
            }
    }


    public Bitmap GetQrCodeBitmap()
    {
        Bitmap qrCodeBitmap = Bitmap.createBitmap(blockArrayHeight*blockSize, blockArrayHeight*blockSize, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(qrCodeMatFromBlockArray,qrCodeBitmap);
        return qrCodeBitmap;
    }

    private int CalculateBlockSize(int finderLength,int width,int height){
        int blockSize = roundNumber((double)finderLength/7.0);  //+1 lub -1
        int qrSize = width/blockSize;
        if((width - qrSize*blockSize)>blockSize/2)
            blockSize++;
        if((qrSize*blockSize-width)>blockSize/2)
            blockSize--;
        qrSize=width/blockSize;
        if((qrSize-17)%4==0){}
        else if((qrSize-17)%4==1) qrSize--;
        else if((qrSize-17)%4==3) qrSize++;
        blockArrayWidth=qrSize;
        blockArrayHeight=qrSize;
        return blockSize;
    }

    private int roundNumber(double number){
        if(number-(int)number>=0.5)
            return (int)number+1;
        else return (int)number;
    }

}