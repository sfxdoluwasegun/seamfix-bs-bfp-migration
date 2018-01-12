package com.seamfix.kyc.bfp.buffer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.contract.IBlockingBuffer;

public abstract class AbstractBlockingBuffer<T> extends BsClazz implements IBlockingBuffer<T> {

	private ConcurrentLinkedQueue<T> localQueue;
	private Map<String, Integer> keys;

	private int queueSize = 0;
	private int capacity;

	public AbstractBlockingBuffer() {
		this(2000);
	}

	/**
	 * Constructs a new Blocking Buffer
	 * @param capacity max number of items that can be contained in the queue
	 */
	public AbstractBlockingBuffer(Integer capacity){
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
	public boolean put(T item){
        if(isFull()){
        	return false;
        }
		if(contains(getId(item))){
			return false;
		}
		boolean offered = localQueue.offer(item);
		if(offered){
			keys.put(getId(item), 1);
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
    public T get() {
    	T item = null;
		item = localQueue.poll();
		if(item != null){
			keys.remove(getId(item));
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
	
	public abstract String getId(T item);

}
