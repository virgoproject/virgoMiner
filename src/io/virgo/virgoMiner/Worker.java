package io.virgo.virgoMiner;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import io.virgo.randomX.RandomX_VM;
import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.virgoCryptoLib.Converter;
import io.virgo.virgoCryptoLib.Sha256;
import io.virgo.virgoCryptoLib.Sha256Hash;

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
			
			Sha256Hash txHash = Sha256.getDoubleHash(Converter.concatByteArrays(Main.header,
					Main.parentBeacon.toBytes(), Converter.longToBytes(date), Converter.longToBytes(Main.machineUid+workerId+nonce)));
			
			byte[] randomXHash = vm.getHash(txHash.toBytes());
			
			Main.hashes++;
						
			byte[] hashPadded = new byte[randomXHash.length + 1];
			for (int i = 0; i < randomXHash.length; i++) {
				hashPadded[i + 1] = randomXHash[i];
			}
			
			BigInteger hashValue = new BigInteger(ByteBuffer.wrap(hashPadded).array());
						
			if(hashValue.compareTo(Main.MAX.divide(Main.difficulty)) < 0) {
				System.out.println("\n found: " + hashValue + " " + Main.MAX.divide(Main.difficulty) + " " + Main.parentBeacon.toString());
				Main.found = true;
				
				JSONObject transaction = new JSONObject();
				transaction.put("parents", Main.parents);
				transaction.put("outputs", Main.outputs);
				transaction.put("parentBeacon", Main.parentBeacon.toString());
				transaction.put("date", date);
				transaction.put("nonce", Converter.bytesToHex(Converter.longToBytes(Main.machineUid+workerId+nonce)));

				VirgoAPI.getInstance().broadcastTransaction(transaction);
				
				continue;
			}
			
			nonce++;
		}
		
	}

	
	
}
