/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.syncs;

import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.enums.BfpProperty;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.service.AppDS;
import com.seamfix.kyc.bfp.validator.ValidationStatusEnum;
import com.sf.biocapture.entity.EnrollmentRef;
import com.sf.biocapture.entity.UserId;
import com.sf.biocapture.entity.audit.BfpSyncLog;
import com.sf.biocapture.entity.audit.BfpFailureLog;
import com.sf.biocapture.entity.enums.BfpSyncStatusEnum;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 *
 * @author Marcel
 * @since 01-Nov-2016, 14:04:08
 */
public class SyncBackup extends BsClazz implements Runnable {

    /**
     * handler for database activities
     */
    protected AppDS appDS;
    /**
     * settings value to determine whether to group backed up files by date
     */
    private boolean includeDateFolderToPath = true;
    /**
     * reference to simple date format object
     */
    private SimpleDateFormat sdf;
    /**
     * grace period allowed before some files with special errors are deleted
     */
    private long DELAY_BEFORE_INCOMPLETE_FTP_FILE_DELETE;

    private long retrialAllowed;

    private long msisdnVerificationRetrialPeriod;

    /**
     * item to be backed up
     */
    private SyncItem syncItem;

    public SyncBackup(AppDS appDS, SyncItem syncItem) {
        this.appDS = appDS;
        this.syncItem = syncItem;
        this.retrialAllowed = getLong(BfpProperty.SIM_SWAP_RETRIAL_PERIOD) * 1000L;
        this.msisdnVerificationRetrialPeriod = getLong(BfpProperty.MSISDN_VERIFICATION_RETRIAL_PERIOD) * 1000L;
        this.includeDateFolderToPath = getBool(BfpProperty.INCLUDE_DATE_TO_FOLDER_PATH);
        this.DELAY_BEFORE_INCOMPLETE_FTP_FILE_DELETE = getLong(BfpProperty.DELAY_BEFORE_INCOMPLETE_FTP_FILE_DELETE) * 1000L;
        this.sdf = new SimpleDateFormat("yyyy-MM-dd");
    }

    private boolean backupItem(SyncItem syncItem) {
        SyncFile syncFile = (SyncFile) syncItem;
        File backupLocation = null;
        String processDay = sdf.format(new Date());
        File pf = syncItem.getFile().getParentFile().getParentFile();
        if (includeDateFolderToPath) {
            backupLocation = new File(pf + "/" + syncFile.getValidationStatusEnum().getBackupLocation() + "/" + processDay);
        } else {
            backupLocation = new File(pf + "/" + syncFile.getValidationStatusEnum().getBackupLocation());
        }

        if (!backupLocation.exists()) {
            backupLocation.mkdirs();
        }
        saveBfpFailureLog(backupLocation, syncFile);
        saveBfpSyncLog(backupLocation, syncFile);
        logger.debug("backup location: " + backupLocation.getAbsolutePath() + ", validation status enum: " + syncFile.getValidationStatusEnum().name());
        boolean success = false;
        if (syncFile.getValidationStatusEnum() == ValidationStatusEnum.DIGEST_EXCEPTION || syncFile.getValidationStatusEnum() == ValidationStatusEnum.DECRYPTION_EXCEPTION) {
            if (new Date().getTime() - syncFile.getFile().lastModified() >= DELAY_BEFORE_INCOMPLETE_FTP_FILE_DELETE) {
                success = deleteFile(syncFile);
            } else {
                success = true;
            }
        } else if (syncFile.getValidationStatusEnum() == ValidationStatusEnum.SIM_SWAP_EXCEPTION) {
            if (new Date().getTime() - syncFile.getFile().lastModified() < retrialAllowed) {
                success = false;
            } else {
                success = moveFile(syncFile, backupLocation);
            }
        } else if (syncFile.getValidationStatusEnum() == ValidationStatusEnum.MSISDN_VERIFICATION_EXCEPTION) {
            if (new Date().getTime() - syncFile.getFile().lastModified() >= msisdnVerificationRetrialPeriod) {
                success = moveFile(syncFile, backupLocation);
            } else {
                success = true;
            }
        } else {
            success = moveFile(syncFile, backupLocation);
        }
        return success;
    }

    @SuppressWarnings("PMD")
    private void saveBfpFailureLog(File backupLocation, SyncFile syncFile) {

        List<BfpFailureLog> bfpFailureLogs = syncFile.getBfpFailureLogs();

        if (bfpFailureLogs != null) {
            for (BfpFailureLog bfpFailureLog : bfpFailureLogs) {
                try {
                    bfpFailureLog.setFileSyncDate(new Date(syncFile.getFile().lastModified()));
                    bfpFailureLog.setTargetPath(backupLocation.getAbsolutePath());
                    bfpFailureLog.setSourcePath(syncFile.getFile().getAbsolutePath());
                    appDS.createEntity(bfpFailureLog);
                } catch (Exception e) {
                    logger.error(syncItem.getFile().getAbsolutePath(), e);
                }
            }
        }
    }

