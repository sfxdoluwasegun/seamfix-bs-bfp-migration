package com.seamfix.kyc.bfp.missingmsisdn;

import java.util.List;
import java.util.UUID;

import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.constants.StringBfpConstants;
import com.seamfix.kyc.bfp.contract.IBlockingBuffer;
import com.seamfix.kyc.bfp.contract.IProcess;
import com.seamfix.kyc.bfp.enums.BfpProperty;
import com.seamfix.kyc.bfp.service.AppDS;
import com.sf.biocapture.entity.MsisdnDetail;
import com.sf.biocapture.entity.SmsActivationRequest;
import com.sf.biocapture.entity.enums.SarMsisdnUpdateStatusCode;

/**
*
* @author dawuzi
*
*/
public class MissingMsisdnWorker extends BsClazz implements IProcess, Runnable {

	private IBlockingBuffer<SmsActivationRequest> buffer;
	private AppDS appDS;
	private BioCache cache;
	private boolean terminate = false;
	private String id = UUID.randomUUID().toString();
	
	
	public MissingMsisdnWorker(AppDS appDS, BioCache cache, IBlockingBuffer<SmsActivationRequest> buffer) {
		this.buffer = buffer;
		this.appDS = appDS;
		this.cache = cache;
	}

	public boolean isTerminate() {
		return terminate;
	}
	public void setTerminate(boolean terminate) {
		this.terminate = terminate;
	}


        @SuppressWarnings("PMD")
	@Override
	public void process() {
		
		logger.debug(getClass().getName()+" process called");
		
		SmsActivationRequest record = buffer.get();
		
		String cacheKey;
		
		int sleepTime = getInt(BfpProperty.MISSING_MSISDN_WORKER_SLEEP_TIME);
		
		logger.debug("sleep time : "+sleepTime);
		
//		terminate only when there is no record in the queue and the terminate flag is set
//		while(record != null || !isTerminate()){
		while(true){
			
			if(record == null){
				
				logger.debug("record is null.  About to sleep for "+sleepTime+" second(s) by id : "+id);
				
				sleep(sleepTime);
				
				record = buffer.get();
				
				continue;
			}
			
			cacheKey = StringBfpConstants.MISSING_MSISDN_QUEUE_KEY_PREFIX+record.getId();
			
			try {
				logger.debug("handling record : "+record.getId());
				handleRecord(record, cacheKey);
			} catch (Exception e) {
				logger.error("error handling record : "+record.getId()+". "+e.getMessage());
				updateRecord(record, SarMsisdnUpdateStatusCode.ERROR, cacheKey);
			}
			
			record = buffer.get();
		}
	}


	/**
	 * @param sleepTime in seconds
	 */
	private void sleep(int sleepTime) {

		try {
			Thread.sleep(sleepTime * 1000);
		} catch (InterruptedException e) {
			logger.error("Missing Msisdn Worker sleeping", e);
		}
	}

	private void handleRecord(SmsActivationRequest record, String cacheKey) {
		String phoneNumber = record.getPhoneNumber();
		String uniqueId = record.getUniqueId();
		String serial = record.getSerialNumber();
		
		String notAvailable = getProperty(BfpProperty.NULL_MSISDN_VALUE);
		
		logger.debug("cacheKey : "+cacheKey+", phoneNumber : "+phoneNumber+", uniqueId : "+uniqueId+", serial : "+serial+", not available val : "+notAvailable);
		
		if(notAvailable.equals(phoneNumber)){
			
			logger.debug("Phone number not updated for record  : "+record.getId());
			
			updateRecord(record, SarMsisdnUpdateStatusCode.ERROR, cacheKey);
			return;
		}
		
		List<MsisdnDetail> msisdnDetails = appDS.getMsisdnDetail(uniqueId, serial);
		
//		there should be only one msisdn detail matching these two criteria
		if(msisdnDetails == null || msisdnDetails.isEmpty() || msisdnDetails.size() > 1){
			
			logger.debug("error msisdnDetails : "+msisdnDetails);
			
			updateRecord(record, SarMsisdnUpdateStatusCode.ERROR, cacheKey);
			return;
		}
		
//		ensure that the msidn start with 0 
		if(!phoneNumber.startsWith("0")){
			phoneNumber = "0" + phoneNumber;
			record.setPhoneNumber(phoneNumber);
		}
		
		MsisdnDetail msisdnDetail = msisdnDetails.get(0);
		
		logger.debug("we are moving on msisdnDetail "+msisdnDetail);
		
		String prevMsisdn = msisdnDetail.getMsisdn();
		
		logger.debug("prevMsisdn : -"+prevMsisdn+"-, notAvailable : -"+notAvailable+"-");
		
//		something is wrong here
		if(!notAvailable.equals(prevMsisdn)){
			updateRecord(record, SarMsisdnUpdateStatusCode.ERROR, cacheKey);
			return;
		}
		
		msisdnDetail.setMsisdn(phoneNumber);
		
		boolean updated = appDS.updateEntity(msisdnDetail);
		
		if(updated){
			updateRecord(record, SarMsisdnUpdateStatusCode.PROCESSED, cacheKey);
		} else {
			updateRecord(record, SarMsisdnUpdateStatusCode.ERROR, cacheKey);
		}
		
		logger.debug("done processing record : "+record.getId());
	}
	
	private void updateRecord(SmsActivationRequest record, SarMsisdnUpdateStatusCode code, String cacheKey) {
		if(code == SarMsisdnUpdateStatusCode.PROCESSED){
			cache.setItem(cacheKey, StringBfpConstants.CACHE_PROCESSED, 86400); 
		} else if(code == SarMsisdnUpdateStatusCode.ERROR){
			cache.setItem(cacheKey, StringBfpConstants.CACHE_FAILED, 86400); 
		} else {
			throw new IllegalStateException("invalid code : "+code);
		}
		record.setMsisdnUpdateStatus(code);
		appDS.updateEntity(record);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		process();
	}

}
