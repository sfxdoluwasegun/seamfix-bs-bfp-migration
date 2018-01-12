package com.seamfix.kyc.bfp.units;

import java.util.UUID;
import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.contract.IWorker;
import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.enums.BfpRejectionReason;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.proxy.BasicData;
import com.seamfix.kyc.bfp.proxy.DynamicData;
import com.seamfix.kyc.bfp.proxy.EnrollmentLog;
import com.seamfix.kyc.bfp.proxy.FileSyncNewEntryMarker;
import com.seamfix.kyc.bfp.proxy.MetaData;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.syncs.SyncFile;
import com.seamfix.kyc.bfp.syncs.SyncUtils;
import com.seamfix.kyc.bfp.validator.ValidationStatusEnum;
import com.sf.biocapture.entity.audit.BfpFailureLog;
import java.util.List;

import nw.commons.StopWatch;

/**
 *
 * @author Ogwara O. Rowland
 *
 */
public class WorkerUnit extends BsClazz implements Runnable {

	private IWorker unit;
	private boolean pauseExecution = false;
	private Long sleepTime = 100L;
        private SyncUtils syncUtils;

	private String workerId;

	private StopWatch stopWatch;

	public WorkerUnit(IWorker unit) {
		this.unit = unit;
		this.stopWatch = new StopWatch();
		workerId = UUID.randomUUID().toString();
                syncUtils = new SyncUtils();
		logger.debug(workerId +  " Initializing Worker Thread " + unit.getClass());
	}

        @SuppressWarnings("PMD")
	@Override
	public void run() {
		logger.debug(workerId + " Running Worker Thread " + unit.getClass());
		while(true){
                        //it is absolute that exception is caught here to avoid thread from breaking should one record fail for unidentified reason.
                        //thus supression was incorporated above
                        SyncItem item = null;
			try {
				item = unit.getSyncItem();
				stopWatch.zero();
				stopWatch.start(); // start timer
				if(item != null && (!unit.wasProcessed(item.getItemId()))){
                                        item = unit.deserialize(item);
                                        SyncFile syncFile = (SyncFile) item;
                                        if (syncFile.getValidationStatusEnum() == ValidationStatusEnum.VALID) {
                                            //proceed to compliance check
                                            syncFile = (SyncFile) unit.validate(syncFile);
                                            if (syncFile.getValidationStatusEnum() == ValidationStatusEnum.VALID) {
                                                syncFile = (SyncFile)unit.process(syncFile);
                                                unit.postProcessing(syncFile); 
                                                unit.backup(syncFile);
                                            } else {
                                                unit.backup(syncFile);
                                            }
                                        } else {
                                            //failed deserialization
                                            unit.backup(syncFile);
                                        }
				}
				if(item != null){
					logger.debug(workerId + " Processing Complete for request: " + item.getItemId() + " completed in [" + stopWatch.elapsedTime() + " ms]");
				}
				if(pauseExecution){
					break;
				}
				sleep();
			} catch (Exception e) {
                            handleUnexpectedException(item, e);
			}
		}
		logger.debug(workerId + " Worker Exited: " + unit.getClass());
	}
        
    private void handleUnexpectedException(SyncItem item, Exception e) {
        SyncFile syncFile = (SyncFile) item;
        try {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.EXCEPTION);
            String result = "";
            for (ProxyKeyEnum key : syncFile.getProxyItems().keySet()) {
                if (key.equals(ProxyKeyEnum.PP) | key.equals(ProxyKeyEnum.WSQS) | key.equals(ProxyKeyEnum.SPECIALS)) {
                    logger.debug(workerId + " -- " + key + " was excluded.");
                    continue;
                }
                result += "\n" + key.name() + "    " + syncUtils.log(syncFile.getProxyItem(key));
            }
            logger.error(workerId + " Exception from: " + unit.getClass(), e);
            logger.error(result);
            String exceptionMessage = e.getMessage();
            Throwable cause = e.getCause();
            String causeMessage = null;
            String causeName = null;
            if (cause != null) {
                causeMessage = cause.getMessage();
                causeName = cause.getClass().getName();
            }

            List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
            if (msisdns != null) {
                BasicData basicData = (BasicData) syncFile.getProxyItem(ProxyKeyEnum.BD);
                DynamicData dynamicData = (DynamicData) syncFile.getProxyItem(ProxyKeyEnum.DD);
                MetaData meta = (MetaData) syncFile.getProxyItem(ProxyKeyEnum.META);
                String appVersion = meta == null ? null : meta.getAppVersion();
                String captureAgent = basicData == null ? null : basicData.getBiometricCaptureAgent();
                String regType = dynamicData == null ? null : dynamicData.getDda11();
                FileSyncNewEntryMarker entryMarker = (FileSyncNewEntryMarker) syncFile.getProxyItem(ProxyKeyEnum.SYNC_MARKER);
                String macAddress = entryMarker == null ? null : entryMarker.getMacAddress();
                EnrollmentLog enrollmentLog = (EnrollmentLog) syncFile.getProxyItem(ProxyKeyEnum.EL);
                String kitTag = "";
                if (enrollmentLog != null && enrollmentLog.getEnrollmentRef() != null) {
                    kitTag = enrollmentLog.getEnrollmentRef().getCode();
                }
                for (MsisdnDetail msisdnDetail : msisdns) {
                    BfpFailureLog bfpFailureLog = new BfpFailureLog();
                    bfpFailureLog.setUniqueId(e.getClass().getName());
                    bfpFailureLog.setMsisdn(msisdnDetail.getMsisdn());
                    bfpFailureLog.setFilename(syncFile.getFile().getName());
                    bfpFailureLog.setMacAddress(macAddress);
                    bfpFailureLog.setSimSerial(msisdnDetail.getSerial());
                    bfpFailureLog.setRejectionReason(BfpRejectionReason.EXCEPTION.getCode());
                    bfpFailureLog.setAppVersion(appVersion);
                    bfpFailureLog.setCaptureAgent(captureAgent);
                    bfpFailureLog.setKitTag(kitTag);
                    bfpFailureLog.setRegType(regType);
                    bfpFailureLog.setReason1(exceptionMessage);
                    bfpFailureLog.setReason2(causeName);
                    bfpFailureLog.setReason3(causeMessage);
                    syncFile.addBfpFailureLog(bfpFailureLog);
                }
            }
            unit.backup(syncFile);
        } catch (Exception ex) {
            unit.backup(syncFile);
            logger.error(workerId + "", ex);
        }
    }

	/**
	 * Pauses the current process until the resume method is called
	 *
	 */
	public void pauseProcess() {
		pauseExecution = true;
	}

	private void sleep(){
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			logger.error(workerId +  " Interrupted Exception while sleeping process: ", e);
		}
	}

}
