package com.seamfix.kyc.bfp.contract;

import java.io.File;
import java.io.IOException;

import org.keyczar.Crypter;

public interface IWorker {

	public SyncItem getSyncItem();

	/**
	 * Does record extraction and saving
	 * @param item
	 * @return true if saving was successful, false if otherwise
	 */
//	public boolean deserialize(SyncItem item);
        
        public SyncItem deserialize(SyncItem syncItem);
        
        /**
         * handles saving of records and other key processes
         * @param syncItem
         * @return boolean
         */
        public SyncItem process(SyncItem syncItem);

	/**
	 * Handle sending of sms etc
	 * @param item
	 */
	public void postProcessing(SyncItem item);

	/**
	 * Backs up processed records
	 * @param item
	 * @return
	 */
	public boolean backup(SyncItem item);

	/**
	 * Handles record validation
	 * @param item
	 */
	public SyncItem validate(SyncItem item);

	public void setBuffer(IBlockingBuffer<SyncItem> buffer);

	public boolean wasProcessed(String itemId);

	public Crypter getEncryptionEngine();

	public byte[] decrypt(File sync) throws IOException;

}
