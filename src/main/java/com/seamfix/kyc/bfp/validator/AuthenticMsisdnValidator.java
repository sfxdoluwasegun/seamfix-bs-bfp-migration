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
import com.seamfix.kyc.client.MsisdnResponse;

/**
 * @author dawuzi
 *
 */
public class AuthenticMsisdnValidator extends AbstractSyncFileValidator implements IValidator {

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
	
        String regType = getRegType(syncFile);

        boolean newRegistration = isNewRegistration(regType);
        boolean clientValidated = isClientValidated(syncFile);
        
        if(!newRegistration || clientValidated){
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
            return syncFile;
        }
        
        @SuppressWarnings("unchecked")
		List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
		
		List<MsisdnDetail> invalids = null;
        	
        for (MsisdnDetail detail : msisdns) {
        	
        	if (detail.getMsisdn() == null || detail.getMsisdn().trim().isEmpty()) {
        		continue;
        	}        	
        	
            MsisdnResponse msisdnResponse;
            int responseCode;
            String status;
            String message;
            try {
                msisdnResponse = checkMsisdnStatus(detail.getMsisdn());
                responseCode = msisdnResponse.getCode();
                status = msisdnResponse.getStatus();
                message = msisdnResponse.getMessage();
            } catch (Exception e) {
                syncFile.setValidationStatusEnum(ValidationStatusEnum.MSISDN_VERIFICATION_EXCEPTION);
//              we have encountered an error so no need to proceed further
              return syncFile;
            }

//			0 - Successfully retrieved the status of the MSISDN from agility
//			-1 - MSISDN supplied to the service is empty
//			-2 - Unable to connect to remote service i.e. unable to connect to agility. This is the only response that should trigger a retrial in BFP
//			-3 - Successfully hit kyc service, but agility returned a failure response e.g. Subscriber does not exist on agility. This response has a null status for msisdn	
//			more info at https://seamfix.atlassian.net/browse/MK-836
//			if the response code is not 0 or -3, we throw an exception in order to retry
            if (responseCode != 0 && responseCode != -3) {
                syncFile.setValidationStatusEnum(ValidationStatusEnum.MSISDN_VERIFICATION_EXCEPTION);
//              we have encountered an error so no need to proceed further
              return syncFile;
            }

//			so we only proceed if the response code is either 0 or -3
//			if the response code is -3 there is no need checking the status
            if (responseCode == -3 || !"Paired".equalsIgnoreCase(status)) {
            	
            	addBfpFailureLog(syncFile, detail, BfpRejectionReason.INVALID_MSISDN, status, message, responseCode + "", null, null);
            	
        		if(invalids == null){
        			invalids = new ArrayList<>();
        		}
        		
        		invalids.add(detail);
            }
		}
        
        if(invalids != null && !invalids.isEmpty()){
        	msisdns.removeAll(invalids);
                syncFile.addFailedMsisdnDetails(invalids);
        }
		
        if(msisdns.isEmpty()){
            syncFile.setValidationStatusEnum(ValidationStatusEnum.INVALID_MSISDN);
        } else {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID); 
        }
        
		return syncFile;
	}
	
    private MsisdnResponse checkMsisdnStatus(String msisdn) {
        KSClient client = kservice.getServiceClient();
        MsisdnResponse msisdnResponse = client.verifyMsisdn(msisdn);
        return msisdnResponse;
    }

    private boolean isNewRegistration(String regType) {
        String type = regType.toUpperCase();
        if (type.contains("NMI") || type.contains("NMC")) {
            return true;
        }
        return false;
    }

	@Override
	public boolean isPrerequisite() {
		return true;
	}

	@Override
	public boolean skip() {
		return getBool(BfpProperty.SKIP_MSISDN_VALIDATION);
	}
}
