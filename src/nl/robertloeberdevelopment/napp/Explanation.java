package nl.robertloeberdevelopment.napp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;


public class Explanation extends Activity {

	 @Override
	 public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.explanation_layout);
 
	        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	        
	          
	        
	       }//einde onCreate
	
	
	 
	//method als op de uitlegbutton wordt geklikt
		public void back(View view) {
		    // Do something in response to button
			
			Intent intent = new Intent(this, NappActivity.class);
			startActivity(intent);

			
		} 
	   
	 
	 
	 
	 
}
