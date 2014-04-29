package com.confused.glass.whatisthat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
import android.view.ViewGroup;
import android.view.WindowManager;
import com.confused.glass.whatisthat.GlassSnapshotActivity;
import com.confused.instarename.post.MultipartPost;
import com.confused.instarename.post.PostParameter;
import com.google.android.glass.app.Card;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;




/**
 * Main activity.
 */
public class WhatIsThatActivity extends Activity {
		

	  private static TextToSpeech _speech;
	
	  private List<Card> mCards;
	  private CardScrollView mCardScrollView;
	  private     ExampleCardScrollAdapter adapter;
	  
	  
	  private Context _context = this;
	  
	  private static final String TAG = WhatIsThatActivity.class.getSimpleName();
	  private static final String IMAGE_FILE_NAME = Environment.getExternalStorageDirectory().getPath()+ "/ImageTest.jpg";
	  private static final String IMAGE_FILE_NAME2 = Environment.getExternalStorageDirectory().getPath()+ "/SegmentedImageTest.jpg";
	  private static final String IMAGE_FILE_NAME3 = Environment.getExternalStorageDirectory().getPath()+ "/subimage-";   
	  private static final String TAIL = ".jpg";
	  private boolean picTaken = false; // flag to indicate if we just returned from the picture taking intent
	  protected boolean mbActive;
  
	  private class ExampleCardScrollAdapter extends CardScrollAdapter {
	
	      @Override
	      public int getPosition(Object item) {
	          return mCards.indexOf(item);
	      }
	
	      @Override
	      public int getCount() {
	          return mCards.size();
	      }
	
	      @Override
	      public Object getItem(int position) {
	          return mCards.get(position);
	      }
	
	      /**
	   * Returns the amount of view types.
	   */
	  @Override
	  public int getViewTypeCount() {
	      return Card.getViewTypeCount();
	  }
	
	  /**
	   * Returns the view type of this card so the system can figure out
	   * if it can be recycled.
	   */
	      @Override
	      public int getItemViewType(int position){
	          return mCards.get(position).getItemViewType();
	      }
	
	      @Override
	      public View getView(int position, View convertView,  ViewGroup parent) {
	          return  mCards.get(position).getView(convertView, parent);
	      }
	
	  }

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
			String best_guess = new String("Best guess for this image");
			String visually_similar = new String("Visually similar");
			String similar_pages = new String("Pages that include matching images");
			if(result2.contains(best_guess)){
				String subresult = result2.substring(result2.indexOf(best_guess )+best_guess.length());
				String subresult1 = subresult.substring(subresult.indexOf("<a")+2);				
				String subresult2 = subresult1.substring(subresult1.indexOf('>')+1);
				tempresult = subresult2.substring(0, subresult2.indexOf("</a>"));

				Log.d("Tokenizer from best match", tempresult);			
			} else if(result2.contains(visually_similar)){
				String subresult1 = result2.substring(0, result2.indexOf(visually_similar));
				String subresult2 = subresult1.substring(subresult1.lastIndexOf("href")+4);
				if(subresult2.contains("q=") && subresult2.contains("&amp")){
					String subresult3 = subresult2.substring(subresult2.indexOf("q=")+2);
					String subresult4 = subresult3.substring(0, subresult3.indexOf("&amp"));	
					tempresult = subresult4.replace('+', ' ');

					Log.d("Tokenizer from similar", tempresult);			
				}
			} else if(result2.contains(similar_pages)){
				String subresult = result2.substring(result2.indexOf(similar_pages )+similar_pages.length());
				String subresult1 = subresult.substring(subresult.indexOf("<a")+2);
				String subresult2 = subresult1.substring(subresult1.indexOf('>')+1);
				tempresult = subresult2.substring(0, subresult2.indexOf("</a>"));			

				Log.d("Tokenizer from matching pages", tempresult);			
			}

			
			return tempresult;
	}
	  
	  @Override
	  protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    // Even though the text-to-speech engine is only used in response to a menu action, we
	    // initialize it when the application starts so that we avoid delays that could occur
	    // if we waited until it was needed to start it up.
	    // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	    
	    
	    
	    _speech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
	      @Override
	      public void onInit(int status) {
	        _speech.speak("Processing Image", TextToSpeech.QUEUE_FLUSH, null);
	      }
	    });
    
	     mCards = new ArrayList<Card>();
	
	    mCardScrollView = new CardScrollView(this);
	    adapter = new ExampleCardScrollAdapter();
	    mCardScrollView.setAdapter(adapter);

	
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
			            List<Thread> threads = new ArrayList<Thread>();
			    	    
			    	    Card new_card = new Card(_context);
			    		new_card.setImageLayout(Card.ImageLayout.FULL);
			    		new_card.addImage(BitmapFactory.decodeFile(f.getAbsolutePath()));
			    		new_card.setFootnote("processing image");
			    		View temp_view = new_card.getView();
			    		temp_view.setKeepScreenOn(true);
			    	    setContentView(temp_view);

			    	    
			    	   threads.add( performOnBackgroundThreadSingleFile(_context, f));

			    	    
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
							    
							    threads.add(performOnBackgroundThreadSingleFile(_context, fss));
							}
			            	
			            	
			            }
			           
			            Highgui.imwrite(IMAGE_FILE_NAME2, segmented);
			            
			            File fss = new File(IMAGE_FILE_NAME2);
			            
			            threads.add(performOnBackgroundThreadSingleFile(_context, fss));
			            
			            for(int i=0; i<threads.size(); i++){
			            	try {
								threads.get(i).join();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
			            }
			            
			            _speech.speak("" + threads.size() + "inquiries were performed, swipe to view results", TextToSpeech.QUEUE_FLUSH, null);
			            
			    	    mCardScrollView.setKeepScreenOn(true);
			    	    mCardScrollView.activate();
			    	    setContentView(mCardScrollView);
		    	    
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
                  	  Card card = new Card(context);
                	  card.setImageLayout(Card.ImageLayout.FULL);
                	  card.addImage( BitmapFactory.decodeFile(f.getAbsolutePath()));
            		  card.setFootnote("Image Info: " + new_result);
            		  mCards.add(card);
            		  _speech.speak("You are viewing " + new_result, TextToSpeech.QUEUE_FLUSH, null);
            	  } else {
                  	  Card card = new Card(context);
                  	  card.setImageLayout(Card.ImageLayout.FULL);
                  	  card.addImage(BitmapFactory.decodeFile(f.getAbsolutePath()));
                  	  card.setFootnote("No Info Found");
                  	  mCards.add(card);
            	  }

                }
            }
        };
        t.start();
        return t;
    }
}
