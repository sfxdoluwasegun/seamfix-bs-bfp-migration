/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.validator;

import com.seamfix.kyc.bfp.contract.IValidator;
import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.service.AppDS;
import com.seamfix.kyc.bfp.syncs.SyncFile;

/**
 *
 * @author Marcel
 * @since 09-May-2017
 */
public class UniqueIdValidator extends AbstractSyncFileValidator implements IValidator {

    private boolean prerequisite = true;
    private boolean skip;
    private ValidatorContext validatorContext;
    private AppDS appDS;

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
        if (!appDS.userIdExists(getUniqueId(syncFile))) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
        } else {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.PROCESSED);
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

}
