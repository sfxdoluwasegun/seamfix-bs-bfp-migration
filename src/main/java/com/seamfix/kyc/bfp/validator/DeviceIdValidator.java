/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.validator;

import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.enums.BfpProperty;
import com.seamfix.kyc.bfp.enums.BfpRejectionReason;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.proxy.BasicData;
import com.seamfix.kyc.bfp.proxy.DynamicData;
import com.seamfix.kyc.bfp.proxy.FileSyncNewEntryMarker;
import com.seamfix.kyc.bfp.proxy.MetaData;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.service.AppDS;
import com.seamfix.kyc.bfp.syncs.SyncFile;
import com.sf.biocapture.entity.EnrollmentRef;
import com.sf.biocapture.entity.UserId;
import com.sf.biocapture.entity.audit.BfpFailureLog;
import java.util.List;

/**
 *
 * @author Marcel
 * @since Aug 21, 2017 - 2:34:56 PM
 */
public class DeviceIdValidator extends AbstractSyncFileValidator {

    private boolean prerequisite = true;
    private boolean skip;
    private AppDS appDS;
    private ValidatorContext validatorContext;

    public DeviceIdValidator() {
        defineConditions();
    }

    @SuppressWarnings("CPD-START")
    private void defineConditions() {
        skip = getBool(BfpProperty.SKIP_DEVICE_ID_VALIDATION);
    }

    @Override
    public void init(ValidatorContext validatorContext) {
        this.validatorContext = validatorContext;
        this.appDS = validatorContext.getAppDS();
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

        if (!isDeprecatedMacAddress(syncFile)) {
            //skip mac address check
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
            logger.debug("Mac Address check is required instead of device id check.");
            return syncFile;
        }

        String kitTag = getKitTag(syncFile);
        MetaData meta = (MetaData) syncFile.getProxyItem(ProxyKeyEnum.META);
        FileSyncNewEntryMarker entryMarker = (FileSyncNewEntryMarker) syncFile.getProxyItem(ProxyKeyEnum.SYNC_MARKER);
        if (entryMarker.getRealtimeDeviceId() == null || entryMarker.getRealtimeDeviceId().isEmpty()) {
            logger.debug(validatorContext.getWorkerId() + " null device id " + kitTag);
            syncFile.setValidationStatusEnum(ValidationStatusEnum.NULL_DEVICE_ID);
            generateBfpFailureLog(syncFile, kitTag, meta, entryMarker, BfpRejectionReason.NULL_DEVICE_ID);
            return syncFile;
        }

        EnrollmentRef enrollmentRef = appDS.getEnrollmentRefByDeviceId(entryMarker.getRealtimeDeviceId().trim());
        if (enrollmentRef == null) {
            logger.debug(validatorContext.getWorkerId() + " device id : " + entryMarker.getRealtimeDeviceId() + " does not exists in enrollment ref");
            syncFile.setValidationStatusEnum(ValidationStatusEnum.UNREGISTERED_DEVICE_ID);
            generateBfpFailureLog(syncFile, kitTag, meta, entryMarker, BfpRejectionReason.UNREGISTERED_DEVICE_ID);
        }

        return syncFile;
    }

    private SyncFile generateBfpFailureLog(SyncFile syncFile, String kitTag, MetaData meta, FileSyncNewEntryMarker entryMarker, BfpRejectionReason bfpRejectionReason) {
        @SuppressWarnings("unchecked")
        List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
        UserId userId = (UserId) syncFile.getProxyItem(ProxyKeyEnum.USER_ID);
        BasicData basicData = (BasicData) syncFile.getProxyItem(ProxyKeyEnum.BD);
        DynamicData dynamicData = (DynamicData) syncFile.getProxyItem(ProxyKeyEnum.DD);
        String appVersion = meta.getAppVersion();
        String captureAgent = basicData.getBiometricCaptureAgent();
        String regType = dynamicData.getDda11();
        for (MsisdnDetail msisdnDetail : msisdns) {
            BfpFailureLog bfpFailureLog = new BfpFailureLog();
            bfpFailureLog.setUniqueId(userId.getUniqueId());
            bfpFailureLog.setMsisdn(msisdnDetail.getMsisdn());
            bfpFailureLog.setFilename(syncFile.getFile().getName());
            bfpFailureLog.setMacAddress(entryMarker.getMacAddress());
            bfpFailureLog.setSimSerial(msisdnDetail.getSerial());
            bfpFailureLog.setRejectionReason(bfpRejectionReason.getCode());
            bfpFailureLog.setAppVersion(appVersion);
            bfpFailureLog.setCaptureAgent(captureAgent);
            bfpFailureLog.setKitTag(kitTag);
            bfpFailureLog.setRegType(regType);
            syncFile.addBfpFailureLog(bfpFailureLog);
        }
        logger.debug(validatorContext.getWorkerId() + " Failed mac-address-kit-tag validation. Is prerequisite: " + isPrerequisite());
        return syncFile;
    }

    @Override
    public boolean skip() {
        return skip;
    }

    @Override
    public boolean isPrerequisite() {
        return prerequisite;
    }
}
