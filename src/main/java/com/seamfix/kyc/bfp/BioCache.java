package com.seamfix.kyc.bfp;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.AddrUtil;

/**
 * High level abstraction for low level memcached activities
 * @author nuke
 *
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class BioCache extends BsClazz{

	private static final String EXCEPTION_TITLE = "Exception ";

	protected String addressList;

	protected MemcachedClient client;

	@PostConstruct
	public void init(){
		logger.debug("Initializing Cache Mechanism");
		addressList = appProps.getProperty("cache-server-list", "localhost:11211");
		connect();
	}

	@PreDestroy
	public void end(){

	}

	protected void connect(){
		try {
			client = new XMemcachedClientBuilder(AddrUtil.getAddresses(addressList)).build();
			logger.debug("Connected to memcached servers: " + addressList);
		} catch (IOException e) {
			logger.error(EXCEPTION_TITLE, e);
		}
	}

	private boolean validateConnection(){
		if(client == null){
			logger.debug("Memcached server connection was unsuccessful, retrying: " + addressList);
			connect();
		}
		return client != null;
	}

	/**
	 * Get an item from memcached
	 * @param key
	 * @return
	 */
	public Object getItem(String key){
		if(!validateConnection()){
			return null;
		}

		Object obj = null;
		try {
                        key = key == null ? key : key.replaceAll(" ", "");
			obj = client.get(key);
		} catch (TimeoutException | InterruptedException | MemcachedException e) {
			logger.error(EXCEPTION_TITLE, e);
		}
		return obj;
	}

	/**
	 * Gets an item from memecached
     * @param <T>
	 * @param key
	 * @param returnClazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T getItem(String key, Class<T> returnClazz){
		T obj = (T) getItem(key);
		return obj;
	}

	/**
	 * Adds/updates an item on Memcached
	 * @param key unique key
	 * @param item target item to cache
	 * @param age seconds to keep in cache
	 * @return
	 */
	public boolean setItem(String key, Object item, Integer age){
		if(!validateConnection()){
			return false;
		}
		boolean success = false;
		try {
                        key = key == null ? key : key.replaceAll(" ", "");
			success = client.set(key, age, item);
		} catch (TimeoutException | InterruptedException | MemcachedException e) {
			logger.error(EXCEPTION_TITLE, e);
		}
		return success;
	}

	/**
	 * Removes an item from memcached
	 * @param key
	 * @return
	 */
	public boolean removeItem(String key){
		if(!validateConnection()){
			return false;
		}
		boolean success = false;
		try {
			success = client.delete(key);
		} catch (TimeoutException | InterruptedException | MemcachedException e) {
			logger.error(EXCEPTION_TITLE, e);
		}
		return success;
	}

}
