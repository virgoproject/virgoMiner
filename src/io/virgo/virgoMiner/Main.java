package io.virgo.virgoMiner;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;

import io.virgo.randomX.RandomX;
import io.virgo.randomX.RandomX.Flag;
import io.virgo.virgoAPI.VirgoAPI;

public class Main {

	public static volatile String parentBeacon = "";
	public static volatile String header = "";
	public static volatile JSONArray parents = null;
	public static volatile JSONArray outputs = null;
	public static volatile BigInteger difficulty = BigInteger.ZERO;
	public static volatile boolean found = true;
	
	public static byte[] key = null;
	
	public static ArrayList<Worker> workers = new ArrayList<Worker>();
	
	public static final BigInteger MAX = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639935");
	
	public static volatile long hashes = 0;
	
	static String[] runningIndicators = {"-", "\\", "|", "/"};
	static int currentIndicator = 0;
	
	public static int coreCount = 0;
	public static int machineUid = new Random().nextInt(100000000);
	
	public static RandomX rx;
	
	public static void main(String[] args) {
		
		System.out.println("Virgo Miner v0.0.1");
		
		try {
			System.out.println("Connecting to data provider");
			VirgoAPI api = new VirgoAPI.Builder().build();
			api.addProvider(new URL("http://35.164.199.2:8000/"));
			while(api.getProvidersWatcher().getProvidersByScore().size() == 0) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
			}
			System.out.println("Retrieving data");
			new Thread(new MiningInformationsUpdater()).start();
			
			while(difficulty.compareTo(BigInteger.ZERO) == 0)
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			System.out.println("Initializing RandomX");
			rx = new RandomX.Builder().flag(Flag.FULL_MEM).flag(Flag.ARGON2).flag(Flag.HARD_AES).flag(Flag.JIT).fastInit(true).build();
			rx.init(key);
			
			coreCount = Runtime.getRuntime().availableProcessors();
			
			
			for(int i = 0; i < coreCount; i++) {
				Worker worker = new Worker(i, rx.createVM());
				workers.add(worker);
				new Thread(worker).start();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//ugly debug stats
		new Timer().scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				System.out.print("\r " + hashes + "h/s | current difficulty: " + difficulty + " | " + coreCount+"/"+workers.size() + " workers " + runningIndicators[currentIndicator] + " " + new String(key));
				hashes = 0;
				currentIndicator++;
				if(currentIndicator > 3)
					currentIndicator = 0;
			}
			
		}, 1000l, 1000l);
		
	}
	
}
