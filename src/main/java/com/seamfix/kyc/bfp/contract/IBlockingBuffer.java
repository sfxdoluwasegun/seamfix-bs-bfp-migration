package com.seamfix.kyc.bfp.contract;

public interface IBlockingBuffer<T> {

	/**
	 * @param item
	 * @return true if added, false otherwise
	 */
	public boolean put(T item);

	/**
	 * Retrieves and remove
	 * @return T or null if queue is empty
	 */
	public T get();

	/**
	 *
	 * @return size of the buffer
	 */
	public boolean isFull();

	/**
	 *
	 * @param itemId
	 * @return true if the buffer already contains the id
	 */
	public boolean contains(String itemId);

}
