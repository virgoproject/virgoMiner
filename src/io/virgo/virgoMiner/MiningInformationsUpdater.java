package io.virgo.virgoMiner;

import org.json.JSONArray;

import io.virgo.geoWeb.ResponseCode;
import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.virgoAPI.crypto.TxOutput;
import io.virgo.virgoAPI.requestsResponses.GetPoWInformationsResponse;

public class MiningInformationsUpdater implements Runnable {

	@Override
	public void run() {
		
		TxOutput out = new TxOutput("V2N5tYdd1Cm1xqxQDsY15x9ED8kyAUvjbWv", (long) (5 * Math.pow(10, VirgoAPI.DECIMALS)));
		JSONArray outputs = new JSONArray();
		outputs.put(out.toString());
		
		Main.outputs = outputs;
		
		while(!Thread.currentThread().isInterrupted()) {
			GetPoWInformationsResponse resp = VirgoAPI.getInstance().getPowInformations();
			if(resp.getResponseCode().equals(ResponseCode.OK)) {
				
				boolean changed = false;
				
				if(!Main.parentBeacon.equals(resp.getParentBeaconUid()))
					changed = true;
				
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
						worker.nonce = worker.baseNonce;
					
					Main.found = false;
				}

			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
			
		}
		
	}

}
