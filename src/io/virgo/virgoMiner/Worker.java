package io.virgo.virgoMiner;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;

import io.virgo.geoWeb.GeoWeb;
import io.virgo.virgoCryptoLib.Sha256;
import io.virgo.virgoCryptoLib.Sha256Hash;

public class Worker implements Runnable {

	long baseNonce = 0;
	volatile long nonce = 0;
	
	public Worker(long baseNonce) {
		this.baseNonce = baseNonce;
	}
	
	@Override
	public void run() {
		
		while(!Thread.currentThread().isInterrupted()) {
			if(Main.found)
				continue;
				
			long date = System.currentTimeMillis();
			
			Sha256Hash txHash = Sha256.getDoubleHash((
					Main.header
					+ date
					+ nonce
					).getBytes());
			
			
			Main.hashes++;
			
			if(txHash.toLong() < Main.MAX/Main.difficulty) {

				Main.found = true;
				
				JSONObject transaction = new JSONObject();
				transaction.put("parents", Main.parents);
				transaction.put("outputs", Main.outputs);
				transaction.put("parentBeacon", Main.parentBeacon);
				transaction.put("date", date);
				transaction.put("nonce", nonce);
				
				JSONObject txMessage = new JSONObject();
				txMessage.put("command", "txs");
				txMessage.put("txs", new JSONArray(Arrays.asList(transaction)));
				
				GeoWeb.getInstance().broadCast(txMessage);
				continue;
			}
			
			nonce++;
		}
		
	}

	
	
}
