package com.garebare.software.shoutfire;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.garebare.software.shoutfire.constants.ShoutfireConstants;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class RetrieverService extends Service {

	static final String TAG = "RetrieverService";
	Timer timer = new Timer();
	private Context context = null;
	private Date lastMessageDate = null;
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		context = getApplicationContext();
		lastMessageDate = MainActivity.lastMessageDate;
		startService();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		Log.d(TAG, "Setting last date...");
		setLastMessageDate(new Date(intent.getExtras().getLong("LAST_DATE")));
		return null;
	}
	
	public void setLastMessageDate(Date date) {
		lastMessageDate = date;
	}
	
	private void startService() {
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Log.d(TAG, "Checking for new messages...");
				lastMessageDate = MainActivity.lastMessageDate;
				if(retrieveShoutfire(false)) {
					Log.d(TAG, "Notifying of new shoutfires...");
					NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);  
					Intent notificationIntent = new Intent(context, MainActivity.class);
					notificationIntent.putExtra("NOTIFICATION_ID", ShoutfireConstants.NOTIFICATION_ID);

					PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

					Notification notification = new Notification(R.drawable.icon, "You have new shoutfires waiting for you.", System.currentTimeMillis());
					notification.setLatestEventInfo(context, "New Shoutfires", "You have new shoutfires waiting for you.", contentIntent);
					notification.defaults |= Notification.DEFAULT_SOUND;

					mNotificationManager.notify(ShoutfireConstants.NOTIFICATION_ID, notification);
				}
			}
			
		}, 0, ShoutfireConstants.SERVICE_INTERVAL);
	}
	
	private void stopService() {
		if(timer != null) 
			timer.cancel();
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		stopService();
	}
	
	private boolean retrieveShoutfire(boolean updateList) {
    	boolean isNewMessages = false;
    	
    	HttpClient client = new DefaultHttpClient();
    	HttpPost request = new HttpPost(ShoutfireConstants.RETRIEVE_URL);
    	HttpResponse response = null;
    	
    	List<NameValuePair> pairs = new ArrayList<NameValuePair>();
    	pairs.add(new BasicNameValuePair("secret", ShoutfireConstants.CORRECT_SECRET));
    	pairs.add(new BasicNameValuePair("key", ShoutfireConstants.CORRECT_KEY));
    	pairs.add(new BasicNameValuePair("count", "10"));
    	try {
			request.setEntity(new UrlEncodedFormEntity(pairs));
			response = client.execute(request);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		HttpEntity entity = response.getEntity();
		Document doc = null;
		if(entity != null) {
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				doc = db.parse(entity.getContent());
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Log.d(TAG, "Entity is null, response failed.");
		}
		
		Element root = null;
		NodeList messages = null;
		// Check if new messages
		if(doc != null) {
			root = doc.getDocumentElement();
	    	root.normalize();
	    	messages = root.getElementsByTagName(ShoutfireConstants.MESSAGE_TAG);
	    	
	    	String lastMessageServer = messages.item(0).getChildNodes().item(1).getFirstChild().getNodeValue();
	    	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    	Date date = null;
			try {
				date = format.parse(lastMessageServer);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	Log.d(TAG, "Server: " + date.toString());
	    	Log.d(TAG, "Client: " + lastMessageDate.toString());
	    	if(lastMessageServer != null && date != null && lastMessageDate.getTime() != 0) {
		    	if(lastMessageDate.before(date)) {
		    		lastMessageDate = date;
		    		isNewMessages = true;
		    	}
	    	} else {
	    	}
		}
		
		return isNewMessages;
    }
}
