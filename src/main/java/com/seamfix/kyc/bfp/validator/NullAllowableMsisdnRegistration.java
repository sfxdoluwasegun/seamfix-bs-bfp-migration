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
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.syncs.SyncFile;

/**
 * @author dawuzi
 *
 */
public class NullAllowableMsisdnRegistration extends AbstractSyncFileValidator implements IValidator {

	private ValidatorContext validatorContext;

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
		
//		a setting that can be used to turn of the saving of null msisdns feature
		boolean allowNullMsisdns = getBool(BfpProperty.ALLOW_NULL_MSISDNS);
		boolean isNullAllowableRegistration = isNullMsisdnAllowableRegistration(syncFile);
		
		if(allowNullMsisdns && isNullAllowableRegistration){
			logger.debug(validatorContext.getWorkerId()+" null msisdn allowable registration");
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
            return syncFile;
		}
		
        @SuppressWarnings("unchecked")
		List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
		
		List<MsisdnDetail> invalids = null;
		
        for (MsisdnDetail msisdn : msisdns) {
        	
        	if(msisdn.getMsisdn() == null || msisdn.getMsisdn().trim().isEmpty()){
        		
        		addBfpFailureLog(syncFile, msisdn, BfpRejectionReason.INVALID_NULL_MSISDN_REGISTRATION);
        		
        		if(invalids == null){
        			invalids = new ArrayList<>();
        		}
        		
        		invalids.add(msisdn);
        	}
		}
		
        if(invalids != null && !invalids.isEmpty()){
        	msisdns.removeAll(invalids);
                syncFile.addFailedMsisdnDetails(invalids);
        }
		
        if(msisdns.isEmpty()){
            syncFile.setValidationStatusEnum(ValidationStatusEnum.INVALID_MSISDN_DETAIL);
        } else {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID); 
        }
		
		return syncFile;
	}

	@Override
	public boolean isPrerequisite() {
		return true;
	}

	@Override
	public boolean skip() {
		return false;
	}

}
