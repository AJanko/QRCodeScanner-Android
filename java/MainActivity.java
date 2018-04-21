package com.example.artur.qrcodeapp;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity {


    private final static String TAG = "tag";
    private static final int CAMERA_REQUEST = 1888;
    TextView startText;
    Button startBtn;
    Context context;

    static {
        System.loadLibrary("opencv_java3");
    }         //ładowanie biblioteki

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        startText = (TextView) findViewById(R.id.startTextView);
        startBtn = (Button) findViewById(R.id.startBtn);
    }

    //Original one
    public void onClickStartButton(View view) {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            Intent intent = new Intent(context, Scanner.class);
            intent.putExtra("photo",photo);
            startActivity(intent);
        }
    }

    //Test one
    /*public void onClickStartButton(View view) {

        Bitmap temp = loadImageFromStorage();
        Bitmap photo = temp.copy(Bitmap.Config.ARGB_8888,true);
        Intent intent = new Intent(context, Scanner.class);
        intent.putExtra("photo",photo);
        startActivity(intent);
    }*/

    private Bitmap loadImageFromStorage()
    {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        String path = directory.getAbsolutePath();
        Bitmap b = null;

        try {
            File f=new File(path, "profile.jpg");
            b = BitmapFactory.decodeStream(new FileInputStream(f));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        return b;
    }
}