/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.validator;

import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;

import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.contract.IValidator;
import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.enums.BfpProperty;
import com.seamfix.kyc.bfp.enums.BfpRejectionReason;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.proxy.EnrollmentLog;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.service.AppDS;
import com.seamfix.kyc.bfp.syncs.SyncFile;
import com.seamfix.kyc.bfp.syncs.SyncUtils;
import com.sf.biocapture.entity.audit.BfpFailureLog;

/**
 *
 * @author Marcel
 * @since 28-Oct-2016, 10:30:48
 */
public class CaptureTimeValidator extends AbstractSyncFileValidator implements IValidator {

    private boolean prerequisite = true;
    private boolean skip;
    private AppDS appDS;
    private SyncUtils syncUtils = new SyncUtils();
    private LocalTime validRegStartTime = null;
    private LocalTime validRegEndTime = null;
    private final Pattern COLON_PATTERN = Pattern.compile(":");
    private BioCache cache;
    private Integer cacheTimeoutLocal;
    @SuppressWarnings("unused")
    private ValidatorContext validatorContext;

    public CaptureTimeValidator() {
        defineConditions();
    }

    private void defineConditions() {
        skip = getBool(BfpProperty.SKIP_CAPTURE_TIME_VALIDATION);
        cacheTimeoutLocal = getInt(BfpProperty.RESET_VALID_REGISTRATION_CACHE_TIMEOUT);
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
        String kitTag = getKitTag(syncFile);
        Timestamp registrationTimestamp = syncUtils.getRegistrationTimestamp(enrollmentLog.to());

        if (syncUtils.isWithinTimeRange(registrationTimestamp, validRegStartTime, validRegEndTime)) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
        } else {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.INVALID_REGISTRATION_TIME);
            @SuppressWarnings("unchecked")
            List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
            String appVersion = getAppVersion(syncFile);
            String captureAgent = getCaptureAgent(syncFile);
            String regType = getRegType(syncFile);
            String uniqueId = getUniqueId(syncFile);
            String macAddress = getMacAddress(syncFile);
            for (MsisdnDetail msisdnDetail : msisdns) {
                BfpFailureLog bfpFailureLog = new BfpFailureLog();
                bfpFailureLog.setUniqueId(uniqueId);
                bfpFailureLog.setMsisdn(msisdnDetail.getMsisdn());
                bfpFailureLog.setFilename(syncFile.getFile().getName());
                bfpFailureLog.setMacAddress(macAddress);
                bfpFailureLog.setSimSerial(msisdnDetail.getSerial());
                bfpFailureLog.setRejectionReason(BfpRejectionReason.BAD_REGISTRATION_TIME.getCode());
                bfpFailureLog.setAppVersion(appVersion);
                bfpFailureLog.setCaptureAgent(captureAgent);
                bfpFailureLog.setKitTag(kitTag);
                bfpFailureLog.setRegType(regType);
                bfpFailureLog.setReason1(registrationTimestamp.toString());
                bfpFailureLog.setReason2(validRegStartTime.toString());
                bfpFailureLog.setReason3(validRegEndTime.toString());
                syncFile.addBfpFailureLog(bfpFailureLog);
            }
            logger.debug(validatorContext.getWorkerId() + " Failed capture time validation. reg time : "
                    + registrationTimestamp + ", start time : " + validRegStartTime + ", end time : " + validRegEndTime + ". Is prerequisite: " + isPrerequisite());

        }
        return syncFile;
    }

    private void reloadValidTimes() {

        if (!isValidRegistrationTimeStale()) {
            logger.debug(validatorContext.getWorkerId() + " valid time ranges is not stale. validRegStartTime : " + validRegStartTime + ", validRegEndTime : " + validRegEndTime);
            return;
        }

        String start = appDS.getSettingValue("VALID_REGISTRATION_START_TIME", "6:00", "The valid start time in the day for validating time of registration. The accepted value should be a 24 hour time in the format HH:mm ie 21:00", true);
        String end = appDS.getSettingValue("VALID_REGISTRATION_END_TIME", "21:30", "The valid end time in the day for validating time of registration. The accepted value should be a 24 hour time in the format HH:mm ie 21:00", true);

        LocalTime startTime = getLocalTime(start, "6:00");
        LocalTime endTime = getLocalTime(end, "21:30");

        if (startTime.equals(endTime) || startTime.isAfter(endTime)) {
            logger.warn(validatorContext.getWorkerId() + " The start time ("
                    + startTime + ") is after or the same as the end time("
                    + endTime + "). So we use the default 6am till 9:30pm");

            startTime = LocalTime.of(6, 0);
            endTime = LocalTime.of(21, 30);
        }

        validRegStartTime = startTime;
        validRegEndTime = endTime;
    }

    private boolean isValidRegistrationTimeStale() {
        String key = "RESET_VALID_REGISTRATION_TIME";
        if (validRegStartTime == null || validRegEndTime == null) {
            cache.setItem(key, "L", cacheTimeoutLocal * 60);
            return true;
        }
        Object item = cache.getItem(key);
        if (item == null) {
            cache.setItem(key, "L", cacheTimeoutLocal * 60);
            return true;
        }

        return false;
    }

    private LocalTime getLocalTime(String hourAndMin, String defaultVal) {
        LocalTime time = null;
        int[] values;
        try {
            String[] valuesString = COLON_PATTERN.split(hourAndMin);
            if (valuesString.length != 2) {
                logger.error(validatorContext.getWorkerId() + " Invalid time format : " + hourAndMin);
                values = getIntArray(COLON_PATTERN.split(defaultVal));
            } else {
                values = getIntArray(valuesString);
            }
            time = LocalTime.of(values[0], values[1]);
        } catch (DateTimeException e) {
            logger.error(validatorContext.getWorkerId() + " EXCEPTION. Invalid time format : " + hourAndMin + ". Exception message : " + e.getMessage());
            values = getIntArray(COLON_PATTERN.split(defaultVal));
        }
        return time;
    }

    private int[] getIntArray(String[] intStrings) {
        int arrLength = intStrings.length;
        int[] intArray = new int[arrLength];
        for (int x = 0; x < arrLength; x++) {
            intArray[x] = Integer.parseInt(intStrings[x].trim());
        }
        return intArray;
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
        this.cache = validatorContext.getBioCache();
        reloadValidTimes();
    }

}
