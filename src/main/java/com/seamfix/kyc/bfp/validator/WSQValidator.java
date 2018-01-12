/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.validator;

import com.seamfix.kyc.bfp.contract.IValidator;
import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.enums.BfpRejectionReason;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.proxy.SpecialData;
import com.seamfix.kyc.bfp.proxy.WsqImage;
import com.seamfix.kyc.bfp.service.AppDS;
import com.seamfix.kyc.bfp.syncs.SyncFile;
import java.util.List;

/**
 *
 * @author Marcel
 * @since 28-Oct-2016, 10:30:48
 */
public class WSQValidator extends AbstractSyncFileValidator implements IValidator {

    private boolean prerequisite = true;
    private boolean skip;
    @SuppressWarnings("unused")
    private AppDS appDS;
    private ValidatorContext validatorContext;

    public WSQValidator() {
        defineConditions();
    }

    private void defineConditions() {
    }

    @SuppressWarnings("unchecked")
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
        List<WsqImage> wsqImages = (List<WsqImage>) syncFile.getProxyItem(ProxyKeyEnum.WSQS);
        List<SpecialData> specialData = (List<SpecialData>) syncFile.getProxyItem(ProxyKeyEnum.SPECIALS);

        if ((wsqImages.size() < 4 && specialData.isEmpty()) || wsqImages.size() > 10) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.ILLEGAL_SYNC_FILE);
            List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
            
            for (MsisdnDetail msisdnDetail : msisdns) {
                addBfpFailureLog(syncFile, msisdnDetail, BfpRejectionReason.INVALID_SYNC_FILE, 
                		"INVALID WSQ COUNT", String.valueOf(wsqImages.size()), String.valueOf(specialData.size()), null, null);
            }
            
            logger.debug(validatorContext.getWorkerId() + " Failed WSQ validation. Is prerequisite: " + isPrerequisite());
        } else {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
        }
        return syncFile;
    }

    @Override
    public boolean isPrerequisite() {
        return prerequisite;
    }

    @Override
    public boolean skip() {
        return skip;
    }

    @Override
    public void init(ValidatorContext validatorContext) {
        this.validatorContext = validatorContext;
        this.appDS = validatorContext.getAppDS();
    }

}
