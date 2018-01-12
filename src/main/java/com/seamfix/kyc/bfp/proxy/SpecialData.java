package com.seamfix.kyc.bfp.proxy;

import java.io.Serializable;

import javax.persistence.Column;

public class SpecialData implements Serializable{

	private static final long serialVersionUID = -7366412615897401280L;

	@Column(name = "BIOMETRIC_DATA", nullable = false)
	private byte[] biometricData;

	@Column(name = "BIOMETRIC_DATA_TYPE", nullable = false)
	private String biometricDataType;

	@Column(name = "REASON", nullable = true)
	private String reason;

	public byte[] getBiometricData() {
		return this.biometricData;
	}

	public void setBiometricData(byte[] biometricData) {
		this.biometricData = biometricData;
	}

	public String getBiometricDataType() {
		return this.biometricDataType;
	}

	public void setBiometricDataType(String biometricDataType) {
		this.biometricDataType = biometricDataType;
	}

	public String getReason() {
		return this.reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public com.sf.biocapture.entity.SpecialData to() {
		com.sf.biocapture.entity.SpecialData sd = new com.sf.biocapture.entity.SpecialData();
		sd.setBiometricData(biometricData);
		sd.setBiometricDataType(biometricDataType);
		sd.setReason(reason);
		return sd;
	}

}
