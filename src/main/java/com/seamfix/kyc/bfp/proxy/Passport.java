package com.seamfix.kyc.bfp.proxy;

import java.io.Serializable;

import com.sf.biocapture.entity.PassportData;

public class Passport implements Serializable{

	/**
	 *
	 */
	private static final long serialVersionUID = -164764090279822128L;

	private byte[] passportData;

	private Integer faceCount = Integer.valueOf(0);

	public byte[] getPassportData() {
		return passportData;
	}

	public void setPassportData(byte[] passportData) {
		this.passportData = passportData;
	}

	public Integer getFaceCount() {
		return faceCount;
	}

	public void setFaceCount(Integer faceCount) {
		this.faceCount = faceCount;
	}

	public PassportData toPassport(){
		PassportData pd = new PassportData();
		pd.setFaceCount(getFaceCount());
		pd.setPassportData(getPassportData());

		return pd;
	}

}
