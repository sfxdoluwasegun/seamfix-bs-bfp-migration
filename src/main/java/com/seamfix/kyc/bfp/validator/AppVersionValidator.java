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
import com.seamfix.kyc.bfp.proxy.MetaData;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.syncs.SyncFile;
import com.seamfix.kyc.bfp.syncs.SyncUtils;

/**
 *
 * @author Marcel
 * @since 27-Oct-2016, 16:20:56
 */
public class AppVersionValidator extends AbstractSyncFileValidator implements IValidator {

    private boolean prerequisite = true;
    private boolean skip;
    private ValidatorContext validatorContext;
    private SyncUtils syncUtils = new SyncUtils();
    /**
     * keeps track of android versions that are allowed
     */
    private String allowedDroidVersions;
    /**
     * keeps track of windows versions that are allowed
     */
    private String allowedWindowsVersion;

    public AppVersionValidator() {
        defineConditions();
    }

    private void defineConditions() {
        skip = getBool(BfpProperty.SKIP_APP_VERSION_VALIDATION);
        allowedDroidVersions = getProperty(BfpProperty.ANDROID_APP_VERSION);
        allowedWindowsVersion = getProperty(BfpProperty.WINDOWS_APP_VERSION);
    }

//    these setters were added strictly for test purposes and ideally should not be invoked anywhere else
    public void setAllowedDroidVersions(String allowedDroidVersions) {
        this.allowedDroidVersions = allowedDroidVersions;
    }

    public void setAllowedWindowsVersion(String allowedWindowsVersion) {
        this.allowedWindowsVersion = allowedWindowsVersion;
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
        String kitTag = getKitTag(syncFile);
        MetaData meta = (MetaData) syncFile.getProxyItem(ProxyKeyEnum.META);
        if (validateAppVersion(meta, kitTag)) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
        } else {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.BLACK_LISTED_APP_VERSION);
            @SuppressWarnings("unchecked")
            List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
            for (MsisdnDetail msisdnDetail : msisdns) {
            	addBfpFailureLog(syncFile, msisdnDetail, BfpRejectionReason.BLACKLISTED_APP_VERSION); 
            }
            logger.debug(validatorContext.getWorkerId() + " Failed app version validation. Is prerequisite: " + isPrerequisite());
        }
        return syncFile;
    }

    private boolean validateAppVersion(MetaData meta, String kitTag) {
        boolean valid = false;
        if (meta == null) {
            logger.debug(validatorContext.getWorkerId() + " MetaData is null");
            return valid;
        }
        String appVersion = meta.getAppVersion();
        if (appVersion == null) {
            return valid;
        }

        if (kitTag.toUpperCase().startsWith("DROID")) {
            if (allowedDroidVersions == null || allowedDroidVersions.trim().isEmpty()) {
                logger.warn(validatorContext.getWorkerId() + " allowed application version not cofigured");
                return valid;
            }
            valid = allowedDroidVersions.contains(appVersion);
        } else {
            if (allowedWindowsVersion == null || allowedWindowsVersion.trim().isEmpty()) {
                logger.warn(validatorContext.getWorkerId() + " allowed application version not cofigured");
                return valid;
            }
            appVersion = syncUtils.getFilteredWindowsAppVersion(appVersion);
            valid = allowedWindowsVersion.contains(appVersion);
        }

        return valid;
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
