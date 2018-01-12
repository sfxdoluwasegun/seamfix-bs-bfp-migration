package com.seamfix.kyc.bfp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.seamfix.kyc.bfp.contract.IBlockingBuffer;
import com.seamfix.kyc.bfp.contract.SyncItem;

/**
 *
 * @author Ogwara O. Rowland
 */
public class BlockingBuffer extends BsClazz implements IBlockingBuffer<SyncItem>{

	private ConcurrentLinkedQueue<SyncItem> localQueue;
	private Map<String, Integer> keys;

	private int queueSize = 0;
	private int capacity;

	public BlockingBuffer() {
		this(2000);
	}

	/**
	 * Constructs a new Blocking Buffer
	 * @param capacity max number of items that can be contained in the queue
	 */
	public BlockingBuffer(Integer capacity){
		this.capacity = capacity + capacity / 4;
		localQueue = new ConcurrentLinkedQueue<>();
		this.keys = new HashMap<>();
	}

	/**
	 *
	 * @param item
	 * @return true if added, false otherwise
	 */
        @Override
	public boolean put(SyncItem item){
        if(isFull()){
        	return false;
        }
		if(contains(item.getItemId())){
			return false;
		}
		boolean offered = localQueue.offer(item);
		if(offered){
			keys.put(item.getItemId(), 1);
			queueSize += 1;
		}
		logger.debug("Queue Size: " + queueSize);
		return offered;
	}

	/**
	 * Retrieves and remove
	 * @return SyncItem or null if queue is empty
	 */
        @Override
	public SyncItem get(){
		SyncItem item = null;
		item = localQueue.poll();
		if(item != null){
			keys.remove(item.getItemId());
			queueSize -= 1;
		}

		return item;
	}

	@Override
	public boolean isFull() {
		return queueSize >= capacity;
	}

	@Override
	public boolean contains(String itemId) {
		return keys.get(itemId) != null;
	}

}
