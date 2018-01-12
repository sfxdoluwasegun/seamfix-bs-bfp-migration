package com.seamfix.kyc.bfp.missingmsisdn;

import java.util.List;

import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.constants.StringBfpConstants;
import com.seamfix.kyc.bfp.contract.IBlockingBuffer;
import com.seamfix.kyc.bfp.contract.IProcess;
import com.seamfix.kyc.bfp.enums.BfpProperty;
import com.seamfix.kyc.bfp.service.AppDS;
import com.sf.biocapture.entity.SmsActivationRequest;

/**
*
* @author dawuzi
*
*/
public class MissingMsisdnLoader extends BsClazz implements IProcess, Runnable  {

	private IBlockingBuffer<SmsActivationRequest> buffer;
	private AppDS appDS;
	private BioCache cache;
	private int pageSize;
	private int pageOffSet = 0;
	private int initialOffSet = 0;
	
	public MissingMsisdnLoader(AppDS appDS, BioCache cache, IBlockingBuffer<SmsActivationRequest> buffer) {
		this.buffer = buffer;
		this.appDS = appDS;
		this.cache = cache;
		
		pageSize = appProps.getInt("missing-msisdn-db-paging-size", 10000);
		pageOffSet = appProps.getInt("missing-msisdn-db-paging-offset", 0);
		
		initialOffSet = pageOffSet;		
	}
	
	@Override
	public void process() {
		
		logger.debug("MissingMsisdnLoader process called");
		
		List<SmsActivationRequest> records = appDS.getMissingMsisdnRecords(pageOffSet, pageSize);
		
		int sleepTime = getInt(BfpProperty.MISSING_MSISDN_LOADER_SLEEP_TIME);
		
		logger.debug("msisdn loader sleep time : "+sleepTime);
		
//		while(records != null && !records.isEmpty()){
		while(true){
		
			if(records != null){
				
				for(SmsActivationRequest record : records){
					
					if(record == null){
						continue;
					}
					
					logger.debug("processing "+record.getId());
					
					Long id = record.getId();
					
					String cacheKey = StringBfpConstants.MISSING_MSISDN_QUEUE_KEY_PREFIX+id;
					
					String recordStatus = (String) cache.getItem(cacheKey);
					
					logger.debug("cacheKey : "+cacheKey+" recordStatus : "+recordStatus);
					
	//				already loaded or processed by another instance so skip
					if(recordStatus != null){
						logger.debug("skipping master record id : "+id);
						continue;
					}
					
					logger.debug("record on course for loading : "+id);
					
					if(!buffer.contains(id.toString())){
						boolean inserted = buffer.put(record);
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
				logger.debug("missing msisdn loader about to sleep for "+sleepTime+" second(s). records : "+records);
				sleep(sleepTime);
				logger.debug("resetting off set to the initial off set : "+initialOffSet);
//				an empty records list implies that we have gotten to the end of the records. We reset the offset to check for those left out
				pageOffSet = initialOffSet;
			} else {
				logger.debug("record size : "+records.size());
				pageOffSet += pageSize;
			}

			records = appDS.getMissingMsisdnRecords(pageOffSet, pageSize); 
			logger.debug("fetching. PageOffSet : "+pageOffSet); 
		}
	}

	/**
	 * @param sleepTime 
	 * 
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
