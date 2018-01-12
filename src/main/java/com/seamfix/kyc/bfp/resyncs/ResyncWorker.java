package com.seamfix.kyc.bfp.resyncs;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.GZIPInputStream;


import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.exception.EncryptionException;
import com.seamfix.kyc.bfp.queue.JmsQueue;
import com.seamfix.kyc.bfp.service.AppDS;
import com.seamfix.kyc.bfp.syncs.BiocaptureObjectInputStream;
import com.seamfix.kyc.bfp.syncs.SyncFile;
import com.seamfix.kyc.bfp.syncs.SyncWorker;
import com.seamfix.kyc.bfp.validator.ValidationStatusEnum;
import com.sf.biocapture.entity.UserId;
import com.sf.biocapture.entity.audit.BfpSyncLog;

public class ResyncWorker extends SyncWorker {
    
    public ResyncWorker(AppDS appDs, BioCache cache, JmsQueue jmsQueue) {
        super(appDs, cache, jmsQueue);
        queueKey = "BFPT3YXRW";
    }
    
    @Override
    public SyncItem deserialize(SyncItem item) {
        SyncFile syncFile = null;
        try {
            syncFile = (SyncFile) item;
            logger.debug("Starting to process : " + item.getItemId());
            if (!checkDigest(item.getFile())) {
                // If the digest did not match, return false so file would be moved to processed_syncs folder as this file
                // is considered already processed for resyncs.
                syncFile.setValidationStatusEnum(ValidationStatusEnum.PROCESSED);
                return syncFile;
            }
            
            byte[] bao = decrypt(item.getFile());
            if (bao == null) {
                syncFile.setValidationStatusEnum(ValidationStatusEnum.DECRYPTION_EXCEPTION);
                return syncFile;
            }
            // Create streams to read the actual objects from the decrypted
            // files
            BufferedInputStream syncFis = new BufferedInputStream(new ByteArrayInputStream(bao));

            // Create streams to read the actual objects from the decrypted
            // files
            syncFis = new BufferedInputStream(new ByteArrayInputStream(bao));
            GZIPInputStream gz = new GZIPInputStream(syncFis);
            BiocaptureObjectInputStream objectIn = new BiocaptureObjectInputStream(gz);
            
            syncFile = readDeserializedData(objectIn, syncFile);
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
        } catch (IOException e) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.DECRYPTION_EXCEPTION);
            logger.error("", e);
        } catch (ClassNotFoundException | EncryptionException e) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.ILLEGAL_SYNC_FILE);            
            logger.error("", e);
        }
        return syncFile;
    }
    
    @Override
    public SyncItem process(SyncItem syncItem) {
        
        SyncFile syncFile = (SyncFile) syncItem;
        Map<ProxyKeyEnum, Object> items = syncFile.getProxyItems();
        if (syncFile.getProxyItem(ProxyKeyEnum.SIGN) != null) {
            if (appDs.saveSignatureData(items)) {
                UserId userId = (UserId) syncFile.getProxyItem(ProxyKeyEnum.USER_ID);
                wrapUpSaveRecord(userId, syncItem, items);
                syncFile.setValidationStatusEnum(ValidationStatusEnum.PROCESSED_RESYNCS);
                
                BfpSyncLog bfpSyncLog = new BfpSyncLog();
                bfpSyncLog.setUniqueId(userId.getUniqueId());
                bfpSyncLog.setFileName(syncFile.getFile().getName());
                bfpSyncLog.setSourcePath(syncFile.getFile().getAbsolutePath());
                syncFile.addBfpSyncLog(bfpSyncLog);
            } else {
                syncFile.setValidationStatusEnum(ValidationStatusEnum.NOT_SAVED);
            }
        } else {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.EXCEPTION);
        }
        return syncFile;
    }
    
}
