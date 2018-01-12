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
import com.seamfix.kyc.bfp.proxy.BasicData;
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
public class BlacklistedAgentValidator extends AbstractSyncFileValidator implements IValidator {

    private boolean prerequisite;
    private boolean skip;
    private AppDS appDS;
    @SuppressWarnings("unused")
    private ValidatorContext validatorContext;

    public BlacklistedAgentValidator() {
        defineCondition();
    }

    @SuppressWarnings("CPD-START")
    private void defineCondition() {
        this.prerequisite = true;
        skip = getBool(BfpProperty.SKIP_BLACKLISTED_AGENT_VALIDATION);
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
        String kitTag = getKitTag(syncFile);
        BasicData basicData = (BasicData) syncFile.getProxyItem(ProxyKeyEnum.BD);
        String item = appDS.checkAgentBlacklistStatus(meta, basicData);
        if (item.equalsIgnoreCase("Y")) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.BLACK_LISTED_AGENT);
            @SuppressWarnings("unchecked")
            List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
            String uniqueId = getUniqueId(syncFile);
            String appVersion = getAppVersion(syncFile);
            String captureAgent = getCaptureAgent(syncFile);
            String regType = getRegType(syncFile);
            String macAddress = getMacAddress(syncFile);
            for (MsisdnDetail msisdnDetail : msisdns) {
                BfpFailureLog bfpFailureLog = new BfpFailureLog();
                bfpFailureLog.setUniqueId(uniqueId);
                bfpFailureLog.setMsisdn(msisdnDetail.getMsisdn());
                bfpFailureLog.setFilename(syncFile.getFile().getName());
                bfpFailureLog.setMacAddress(macAddress);
                bfpFailureLog.setSimSerial(msisdnDetail.getSerial());
                bfpFailureLog.setRejectionReason(BfpRejectionReason.BLACKLISTED_AGENT.getCode());
                bfpFailureLog.setAppVersion(appVersion);
                bfpFailureLog.setCaptureAgent(captureAgent);
                bfpFailureLog.setKitTag(kitTag);
                bfpFailureLog.setRegType(regType);
                bfpFailureLog.setReason1(metaDataMacAddress);
                bfpFailureLog.setReason2(enrollmentRefMacAddress);
                syncFile.addBfpFailureLog(bfpFailureLog);
            }
            logger.debug(validatorContext.getWorkerId() + " Failed blacklisted agent validation. Agent : " + captureAgent + " is blacklised. Is prerequisite: " + isPrerequisite());
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
