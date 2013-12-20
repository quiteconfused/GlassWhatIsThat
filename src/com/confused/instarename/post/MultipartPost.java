package com.confused.instarename.post;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import android.text.format.Time;
import android.util.Log;

//http://www.codenet.ru/webmast/php/HTTP-POST.php
public class MultipartPost {

	private final String TAG = "MultipartPost";
	@SuppressWarnings("rawtypes")
	private List<PostParameter> params;
	private static final String CRLF = "\r\n";
	private static final String BOUNDARY = "----------------------------" + Time.EPOCH_JULIAN_DAY;

	public MultipartPost(@SuppressWarnings("rawtypes") List<PostParameter> params) {
		this.params = params;
	}
	
	public String send(String urlString, String referer) throws Exception {
	    
	    HttpURLConnection conn = null;
	    DataOutputStream dos = null;
	    String response = null;
	    InputStream is = null;
	    OutputStream os = null;
	    try {
	    	Log.d("Multipart Send", urlString);
			conn = (HttpURLConnection) ( new URL( urlString )).openConnection();			
			conn.setDoInput(true);
			//conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:8.0) Gecko/20100101 Firefox/8.0");
			conn.setRequestProperty("Accept", "*/*");
			conn.setRequestProperty("Referer", referer);
			conn.setRequestProperty("Accept-Encoding", "");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Expect", "100-continue");
			
			try{
				os = conn.getOutputStream();
			} catch(IOException e){
				Log.d("getOutputStream", "couldn't create stream " + e.getMessage());
				return null;
			}
			
			try{
				dos = new DataOutputStream(os);
			} catch(Exception e){
				Log.d("getOutputStream", "couldn't create stream " + e.getMessage());
				return null;
				
			}

			for( PostParameter param : params) {
				Log.d(TAG, "Processing param: " + param.getParamName());
				if(param.getValue() == null) {
				    param.setValue("");
				}
				if(param.getValue().getClass() == File.class) {
					postFileParameter(dos, param.getParamName(), (File) param.getValue(), /*param.getContentType()*/"image/jpeg");
				} 
				else {
					postStringParameter(dos, param.getParamName(), param.getValue().toString());
				}
			}
			dos.writeBytes(closeBoundary());
			dos.flush();

			boolean redirect = false;			
			
			int status = conn.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				if (status == HttpURLConnection.HTTP_MOVED_TEMP
					|| status == HttpURLConnection.HTTP_MOVED_PERM
						|| status == HttpURLConnection.HTTP_SEE_OTHER)
				redirect = true;
			}
			
			if (redirect) {
				 
				// get redirect url from "location" header field
				String newUrl = conn.getHeaderField("Location");
		 
				// get the cookie if need, for login
				String cookies = conn.getHeaderField("Set-Cookie");
		 
				// open the new connnection again
				conn = (HttpURLConnection) new URL(newUrl).openConnection();
				conn.setDoInput(true);
				conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:8.0) Gecko/20100101 Firefox/8.0");
				conn.setRequestProperty("Accept", "*/*");
				conn.setRequestProperty("Referer", referer);
				conn.setRequestProperty("Accept-Encoding", "");
				conn.setRequestProperty("Connection", "Keep-Alive");
				conn.setRequestProperty("Expect", "100-continue");
	 
			}
		 			

			is = conn.getInputStream();
			int ch;

			StringBuffer b = new StringBuffer();
			while ((ch = is.read()) != -1) {
				b.append((char) ch);
			}
			response = b.toString();
			
			is.close();
			dos.close();
			conn.disconnect();
		} 
		catch (Exception e) {
    		Log.e(TAG, "Bad Setup: " + e.getMessage(), e);
		}
		finally {
		    if(dos != null) try { dos.close(); } catch(IOException ioe) { /* that's it */ }
		    if(is  != null) try { is .close(); } catch(IOException ioe) { /* that's it */ }
		}
		
		return response;
	}

	private void postStringParameter(DataOutputStream dos, String paramName, String paramValue) throws IOException {
		Log.d(TAG, "ParamName: " + paramName);
		dos.writeBytes(boundary() + CRLF);
		dos.writeBytes("Content-Disposition: form-data; name=\"" + paramName + "\"" + CRLF + CRLF);
		dos.writeBytes(paramValue + CRLF);
	}

	private void postFileParameter(DataOutputStream dos, String paramName, File file, String contentType) throws IOException {
		Log.d(TAG, "paramName: " + paramName + " file: " + file.getName() + " content-Type: " + contentType);
		dos.writeBytes(boundary() + CRLF);
		dos.writeBytes("Content-Disposition: form-data; name=\"" + paramName + "\"; filename=\"" + file.getName() + "\"" + CRLF);
		dos.writeBytes("Content-Type: "+ contentType + CRLF);
		dos.writeBytes(CRLF);

		FileInputStream fileInputStream = new FileInputStream(file);
		int bytesAvailable = fileInputStream.available();
		int maxBufferSize = 1024;
		int bufferSize = Math.min(bytesAvailable, maxBufferSize);
		byte[] buffer = new byte[bufferSize];

		int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

		while (bytesRead > 0) {
			dos.write(buffer, 0, bufferSize);
			bytesAvailable = fileInputStream.available();
			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			buffer = null;
			buffer = new byte[bufferSize];
			bytesRead = fileInputStream.read(buffer, 0, bufferSize);

		}
		dos.writeBytes(CRLF);
		dos.flush();
		fileInputStream.close();
		System.gc();
	}


	private String closeBoundary() {
		return boundary() + "--" + CRLF;
	}

	private String boundary() {
		return "--" + BOUNDARY;
	}

}
