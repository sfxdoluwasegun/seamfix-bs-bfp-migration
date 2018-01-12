package com.seamfix.kyc.bfp.validator;

import java.util.List;

import com.seamfix.kyc.bfp.contract.IValidator;
import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.enums.BfpRejectionReason;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.proxy.DynamicData;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.syncs.SyncFile;
import com.seamfix.kyc.client.KSClient;
import com.seamfix.kyc.client.SimSwapResponse;
/**
 * 
 * @author Nnanna
 * @since 19/11/2016
 */

@Deprecated
public class SimSwapValidator extends AbstractSyncFileValidator implements IValidator {
	private ValidatorContext validatorContext;
	private boolean prerequisite = true;

	@Override
	public void init(ValidatorContext validatorContext) {
		this.validatorContext = validatorContext;
	}

        @SuppressWarnings("PMD")
	@Override
	public SyncItem validate(SyncItem syncItem) {
		if (syncItem == null) {
            logger.debug(validatorContext.getWorkerId() + " sync item is null");
            return syncItem;
        }
		
		SyncFile syncFile = (SyncFile) syncItem;
		String regType = getRegType(syncFile);
		boolean isSwap = isSimSwap(regType);
		if(!isSwap){
			syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
            return syncFile;
		}
		
		@SuppressWarnings("unchecked")
		MsisdnDetail md = ((List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS)).get(0);
		DynamicData dd = (DynamicData) syncFile.getProxyItem(ProxyKeyEnum.DD);
		String puk = dd == null ? null : dd.getDa4();
		if(md != null && puk != null){
			SimSwapResponse resp = null;
			try{
				resp = doSimSwap(md.getMsisdn(), null, puk, md.getSerial());
			}catch(Exception ex){
				logger.error(validatorContext.getWorkerId() + " ", ex);
                //allow conditions below to determine if sync file should be moved or not
			}
			if(resp != null && resp.getCode() != null && resp.getCode().equalsIgnoreCase("0")){
				logger.debug("Sim Swap successful!! Message: " + resp.getMessage());
				syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
			}else{
				syncFile.setValidationStatusEnum(ValidationStatusEnum.SIM_SWAP_EXCEPTION);
				//log failure to bfp failure log
				addBfpFailureLog(syncFile, md, BfpRejectionReason.FAILED_SIM_SWAP);
			}
		}else{
			//possibly an issue with the data in the sync file
			syncFile.setValidationStatusEnum(ValidationStatusEnum.SIM_SWAP_EXCEPTION);
		}
		
		return syncFile;
	}

	@Override
	public boolean isPrerequisite() {
		return prerequisite;
	}

	@Override
	public boolean skip() {
		return false;
	}
	
	private SimSwapResponse doSimSwap(String msisdn, String orderNumber, 
			String newPUK, String newSimSerial){
		KSClient client = kservice.getServiceClient();
		SimSwapResponse resp = client.doSwap(msisdn, orderNumber, newPUK, newSimSerial);
		return resp;
	}

	private boolean isSimSwap(String regType) {
        String type = regType.toUpperCase();
        if (type.contains("SSW") || type.contains("SSC") || type.contains("SSI")) {
            return true;
        }
        return false;
    }
	
}