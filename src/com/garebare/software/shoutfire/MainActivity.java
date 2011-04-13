package com.garebare.software.shoutfire;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String TAG = "ShoutfireMain";
	
	private static ListView lv = null;
	private Button btnPost = null;
	private EditText txtMsg = null;
	private Dialog dialog = null, nickDialog = null;
	private SimpleAdapter adapter = null;
	public Handler handler = null;
	public Context context;
	public static Date lastMessageDate = null;
	private Intent svc = null;
	private String nickname = "";
	private EditText txtNick = null;
	private Button btnNick = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        context = this;
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        nickname = prefs.getString("nickname", "");
        
        try {
	        int notification_id = this.getIntent().getExtras().getInt("NOTIFICATION_ID");
	        if(notification_id != 0) {
	        	NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	        	mNotificationManager.cancel(notification_id);
	        }
        } catch(NullPointerException e) {
        	
        }
        
        findViews();
        
        TextView header = new TextView(this);
    	header.setText("SEND SHOUTFIRE");
    	header.setTextSize(24);
    	header.setShadowLayer(1f, 1f, 1f, Color.GRAY);
    	header.setTextColor(getResources().getColorStateList(R.drawable.header_selector));
    	header.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				addShoutfire();
			}
		});
    	lv.addHeaderView(header);
    	handler = new Handler();
    	lastMessageDate = new Date(prefs.getLong("LAST_DATE", 0));
    	
    	svc = new Intent(this, RetrieverService.class);
    	svc.putExtra("LAST_DATE", lastMessageDate.getTime());
    	startService(svc);
    }
    
    @Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		new ShoutfireRetriever().start();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("LAST_DATE", lastMessageDate.getTime());
        editor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		
		switch(item.getItemId()) {
		case R.id.menu_refresh:
			new ShoutfireRetriever().start();
			break;
		case R.id.menu_nickname:
			nickDialog = new Dialog(this);
	    	nickDialog.setContentView(R.layout.change_nick);
	    	nickDialog.setTitle("Change Nickname");
	    	
	    	txtNick = (EditText) nickDialog.findViewById(R.id.change_nickname);
	    	btnNick = (Button) nickDialog.findViewById(R.id.change_button);
	    	btnNick.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					nickname = txtNick.getText().toString();
					nickname = nickname.replaceAll("[^A-Za-z0-9 ?.!,]", "");
					nickDialog.dismiss();
					
					if(nickname.length() > 20) {
						Toast.makeText(context, "Sorry, nickname must be 20 characters or less.", Toast.LENGTH_SHORT).show();
					} else {
				        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
				        Editor editor = prefs.edit();
				        editor.putString("nickname", nickname);
				        editor.commit();
					}
				}
	    		
	    	});
	    	
	    	nickDialog.show();
		default:
			break;
		}
		
		return true;
	}

	private void findViews() {
    	lv = (ListView) findViewById(R.id.list_messages);
    }
    
    private void postShoutfire(String newMsg) {
		HttpClient client = new DefaultHttpClient();
    	HttpPost request = new HttpPost(ShoutfireConstants.ADD_URL);
    	HttpResponse response = null;
    	
    	List<NameValuePair> pairs = new ArrayList<NameValuePair>();
    	pairs.add(new BasicNameValuePair("secret", ShoutfireConstants.CORRECT_SECRET));
    	pairs.add(new BasicNameValuePair("key", ShoutfireConstants.CORRECT_KEY));
    	
    	newMsg = newMsg.replaceAll("[^A-Za-z0-9 ?.!,]", "");
    	pairs.add(new BasicNameValuePair("message_content", newMsg));
    	pairs.add(new BasicNameValuePair("nickname", nickname));
    	try {
			request.setEntity(new UrlEncodedFormEntity(pairs));
			response = client.execute(request);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Toast.makeText(this, "Sorry, please shoutfire again.", Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private void addShoutfire() {
    	
    	dialog = new Dialog(this);
    	dialog.setContentView(R.layout.post);
    	dialog.setTitle("Add a New Shoutfire");
    	
    	txtMsg = (EditText) dialog.findViewById(R.id.post_message);
    	btnPost = (Button) dialog.findViewById(R.id.post_button);
    	btnPost.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String content = txtMsg.getText().toString();
				dialog.dismiss();
				if(content.length() > ShoutfireConstants.MAX_SHOUTFIRE_LENGTH) {
					Toast.makeText(context, "Max length of shoutfire is " + ShoutfireConstants.MAX_SHOUTFIRE_LENGTH + ". Please shoutfire again.", Toast.LENGTH_SHORT).show();
				} else if(content.length() == 0) {
					Toast.makeText(context, "You didn't shoutfire anything.", Toast.LENGTH_SHORT).show();
				} else {
					new ShoutfireAdder(content).start();
					new ShoutfireRetriever().start();
				}
			}
		});
    	
    	dialog.show();
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
	    	Log.d(TAG, lastMessageServer);
	    	if(lastMessageServer != null && date != null) {
		    	if(lastMessageDate.before(date) && lastMessageDate.getTime() != 0) {
		    		lastMessageDate = date;
		    		isNewMessages = true;
		    	}
	    	} else {
	    	}
		}
		
		if(messages != null && updateList == true) {
			populateList(messages);
		}
		
		return isNewMessages;
    }
    
    private void populateList(final NodeList messages) {
    	//Element root = doc.getDocumentElement();
    	//root.normalize();
    	//messages = root.getElementsByTagName(ShoutfireConstants.MESSAGE_TAG);
    	int size = messages.getLength();
    	
    	List<Map> list = new ArrayList<Map>();
    	
    	for(int i = 0; i < ShoutfireConstants.MAX_SHOUTFIRES; i++) {
    		if(i > size - 1)
    			break;
    		
    		Map map = new HashMap();
    		NodeList children = messages.item(i).getChildNodes();
    		
    		if(children.item(0).getFirstChild() == null) {
    			continue;
    		} else {
	    		map.put("content", children.item(0).getFirstChild().getNodeValue());
	    		
	    		String date = children.item(1).getFirstChild().getNodeValue();
	    		SimpleDateFormat format = new SimpleDateFormat("MM/dd hh:mm aa");
	    		
	    		Date newDate = null;
	    		SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    		try {
					newDate = newFormat.parse(date);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	
	    		if(newDate != null)
	    			map.put("timestamp", "At " + format.format(newDate));
	    		else
	    			map.put("timestamp", "At " + "unknown time");
	    		if(children.item(2).getFirstChild() != null)
	    			map.put("nickname", "by " + children.item(2).getFirstChild().getNodeValue());
	    		else map.put("nickname", "by " + "Guest");
	    		list.add(map);
    		}
    	}
    	
    	String[] from = {"content", "timestamp", "nickname"};
    	int[] to = {R.id.msg_content, R.id.msg_timestamp, R.id.msg_nickname};
    	
    	adapter = new SimpleAdapter(context, (List<? extends Map<String, ?>>) list, R.layout.row, from, to);
    	
    	handler.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				lv.setAdapter(adapter);
			}
    		
    	});
    	
    }
    
    private static String convertStreamToString(InputStream is) {
    	BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8192);
    	StringBuilder sb = new StringBuilder();
    	
    	String line = null;
    	try {
    		while ((line = reader.readLine()) != null) {
    			sb.append(line + "\n");
    		}
    	} catch (IOException e) {
    		e.printStackTrace();
    	} finally {
    		try {
    			is.close();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	}
    		
    	return sb.toString();
    }
    
    class ShoutfireRetriever extends Thread {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			retrieveShoutfire(true);
		}
    	
    }
    
    class ShoutfireAdder extends Thread {

    	private String newMsg = null;
    	private ProgressDialog progress = null;
    	public ShoutfireAdder(String msg) {
    		newMsg = msg;
    	}
    	
		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			handler.post(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					progress = ProgressDialog.show(context,"", "Posting now...", true);
				}
			});
			postShoutfire(newMsg);
			handler.post(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					progress.dismiss();				
				}
			});

		}
    	
    }
}