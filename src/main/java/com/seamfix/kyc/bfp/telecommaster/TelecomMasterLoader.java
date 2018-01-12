package com.seamfix.kyc.bfp.telecommaster;

import java.util.List;

import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.constants.StringBfpConstants;
import com.seamfix.kyc.bfp.contract.IBlockingBuffer;
import com.seamfix.kyc.bfp.contract.IProcess;
import com.seamfix.kyc.bfp.enums.BfpProperty;
import com.seamfix.kyc.bfp.service.AppDS;
import com.sf.biocapture.entity.TelecomMasterRecords;

/**
 *
 * @author dawuzi
 *
 */
public class TelecomMasterLoader extends BsClazz implements IProcess, Runnable {

	private IBlockingBuffer<TelecomMasterRecords> buffer;
	private AppDS appDS;
	private BioCache cache;
	private int pageSize;
	private int pageOffSet = 0;
	private int initialOffSet = 0;

	public TelecomMasterLoader(AppDS appDS, BioCache cache, IBlockingBuffer<TelecomMasterRecords> buffer) {
		this.buffer = buffer;
		this.appDS = appDS;
		this.cache = cache;
		
		pageSize = appProps.getInt("telecom-master-db-paging-size", 1000);
		pageOffSet = appProps.getInt("telecom-master-db-paging-offset", 0);
		
		initialOffSet = pageOffSet;
	}

	@Override
	public void process() {
		
		logger.debug("TelecomMasterLoader process called");
		
		List<TelecomMasterRecords> records = appDS.getUnprocessedRecords(pageOffSet, pageSize);
		
		int sleepTime = getInt(BfpProperty.TELECOM_MASTER_LOADER_SLEEP_TIME);
		
		logger.debug("sleep time : "+sleepTime);
		
//		while(records != null && !records.isEmpty()){
		while(true){
		
			if(records != null){
				for(TelecomMasterRecords masterRecord : records){
					
					if(masterRecord == null){
						continue;
					}
					
					logger.debug("processing "+masterRecord.getId());
					
					Long id = masterRecord.getId();
					
					String cacheKey = StringBfpConstants.TELECOM_MASTER_QUEUE_KEY_PREFIX+id;
					
					logger.debug("cacheKey : "+cacheKey);
					
					String recordStatus = (String) cache.getItem(cacheKey);
					
					logger.debug("recordStatus : "+recordStatus);
					
	//				already loaded or processed by another instance so skip
					if(recordStatus != null){
						logger.debug("skipping master record id : "+id);
						continue;
					}
					
					logger.debug("master record on course for loading : "+id);
					
					if(!buffer.contains(id.toString())){
						boolean inserted = buffer.put(masterRecord);
						logger.debug("inserted : "+inserted);
						if(inserted){
							cache.setItem(cacheKey, StringBfpConstants.CACHE_LOADED, 86400); 
						} else {
							logger.debug("nothing set in cache");
						}
					} else {
						logger.debug("buffer.contains(id.toString()) for record with id : "+id);
					}
				}
			}
		
			if(records == null || records.isEmpty()){
				logger.debug("about to sleep for "+sleepTime+" second(s). records : "+records);
				sleep(sleepTime);
				logger.debug("resetting off set to the initial off set : "+initialOffSet);
//				an empty records list implies that we have gotten to the end of the records. We reset the offset to check for those left out
				pageOffSet = initialOffSet;
			} else {
				logger.debug("record size : "+records.size());
				pageOffSet += pageSize;
			}

			records = appDS.getUnprocessedRecords(pageOffSet, pageSize); 
			logger.debug("fetching. PageOffSet : "+pageOffSet); 
		}
	}

	/**
	 * @param sleepTime
	 */
	private void sleep(int sleepTime) {
		try {
			Thread.sleep(sleepTime * 1000);
		} catch (InterruptedException e) {
			logger.error("Missing Msisdn Worker sleeping", e);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		process();
	}
	
}
