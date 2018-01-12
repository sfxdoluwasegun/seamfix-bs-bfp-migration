/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.validator;

import java.util.List;

import com.seamfix.kyc.bfp.contract.IValidator;
import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.enums.BfpProperty;
import com.seamfix.kyc.bfp.enums.BfpRejectionReason;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.proxy.FileSyncNewEntryMarker;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.service.AppDS;
import com.seamfix.kyc.bfp.syncs.SyncFile;

/**
 *
 * @author Marcel
 * @since 28-Oct-2016, 10:30:48
 */
public class MarkerValidator extends AbstractSyncFileValidator implements IValidator {

    private boolean prerequisite = true;
    private boolean skip;
    @SuppressWarnings("unused")
    private AppDS appDS;
    private ValidatorContext validatorContext;

    public MarkerValidator() {
        defineConditions();
    }

    private void defineConditions() {
        skip = getBool(BfpProperty.SKIP_MARKER_BLACKLIST_VALIDATION);
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
        FileSyncNewEntryMarker entryMarker = (FileSyncNewEntryMarker) syncFile.getProxyItem(ProxyKeyEnum.SYNC_MARKER);
        String kitTag = getKitTag(syncFile);
        if (!entryMarker.isBlacklisted()) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
        } else {
            logger.debug(validatorContext.getWorkerId() + " Failed black list validation. Code: " + kitTag + ". Is prerequisite: " + isPrerequisite());
            syncFile.setValidationStatusEnum(ValidationStatusEnum.BLACKLISTED_MAC_ADDRESS_OR_KIT);
            @SuppressWarnings("unchecked")
            List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
            for (MsisdnDetail msisdnDetail : msisdns) {
                addBfpFailureLog(syncFile, msisdnDetail, BfpRejectionReason.BLACKLISTED_MAC_ADDRESS_OR_KIT, 
                		BfpRejectionReason.BLACKLISTED_SYNC_FILE.getCode(), null, null, null, null); 
            }
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
