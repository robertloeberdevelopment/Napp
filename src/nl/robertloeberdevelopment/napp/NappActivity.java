package nl.robertloeberdevelopment.napp;


import java.util.HashMap;

import nl.robertloeberdevelopment.napp.NappActivity.FadeOutMusic;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaPlayer;

public class NappActivity extends Activity {
	

	int aantalMinutenSlaap=10;
	Button button1;
	int inslaapMuziek=0;//variabele voor keuze inslaapmuziek, standaard op silence
	int wordtWakkerMuziek=0;
	int wekker=0;
	MediaPlayer mp = null;
	MediaPlayer mpLoop = null;
	MediaPlayer mpWekker=null;

    boolean toggleStart=false;
    int duurSlaapMuziek=260000; // totale duur eerste slaapgeluid in miliseconden 
    int duurFade=160000; //lengte van fade out
    int startFade= duurSlaapMuziek-duurFade;// wanneer de fade out begint
    int duurGeluidWakkerworden=160000; //duur wakkerwordgeluid voordat wekker begint
    AsyncTask<Float, Float, String> mtask_fadeout_music = null;
    
    private PowerManager.WakeLock wakeLock; 
    
    
    //private Handler handlerFade=null;
    //private Runnable rFade=null;
    
   // private Handler handlerStop=null;
   // private Runnable rStopAchtergrond=null;
    
