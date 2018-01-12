package com.seamfix.kyc.bfp.proxy;

import java.io.Serializable;
import java.sql.Timestamp;

public class MsisdnDetail implements Serializable{

	private static final long serialVersionUID = -2345671283292L;

	private String msisdn;

	private String serial;

	private SubscriberTypes subscriberType;

	private Boolean newSubscriber;

	private Boolean zap;

	private Timestamp activationTimestamp;

	private Boolean activationStatus;

	private Timestamp msisdnPartKey;
        
        private Boolean yellowAccountEnabled;
        
        private String yellowAccountTypes;

	public String getMsisdn() {
		return this.msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public String getSerial() {
		return this.serial;
	}

	public void setSerial(String serial) {
		this.serial = serial;
	}

	public SubscriberTypes getSubscriberType() {
		return this.subscriberType;
	}

	public void setSubscriberType(SubscriberTypes subscriberType) {
		this.subscriberType = subscriberType;
	}

	public Boolean getNewSubscriber() {
		return this.newSubscriber;
	}

	public void setNewSubscriber(Boolean newSubscriber) {
		this.newSubscriber = newSubscriber;
	}

	public Boolean getZap() {
		return this.zap;
	}

	public void setZap(Boolean zap) {
		this.zap = zap;
	}

	public Timestamp getActivationTimestamp() {
		return this.activationTimestamp;
	}

	public void setActivationTimestamp(Timestamp activationTimestamp) {
		this.activationTimestamp = activationTimestamp;
	}

	public Boolean getActivationStatus() {
		return this.activationStatus;
	}

	public void setActivationStatus(Boolean activationStatus) {
		this.activationStatus = activationStatus;
	}

	public Timestamp getMsisdnPartKey() {
		return msisdnPartKey;
	}

	public void setMsisdnPartKey(Timestamp msisdnPartKey) {
		this.msisdnPartKey = msisdnPartKey;
	}

	public com.sf.biocapture.entity.MsisdnDetail to(){
		com.sf.biocapture.entity.MsisdnDetail m = new com.sf.biocapture.entity.MsisdnDetail();
		m.setActivationStatus(activationStatus);
		m.setActivationTimestamp(activationTimestamp);
		m.setMsisdn(msisdn);
		m.setNewSubscriber(newSubscriber);
		m.setSerial(null == serial || "".equals(serial) ? "NA" : serial);
		m.setZap(zap);
                m.setYellowAccountEnabled(yellowAccountEnabled);
                m.setYellowAccountTypes(yellowAccountTypes);
		m.setSubscriberType(com.sf.biocapture.entity.enums.SubscriberTypes.valueOf(subscriberType.getValue()));

		return m;
	}

	@Override
	public String toString() {
		return "MSISDN {msisdn: " + msisdn + ", serial: " + serial + "}";
	}

        public Boolean getYellowAccountEnabled() {
            return yellowAccountEnabled;
        }

        public void setYellowAccountEnabled(Boolean yellowAccountEnabled) {
            this.yellowAccountEnabled = yellowAccountEnabled;
        }

        public String getYellowAccountTypes() {
            return yellowAccountTypes;
        }

        public void setYellowAccountTypes(String yellowAccountTypes) {
            this.yellowAccountTypes = yellowAccountTypes;
        }        
}
