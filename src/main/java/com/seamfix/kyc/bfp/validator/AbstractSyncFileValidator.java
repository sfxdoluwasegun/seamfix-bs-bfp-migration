/**
 *
 */
package com.seamfix.kyc.bfp.validator;

import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.contract.IValidator;
import com.seamfix.kyc.bfp.enums.BfpProperty;
import com.seamfix.kyc.bfp.enums.BfpRejectionReason;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.proxy.BasicData;
import com.seamfix.kyc.bfp.proxy.DynamicData;
import com.seamfix.kyc.bfp.proxy.EnrollmentLog;
import com.seamfix.kyc.bfp.proxy.FileSyncNewEntryMarker;
import com.seamfix.kyc.bfp.proxy.MetaData;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.syncs.SyncFile;
import com.seamfix.kyc.client.KsService;
import com.sf.biocapture.entity.UserId;
import com.sf.biocapture.entity.audit.BfpFailureLog;

/**
 * @author dawuzi
 *
 */
public abstract class AbstractSyncFileValidator extends BsClazz implements IValidator {

    private static final String NEW_REGISTRATION = "NS"; // New Registration -> New Registration (Sim Serial) -> NEW_SERIAL -> NS
    private static final String ADDITIONAL_REGISTRATION = "AR"; // Additional Registration -> Additional Registration -> ADD_REG -> AR
    public KsService kservice = new KsService();

    public String getAppVersion(SyncFile syncFile) {
        MetaData meta = (MetaData) syncFile.getProxyItem(ProxyKeyEnum.META);
        if (meta == null) {
            logger.error("Meta data is null");
            return null;
        }
        return meta.getAppVersion();
    }

    public boolean isDeprecatedMacAddress(SyncFile syncFile) {
        boolean deprecated = false;
        try {
            String appVersion = getAppVersion(syncFile);
            float deprecatedMacAddressVersion = 0;
            String kitTag = getKitTag(syncFile);
            if (kitTag.toUpperCase().startsWith("DROID")) {
                //droid
                deprecatedMacAddressVersion = getFloat(BfpProperty.DEPRECATED_MAC_ADDRESS_APP_VERSION_ANDROID);
            } else {
                //windows
                deprecatedMacAddressVersion = getFloat(BfpProperty.DEPRECATED_MAC_ADDRESS_APP_VERSION_WINDOWS);
            }
            deprecated = Float.valueOf(appVersion) > deprecatedMacAddressVersion;

        } catch (IllegalArgumentException e) {
            logger.error("Unable to determine Mac Address Deprecated Status", e);
        }
        return deprecated;
    }

    public String getKitTag(SyncFile syncFile) {
        EnrollmentLog enrollmentLog = (EnrollmentLog) syncFile.getProxyItem(ProxyKeyEnum.EL);
        if (enrollmentLog != null && enrollmentLog.getEnrollmentRef() != null) {
            return enrollmentLog.getEnrollmentRef().getCode();
        }
        return null;
    }

    public String getRegType(SyncFile syncFile) {
        DynamicData dynamicData = (DynamicData) syncFile.getProxyItem(ProxyKeyEnum.DD);
        return dynamicData.getDda11();
    }

    public String getCaptureAgent(SyncFile syncFile) {
        BasicData basicData = (BasicData) syncFile.getProxyItem(ProxyKeyEnum.BD);
        return basicData.getBiometricCaptureAgent();
    }

    public String getMacAddress(SyncFile syncFile) {
        FileSyncNewEntryMarker entryMarker = (FileSyncNewEntryMarker) syncFile.getProxyItem(ProxyKeyEnum.SYNC_MARKER);
        return entryMarker.getMacAddress();
    }
    
    public String getDeviceId(SyncFile syncFile) {
        FileSyncNewEntryMarker entryMarker = (FileSyncNewEntryMarker) syncFile.getProxyItem(ProxyKeyEnum.SYNC_MARKER);
        return entryMarker.getRealtimeDeviceId();
    }

    public String getUniqueId(SyncFile syncFile) {
        UserId userId = (UserId) syncFile.getProxyItem(ProxyKeyEnum.USER_ID);
        return userId.getUniqueId();
    }

    public void addBfpFailureLog(SyncFile syncFile, MsisdnDetail msisdnDetail, BfpRejectionReason bfpRejectionReason) {
        addBfpFailureLog(syncFile, msisdnDetail, bfpRejectionReason, null, null, null, null, null);
    }

    public boolean isNullMsisdnAllowableRegistration(SyncFile syncFile) {

        String regType = getRegType(syncFile);

        if (regType == null) {
            return false;
        }

        regType = regType.toLowerCase();

        if (regType.contains(NEW_REGISTRATION.toLowerCase()) || regType.contains(ADDITIONAL_REGISTRATION.toLowerCase())) {
            return true;
        }
        return false;
    }

    public boolean isClientValidated(SyncFile syncFile) {

        DynamicData dynamicData = (DynamicData) syncFile.getProxyItem(ProxyKeyEnum.DD);

        String validatedStatus = dynamicData.getDa14();
        if (validatedStatus == null) {
            return false;
        } else {
            validatedStatus = validatedStatus.trim().toLowerCase();
            return validatedStatus.equals("verified");
        }
    }

    public void addBfpFailureLog(SyncFile syncFile, MsisdnDetail msisdnDetail, BfpRejectionReason bfpRejectionReason, String reason1, String reason2, String reason3, String reason4, String reason5) {

        BfpFailureLog bfpFailureLog = new BfpFailureLog();

        bfpFailureLog.setUniqueId(getUniqueId(syncFile));
        bfpFailureLog.setMsisdn(msisdnDetail.getMsisdn());
        bfpFailureLog.setFilename(syncFile.getFile().getName());
        bfpFailureLog.setMacAddress(getMacAddress(syncFile));
        bfpFailureLog.setSimSerial(msisdnDetail.getSerial());
        bfpFailureLog.setRejectionReason(bfpRejectionReason.getCode());
        bfpFailureLog.setAppVersion(getAppVersion(syncFile));
        bfpFailureLog.setCaptureAgent(getCaptureAgent(syncFile));
        bfpFailureLog.setKitTag(getKitTag(syncFile));
        bfpFailureLog.setRegType(getRegType(syncFile));

        bfpFailureLog.setReason1(reason1);
        bfpFailureLog.setReason2(reason2);
        bfpFailureLog.setReason3(reason3);
        bfpFailureLog.setReason4(reason4);
        bfpFailureLog.setReason5(reason5);

        syncFile.addBfpFailureLog(bfpFailureLog);

    }
}