    //private Handler handlerWekker=null;
   // private Runnable rWekker=null;
    
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

       
        
        
       // uit een tutorial mediaplayer: dit zet hij in onCreate:
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "MyMediaPlayer");
        
       //op landscape orientatie zetten
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
		registerListeners();// listeners activeren in de onderstaande method
        
		
       }//einde onCreate
    
    
    
  
	    @Override
	    protected void onPause() {
	        super.onPause();
	   	 //Log.d("NappAcivity","onPause geactiveerd");//test
	        wakeLock.release();
	        
	    }//End of onPause
	
	
	    
	    
	    //als je app aflsuit, alles uit geheugen halen 
	    protected void onStop() {
	 	    super.onStop();
	 	  	 //Log.d("NappAcivity","onStop geactiveerd");//test
	 	   
	 	  	 if (mpLoop != null) {
	 	        if (mpLoop.isPlaying()) {
	 	            mpLoop.stop();
	 	         
	 	        }
	 	        mpLoop.release();
	 	        mpLoop = null;
	 	    }
	 	    
	 	    if (mpWekker != null) {
	 	        if (mpWekker.isPlaying()) {
	 	        	mpWekker.stop();
	 	        }
	 	        mpWekker.release();
	 	        mpWekker = null;
	 	    }
	 	    

	 	  	 if (mp != null) {
	 	        if (mp.isPlaying()) {
	 	            mp.stop();
	 	         
	 	        }
	 	        mp.release();
	 	        mp = null;
	 	    } 
	 	    
	 	    
	 	  	if(mtask_fadeout_music!=null){
			      mtask_fadeout_music.cancel(true);
			      mtask_fadeout_music=null;
			    }
			  
			  Log.d("NappAcivity","remove callbacks hierna");//test
			  
		//lopende vertraagde runnables stoppen.	 Als ie niet gestart is een crash hierdoor, dus met try en catch
			  try {handlerWekker.removeCallbacks(rWekker); 
				  } 
			  catch (Exception e){//do nothing  
				   e.printStackTrace();}
			  
			  try {handlerFade.removeCallbacks(rFade); 
			  	} 
			  catch (Exception e){//do nothing  
			   e.printStackTrace();}

			  try {handlerStop.removeCallbacks(rStopAchtergrond); 
			  } 
			  catch (Exception e){//do nothing  
			   e.printStackTrace();}
			  
			  try {handlerEinde.removeCallbacks(rStartEinde); 
			  } 
			  catch (Exception e){//do nothing  
			   e.printStackTrace();}
	 		    //wakeLock.release();//dit zorgt voor crash. Hij moet in de onPause
	 	    
	 	}//einde onStop 
	    
	    
	    
	    @Override
	    protected void onResume() {
	        super.onResume();
	        Log.d("NappAcivity","onResume geactiveerd");//test
	        wakeLock.acquire();
	        
	    }//End of onResume
    
   
    
    
    
    
    
    
    
    //testmethode om toast te showen
    void showToast(CharSequence msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }  
   
    
	
    //hier de listeners aanzetten enz.
    private void registerListeners() {
		

    			//tekst op de startknop zetten. Na start moet er andere tekst op zie onder
			  button1 = (Button)findViewById(R.id.button1);
			  button1.setText("Start your Italian nap");
        	
        		
			    	
			    	
			    	
			    	
        		final TextView tekstSlaaptijd = (TextView) findViewById(R.id.zoekbar_value);
        		
        	   SeekBar  seekbar=(SeekBar)findViewById(R.id.seekbar);
               seekbar.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {
								public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
										{
                                              
									//schaal anders indelen
									if(progress<=10) aantalMinutenSlaap=10;
									else if (progress>10 && progress<= 20) aantalMinutenSlaap=15;
									else if (progress>20 && progress<= 30) aantalMinutenSlaap=20;
									else if (progress>30 && progress<= 40) aantalMinutenSlaap=25;
									else if (progress>40 && progress<= 50) aantalMinutenSlaap=30;
									else if (progress>50 && progress<= 60) aantalMinutenSlaap=35;
									else if (progress>60 && progress<= 70) aantalMinutenSlaap=40;
									else if (progress>70 && progress<= 80) aantalMinutenSlaap=45;
									else if (progress>80 && progress<= 90) aantalMinutenSlaap=50;
									else if (progress>90 && progress<=100) aantalMinutenSlaap=60;
									else if (progress>100 && progress<=110) aantalMinutenSlaap=75;
									else if (progress>110 && progress<=119) aantalMinutenSlaap=90;
									else aantalMinutenSlaap=120;

									tekstSlaaptijd.setText(" " + aantalMinutenSlaap);
										}

                               public void onStartTrackingTouch(SeekBar seekBar)
                               			{
                                               // TODO dit moet hij blijkbaar ook hebben, ook al doe je er niks mee
                               			}

                               public void onStopTrackingTouch(SeekBar seekBar)
                               			{
                                               // TODO Auto-generated method stub
                               			}
                               			
								});
        		
        		
        		
        		
        		//eerste radiogroep
        		 RadioGroup snozeSound = (RadioGroup) findViewById(R.id.snoze_sound);        
        		   snozeSound.setOnCheckedChangeListener(new OnCheckedChangeListener() 
        		    {
        		        public void onCheckedChanged(RadioGroup group, int checkedIdSnozeSound) {
        		           
        		        	switch(checkedIdSnozeSound) { //variabele inslaapMuziek bepalen
        		            case R.id.sound_sleep_silence:
        		                inslaapMuziek=0;
        		                break;
        		            case R.id.sound_sleep_sea:
        		            	inslaapMuziek=1;
        		                break;
        		            case R.id.sound_sleep_grasshoppers:
        		            	inslaapMuziek=2;
        		                break;	
	        		        case R.id.sound_sleep_forest:
	        		        	inslaapMuziek=3;
	    		                break;
	    		            }
        		        	if(inslaapMuziek!=0){
	        		        	geluidDemonstratie(inslaapMuziek);
	        		        	}
        		        	//showToast("radio groep snoze waarde: " + inslaapMuziek);
        		        }
        		    });
        		
        		
        		  
        		   RadioGroup slowWakeupSound = (RadioGroup) findViewById(R.id.slow_wakeup_sound);        
        		   slowWakeupSound.setOnCheckedChangeListener(new OnCheckedChangeListener() 
        		    {
        		        public void onCheckedChanged(RadioGroup group, int checkedIdSlowWakeupSound) {
        		           
        		        	switch(checkedIdSlowWakeupSound) { //variabele wakkerwordtMuziek bepalen
        		            case R.id.sound_wakeup_silence:
        		            	wordtWakkerMuziek=0;
        		                break;
        		            case R.id.sound_wakeup_sea:
        		            	wordtWakkerMuziek=1;
        		                break;
        		            case R.id.sound_wakeup_grasshoppers:
        		            	wordtWakkerMuziek=2;
        		                break;	
	        		        case R.id.sound_wakeup_forest:
	        		        	wordtWakkerMuziek=3;
	    		                break;
	    		            }
        		        	
        		        	if(wordtWakkerMuziek!=0){
        		        	geluidDemonstratie(wordtWakkerMuziek);
        		        	}
        		        	//showToast("radio groep snoze waarde: " + wordtWakkerMuziek);
        		        }
        		    });
        		    
        		    
    
        		   
        		   RadioGroup finalWakupCall = (RadioGroup) findViewById(R.id.final_wakeup_call);        
        		   finalWakupCall.setOnCheckedChangeListener(new OnCheckedChangeListener() 
        		    {
        		        public void onCheckedChanged(RadioGroup group, int checkedIdFinalWakeupCall) {
        		           
        		        	switch(checkedIdFinalWakeupCall) { //variabele wekker bepalen
        		            case R.id.final_wakeup_callas:
        		            	wekker=10;
        		                break;
        		            case R.id.final_wakeup_bell1:
        		            	wekker=11;
        		                break;
        		            case R.id.final_wakeup_zen_bell:
        		            	wekker=12;
        		                break;	
	        		        case R.id.final_wakeup_waky_waky:
	        		        	wekker=13;
	    		                break;
	    		            }	
        		        	
        		        	if(wekker!=0){
        		        	geluidDemonstratie(wekker);
        		        	}
        		        	//showToast("radio groep snoze waarde: " + wekker);
        		        }
        		    });  
   		   
    	
	}// einde listeners
   
   
    
    
   //eenmalig afspelen als buttonkeuze wordt gemaakt
    public void geluidDemonstratie(int geluidsKeuze){
	   int uri = 0;	
	   
	   		
		    	switch(geluidsKeuze) { 
		    	 case 0:
			        uri=0;
			        break;
		        case 1:
		        	uri= R.raw.zee_voorbeeld;
		            break;
		        case 2:
		        	uri=R.raw.krekels_voorbeeld;
		            break;	
		        case 3:
		        	uri=R.raw.woud_voorbeeld;
		            break;      
		        case 10:
		        	uri=R.raw.callas_voorbeeld;
		            break;  
		        case 11:
		        	uri=R.raw.kerkbel_italiaans;
		            break;    
		        case 12:
		        	uri=R.raw.bellen_loop;
		            break;    
		        case 13:
		        	uri=R.raw.waky_waky;
		            break;
		            
		    	}	
    
	    mp = MediaPlayer.create(this, uri); 
	    mp.setVolume(0.8f,0.8f);// hier zet je volume tussen 0.0f en 1f
	    mp.start();	 	 
    	}
    
    
    

    
    
    // de method door klikken op startbutton 
    public void startNap(View view) {

    		if(toggleStart==false){
    		
    				achtergrondGeluidBegin(); //achtergrondgeluid begin aanzetten en uitzetten. Todo: ze een voor een aanzetten, dus de tweede en wekker bij stoppen eerste
    				achtergrondGeluidEindeVertraging();//wakkerword geluiden, start via deze
    				wekkerVertraging();//wekkerfunctie, start via deze
    				
    				toggleStart=true;//op true ztten als hij gestart is.		
    				showToast("Have a nice sleep, I wake you in " + aantalMinutenSlaap  + " minutes.");
    		
    				button1.setText("Nap guiding process started...");	
    				

    				//achtergrondplaatje veranderen naar donker plaatje
    		       	View mainLayout = findViewById(R.id.main); // je moet in de main dit zetten:  android:id="@+id/main" 
    		       	mainLayout.setBackgroundResource(R.drawable.achtergrond2_zon_onder); 
    				
    		   
    				
    		}//einde if	 
 
    		
    		else{ //als toggleStart != false is hij al gestart, en dan dus de zaak uit zetten
    		  
    			stopNap(view);  
    		   	toggleStart=false;	
    		}

    
    		
    		
    }//einde startNap   	  
    
   
 
    
    
   //alles stoppen als op de stopbutton wordt geklikt. 
   public void stopNap(View view){
		 //Log.d("NappAcivity","MpLoop stoppen");//test
		 
		 button1.setText("Start your Italian nap");
		 
		View mainLayout2 = findViewById(R.id.main); // je moet in de main dit zetten:  android:id="@+id/main" 
    	mainLayout2.setBackgroundResource(R.drawable.achtergrond2); //plaatje weer terugzetten als de wekker gaat
        	 
		 
	   if (mpLoop != null) {
	        if (mpLoop.isPlaying()) {
	            mpLoop.stop();
	            mpLoop.release();
	            mpLoop = null;
	            
	        }
	      }
	   
	   	
		if (mpWekker != null) {
			//Log.d("NappAcivity","Mpwekker stoppen");//test
	   		if (mpWekker.isPlaying()) {
	   			mpWekker.stop();
	   			mpWekker = null;
   				}
				} 
	   	

	  	 if (mp != null) {
	        if (mp.isPlaying()) {
	            mp.stop();
	         
	        }
	        mp.release();
	        mp = null;
	    } 
		
		  if(mtask_fadeout_music!=null){
		      mtask_fadeout_music.cancel(true);
		      mtask_fadeout_music=null;
		    }
		  
		 // Log.d("NappAcivity","remove callbacks hierna");//test
		  
	//lopende vertraagde runnables stoppen.	 Als ie niet gestart is een crash hierdoor, dus met try en catch
		  try {handlerWekker.removeCallbacks(rWekker); 
			  } 
		  catch (Exception e){//do nothing  
			   e.printStackTrace();}
		  
		  try {handlerFade.removeCallbacks(rFade); 
		  	} 
		  catch (Exception e){//do nothing  
		   e.printStackTrace();}

		  try {handlerStop.removeCallbacks(rStopAchtergrond); 
		  } 
		  catch (Exception e){//do nothing  
		   e.printStackTrace();}
		  
		  try {handlerEinde.removeCallbacks(rStartEinde); 
		  } 
		  catch (Exception e){//do nothing  
		   e.printStackTrace();}
		  
		  setBrightness(1f);
		  
		  showToast("Your nap guiding process has stopped..."); 
   } 
   
  
   
   
   
   //handler voor start uitfaden begingeluid
   Handler handlerFade = new Handler();
   Runnable rFade = new Runnable() {
		        public void run() 
		        {

				   float level=0.70f;//begin niveau meesturen naar fadeout class, moet aansluiten bij niveau waarop geluid begon
				   float factor=0.987f;//factor afname meesturen
				   
				 

				   mtask_fadeout_music = (FadeOutMusic)new FadeOutMusic( ).execute(level, factor);

		        }
		}; 
   

    
 //handler buiten de method achtergrondGeluidBegin gehaald 
   Handler handlerStop = new Handler();
   Runnable rStopAchtergrond = new Runnable() {
		        public void run() 
		        {

		  		  if(mtask_fadeout_music!=null){  //fade out op achtergrond stoppen. In feite overbodig, want hij moet al klaar zijn metr faden voor je beginmuziek uitzet
		  		      mtask_fadeout_music.cancel(true);
		  		      mtask_fadeout_music=null;
		  		    }
		        
		  		  
		  		  
		  		 // To Do: dit is om scherm te laten slapen, als inslaapgeluiden klaar zijn. Nodig is dan weer een methode met TimeManager, om het scherm weer aan te zetten. 
		  		 //wakeLock.release(); 
		  		  
		  		  
		       	//Log.d("NappAcivity"," fadeoutmusic taak gecancelled");//test
		       	 
		       	
		        //scherm nog verder dimmen, bij 0.0 gaat app op pause!
		       	setBrightness(0.01f);
	        	

				//achtergrondplaatje veranderen naar donker plaatje
		       	View mainLayout = findViewById(R.id.main); // je moet in de main dit zetten:  android:id="@+id/main" 
		       	mainLayout.setBackgroundResource(R.drawable.sterren); 
				
			
				   
		       	
		       	
		        	if (mpLoop != null) {
		 		   		if (mpLoop.isPlaying()) {
		 		   			mpLoop.setVolume(0f,0f);// hier zet je volume tussen 0.0f en 1f
		 		   			mpLoop.stop();
		 		   			mpLoop = null;
		 		   			//mpLoop.release();//uit geheugen verwijderen?. Je verwijdert object! dus hier absloluut n iet de bedoeling!!!!
				   				}
			   				} 
		        }
		}; 
   
 
		
 Handler handlerEinde = new Handler();
 Runnable rStartEinde = new Runnable() {
				        public void run() 
				        {
				        	if(toggleStart==true){ //alleen uitvoeren als er niet opnieuw op start/stop is geklikt
				        	//Log.d("NappAcivity","wakkerwordt geluid gestart");//test
				        	achtergrondGeluidEinde();//start functie achtergrondeinde
				 		  
				        	  //scherm weer aanzetten met method hieronder
				        	 //setBrightness(1f);
				        	
				        	
				        	}
				        
				        }
	    	};

		

