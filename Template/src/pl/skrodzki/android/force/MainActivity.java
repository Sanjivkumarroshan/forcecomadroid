package pl.skrodzki.android.force;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.salesforce.androidsdk.app.ForceApp;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.ClientManager.LoginOptions;
import com.salesforce.androidsdk.rest.ClientManager.RestClientCallback;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.util.EventsObservable;
import com.salesforce.androidsdk.util.EventsObservable.EventType;

public class MainActivity extends ListActivity{

    private String[] accounts;
    private String[] accountIds;
    private RestClient client;
    private String apiVersion;
    
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ListView lv = getListView();
        apiVersion = getString(R.string.api_version);
        
		lv.setTextFilterEnabled(true);
	
		lv.setOnItemClickListener(new OnItemClickListener() {
		    public void onItemClick(AdapterView<?> parent, View view,
		        int position, long id) {
	
		      Toast.makeText(getApplicationContext(), ((TextView) view).getText(),
		          Toast.LENGTH_SHORT).show();
			  Intent i = new Intent(MainActivity.this, ContactListActivity.class);
			  i.putExtra("ACCOUNT_ID", accountIds[position]);
			  startActivity(i);
		    }
		});
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
					ForceApp.APP.logout(MainActivity.this);
					return;
				}
				MainActivity.this.client = client;
				getAlbums();
			}
		});
	}	

	

	private void getAlbums(){

		try {
			
			String soql = "select id, name from Account";
			RestRequest request = RestRequest.getRequestForQuery(apiVersion, soql);

			client.sendAsync(request, new AsyncRequestCallback() {

				public void onSuccess(RestResponse response) {
					try {
						if (response == null || response.asJSONObject() == null)
							return;
						
						JSONArray records = response.asJSONObject().getJSONArray("records");
	
						if (records.length() == 0)
							return;
										
				    	accounts = new String[records.length()];
				    	accountIds = new String[records.length()];
						
						for (int i = 0; i < records.length(); i++){
							JSONObject album = (JSONObject)records.get(i);
							accounts[i] = album.getString("Name");
							accountIds[i] = album.getString("Id");
						}
				        ArrayAdapter<String> ad = new ArrayAdapter<String>(MainActivity.this, 
				        												   R.layout.list_item, 
				        												   accounts);
				        setListAdapter(ad);
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
        ArrayAdapter<String> ad = new ArrayAdapter<String>(	MainActivity.this, 
        													R.layout.list_item, 
        													new String[]{"Error retrieving Album data - " + error});
        setListAdapter(ad);
		
	}

}
