package com.confused.glass.whatisthat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.view.WindowManager;
import com.confused.glass.whatisthat.GlassSnapshotActivity;
import com.confused.instarename.post.MultipartPost;
import com.confused.instarename.post.PostParameter;
import com.google.android.glass.app.Card;




/**
 * Main activity.
 */
public class WhatIsThatActivity extends Activity {
	
  private static Card _card;
  private static View _cardView;
  private static TextToSpeech _speech;

  private Context _context = this;
  
  private static final String TAG = WhatIsThatActivity.class.getSimpleName();
  private static final String IMAGE_FILE_NAME = Environment.getExternalStorageDirectory().getPath()+ "/ImageTest.jpg";
  private static final String IMAGE_FILE_NAME2 = Environment.getExternalStorageDirectory().getPath()+ "/SegmentedImageTest.jpg";
  private static final String IMAGE_FILE_NAME3 = Environment.getExternalStorageDirectory().getPath()+ "/subimage-";   
  private static final String TAIL = ".jpg";
  private boolean picTaken = false; // flag to indicate if we just returned from the picture taking intent
  protected boolean mbActive;

  /*
  public static void updateResultsInUi(Context context, String results, File image){
	  
	  Card card = new Card(context);
	  card.setImageLayout(Card.ImageLayout.FULL);
	  card.addImage( BitmapFactory.decodeFile(image.getAbsolutePath()));

	  if(results!=null){
		  card.setFootnote("Image Info: " + results);
		  _speech.speak("You are viewing " + results, TextToSpeech.QUEUE_FLUSH, null);
	  } else {
		  card.setFootnote("No results found.");
		  _speech.speak("No results found." , TextToSpeech.QUEUE_FLUSH, null);
	  }
	  
	  _cardView = card.getView();

	  
	  WhatIsThatActivity.this.runOnUiThread(new Runnable() {
		  	public void run(){
		  		setContentView(_cardView);
		  	}
	  });
	  
  }
  */
  

  private static String sendPostRequest(File filename) {
  	String response = null;
  	
  	Log.d("SendPostRequest", "sendPostRequest");
  	@SuppressWarnings("rawtypes")
		List<PostParameter> params = new ArrayList<PostParameter>();
  	params.add(new PostParameter<String>("image_url", ""));
  	params.add(new PostParameter<String>("btnG", "Search"));
  	params.add(new PostParameter<String>("image_content", ""));
  	params.add(new PostParameter<String>("filename", ""));
  	params.add(new PostParameter<String>("hl", "en"));
  	params.add(new PostParameter<String>("safe", "off"));
  	params.add(new PostParameter<String>("bih", ""));
  	params.add(new PostParameter<String>("biw", ""));
  	params.add(new PostParameter<File>("encoded_image", filename));
  	
  	try {
  		Log.d("INSTANT", "multipart post created");
  		MultipartPost post = new MultipartPost(params);
  		Log.d("INSTANT", "multipart post created");
  		response = post.send("http://www.google.com/searchbyimage/upload", "http://images.google.com");
    		
  	} catch (Exception e) {
  		Log.e("INSTANT", "Bad Post", e);
	}
  	
  	params.clear();
  	params = null;
  	return response;
  } 
  
