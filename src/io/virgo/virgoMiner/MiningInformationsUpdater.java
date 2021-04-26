package io.virgo.virgoMiner;

import java.util.Arrays;

import org.json.JSONArray;

import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.virgoAPI.crypto.TxOutput;
import io.virgo.virgoAPI.network.ResponseCode;
import io.virgo.virgoAPI.requestsResponses.GetPoWInformationsResponse;

public class MiningInformationsUpdater implements Runnable {

	@Override
	public void run() {
		
		TxOutput out = new TxOutput("V2FRYJPZeSKW6cnam79ZHyaaYxRbzt9fVXG", (long) (5 * Math.pow(10, VirgoAPI.DECIMALS)));
		JSONArray outputs = new JSONArray();
		outputs.put(out.toString());
		
		Main.outputs = outputs;
		
		while(!Thread.currentThread().isInterrupted()) {
			GetPoWInformationsResponse resp = VirgoAPI.getInstance().getPowInformations();
			if(resp.getResponseCode() == ResponseCode.OK) {
				
				boolean changed = false;
				
				if(!Main.parentBeacon.equals(resp.getParentBeaconUid()))
					changed = true;
				
				if(!Arrays.equals(Main.key, resp.getRandomXKey().getBytes())) {
					Main.key = resp.getRandomXKey().getBytes();
					if(Main.rx != null) {
						changed = true;
						Main.found = true;
						Main.rx.changeKey(resp.getRandomXKey().getBytes());
					}

				}
				
				Main.parentBeacon = resp.getParentBeaconUid();
				Main.difficulty = resp.getDifficulty();
				
				JSONArray parents = new JSONArray();
				for(String parent : resp.getParents())
					parents.put(parent);
				
				if(Main.parents != null && !parents.toString().equals(Main.parents.toString()))
					changed = true;
				
				Main.parents = parents;
				
				Main.header = Main.parents.toString()
						+ Main.outputs.toString()
						+ Main.parentBeacon;
			
				if(changed == true) {
					for(Worker worker : Main.workers)
						worker.nonce = 0;
					
					Main.found = false;
				}

			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
			
		}
		
	}

}
