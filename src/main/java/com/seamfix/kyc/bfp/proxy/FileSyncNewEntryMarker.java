package com.seamfix.kyc.bfp.proxy;

import java.io.Serializable;

/**
 * This class is as a simple marker to separate object serialized to a file.
 */
public class FileSyncNewEntryMarker implements Serializable{

	private static final long serialVersionUID = -7961530026117568914L;

	/**
     * Indicates if the object preceding it is the last in the file.
     * <p>
     * The use of this property varys based on context.
     * @see com.sf.biocapture.gui.SyncPanel#handleFileSyncToServer()
     * @see com.sf.biocapture.gui.SyncPanel#handleSynchroniseToFile()
     * @see FtpFileThread#run()
     */
    private boolean last = false;

    /**
     * Used to mark that the all eligible audit trail records have been written to/read from the file.
     * @see com.sf.biocapture.gui.SyncPanel#handleFileSyncToServer()
     * @see com.sf.biocapture.gui.SyncPanel#handleSynchroniseToFile()     */
    private boolean auditTrailSynced = false;

    private String clientTag;
    private String macAddress;
    private String appVersion;

    private String clientKey;

    private boolean blacklisted;
    
    private String realtimeDeviceId;

    /**
     * Getter for {@link #auditTrailSynced}.
     * @return {@link #auditTrailSynced}
     */
    public boolean isAuditTrailSynced() {
        return auditTrailSynced;
    }

    /**
     * Setter for {@link #auditTrailSynced}.
     * @param auditTrailSynced new value for {@link #auditTrailSynced}
     */
    public void setAuditTrailSynced(boolean auditTrailSynced) {
        this.auditTrailSynced = auditTrailSynced;
    }

    /**
     * Getter for {@link #last}.
     * @return {@link #last}
     */
    public boolean isLast() {
        return last;
    }

    /**
     * Setter for {@link #last}.
     * @param last new value for {@link #last}
     */
    public void setLast(boolean last) {
        this.last = last;
    }

	public String getAppVersion() {
		return appVersion;
	}

	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	public String getClientTag() {
		return clientTag;
	}

	public void setClientTag(String clientTag) {
		this.clientTag = clientTag;
	}

	public String getClientKey() {
		return clientKey;
	}

	public void setClientKey(String clientKey) {
		this.clientKey = clientKey;
	}

	public boolean isBlacklisted() {
		return blacklisted;
	}

	public void setBlacklisted(boolean blacklisted) {
		this.blacklisted = blacklisted;
	}

    public String getRealtimeDeviceId() {
        return realtimeDeviceId;
    }

    public void setRealtimeDeviceId(String realtimeDeviceId) {
        this.realtimeDeviceId = realtimeDeviceId;
    }
        
}