  public static String getResult(File file){
		String tempresult = null;
		String result2 = sendPostRequest(file);
		boolean best_guess_found=false;

		StringTokenizer tok = new StringTokenizer(result2, "<>");
		String previous_entity=null;
		
		while(tok.hasMoreElements()){
			String nextitem = tok.nextElement().toString();
		
			if (best_guess_found==false && nextitem.startsWith("Best guess for this image")){
				Log.d("Tokenizer", nextitem);
				best_guess_found=true;
			} else if (best_guess_found==true && nextitem.contains("q=") && nextitem.contains("&amp")){
				int start = nextitem.indexOf("q=")+2;
				int end = nextitem.indexOf("&amp", start);
				String contents = previous_entity.substring( start , end);
				contents = contents.replace('+', ' ');
				Log.d("Result:", contents);
				
				tempresult = contents;
				break;
			} else if(nextitem.startsWith("Visually similar") && best_guess_found==false){
				Log.d("Tokenizer", "nextitem: " + nextitem + " previousitem: " + previous_entity);
				try{
					if(previous_entity.contains("q=") && previous_entity.contains("&amp")){
						int start = previous_entity.indexOf("q=")+2;
						int end = previous_entity.indexOf("&amp", start);
						String contents = previous_entity.substring( start , end);
						contents = contents.replace('+', ' ');
					
						Log.d("Result:", contents);
				
						tempresult = contents;
					} else {
						
						
					}
				} catch (Exception e){
					e.printStackTrace();						
				}
				break;
			}
			
			if(nextitem.startsWith("a")){
				StringTokenizer tok2 = new StringTokenizer(nextitem);
				
				while(tok2.hasMoreElements()){
					String subitem = tok2.nextElement().toString();
					if( subitem.startsWith("href") ){
						previous_entity=nextitem;
					}
				}
			}
		}
		
		return tempresult;
	}
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Even though the text-to-speech engine is only used in response to a menu action, we
    // initialize it when the application starts so that we avoid delays that could occur
    // if we waited until it was needed to start it up.
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    
    
    
    _speech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int status) {
        _speech.speak("Processing Image", TextToSpeech.QUEUE_FLUSH, null);
      }
    });
    

    _card = new Card(_context);
    _card.setFootnote("Processing Image");
    
    _cardView= _card.getView();
    _cardView.setKeepScreenOn(true);
    setContentView(_cardView);

    if (!OpenCVLoader.initDebug()) {
    	
    	finish();
        // Handle initialization error
    }
    
	if (!picTaken) {
		Intent intent = new Intent(this, GlassSnapshotActivity.class);
	      intent.putExtra("imageFileName",IMAGE_FILE_NAME);
	      intent.putExtra("previewWidth", 640);
	      intent.putExtra("previewHeight", 360);
	      intent.putExtra("snapshotWidth", 1280);
	      intent.putExtra("snapshotHeight", 720);
	      intent.putExtra("maximumWaitTimeForCamera", 2000);
	    startActivityForResult(intent,1);
	}
	else {
		// do nothing
	}
    

  }

  /**
   * Handle the tap event from the touchpad.
   */
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    switch (keyCode) {
    // Handle tap events.
    case KeyEvent.KEYCODE_DPAD_CENTER:
    case KeyEvent.KEYCODE_ENTER:

      // Change the text of the card when the touchpad is touched
      _cardView = _card.getView();
      setContentView(_cardView);

      // Status message below the main text in the alternative UX layout
      //_statusTextView.setText(R.string.touchpad_touched);

      //_speech.speak("Tap to Get Info", TextToSpeech.QUEUE_FLUSH, null);
      
      return true;
    default:
      finish();
      return super.onKeyDown(keyCode, event);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }
  
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	  super.onActivityResult(requestCode, resultCode, data);
	  picTaken = true;
	  switch(requestCode) {
	    case (1) : {
	      if (resultCode == Activity.RESULT_OK) {
	        // TODO Extract the data returned from the child Activity.
	    	  Log.v(TAG,"onActivityResult"); 
	    	  
	    	  File f = new File(IMAGE_FILE_NAME);
			   if (f.exists()) {
				   Log.v(TAG,"image file from camera was found");
		    	    
		    	    performOnBackgroundThreadSingleFile(_context, f);
		    		_card.setImageLayout(Card.ImageLayout.FULL);
		    		_card.addImage(BitmapFactory.decodeFile(f.getAbsolutePath()));
		    		_card.setFootnote("processing image");
		    		_cardView.invalidate();
		    		
		    		_cardView = _card.getView();
		    		setContentView(_cardView);
		    		
		    		Mat m = Highgui.imread(f.getAbsolutePath());
		    		Mat edges = new Mat();
		    		Mat dst_edges = new Mat();
		            Mat lines = new Mat();
		            Mat hierarchy = new Mat();
		            
		            Point a = new Point(0, 0);
		            
		            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		            
		            List<MatOfPoint> newcontours = new ArrayList<MatOfPoint>();
		            List<Rect> rects = new ArrayList<Rect>();
		            List<Point> center = new ArrayList<Point>();
		            List<Integer> radius = new ArrayList<Integer>();
		            
		            Size s = new Size(3,3);
		            Imgproc.cvtColor(m, edges, Imgproc.COLOR_BGR2GRAY);
		            Imgproc.blur(edges, dst_edges, s);
		            Imgproc.threshold(dst_edges, lines,  100,  255,  Imgproc.THRESH_BINARY );
		            Imgproc.findContours( lines, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, a );
		            
		            for(int i=0;i<contours.size();i++){
		                MatOfPoint2f mMOP2f1 = new MatOfPoint2f();
		                MatOfPoint2f mMOP2f2 = new MatOfPoint2f();
		                MatOfPoint rectpoint = new MatOfPoint();
		                float[] r = new float[] { 347.0f };
		                contours.get(i).convertTo(mMOP2f1, CvType.CV_32FC2);

		                Imgproc.approxPolyDP(mMOP2f1, mMOP2f2, (double)3, true); 
		                mMOP2f2.convertTo(rectpoint, CvType.CV_32S);
		                newcontours.add(rectpoint);
		                rects.add(Imgproc.boundingRect(rectpoint));
		                
		                Point ccenter = new Point();
		                Imgproc.minEnclosingCircle(mMOP2f2, ccenter, r);
		                radius.add((int)r[0]);
		                center.add(ccenter);
		                
		            }
		            
		            Mat segmented = m.clone();
		            
		            for( int i = 0; i< contours.size(); i++ ){ 
		            
						int minimum = 0;
						int maximum = 255;
						int r =  minimum + (int)(Math.random()*maximum); 
						int b =  minimum + (int)(Math.random()*maximum); 
						int g =  minimum + (int)(Math.random()*maximum); 
						Scalar color = new Scalar( r, b, g );
						Imgproc.drawContours(segmented, newcontours, i, color);
						Core.rectangle(segmented, rects.get(i).tl(), rects.get(i).br(), color, 3);
						Core.circle( segmented, center.get(i), radius.get(i).intValue(), color, 3);
		            
						if(radius.get(i).intValue() > 40){
							Mat temp = m.submat(rects.get(i));
						    Highgui.imwrite(IMAGE_FILE_NAME3 + i + TAIL, temp);
						    
						    File fss = new File(IMAGE_FILE_NAME3 + i + TAIL);
						    
						    performOnBackgroundThreadSingleFile(_context, fss);
						}
		            	
		            	
		            }
		           
		            Highgui.imwrite(IMAGE_FILE_NAME2, segmented);
		            
		            File fss = new File(IMAGE_FILE_NAME2);
		            
		            performOnBackgroundThreadSingleFile(_context, fss);
		            
		            
			   }
	      }
	      else {
	    	  Log.v(TAG,"onActivityResult returned bad result code");
	    	  finish();
	      }
	      break;
	    } 
	  }
	}
	
    public Thread performOnBackgroundThreadSingleFile(final Context context, final File f) {
        final Thread t = new Thread() {
            @Override
            public void run() {
            	String new_result="";
                try {
           			Log.d("Inside thread", "");
           			new_result = getResult(f);
	        		Log.d("Stuff", "Result is: " + new_result);                

                } finally {
            	  if(new_result!=null){
                  	  final Card card = new Card(context);
                	  card.setImageLayout(Card.ImageLayout.FULL);
                	  card.addImage( BitmapFactory.decodeFile(f.getAbsolutePath()));
            		  card.setFootnote("Image Info: " + new_result);
            		  _speech.speak("You are viewing " + new_result, TextToSpeech.QUEUE_FLUSH, null);
            		  WhatIsThatActivity.this.runOnUiThread(new Runnable() {
	              		  	public void run(){
	              		  		setContentView( card.getView());
	              		  	}
	              	  });
            	  }
                }
            }
        };
        t.start();
        return t;
    }
}
