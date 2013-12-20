package com.confused.glass.whatisthat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import com.confused.glass.whatisthat.GlassSnapshotActivity;
import com.confused.instarename.post.MultipartPost;
import com.confused.instarename.post.PostParameter;
import com.google.android.glass.app.Card;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;

/**
 * Main activity.
 */
public class WhatIsThatActivity extends Activity {
	
  private static TimelineManager tlm;
	
  private static Card _card;
  private static View _cardView;
  private static TextToSpeech _speech;

  private Context _context = this;
  
  private static final String TAG = WhatIsThatActivity.class.getSimpleName();
  private static final String IMAGE_FILE_NAME = Environment.getExternalStorageDirectory().getPath()+ "/ImageTest.jpg";

  private boolean picTaken = false; // flag to indicate if we just returned from the picture taking intent
  protected boolean mbActive;

  final Handler myHandler = new Handler(); // handles looking for the returned image file
  
  @SuppressWarnings("null")
  public static void updateResultsInUi(Context context, String results, File image){
	  
	  Card card = new Card(context);
	  card.setImageLayout(Card.ImageLayout.FULL);
	  card.addImage( Uri.fromFile(image));

	  if(results!=null){
		  card.setFootnote("Image Info: " + results);
		  _speech.speak("You are viewing " + results, TextToSpeech.QUEUE_FLUSH, null);
	  } else {
		  card.setFootnote("No results found.");
		  _speech.speak("No results found." , TextToSpeech.QUEUE_FLUSH, null);
	  }
	  
	  tlm.insert(card);
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
    tlm = TimelineManager.from(_context);
    
    _speech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int status) {
        _speech.speak("Processing Image", TextToSpeech.QUEUE_FLUSH, null);
      }
    });
    

    _card = new Card(_context);
    _card.setFootnote("Processing Image");
    _cardView = _card.toView();
    _cardView.setKeepScreenOn(true);
    setContentView(_cardView);

    // An alternative way to layout the UX
    //setContentView(R.layout.layout_helloworld);
    //_statusTextView = (TextView)findViewById(R.id.status);
    
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
      _cardView = _card.toView();
      setContentView(_cardView);

      // Status message below the main text in the alternative UX layout
      //_statusTextView.setText(R.string.touchpad_touched);

      //_speech.speak("Tap to Get Info", TextToSpeech.QUEUE_FLUSH, null);
      
      return true;
    default:
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
		    	    
		    	    Thread t = performOnBackgroundThreadSingleFile(_context, f);
		    		_card.setImageLayout(Card.ImageLayout.FULL);
		    		_card.addImage( Uri.fromFile(f));
		    		_card.setFootnote("processing image");
		    		_cardView.invalidate();
		    		_card.toView();
		    		setContentView(_cardView);
		    		
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
	
    public static Thread performOnBackgroundThreadSingleFile(final Context context, final File f) {
        final Thread t = new Thread() {
            @Override
            public void run() {
            	String new_result="";
                try {
           			Log.d("Inside thread", "");
           			new_result = getResult(f);
	        		Log.d("Stuff", "Result is: " + new_result);                

                } finally {
                	updateResultsInUi(context, new_result, f);
                }
            }
        };
        t.start();
        return t;
    }
}