    @SuppressWarnings("PMD")
    private void saveBfpSyncLog(File backupLocation, SyncFile syncFile) {

        BfpSyncStatusEnum bfpSyncStatusEnum = syncFile.getValidationStatusEnum() != null && (syncFile.getValidationStatusEnum().equals(ValidationStatusEnum.PROCESSED)
                || syncFile.getValidationStatusEnum().equals(ValidationStatusEnum.PROCESSED_RESYNCS)) ? BfpSyncStatusEnum.SUCCESS : BfpSyncStatusEnum.ERROR;

        List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
        List<MsisdnDetail> failedMsisdns = syncFile.getFailedMsisdnDetails();
        List<MsisdnDetail> totalMsisdns = new ArrayList<>();
        if (msisdns != null) {
            totalMsisdns.addAll(msisdns);
        }
        if (failedMsisdns != null) {
            totalMsisdns.addAll(failedMsisdns);
        }
        Object userIdObject = syncFile.getProxyItem(ProxyKeyEnum.USER_ID);
        Object enrollmentRefObject = syncFile.getProxyItem(ProxyKeyEnum.SAVED_EF);
        EnrollmentRef enrollmentRef = enrollmentRefObject == null ? null : (EnrollmentRef) enrollmentRefObject;
        totalMsisdns.stream().forEach((msisdnDetail) -> {
            try {
                String uniqueId = "";
                if (userIdObject != null) {
                    UserId userId = (UserId) userIdObject;
                    uniqueId = userId.getUniqueId() != null ? userId.getUniqueId().trim() : userId.getUniqueId();
                }
                String msisdn = msisdnDetail.getMsisdn() != null ? msisdnDetail.getMsisdn().trim() : msisdnDetail.getMsisdn();
                String simSerial = msisdnDetail.getSerial() != null ? msisdnDetail.getSerial().trim() : msisdnDetail.getSerial();
                BfpSyncLog syncLog = appDS.getBfpSyncLog(uniqueId, msisdn, simSerial);
                if (syncLog != null) {
                    syncLog.setMsisdn(msisdn);
                    syncLog.setBfpSyncStatusEnum(bfpSyncStatusEnum);
                    syncLog.setSimSerial(simSerial);
                    syncLog.setFileSyncDate(new Date(syncFile.getFile().lastModified()));
                    syncLog.setTargetPath(backupLocation.getAbsolutePath());
                    syncLog.setEnrollmentRef(enrollmentRef);
                    appDS.updateEntity(syncLog);
                } else {
                    syncLog = new BfpSyncLog();
                    syncLog.setBfpSyncStatusEnum(bfpSyncStatusEnum);
                    syncLog.setUniqueId(uniqueId);
                    syncLog.setFileName(syncFile.getFile().getName());
                    syncLog.setSourcePath(syncFile.getFile().getAbsolutePath());
                    syncLog.setMsisdn(msisdn);
                    syncLog.setSimSerial(simSerial);
                    syncLog.setEnrollmentRef(enrollmentRef);
                    syncLog.setFileSyncDate(new Date(syncFile.getFile().lastModified()));
                    syncLog.setTargetPath(backupLocation.getAbsolutePath());
                    appDS.createEntity(syncLog);
                }
            } catch (Exception e) {
                logger.error(syncItem.getFile().getAbsolutePath(), e);
            }
        });
    }

    private boolean deleteFile(SyncFile syncFile) {
        try {
            //is file still available to be deleted
            if (syncFile.getFile().exists()) {
                FileUtils.forceDelete(syncFile.getFile());
            }
        } catch (IOException e) {
            logger.error("", e);
            return false;
        }
        return true;
    }

    private boolean moveFile(SyncFile syncFile, File backupLocation) {
        try {
            File bsync = new File(backupLocation + "/" + syncFile.getFile().getName());
            if (!bsync.exists()) {
                //is file still available to be moved
                if (syncFile.getFile().exists()) {
                    FileUtils.moveFileToDirectory(syncFile.getFile(), backupLocation, true);
                }
            } else {
                //is file still available to be deleted
                if (syncFile.getFile().exists()) {
                    FileUtils.forceDelete(syncFile.getFile());
                }
            }
            //starts a recursive call to ensure file gets moved
            if (!bsync.exists()) {
                moveFile(syncFile, backupLocation);
            }
        } catch (IOException e) {
            logger.error("File Backup unsuccessful: " + syncFile.getFile().getAbsolutePath(), e);
            return false;
        }

        return true;
    }

    @SuppressWarnings("PMD")
    @Override
    public void run() {
        try {
            backupItem(syncItem);
        } catch (Exception e) {
            logger.error(syncItem.getFile().getAbsolutePath(), e);
        }
    }

}
