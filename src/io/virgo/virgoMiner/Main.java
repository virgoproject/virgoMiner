package io.virgo.virgoMiner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.virgo.randomX.RandomX;
import io.virgo.randomX.RandomX.Flag;
import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.virgoCryptoLib.Utils;
import io.virgo.virgoMiner.Utils.Miscellaneous;

public class Main {

	public static final String VERSION = "0.0.2";
	
	private static ArrayList<String> providersList = new ArrayList<String>(Arrays.asList("http://35.164.199.2:8000/"));
	public static String rewardAddress = "V2FRYJPZeSKW6cnam79ZHyaaYxRbzt9fVXG";
	
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
		
		System.out.println("Virgo Miner v"+VERSION);
		
		loadConfig();
		
		try {
			
			//connect to nodes
			VirgoAPI api = new VirgoAPI.Builder().build();
			for(String provider : providersList)
				api.addProvider(new URL(provider));
			
			
			System.out.println("Connecting to Virgo network");
			while(api.getProvidersWatcher().getProvidersByScore().size() == 0) {
				try {
					Thread.sleep(50);
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
			rx = new RandomX.Builder().recommendedFlags().flag(Flag.FULL_MEM).fastInit(true).build();
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
				System.out.print("\r " + hashes + "h/s | current difficulty: " + difficulty + " " + runningIndicators[currentIndicator]);
				hashes = 0;
				currentIndicator++;
				if(currentIndicator > 3)
					currentIndicator = 0;
			}
			
		}, 1000l, 1000l);
		
	}
	
	/**
	 * searches for config file and load it, if not found create one using default values
	 */
	private static void loadConfig() {
		System.out.println("Loading config..");
		
		File configFile = new File("config.json");
		
		if(configFile.exists()) {
			  
			String configString = Miscellaneous.fileToString("config.json");
			
			try {
				JSONObject config = new JSONObject(configString);
				
				JSONArray providers = config.getJSONArray("providers");
				for(int i = 0; i< providers.length(); i++)
					providersList.add(providers.getString(i));
				
				String address = config.getString("address");
				if(Utils.validateAddress(address, VirgoAPI.ADDR_IDENTIFIER))
					rewardAddress = address;
				else
					System.out.println("Warning: invalid reward address, miner will give reward to Virgo's faucet");
					
				System.out.println("config.json successfully loaded !");
			}catch(JSONException e) {
				System.out.println("Error in config.json, using default values");
			}
			
			return;
			
		}
		
		System.out.println("config.json don't exist, creating one with default values");
		
		try {
			configFile.createNewFile();
			
			JSONObject defaultConfig = new JSONObject();

			JSONArray providers = new JSONArray(providersList);
			defaultConfig.put("providers", providers);
			
			defaultConfig.put("address", rewardAddress);
			System.out.println("Warning: reward address not set, miner will give reward to Virgo's faucet");
			
			FileWriter writer = new FileWriter(configFile);
			writer.write(defaultConfig.toString(4));
			writer.close();
		} catch (IOException e) {
			System.out.println("Unable to write config.json, please check permissions and disk usage\n"
					+ "Launching with default values");
			e.printStackTrace();
		}
	}
	
}
