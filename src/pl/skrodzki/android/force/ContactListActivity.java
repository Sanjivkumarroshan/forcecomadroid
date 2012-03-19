package pl.skrodzki.android.force;

import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.salesforce.androidsdk.app.ForceApp;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.ClientManager.LoginOptions;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.rest.ClientManager.RestClientCallback;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.util.EventsObservable;
import com.salesforce.androidsdk.util.EventsObservable.EventType;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import pl.skrodzki.android.force.objects.*;

public class ContactListActivity  extends Activity{
	
    private LinkedList<SalesforceContact> contactsList;
    private ListView lv;
    private RestClient client;
    private String apiVersion;
    
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.track_list);
        apiVersion = getString(R.string.api_version);
        
        lv = (ListView)findViewById(R.id.trackList);
		lv.setOnItemClickListener(new OnItemClickListener() {
		    public void onItemClick(AdapterView<?> parent, View view,
		        final int position, long id) {
		    	Context mContext = view.getContext();
		    	final Dialog dialog = new Dialog(mContext);

		    	dialog.setContentView(R.layout.contact_details);
		    	dialog.setTitle("Custom Dialog");

		    	TextView text = (TextView) dialog.findViewById(R.id.text);
		    	text.setText(contactsList.get(position).Phone);
		    	Button button = (Button) dialog.findViewById(R.id.contactDetailsOkButton);
		    	button.setOnClickListener(new OnClickListener() {
					
					public void onClick(View v) {
						String url = "tel:" + contactsList.get(position).Phone;
					    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(url));
					    startActivity(intent);
					}
				});
		    	dialog.show();
		    }
		});       
		//lv.setEnabled(false);
    }
    
	public void doneBtnInvoked(View v)
	{
		setResult(RESULT_OK);
        finish();		
	}
	
	@Override
	public void onResume(){
		super.onResume();
		
		// Login options
		String accountType = getString(R.string.account_type);
		LoginOptions loginOptions = new LoginOptions(
				null, // gets overridden by LoginActivity based on server picked by uuser 
				ForceApp.APP.getPasscodeHash(),
				getString(R.string.oauth_callback_url),
				getString(R.string.oauth_client_id),
				new String[] {"api"});
		
		new ClientManager(this, accountType, loginOptions).getRestClient(this, new RestClientCallback() {
			public void authenticatedRestClient(RestClient client) {
				if (client == null) {
					ForceApp.APP.logout(ContactListActivity.this);
					return;
				}
				ContactListActivity.this.client = client;
				getAccounts();
			}
		});
			
	}
	
	private void getAccounts(){
		try {
			String accountId = getIntent().getStringExtra("ACCOUNT_ID");
			String soql = "select Id, Name, Phone, Fax from Contact where AccountId = '"+accountId+"'";
			RestRequest request = RestRequest.getRequestForQuery(apiVersion, soql);

			client.sendAsync(request, new AsyncRequestCallback() {

				public void onSuccess(RestResponse response) {
					try {
						if (response == null || response.asJSONObject() == null)
							return;
						
						JSONArray records = response.asJSONObject().getJSONArray("records");
	
						if (records.length() == 0)
							return;
										
						contactsList = new LinkedList<SalesforceContact>();
						LinkedList<String> contactNames = new LinkedList<String>();
						for (int i = 0; i < records.length(); i++){
							JSONObject contacts = (JSONObject)records.get(i);
							SalesforceContact newContact = new SalesforceContact();
							newContact.Id = contacts.getString("Id");
							newContact.Name = contacts.getString("Name");
							newContact.Phone = contacts.getString("Phone");
							newContact.Fax = contacts.getString("Fax");
							contactsList.add(newContact);
							contactNames.add(i + "." + newContact.Name);
						}
						ArrayAdapter<String> ad = new ArrayAdapter<String>(ContactListActivity.this, 
				        												   R.layout.list_item, 
				        												   contactNames);
				        lv.setAdapter(ad);
				        EventsObservable.get().notifyEvent(EventType.RenditionComplete);
					} catch (Exception e) {
						e.printStackTrace();
						displayError(e.getMessage());
					}
				}
				
				public void onError(Exception exception) {
					displayError(exception.getMessage());
					EventsObservable.get().notifyEvent(EventType.RenditionComplete);
				}
			});
			
		} catch (Exception e) {
			e.printStackTrace();
			displayError(e.getMessage());
		}		
	}
	
	
	private void displayError(String error)	{
        ArrayAdapter<String> ad = new ArrayAdapter<String>(	ContactListActivity.this, 
        													R.layout.list_item, 
        													new String[]{"Error retrieving Track data - "+error});
        lv.setAdapter(ad);
		
	}

}
