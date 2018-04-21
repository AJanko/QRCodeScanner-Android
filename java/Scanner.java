package com.example.artur.qrcodeapp;

import android.app.Activity;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Point;
import org.opencv.utils.Converters;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public class Scanner extends AppCompatActivity {


    private final static String TAG = "tag";
    private Context context;
    private Button proccessBtn;
    private Bitmap bitmap = null;
    private ImageView imgView;
    private TextView scannerTxtView;
    private ImageButton imgClose;
    private Mat binaryMat,grayscaleMat;
    private int bitmapWidth,bitmapHeight;
    private Point[] qrCodeVertices;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        proccessBtn = (Button) this.findViewById(R.id.button2);
        imgView = (ImageView) this.findViewById(R.id.imageView);
        scannerTxtView = (TextView) this.findViewById(R.id.textViewQRCodeDoNotFind);
        imgClose = (ImageButton) this.findViewById(R.id.imgClose);

        bitmap = (Bitmap) getIntent().getParcelableExtra("photo");
        imgView.setImageBitmap(bitmap);

        bitmapWidth = bitmap.getWidth();
        bitmapHeight = bitmap.getHeight();
    }


    public void onClickImageProccess(View view) {
		
        Image imageToProccess = new Image(bitmap);          //przekazanie zdjecia do klasy binaryzujacej bitmape
        QrCodeDetector qrCode = new QrCodeDetector(imageToProccess.getOriginalMat(),imageToProccess.getBinaryMat(), imageToProccess.getGrayscaleMat(), bitmapHeight, bitmapWidth);
        imgView.setImageBitmap(qrCode.GetContourBitmap());
        qrCodeVertices = new Point[4];
        qrCodeVertices = qrCode.GetQrCode();            //Punkty sa w losowej kolejnosci

        proccessBtn.setVisibility(View.INVISIBLE);

        if(qrCodeVertices==null) {
            scannerTxtView.setVisibility(View.VISIBLE);
        }

        else{
            RotateMat rotateMat = new RotateMat(imageToProccess.getBinaryMat(),qrCodeVertices);
            imgView.setImageBitmap(rotateMat.getQrCodeBitmap());

            QrCodeDecoder qrCodeDecoder = new QrCodeDecoder(rotateMat.getQrCodeMat(),qrCode.getFinderPatternLength());
            imgView.setImageBitmap(qrCodeDecoder.GetQrCodeBitmap());
        }
    }

	
    public void onClickImageClose(View view) {
        finish();
    }


    private String saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,"profile.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }



}