//handler wekkervertraging	    	
Handler handlerWekker = new Handler();
Runnable rWekker = new Runnable() {
					        public void run() 
					        {
					        	if(toggleStart==true){ //alleen uitvoeren als er niet opnieuw op start/stop is geklikt
					        	
					        	
					        	 setBrightness(1f);
					        	 
					        	 Log.d("NappAcivity","Wekker wordt gestart");//test
					        	 wekker();	
					        	
					        	 Log.d("NappAcivity","plaatje wordt gewisseld");//test
					        	View mainLayout2 = findViewById(R.id.main); // je moet in de main dit zetten:  android:id="@+id/main" 
					    		mainLayout2.setBackgroundResource(R.drawable.achtergrond2_zon_stralend); //plaatje weer terugzetten als de wekker gaat
					        	 
					        	 
					        	 showToast("Andiamo! Time to get up!");
					        	
					        	 
					            //handler.postDelayed(this, 10000);// om de tien sec herhalen, hoeft niet meer, want geluid loopt zelf
					        	}
					        
					        }
		    	};
	    	
	    	
	    	
	    	
	    	
	    	
	    	
	 //scherm helderheid methode
	public void setBrightness(float brightness){ 
		try{
			Log.d("NappAcivity","Brightness methode, param is: " + brightness);//test	
			int brightnessMode = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE);
			 
			       if (brightnessMode == android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
					android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
				}
			 
		WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
		layoutParams.screenBrightness = brightness;
		getWindow().setAttributes(layoutParams);
			} catch (Exception e){
			        // do something useful
			  }
		}
	    	
	    	
	    	
	    	
		
   
   
				//begin geluiden aanzetten en vertraagd uitzetten. Deze method zou ook in een service moeten, die wakker blijft. Want als dit uitgaat, start hij ook niet de fade out
	private void achtergrondGeluidBegin(){
		
		//MediaPlayer mpLoop; Wakelock zetten, zodat hij blijft doorgaan, werktniet gaat toch in slaap. Het moet als een service lijkt het, zo werkt het niet
		//mpLoop = new MediaPlayer();
		//mpLoop.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK); niet meer nodig? Dit gaf bij onStop crash: 'WakeLock under-locked MyMediaPlayer'
		  	
			//Helderheids method bij start meteen aanroepen en scherm lager zetten
			setBrightness(0.5f);

		 	int uri = 0;	
			
	    	switch(inslaapMuziek) { //variabele wekker bepalen
	        case 1:
	        	uri= R.raw.zee_loop_lang2;
	            break;
	        case 2:
	        	uri=R.raw.krekels_lang2;
	            break;	
	        case 3:
	        	uri=R.raw.woud_lang2;
	            break;
	        }	

				 if(inslaapMuziek!=0){
					mpLoop = MediaPlayer.create(this, uri);
					//mpLoop.setLooping(true);
					mpLoop.setVolume(0.4f,0.4f);// hier zet je volume tussen 0.0f en 1f
				 	mpLoop.start();	 	
				 	}

		   handlerStop.postDelayed(rStopAchtergrond, duurSlaapMuziek);//na zoveel miliseconden vertraagd runnable r uitvoeren

		   handlerFade.postDelayed(rFade, startFade);//na zoveel miliseconden vertraagd runnable r uitvoeren
		   
		}//einde method  
	   
   

	
	
	
	
	//class die fade out
	public class FadeOutMusic extends AsyncTask<Float,Float,String> {

		  @Override
		  protected String doInBackground(Float... args) {
			  
			float level = args[0];
			float factor=args[1];
			long sleep=2000;
			
		    int i = 1;

		    while(i<70){
		        i++;
		        if(mpLoop != null){
		        	
		        	factor=(float) (factor - (i*0.00005) );//curve progressiever maken. Beginnen met minder afname, naarmate i hoger wordt de afname vergroten, of toename verminderen(bij fade in)
		        	
			        level=level*factor;
			        
			        if (i==70){ sleep=360000;}//laatste keer lang laten slapen om te voorkomen dat achtergrond geluid stopt    
			          
			        //if (level >=1){level=1f;} straks implementeren, andere manier van begrenzen
			        
			           if(level<1.0f && level >0.001f) {	
			        	   mpLoop.setVolume(level, level); //volume naar omlaag of omhoog, maar niet als level boven 1 komt! (dan crashed ie)
			        	   Log.d("NappAcivity","level is:" + level + " i = " +i + " factor="  + factor + "sleep= " +sleep);//test
			           
			           }
		          
			           
		        }
		        try {
		          Thread.sleep(sleep);
		        } catch (InterruptedException e) {
		          e.printStackTrace();
		        }
		    }
		    return "dummy";// de asynchronische taak moet blijkbaar een return doen van type string
		  }

		  
		  @Override
		  protected void onPostExecute( String dummy ) {
		    if(mpLoop != null){
		      //mpLoop.setVolume(1,1);         
		      mpLoop.release();
		      mpLoop = null;  
		    }
		    if(mtask_fadeout_music!=null){
		      mtask_fadeout_music = null;
		    }
		  }       

		  @Override
		  public void onCancelled() {
			  
			  //showToast("Fader async task canceled ");
		    if(mpLoop != null){
		      //mpLoop.setVolume(1,1);         
		      mpLoop.release();
		      mpLoop = null;  
		    }
		    if(mtask_fadeout_music!=null){
		      mtask_fadeout_music = null;
		    }
		  }
		  
		  
		}//einde class
		
		
		
		
		

	
	
	//eind geluiden vertraagd aanzetten, maar wel bepaalde tijd voor wekker
	private void achtergrondGeluidEindeVertraging(){
		
		int vertragingMs= aantalMinutenSlaap*60*1000;//aantal gewenste minuten slaap omzetten naar miliseconden in integerformaat
		vertragingMs=vertragingMs-duurGeluidWakkerworden;
		

	    handlerEinde.postDelayed(rStartEinde, vertragingMs);//dit vertraagt uitvoering van de runnable r
	    
		
	}//einde
	
	
	
	
	
   
   
	
	private void achtergrondGeluidEinde(){
		
		/* even uit omdat in onCreate nu een wakelock staat
		// scherm weer aanzetten is de bedoeling. Dit lijkt wel te werken, ook het lockscherm wordt overgeslagen en 
		  PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
          WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
          wakeLock.acquire();
		
		// screenlock omzeilen, moet je permissies voor instellen in manifest.xml  < uses-permission android:name="android.permission.DISABLE_KEYGUARD" />
          KeyguardManager keyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE); 
          KeyguardLock keyguardLock =  keyguardManager.newKeyguardLock("TAG");
          keyguardLock.disableKeyguard();
		*/
		
		setBrightness(0.3f);
		
		int uri = 0;	
			
	    	switch(wordtWakkerMuziek) { //variabele wekker bepalen
	        case 1:
	        	uri= R.raw.zee_loop_lang2;
	            break;
	        case 2:
	        	uri=R.raw.krekels_lang2;
	            break;	
	        case 3:
	        	uri=R.raw.woud_lang2;
	            break;
	        }	

				 if(wordtWakkerMuziek!=0){
					mpLoop = MediaPlayer.create(this, uri);//hier het het geluid zetten 
					mpLoop.setLooping(true);
					mpLoop.setVolume(0.01f,0.01f);// hier zet je volume tussen 0.0f en 1f
				 	mpLoop.start();	 	
				 	}
		
				 float level=0.01f;
				 float factor=1.115f;	   
				 mtask_fadeout_music = (FadeOutMusic)new FadeOutMusic( ).execute(level, factor);
	
	
	}//einde
	
	
	
   
   
   
	//wekker vertraagd aanzetten
	private void  wekkerVertraging(){

		int vertragingMs= aantalMinutenSlaap*60*1000;//aantal gewenste minuten slaap omzetten naar miliseconden in integerformaat
	    	
	    handlerWekker.postDelayed(rWekker, vertragingMs);//dit vertraagt uitvoering van de runnable r

	 }
	
 
	

   //wekkergeluid spelen
	private void wekker(){
		 	// to do: hier eerst mp.stop doen? want hij wordt elke zoveel seconden opnieuw aangezet
			int uri =0;
	    	switch(wekker) { //variabele wekker bepalen
	    	case 10:
	        	uri= R.raw.callas_fragment;
	            break;
	    	case 11:
	        	uri= R.raw.kerkbel_italiaans;
	            break;
	        case 12:
	        	uri=R.raw.bellen_loop;
	            break;	
	        case 13:
	        	uri=R.raw.waky_waky;
	            break;
	            
	        default: uri= R.raw.kerkbel_italiaans;//dit moet want als je radio button laat staan waar hij stond, wordt var wekker niet gezet
           		break;
	    	
	    	}	
	    	
	    	
	    	
			 mpWekker = MediaPlayer.create(this, uri); //andere instantie maken, lijkt wel of hij beinvloed wordt, of moet hij ook telkens afsluiten nadat hij het heeft afgespeekld?
			 mpWekker.setLooping(true);//ook wekker loopen, want geluiden herhalen zich
			 mpWekker.setVolume(1f,1f);// hier zet je volume tussen 0.0f en 1f 
			 mpWekker.start();	 
			 
		
			 
			 
		
	}
   
   
   
  
	//method als op de uitlegbutton wordt geklikt
	public void explanation(View view) {
	    // Do something in response to button
		
		Intent intent = new Intent(this, Explanation.class);
		startActivity(intent);

		
	} 
   
 
  
   
    
    
}//einde class