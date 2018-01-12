package com.seamfix.kyc.bfp.contract;

import java.io.File;
import java.util.List;

public interface IReader {

	/**
	 * Checks if the item with the provided id has already been queued
	 * @param itemId id representing the current item
	 * @return true if item has been previously queued or false if it has not
	 */
	public boolean isQueued(String itemId);

	/**
	 * Ads an item to the processing queue
	 * @param item
	 * @return
	 */
	public boolean queueItem(SyncItem item);

	/**
	 * Specifies the location to search for sync files
	 * @param src
	 */
	public void sourceLocation(String src);

	/**
	 * Checks if the source location is readable
	 * @return true if the location can be read and written to, false if t cannot
	 */
	public boolean isProcessable();

	public List<SyncItem> readSyncs();

	public File getLocation();

	/**
	 * Sets the buffer to use in queuing records
	 * @param buffer
	 */
	public void setBuffer(IBlockingBuffer<SyncItem> buffer);

}
