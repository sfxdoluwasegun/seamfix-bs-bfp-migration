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
import com.seamfix.kyc.bfp.proxy.EnrollmentLog;
import com.seamfix.kyc.bfp.proxy.MetaData;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.service.AppDS;
import com.seamfix.kyc.bfp.syncs.SyncFile;

/**
 *
 * @author Marcel
 * @since 28-Oct-2016, 10:30:48
 */
public class BlacklistedKitValidator extends AbstractSyncFileValidator implements IValidator {

    private boolean prerequisite = true;
    private boolean skip;
    private AppDS appDS;
    private ValidatorContext validatorContext;

    @SuppressWarnings("CPD-START")
    public BlacklistedKitValidator() {
        skip = getBool(BfpProperty.SKIP_BLACKLIST_STATUS_VALIDATION);
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
        MetaData meta = (MetaData) syncFile.getProxyItem(ProxyKeyEnum.META);
        EnrollmentLog enrollmentLog = (EnrollmentLog) syncFile.getProxyItem(ProxyKeyEnum.EL);
        String metaDataMacAddress = meta.getCaptureMachineId();
        String enrollmentRefMacAddress = enrollmentLog.getEnrollmentRef().getMacAddress();
        String macAddress = getMacAddress(syncFile);
        String kitTag = getKitTag(syncFile);

        boolean blacklisted = appDS.checkBlacklistStatusByDeviceId(macAddress, getDeviceId(syncFile));

        if (blacklisted) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.BLACKLISTED_MAC_ADDRESS_OR_KIT);
            @SuppressWarnings("unchecked")
            List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
            for (MsisdnDetail msisdnDetail : msisdns) {
                addBfpFailureLog(syncFile, msisdnDetail, BfpRejectionReason.BLACKLISTED_MAC_ADDRESS_OR_KIT,
                        metaDataMacAddress, enrollmentRefMacAddress, null, null, null);
            }
            logger.debug(validatorContext.getWorkerId() + " Failed black list status validation. tag: " + kitTag + "  or mac address: " + macAddress + " is blacklised. Is prerequisite: " + isPrerequisite());
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
