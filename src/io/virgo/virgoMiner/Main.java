package io.virgo.virgoMiner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;

import io.virgo.geoWeb.GeoWeb;
import io.virgo.geoWeb.exceptions.PortUnavailableException;
import io.virgo.virgoAPI.VirgoAPI;

public class Main {

	public static volatile String parentBeacon = "";
	public static volatile String header = "";
	public static volatile JSONArray parents = null;
	public static volatile JSONArray outputs = null;
	public static volatile long difficulty = 0;
	public static volatile boolean found = true;
	
	public static ArrayList<Worker> workers = new ArrayList<Worker>();
	
	public static final long MAX = 72056494526300160l;
	
	public static volatile long hashes = 0;
	
	static String[] runningIndicators = {"-", "\\", "|", "/"};
	static int currentIndicator = 0;
	
	public static int coreCount = 0;
	
	public static void main(String[] args) {
		
		try {
			VirgoAPI api = new VirgoAPI.Builder().port(27750).build();
			GeoWeb.getInstance().connectTo("35.164.199.2", 25565);
			
			while(api.getPeersWatcher().getPeersByScore().size() == 0) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {}
			}
			
			new Thread(new MiningInformationsUpdater()).start();
			
			while(difficulty == 0)
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			coreCount = Runtime.getRuntime().availableProcessors();
			
			long baseNonce = MAX/coreCount;
			
			for(int i = 0; i < coreCount; i++) {
				Worker worker = new Worker(baseNonce*i);
				workers.add(worker);
				new Thread(worker).start();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PortUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//ugly debug stats
		new Timer().scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				System.out.print("\r " + hashes + "h/s | current difficulty: " + difficulty + " | " + coreCount+"/"+workers.size() + " workers " + runningIndicators[currentIndicator]);
				hashes = 0;
				currentIndicator++;
				if(currentIndicator > 3)
					currentIndicator = 0;
			}
			
		}, 1000l, 1000l);
		
	}
	
}
