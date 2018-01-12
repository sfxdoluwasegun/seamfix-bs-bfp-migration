/**
 * 
 */
package com.seamfix.kyc.bfp.validator;

import java.util.ArrayList;
import java.util.List;

import com.seamfix.kyc.bfp.contract.IValidator;
import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.enums.BfpProperty;
import com.seamfix.kyc.bfp.enums.BfpRejectionReason;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.proxy.MetaData;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.syncs.SyncFile;
import com.seamfix.kyc.bfp.syncs.SyncUtils;
import com.sf.biocapture.entity.SmsActivationRequest;

/**
 * @author dawuzi
 *
 */
public class UniqueSimSerialValidator extends AbstractSyncFileValidator implements IValidator {

	private ValidatorContext validatorContext;
    private String androidAppVersionToSkipUniqueSimSerialValidation;
    private String windowsAppVersionToSkipUniqueSimSerialValidation;
    private SyncUtils syncUtils = new SyncUtils();
    
    

	public UniqueSimSerialValidator() {
        androidAppVersionToSkipUniqueSimSerialValidation = getProperty(BfpProperty.SKIP_SIM_SERIAL_ANDROID_APP_VERSION_VALIDATION);
        windowsAppVersionToSkipUniqueSimSerialValidation = getProperty(BfpProperty.SKIP_SIM_SERIAL_WINDOWS_APP_VERSION_VALIDATION);
	}

	@Override
	public void init(ValidatorContext validatorContext) {
        this.validatorContext = validatorContext;
	}

	@Override
	public SyncItem validate(SyncItem syncItem) {
       
		if (syncItem == null) {
            logger.debug(validatorContext.getWorkerId() + " sync item is null");
            return syncItem;
        }
        
        SyncFile syncFile = (SyncFile) syncItem;
        
        if (skip()) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
            return syncFile;
        }

        String kitTag = getKitTag(syncFile);
        MetaData meta = (MetaData) syncFile.getProxyItem(ProxyKeyEnum.META);
        
		boolean simSerialSkippableAppVersion = canSkipSimSerialValidationByAppVersion(meta, kitTag);
		boolean nullMsisdnAllowableRegistration = isNullMsisdnAllowableRegistration(syncFile);
		
		
        @SuppressWarnings("unchecked")
		List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
		
		List<MsisdnDetail> invalids = null;
		
		if(!simSerialSkippableAppVersion && nullMsisdnAllowableRegistration){
        	
	        for (MsisdnDetail detail : msisdns) {
	        	
	        	
				String serial = detail.getSerial();
				
				if(serialNumberAlreadyExists(serial)){ 
	        		
					addBfpFailureLog(syncFile, detail, BfpRejectionReason.DUPLICATE_SIM_SERIAL);
	        		
	        		if(invalids == null){
	        			invalids = new ArrayList<>();
	        		}
	        		
	        		invalids.add(detail);
	        		
				}				
			}
        }
        
        if(invalids != null && !invalids.isEmpty()){
        	msisdns.removeAll(invalids);
                syncFile.addFailedMsisdnDetails(invalids);
        }
		
        if(msisdns.isEmpty()){
            syncFile.setValidationStatusEnum(ValidationStatusEnum.DUPLICATE_SIM_SERIAL);
        } else {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID); 
        }
		
		return syncFile;
	}
	

    /**
	 * @param serial
	 * @return
	 */
	private boolean serialNumberAlreadyExists(String serial) {
		String nullMsisdnVal = getProperty(BfpProperty.NULL_MSISDN_VALUE);
		List<SmsActivationRequest> smsActivationRequests = validatorContext.getAppDS().getSmsActivationRequest(serial, 1, nullMsisdnVal);
		if(smsActivationRequests == null || smsActivationRequests.isEmpty()){
			return false;
		}
		return true;
	}

	private boolean canSkipSimSerialValidationByAppVersion(MetaData meta, String tag) {
        if (meta == null) {
            return false;
        }
        String appVersion = meta.getAppVersion();
        if (appVersion == null) {
            return false;
        }
        if (tag.toUpperCase().startsWith("DROID")) {
            if (androidAppVersionToSkipUniqueSimSerialValidation == null || androidAppVersionToSkipUniqueSimSerialValidation.trim().isEmpty()) {
                return false;
            }
            return androidAppVersionToSkipUniqueSimSerialValidation.contains(appVersion);
        } else {
            if (windowsAppVersionToSkipUniqueSimSerialValidation == null || windowsAppVersionToSkipUniqueSimSerialValidation.trim().isEmpty()) {
                return false;
            }
            appVersion = syncUtils.getFilteredWindowsAppVersion(appVersion);
            return windowsAppVersionToSkipUniqueSimSerialValidation.contains(appVersion);
        }
    }
	
	@Override
	public boolean isPrerequisite() {
		return true;
	}

	@Override
	public boolean skip() {
		return getBool(BfpProperty.SKIP_UNIQUE_SIM_SERIAL_VALIDATION);
	}

}
