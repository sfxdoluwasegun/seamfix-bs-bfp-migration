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
import com.seamfix.kyc.bfp.proxy.EnrollmentLog;
import com.seamfix.kyc.bfp.proxy.MetaData;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.service.AppDS;
import com.seamfix.kyc.bfp.syncs.SyncFile;
import com.sf.biocapture.entity.audit.BfpFailureLog;
import java.util.List;

/**
 *
 * @author Marcel
 * @since Aug 17, 2017 - 3:52:33 PM
 */
public class DeviceIdMismatchValidator extends AbstractSyncFileValidator {

    private boolean prerequisite = true;
    private boolean skip;
    private AppDS appDS;
    private ValidatorContext validatorContext;

    public DeviceIdMismatchValidator() {
        defineConditions();
    }

    @SuppressWarnings("CPD-START")
    private void defineConditions() {
        skip = getBool(BfpProperty.SKIP_DEVICE_ID_MISMATCH_VALIDATION);
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
        
        MetaData meta = (MetaData) syncFile.getProxyItem(ProxyKeyEnum.META);
        EnrollmentLog enrollmentLog = (EnrollmentLog) syncFile.getProxyItem(ProxyKeyEnum.EL);
        String metaDataDeviceId = meta.getEnrollmentRef().getDeviceId();
        String enrollmentRefDeviceId = enrollmentLog.getEnrollmentRef().getDeviceId();
        String deviceId = getDeviceId(syncFile);
        if (matchDeviceId(deviceId, metaDataDeviceId) || matchDeviceId(deviceId, enrollmentRefDeviceId) || matchDeviceId(metaDataDeviceId, enrollmentRefDeviceId)) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
            logger.debug(validatorContext.getWorkerId() + " at least two deviceId are equal : " + deviceId);
        } else {
            logger.debug(validatorContext.getWorkerId() + " deviceId: " + deviceId + ", metaDataDeviceId: " + metaDataDeviceId + ", enrollmentRefDeviceId: " + enrollmentRefDeviceId);
            syncFile.setValidationStatusEnum(ValidationStatusEnum.CLIENT_DEVICE_ID_MISMATCH);
            @SuppressWarnings("unchecked")
            List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
            String appVersion = getAppVersion(syncFile);
            String captureAgent = getCaptureAgent(syncFile);
            String regType = getRegType(syncFile);
            String kitTag = getKitTag(syncFile);
            String uniqueId = getUniqueId(syncFile);
            for (MsisdnDetail msisdnDetail : msisdns) {
                BfpFailureLog bfpFailureLog = new BfpFailureLog();
                bfpFailureLog.setUniqueId(uniqueId);
                bfpFailureLog.setMsisdn(msisdnDetail.getMsisdn());
                bfpFailureLog.setFilename(syncFile.getFile().getName());
                bfpFailureLog.setMacAddress("");
                bfpFailureLog.setSimSerial(msisdnDetail.getSerial());
                bfpFailureLog.setRejectionReason(BfpRejectionReason.CLIENT_DEVICE_ID_MISMATCH.getCode());
                bfpFailureLog.setAppVersion(appVersion);
                bfpFailureLog.setCaptureAgent(captureAgent);
                bfpFailureLog.setKitTag(kitTag);
                bfpFailureLog.setRegType(regType);
                bfpFailureLog.setReason1(deviceId);
                bfpFailureLog.setReason2(metaDataDeviceId);
                bfpFailureLog.setReason3(enrollmentRefDeviceId);
                syncFile.addBfpFailureLog(bfpFailureLog);
            }
            logger.debug(validatorContext.getWorkerId() + " Failed device id mis-match validation. Is prerequisite: " + isPrerequisite());
        }
        return syncFile;
    }
    
    private boolean matchDeviceId(String device1, String device2) {
        if (device1 != null && device2 != null) {
            return device1.equals(device2);
        }
        return false;
    }

    @Override
    public boolean isPrerequisite() {
        return prerequisite;
    }

    @Override
    public boolean skip() {
        return skip;
    }

}
