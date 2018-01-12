package com.seamfix.kyc.bfp.syncs;

import com.seamfix.kyc.bfp.validator.ValidationStatusEnum;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.sf.biocapture.entity.audit.BfpFailureLog;
import com.sf.biocapture.entity.audit.BfpSyncLog;
import java.util.EnumMap;
import java.util.Map;

public class SyncFile implements SyncItem {

    private File file;
    private final List<String> recordMsisdns = new ArrayList<>();;
    private ValidationStatusEnum validationStatusEnum;
    private final Map<ProxyKeyEnum, Object> proxyItems = new EnumMap<>(ProxyKeyEnum.class);
    /**
     * keeps track of possible failure logs generated during validation
     */
    private final List<BfpFailureLog> bfpFailureLogs = new ArrayList<>();;
    /**
     * keeps track of possible sync logs generated during validation or for
     * successful transaction
     */
    private final List<BfpSyncLog> bfpSyncLogs = new ArrayList<>();;

    private final List<MsisdnDetail> failedMsisdnDetails = new ArrayList<>();

    @Override
    public String getItemId() {
        return "SFXSF_" + file.getName();
    }

    @Override
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public boolean delete() {
        return file.delete();

    }

    @Override
    public List<String> recordMsisdns() {
        return this.recordMsisdns;
    }

    @Override
    public void addMsisdn(String msisdn) {
        this.recordMsisdns.add(msisdn);
    }

    @Override
    public boolean endMarker() {
        return getItemId().equals("SFXSF_END.MARKER");
    }

    public ValidationStatusEnum getValidationStatusEnum() {
        return validationStatusEnum;
    }

    public void setValidationStatusEnum(ValidationStatusEnum validationStatusEnum) {
        this.validationStatusEnum = validationStatusEnum;
    }

    public Map<ProxyKeyEnum, Object> getProxyItems() {
        return proxyItems;
    }

    public void addProxyItem(ProxyKeyEnum proxyKeyEnum, Object item) {
        proxyItems.put(proxyKeyEnum, item);
    }

    public Object getProxyItem(ProxyKeyEnum proxyKeyEnum) {
        return proxyItems.get(proxyKeyEnum);
    }

    public List<BfpFailureLog> getBfpFailureLogs() {
        return bfpFailureLogs;
    }

    public void addBfpFailureLog(BfpFailureLog bfpFailureLog) {
        bfpFailureLogs.add(bfpFailureLog);
    }

    public List<BfpSyncLog> getBfpSyncLogs() {
        return bfpSyncLogs;
    }

    public void addBfpSyncLog(BfpSyncLog bfpSyncLog) {
        bfpSyncLogs.add(bfpSyncLog);
    }

    public List<MsisdnDetail> getFailedMsisdnDetails() {
        return failedMsisdnDetails;
    }

    public void addFailedMsisdnDetails(List<MsisdnDetail> failedMsisdnDetails) {
        this.failedMsisdnDetails.addAll(failedMsisdnDetails);
    }

    public void addFailedMsisdnDetails(MsisdnDetail failedMsisdnDetail) {
        failedMsisdnDetails.add(failedMsisdnDetail);
    }

}
