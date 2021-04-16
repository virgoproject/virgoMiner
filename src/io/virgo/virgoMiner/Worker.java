package io.virgo.virgoMiner;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import io.virgo.randomX.RandomX_VM;
import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.virgoCryptoLib.Converter;

public class Worker implements Runnable {

	long workerId = 0;
	volatile long nonce = 0;
	
	RandomX_VM vm;
	
	public Worker(long workerId, RandomX_VM vm) {
		this.workerId = workerId;
		this.vm = vm;
	}
	
	@Override
	public void run() {
		
		while(!Thread.currentThread().isInterrupted()) {
			if(Main.found)
				continue;
				
			long date = System.currentTimeMillis();
			
			byte[] txHash = vm.getHash((
					Main.header
					+ date
					+ Main.machineUid
					+ workerId
					+ nonce
					).getBytes());
			
			
			Main.hashes++;
						
			byte[] hashPadded = new byte[txHash.length + 1];
			for (int i = 0; i < txHash.length; i++) {
				hashPadded[i + 1] = txHash[i];
			}
			
			BigInteger hashValue = new BigInteger(ByteBuffer.wrap(hashPadded).array());
						
			if(hashValue.compareTo(Main.MAX.divide(Main.difficulty)) < 0) {
				System.out.println("found: " + hashValue + " " + Main.MAX.divide(Main.difficulty));
				Main.found = true;
				
				JSONObject transaction = new JSONObject();
				transaction.put("parents", Main.parents);
				transaction.put("outputs", Main.outputs);
				transaction.put("parentBeacon", Main.parentBeacon);
				transaction.put("date", date);
				transaction.put("nonce", ""+Main.machineUid+workerId+nonce);

				VirgoAPI.getInstance().broadcastTransaction(transaction);
				
				continue;
			}
			
			nonce++;
		}
		
	}

	
	
}
