package com.seamfix.kyc.bfp.telecommaster;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.constants.StringBfpConstants;
import com.seamfix.kyc.bfp.contract.IBlockingBuffer;
import com.seamfix.kyc.bfp.contract.IProcess;
import com.seamfix.kyc.bfp.enums.BfpProperty;
import com.seamfix.kyc.bfp.service.AppDS;
import com.sf.biocapture.entity.SmsActivationRequest;
import com.sf.biocapture.entity.TelecomMasterRecords;
import com.sf.biocapture.entity.enums.ProcessingStatusCode;

/**
*
* @author dawuzi
*
*/
@SuppressWarnings("PMD")
public class TelecomMasterWorker extends BsClazz implements IProcess, Runnable {

	private IBlockingBuffer<TelecomMasterRecords> buffer;
	private AppDS appDS;
	private BioCache cache;
	private final Pattern SPACE_PATTERN = Pattern.compile(" ");
	
	
	public TelecomMasterWorker(AppDS appDS, BioCache cache, IBlockingBuffer<TelecomMasterRecords> buffer) {
		this.buffer = buffer;
		this.appDS = appDS;
		this.cache = cache;
	}


	@Override
	public void process() {
		
		logger.debug(getClass().getName()+" process called");
		
		TelecomMasterRecords record = buffer.get();
		
		logger.debug("record : "+record);
		
		String cacheKey;
		
		int sleepTime = getInt(BfpProperty.TELECOM_MASTER_WORKER_SLEEP_TIME);
		
		logger.debug("sleep time : "+sleepTime);
		
//		while(telecomMasterRecord != null){
		while(true){
			
			if(record == null){
				
				logger.debug("record is null.  About to sleep for "+sleepTime+" second(s)");
				
				sleep(sleepTime);
				
				record = buffer.get();
				
				continue;
			}
			
			cacheKey = StringBfpConstants.TELECOM_MASTER_QUEUE_KEY_PREFIX+record.getId();
			
			try {
				logger.debug("handling record : "+record.getId());
				handleTelecomMasterRecord(record, cacheKey);
			} catch (Exception e) {
				logger.error("error handling telecom master record : "+record.getId(), e);
				updateRecord(record, ProcessingStatusCode.EXCEPTION, cacheKey);
			}
			
			record = buffer.get();
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


	private void updateRecord(TelecomMasterRecords telecomMasterRecord, ProcessingStatusCode code, String cacheKey) {
		telecomMasterRecord.setProcessingDate(new Timestamp(new Date().getTime()));
		telecomMasterRecord.setProcessingStatus(code);
		appDS.updateEntity(telecomMasterRecord);
		if(code == ProcessingStatusCode.PROCESSED){
			cache.setItem(cacheKey, StringBfpConstants.CACHE_PROCESSED, 86400); 
		} else {
			cache.setItem(cacheKey, StringBfpConstants.CACHE_FAILED, 86400); 
		}
	}


	private void handleTelecomMasterRecord(TelecomMasterRecords telecomMasterRecord, String cacheKey) {
		
		String status = (String) cache.getItem(cacheKey);
		
		logger.debug("status : "+status);
		
//		they have been processed by some other worker probably on another instance so we skip
		if(status.equals(StringBfpConstants.CACHE_PROCESSED)
				|| status.equals(StringBfpConstants.CACHE_FAILED)){
			
			logger.debug("skipping record : "+telecomMasterRecord.getId());
			
			return;
		}
		
		logger.debug("so we proceed with the record record : "+telecomMasterRecord.getId());
		
		String primaryMsisdn = telecomMasterRecord.getPrimaryMsisdn();
		String uniqueId = telecomMasterRecord.getPinRef();
		String msisdn = telecomMasterRecord.getMsisdn();
		
		
		if(msisdn == null || msisdn.trim().isEmpty()){
			updateRecord(telecomMasterRecord, ProcessingStatusCode.EMPTY_MSISDN, cacheKey); 
			return;
		}
		
		if(primaryMsisdn == null || primaryMsisdn.trim().isEmpty()){
			updateRecord(telecomMasterRecord, ProcessingStatusCode.EMPTY_PRIMARY_MSISDN, cacheKey); 
			return;
		}
		
		if(uniqueId == null || uniqueId.trim().isEmpty()){
			updateRecord(telecomMasterRecord, ProcessingStatusCode.EMPTY_UNIQUE_ID, cacheKey); 
			return;
		}
		
		primaryMsisdn = getFixedMsisdn(primaryMsisdn);
		msisdn = getFixedMsisdn(msisdn);
		
		telecomMasterRecord.setMsisdn(msisdn);
		telecomMasterRecord.setPrimaryMsisdn(primaryMsisdn);
		
		if(primaryMsisdn.equals(msisdn)){
			updateRecord(telecomMasterRecord, ProcessingStatusCode.EQUAL_MSISDNS, cacheKey); 
			return;
		}
		
		if(isDuplicateUniqueIdMsisdnPair(telecomMasterRecord)){
			updateRecord(telecomMasterRecord, ProcessingStatusCode.DUPLICATE_UNIQUE_ID_MSISDN_PAIR, cacheKey);
			return;
		}
		
		logger.debug("cacheKey : "+cacheKey+", primaryMsisdn : "+primaryMsisdn+", uniqueId : "+uniqueId+", msisdn : "+msisdn);
		
//		the unique id and primary msisdn is missing so we flag it as an error
		if(!exists(StringBfpConstants.TELECOM_MASTER_UID_PRIMARY_MSISDN_KEY_PREFIX, uniqueId, primaryMsisdn)){
			updateRecord(telecomMasterRecord, ProcessingStatusCode.UNAVAILABLE_UNIQUE_ID_PRIMARY_MSISDN_MATCH, cacheKey);
			return;
		}
		
		if(exists(StringBfpConstants.TELECOM_MASTER_UID_MSISDN_KEY_PREFIX, uniqueId, msisdn)){
			updateRecord(telecomMasterRecord, ProcessingStatusCode.UNIQUE_ID_MSISDN_PAIR_ALREADY_EXISTS, cacheKey);
			return;
		}
		
		ProcessingStatusCode saveStatus = appDS.saveRecordTelecomMaster(telecomMasterRecord);
		
		logger.debug("saveStatus : "+saveStatus+" for record : "+telecomMasterRecord.getId());
		
		updateRecord(telecomMasterRecord, saveStatus, cacheKey);
	}


	/**
	 * @param telecomMasterRecord
	 * @return
	 */
	private boolean isDuplicateUniqueIdMsisdnPair(TelecomMasterRecords record) {

		String pCacheKey = getProcessedCacheKey(record);
		
		Long lowestTelecomMasterRecordId = (Long) cache.getItem(pCacheKey);
		
		logger.debug("lowestTelecomMasterRecordId : "+lowestTelecomMasterRecordId);
		
		if(lowestTelecomMasterRecordId == null){
		
			String unfixedMsisdn = getUnfixedMsisdn(record.getMsisdn()); 
			
			List<TelecomMasterRecords> recordsWithLowestId = appDS.getTelecomMasterRecordsWithLowestId(record.getPinRef(), record.getMsisdn(), unfixedMsisdn, 0, 1);
	
//			this should never happen
			if(recordsWithLowestId.isEmpty()){
				return true;
			}
			
			TelecomMasterRecords lowestIdRecord = recordsWithLowestId.get(0);
			
			cache.setItem(pCacheKey, lowestIdRecord.getId(), 86400);
			
			lowestTelecomMasterRecordId = lowestIdRecord.getId();
		}
		
		logger.debug("lowestTelecomMasterRecordId : "+lowestTelecomMasterRecordId);
		
		if(lowestTelecomMasterRecordId.equals(record.getId())){
			return false;
		}
		
		return true;
	}


	/**
	 * @param msisdn
	 * @return
	 */
	private String getUnfixedMsisdn(String msisdn) {
		if(msisdn.startsWith("0")){
			msisdn = msisdn.substring(1);
		}
		return msisdn;
	}


	/**
	 * @param uniqueId
	 * @param msisdn
	 * @return true if the unique id/msisdn pair exists in sms activation request table. Uses cache to enhance performance based on the prefix argument
	 */
	private boolean exists(String prefix, String uniqueId, String msisdn) {
		
		String cacheKey = prefix 
				+ SPACE_PATTERN.matcher(uniqueId).replaceAll("-") 
				+ SPACE_PATTERN.matcher(msisdn).replaceAll("-");
		
		String status = (String) cache.getItem(cacheKey);
		
		logger.debug("prefix : "+prefix+", status : "+status);
		
		if(status == null){
			SmsActivationRequest smsActivationRequest = appDS.getSmsActivationRequest(uniqueId, msisdn);
			
			if(smsActivationRequest != null){
				status = "Y";
			} else {
				status = "N";
			}
			
			Integer cacheTime = appProps.getInt("telecom-master-unique-id-msisdn-cache-time-out", 86400);

			cache.setItem(cacheKey, status, cacheTime);
		}
		
		logger.debug("prefix : "+prefix+", status : "+status);
		
		boolean exists = status.equals("Y");
		
		return exists;
	}

	private String getProcessedCacheKey(TelecomMasterRecords record){
		
		String uniqueId = record.getPinRef();
		String msisdn = record.getMsisdn();
		
		String cacheKey = StringBfpConstants.TELECOM_MASTER_PROCESSED_KEY_PREFIX  
				+ SPACE_PATTERN.matcher(uniqueId).replaceAll("-") 
				+ SPACE_PATTERN.matcher(msisdn).replaceAll("-");
		
		return cacheKey;
	}	

	/**
	 * @param msisdn
	 * @return
	 */
	private String getFixedMsisdn(String msisdn) {
		if(!msisdn.startsWith("0")){
			msisdn = "0" + msisdn;
		}
		return msisdn;
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		process();
	}
}
