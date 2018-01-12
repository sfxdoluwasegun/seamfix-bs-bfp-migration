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
import com.seamfix.kyc.client.KSClient;
import com.seamfix.kyc.client.KsService;
import com.seamfix.kyc.client.ServerResponse;

/**
 * @author dawuzi
 *
 */
public class SimSerialValidator extends AbstractSyncFileValidator implements IValidator {

	private ValidatorContext validatorContext;
    private KsService kservice = new KsService();

	@Override
	public void init(ValidatorContext validatorContext) {
        this.validatorContext = validatorContext;
	}

	@Override
    @SuppressWarnings("PMD")
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
		
		boolean simSerialClientValidated = isClientValidated(syncFile);
		
		if(simSerialClientValidated){
			syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
			return syncFile;
		}
		
		
        @SuppressWarnings("unchecked")
		List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
		
		List<MsisdnDetail> invalids = null;
		
        for (MsisdnDetail msisdn : msisdns) {
        	
        	if(msisdn.getMsisdn() == null || msisdn.getMsisdn().trim().isEmpty()){
        		
                int status = 0;
                
                try {
                    status = checkSimSerialStatus(msisdn.getSerial());
                } catch (Exception e) {
                    logger.error(validatorContext.getWorkerId() + " ", e);
                    syncFile.setValidationStatusEnum(ValidationStatusEnum.SIM_SERIAL_VERIFICATION_EXCEPTION);
                    
//                    we have encountered an error so no need to proceed further
                    return syncFile;
                }
                if (status != 0 && status != 1) {
                    logger.debug(validatorContext.getWorkerId() + " Failed validation. Sim serial validation response code: " + status);
                    syncFile.setValidationStatusEnum(ValidationStatusEnum.SIM_SERIAL_VERIFICATION_EXCEPTION);
                    
//                  we have encountered an error so no need to proceed further
                  return syncFile;
                }

                //at this point status is either 0 or 1
                //0 => invalid sim serial, 1 => valid sim serial
                if (status == 0) {
                	
                	addBfpFailureLog(syncFile, msisdn, BfpRejectionReason.INVALID_SIM_SERIAL, "0", null, null, null, null);
                	
            		if(invalids == null){
            			invalids = new ArrayList<>();
            		}
            		
            		invalids.add(msisdn);
            		
                }
        	}
		}
		
        if(invalids != null && !invalids.isEmpty()){
        	msisdns.removeAll(invalids);
                syncFile.addFailedMsisdnDetails(invalids);
        }
		
        if(msisdns.isEmpty()){
            syncFile.setValidationStatusEnum(ValidationStatusEnum.INVALID_SIM_SERIAL);
        } else {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID); 
        }
		
		return syncFile;
	}
	
    /**
	 * @param serial
	 * @return
	 */
	private int checkSimSerialStatus(String serial) {
        KSClient client = kservice.getServiceClient();
        ServerResponse response = client.verifySimSerial(serial, null);
        int result = response.getStatus();
        return result;
	}

	@Override
	public boolean isPrerequisite() {
		return true;
	}

	@Override
	public boolean skip() {
		return getBool(BfpProperty.SKIP_SIM_SERIAL_VALIDATION);
	}
}
