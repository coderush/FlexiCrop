/**
Copyright (c) 2013 Sumit Ranjan

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
**/
package com.project.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import android.net.Uri;

import android.os.Bundle;
import android.os.Environment;

import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

/**
@author : Sumit Ranjan <sumit.nitt@gmail.com>
@description : Main Activity
**/

public class MainActivity extends Activity implements OnTouchListener {
	private Uri mImageCaptureUri;
	private ImageView drawImageView;
	private Bitmap sourceBitmap;
	private Path clipPath;

	private static final int PICK_FROM_FILE = 1;
	private static final int CAMERA_CAPTURE = 2;
	
	Bitmap bmp;
	Bitmap alteredBitmap;
	Canvas canvas;
	Paint paint;
	Matrix matrix;
	float downx = 0;
	float downy = 0;
	float upx = 0;
	float upy = 0;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);     
        setContentView(R.layout.main);
        final String [] items			= new String [] {"Take from camera", "Select from gallery"};				
		ArrayAdapter<String> adapter	= new ArrayAdapter<String> (this, android.R.layout.select_dialog_item,items);
		AlertDialog.Builder builder		= new AlertDialog.Builder(this);
		
		builder.setTitle("Select Image");
		builder.setAdapter( adapter, new DialogInterface.OnClickListener() {
			public void onClick( DialogInterface dialog, int item ) { //pick from camera
				if (item == 0) {
					try {
					    //use standard intent to capture an image
					    Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					    //we will handle the returned data in onActivityResult
					    startActivityForResult(captureIntent, CAMERA_CAPTURE);
					} catch(ActivityNotFoundException e){
					    e.printStackTrace();
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else { //pick from file
					Intent intent = new Intent();
	                intent.setType("image/*");
	                intent.setAction(Intent.ACTION_GET_CONTENT);
	                startActivityForResult(Intent.createChooser(intent, "Complete action using"), PICK_FROM_FILE);
				}
			}
		} );
		
		final AlertDialog dialog = builder.create();
		
		Button button 	= (Button) findViewById(R.id.btn_crop);
		Button saveButton 	= (Button) findViewById(R.id.btn_save);
		Button discardButton 	= (Button) findViewById(R.id.btn_discard);
		
		drawImageView = (ImageView) findViewById(R.id.DrawImageView);
		drawImageView.setOnTouchListener(this);
		
		button.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				dialog.show();
			}
		});
		
		saveButton.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				try {
					String path = Environment.getExternalStorageDirectory().toString();
					OutputStream fOut = null;
					File file = new File(path, String.valueOf(Math.round(Math.random()*100000))+".jpg");
					fOut = new FileOutputStream(file);

					alteredBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
					fOut.flush();
					fOut.close();

					MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
					AlertDialog.Builder builder		= new AlertDialog.Builder(MainActivity.this);
					builder.setMessage("Saved");
					AlertDialog dialog = builder.create();
					dialog.show();
					
					//Now deleting the temporary file created on camera capture
					File f = new File(mImageCaptureUri.getPath());            
			        if (f.exists()) {
			            f.delete();
			        }
					
				} catch (Exception e) {
				       e.printStackTrace();
				}
			}
		});
		
		discardButton.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
		        if (mImageCaptureUri != null) {
		        	//Now deleting the temporary file created on camera capture
					File f = new File(mImageCaptureUri.getPath());            
			        if (f.exists()) {
			            f.delete();
			        }
			        
                    getContentResolver().delete(mImageCaptureUri, null, null );
                    mImageCaptureUri = null;
                    //resetting the image view
                    ImageView iv = (ImageView) findViewById(R.id.DrawImageView);
                    iv.setImageDrawable(null);
                    
                    
		        }
			}
		});
    }
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (resultCode != RESULT_OK) return;
	    switch (requestCode) {
	    
		    case PICK_FROM_FILE: 
		    	mImageCaptureUri = data.getData();
		    	doCrop();
		    	break;
		    	
		    case CAMERA_CAPTURE:
		    	mImageCaptureUri = data.getData();
		    	doCrop();
		    	break;
	    }
	}
    
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        switch (action) {
        case MotionEvent.ACTION_DOWN:
          downx = event.getX();
          downy = event.getY();
          clipPath = new Path();
          clipPath.moveTo(downx, downy);
          break;
        case MotionEvent.ACTION_MOVE:
          upx = event.getX();
          upy = event.getY();
          canvas.drawLine(downx, downy, upx, upy, paint);
          clipPath.lineTo(upx, upy);
          drawImageView.invalidate();
          downx = upx;
          downy = upy;
          break;
        case MotionEvent.ACTION_UP:
          upx = event.getX();
          upy = event.getY();
          canvas.drawLine(downx, downy, upx, upy, paint);
          clipPath.lineTo(upx, upy);
          drawImageView.invalidate();
          cropImageByPath();
          break;
        case MotionEvent.ACTION_CANCEL:
          break;
        default:
          break;
        }
        return true;
    }
    
    private void cropImageByPath() {
    	//closing the path now.
    	clipPath.close();
    	//setting the fill type to inverse, so that the outer part of the selected path gets filled.
    	clipPath.setFillType(FillType.INVERSE_WINDING);
        Paint xferPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xferPaint.setColor(Color.BLACK);
        canvas.drawPath(clipPath, xferPaint);
        xferPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(alteredBitmap, 0, 0, xferPaint);
    }
    
    private void doCrop() {
		try {
			    sourceBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mImageCaptureUri);
				BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
			    bmpFactoryOptions.inJustDecodeBounds = true;
			    bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(
			    		mImageCaptureUri), null, bmpFactoryOptions);
			    bmpFactoryOptions.inJustDecodeBounds = false;
			    bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(
			    		mImageCaptureUri), null, bmpFactoryOptions);
			    alteredBitmap = Bitmap.createBitmap(bmp.getWidth(), bmp
			        .getHeight(), bmp.getConfig());
			    canvas = new Canvas(alteredBitmap);
			    paint = new Paint();
			    paint.setColor(Color.GREEN);
			    paint.setStrokeWidth(5);
			    matrix = new Matrix();
			    canvas.drawBitmap(bmp, matrix, paint);
			    //loading the image bitmap in image view
			    drawImageView.setImageBitmap(alteredBitmap);
			    //setting the touch listener
			    drawImageView.setOnTouchListener(this);
	    	} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
    }
    
}

