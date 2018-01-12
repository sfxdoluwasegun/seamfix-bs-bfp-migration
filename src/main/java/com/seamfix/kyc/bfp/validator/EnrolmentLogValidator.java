/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.validator;

import java.util.List;

import com.seamfix.kyc.bfp.contract.IValidator;
import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.enums.BfpRejectionReason;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.proxy.EnrollmentLog;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.syncs.SyncFile;

/**
 *
 * @author Marcel
 * @since 27-Oct-2016, 16:20:56
 */
public class EnrolmentLogValidator extends AbstractSyncFileValidator implements IValidator {

    private boolean prerequisite = true;
    private boolean skip;
    private ValidatorContext validatorContext;

    public EnrolmentLogValidator() {
        defineConditions();
    }

    private void defineConditions() {
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
        EnrollmentLog enrollmentLog = (EnrollmentLog) syncFile.getProxyItem(ProxyKeyEnum.EL);
        if (enrollmentLog == null || enrollmentLog.getEnrollmentRef() == null) {
            logger.debug(validatorContext.getWorkerId() + " Enrolment log is null");
            syncFile.setValidationStatusEnum(ValidationStatusEnum.NULL_ENROLMENT_LOG);
            List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
            for (MsisdnDetail msisdnDetail : msisdns) {
            	addBfpFailureLog(syncFile, msisdnDetail, BfpRejectionReason.NULL_ENROLMENT_LOG, ValidationStatusEnum.NULL_ENROLMENT_LOG.name(), null, null, null, null); 
            }
            return syncFile;
        } else {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
        }
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

    @Override
    public void init(ValidatorContext validatorContext) {
        this.validatorContext = validatorContext;
    }

}
