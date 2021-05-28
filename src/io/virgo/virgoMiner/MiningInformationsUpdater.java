package io.virgo.virgoMiner;

import java.util.Arrays;

import org.json.JSONArray;

import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.virgoAPI.crypto.TxOutput;
import io.virgo.virgoAPI.network.ResponseCode;
import io.virgo.virgoAPI.requestsResponses.GetPoWInformationsResponse;
import io.virgo.virgoCryptoLib.Sha256Hash;

public class MiningInformationsUpdater implements Runnable {

	@Override
	public void run() {
		
		TxOutput out = new TxOutput(Main.rewardAddress, (long) (5 * Math.pow(10, VirgoAPI.DECIMALS)));
		JSONArray outputs = new JSONArray();
		outputs.put(out.toString());
		
		Main.outputs = outputs;
		
		while(!Thread.currentThread().isInterrupted()) {
			GetPoWInformationsResponse resp = VirgoAPI.getInstance().getPowInformations();
			if(resp.getResponseCode() == ResponseCode.OK) {

				boolean changed = false;
				
			
				if(Main.parentBeacon != null && !Main.parentBeacon.equals(resp.getParentBeaconUid()))
					changed = true;
				
				if(!Arrays.equals(Main.key, resp.getRandomXKey().toBytes())) {
					Main.key = resp.getRandomXKey().toBytes();
					if(Main.rx != null) {
						changed = true;
						Main.found = true;
						Main.rx.changeKey(resp.getRandomXKey().toBytes());
					}

				}
				
				Main.parentBeacon = resp.getParentBeaconUid();
				Main.difficulty = resp.getDifficulty();
				
				JSONArray parents = new JSONArray();
				for(Sha256Hash parent : resp.getParents())
					parents.put(parent.toString());
								
				if(Main.parents != null && !parents.toString().equals(Main.parents.toString()))
					changed = true;
				
				Main.parents = parents;
				
				Main.header = (Main.parents.toString() + Main.outputs.toString()).getBytes();
			
				if(changed == true) {
					for(Worker worker : Main.workers)
						worker.nonce = 0;
					
					Main.found = false;
				}

			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
			
		}
		
	}

}
