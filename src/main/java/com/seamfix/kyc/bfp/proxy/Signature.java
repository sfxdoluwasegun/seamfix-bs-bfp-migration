package com.seamfix.kyc.bfp.proxy;

import java.io.Serializable;

import com.sf.biocapture.entity.SignatureData;

public class Signature implements Serializable {

	private static final long serialVersionUID = 7673175173984965731L;
	private byte[] signatureData;

	public byte[] getSignatureData() {
		return this.signatureData;
	}

	public void setSignatureData(byte[] signatureData) {
		this.signatureData = signatureData;
	}

	public SignatureData to() {
		SignatureData sd = new SignatureData();
		sd.setSignatureData(signatureData);
		return sd;
	}

}
