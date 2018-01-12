/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.validator;

import java.util.List;
import java.util.regex.Pattern;

import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.contract.IValidator;
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

/**
 *
 * @author Marcel
 * @since 27-Oct-2016, 16:20:56
 */
public class MacAddressValidator extends AbstractSyncFileValidator implements IValidator {

    private boolean prerequisite = true;
    private boolean skip;
    private AppDS appDS;
    private BioCache cache;
    private final Pattern SPACE_PATTERN = Pattern.compile(" ");
    private int macAddressCacheTimeout;
    @SuppressWarnings("unused")
    private ValidatorContext validatorContext;

    public MacAddressValidator() {
        defineConditions();
    }

    private void defineConditions() {
        skip = getBool(BfpProperty.SKIP_MAC_ADDRESS_KIT_TAG_MATCH_VALIDATION);
        macAddressCacheTimeout = getInt(BfpProperty.MAC_ADDRESS_CHECK_CACHE_TIMEOUT);
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
        
        String kitTag = getKitTag(syncFile);
        MetaData meta = (MetaData) syncFile.getProxyItem(ProxyKeyEnum.META);
        FileSyncNewEntryMarker entryMarker = (FileSyncNewEntryMarker) syncFile.getProxyItem(ProxyKeyEnum.SYNC_MARKER);
        if (entryMarker.getMacAddress() == null || entryMarker.getMacAddress().isEmpty()) {
            logger.debug(validatorContext.getWorkerId() + " null mac address " + kitTag);
            syncFile.setValidationStatusEnum(ValidationStatusEnum.NULL_MAC_ADDRESS);
            generateBfpFailureLog(syncFile, kitTag, meta, entryMarker, BfpRejectionReason.NULL_MAC_ADDRESS);
            return syncFile;
        }
        String cacheKey = "MAC-ADDRESS-" + SPACE_PATTERN.matcher(entryMarker.getMacAddress()).replaceAll("_");
        String tag = (String) cache.getItem(cacheKey);

        String macAddressDoesNotExistTag = "MAC_ADDRESS_DOES_NOT_EXIST_TAG";
        String macAddressMultipleKitTag = "MAC_ADDRESS_TIED_TO_MULTIPLE_KITS_TAG";

        if (tag == null) {
            List<EnrollmentRef> enrollmentRefs = appDS.getEnrollmentRefsByMacAddress(entryMarker.getMacAddress(), 0, 2);
            if (enrollmentRefs == null || enrollmentRefs.isEmpty()) {
                tag = macAddressDoesNotExistTag;
            } else if (enrollmentRefs.size() > 1) {
                tag = macAddressMultipleKitTag;
            } else {
                EnrollmentRef enrollmentRef = enrollmentRefs.get(0);
                tag = enrollmentRef.getCode();
            }
            logger.debug(validatorContext.getWorkerId() + " tag not in cache. Server tag : " + tag);
            cache.setItem(cacheKey, tag, macAddressCacheTimeout * 60);
        } else {
            logger.debug(validatorContext.getWorkerId() + " tag found in cache : " + tag);
        }

        if (tag.equals(macAddressDoesNotExistTag)) {
            logger.debug(validatorContext.getWorkerId() + " macAddress : " + entryMarker.getMacAddress() + " does not exists in enrollment ref");
            syncFile.setValidationStatusEnum(ValidationStatusEnum.UNREGISTERED_MAC_ADDRESS);
            generateBfpFailureLog(syncFile, kitTag, meta, entryMarker, BfpRejectionReason.UNREGISTERED_MAC_ADDRESS);
        } else if (tag.equals(macAddressMultipleKitTag)) {
            logger.debug(validatorContext.getWorkerId() + " macAddress : " + entryMarker.getMacAddress() + " is attached to multiple kits");
            syncFile.setValidationStatusEnum(ValidationStatusEnum.MAC_ADDRESS_ASSOCIATED_WITH_MULTIPLE_KITS);
            generateBfpFailureLog(syncFile, kitTag, meta, entryMarker, BfpRejectionReason.MAC_ADDRESS_ASSOCIATED_WITH_MULTIPLE_KITS);
        } else if (!tag.equals(kitTag)) {
            logger.debug(validatorContext.getWorkerId() + " kit tag mismatch for " + kitTag);
            syncFile.setValidationStatusEnum(ValidationStatusEnum.MAC_ADDRESS_KIT_TAG_MISMATCH);
            generateBfpFailureLog(syncFile, kitTag, meta, entryMarker, BfpRejectionReason.MAC_ADDRESS_KIT_TAG_MISMATCH);
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

    @Override
    public void init(ValidatorContext validatorContext) {
        this.validatorContext = validatorContext;
        this.appDS = validatorContext.getAppDS();
        this.cache = validatorContext.getBioCache();
    }
}
