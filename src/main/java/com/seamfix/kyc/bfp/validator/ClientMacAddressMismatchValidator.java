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
import com.sf.biocapture.entity.audit.BfpFailureLog;

/**
 *
 * @author Marcel
 * @since 28-Oct-2016, 10:30:48
 */
public class ClientMacAddressMismatchValidator extends AbstractSyncFileValidator implements IValidator {
    
    private boolean prerequisite = true;
    private boolean skip;
    @SuppressWarnings("unused")
    private AppDS appDS;
    @SuppressWarnings("unused")
    private ValidatorContext validatorContext;
    
    public ClientMacAddressMismatchValidator() {
        defineConditions();
    }
    
    @SuppressWarnings("CPD-START")
    private void defineConditions() {
        skip = getBool(BfpProperty.SKIP_MAC_ADDRESS_MISMATCH_VALIDATION);
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
        
        if (isDeprecatedMacAddress(syncFile)) {
            //skip mac address check
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
            logger.debug("Mac Address check is deprecated. Device Id check is required instead.");
            return syncFile;
        }
        
        MetaData meta = (MetaData) syncFile.getProxyItem(ProxyKeyEnum.META);
        EnrollmentLog enrollmentLog = (EnrollmentLog) syncFile.getProxyItem(ProxyKeyEnum.EL);
        String metaDataMacAddress = meta.getCaptureMachineId();
        String enrollmentRefMacAddress = enrollmentLog.getEnrollmentRef().getMacAddress();
        String macAddress = getMacAddress(syncFile);
        if (matchMacAddress(macAddress, metaDataMacAddress) || matchMacAddress(macAddress, enrollmentRefMacAddress) || matchMacAddress(metaDataMacAddress, enrollmentRefMacAddress)) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
            logger.debug(validatorContext.getWorkerId() + " at least two mac addresses are equal : " + macAddress);
        } else {
            logger.debug(validatorContext.getWorkerId() + " macAddress : " + macAddress + ", metaDataMacAddress : " + metaDataMacAddress + ", enrollmentRefMacAddress : " + enrollmentRefMacAddress);
            syncFile.setValidationStatusEnum(ValidationStatusEnum.CLIENT_MAC_ADDRESS_MISMATCH);
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
                bfpFailureLog.setMacAddress(macAddress);
                bfpFailureLog.setSimSerial(msisdnDetail.getSerial());
                bfpFailureLog.setRejectionReason(BfpRejectionReason.CLIENT_MAC_ADDRESS_MISMATCH.getCode());
                bfpFailureLog.setAppVersion(appVersion);
                bfpFailureLog.setCaptureAgent(captureAgent);
                bfpFailureLog.setKitTag(kitTag);
                bfpFailureLog.setRegType(regType);
                bfpFailureLog.setReason1(metaDataMacAddress);
                bfpFailureLog.setReason2(enrollmentRefMacAddress);
                syncFile.addBfpFailureLog(bfpFailureLog);
            }
            logger.debug(validatorContext.getWorkerId() + " Failed mac address mis-match validation. Is prerequisite: " + isPrerequisite());
        }
        return syncFile;
    }
    
    private boolean matchMacAddress(String mac1, String mac2) {
        if (mac1 != null && mac2 != null) {
            return mac1.equals(mac2);
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
    
    @Override
    public void init(ValidatorContext validatorContext) {
        this.validatorContext = validatorContext;
        this.appDS = validatorContext.getAppDS();
    }
    
}
