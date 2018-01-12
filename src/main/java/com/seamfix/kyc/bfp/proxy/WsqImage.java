package com.seamfix.kyc.bfp.proxy;

import java.io.Serializable;

public class WsqImage implements Serializable {

	private static final long serialVersionUID = 234554321111L;

	private byte[] wsqData;

	private int nfiq;

	private float compressionRatio;

	private String finger;

	private FingerReasonCodes reasonCode;

	public byte[] getWsqData() {
		return this.wsqData;
	}

	public void setWsqData(byte[] wsqData) {
		this.wsqData = wsqData;
	}

	public int getNfiq() {
		return this.nfiq;
	}

	public void setNfiq(int nfiq) {
		this.nfiq = nfiq;
	}

	public float getCompressionRatio() {
		return this.compressionRatio;
	}

	public void setCompressionRatio(float compressionRatio) {
		this.compressionRatio = compressionRatio;
	}

	public String getFinger() {
		return this.finger;
	}

	public void setFinger(String finger) {
		this.finger = finger;
	}

	public FingerReasonCodes getReasonCode() {
		return reasonCode;
	}

	public void setReasonCode(FingerReasonCodes reasonCode) {
		this.reasonCode = reasonCode;
	}

	/**
	 * Converts to the entity mapped version of the object
	 *
	 * @return
	 */
	public com.sf.biocapture.entity.WsqImage to() {
		com.sf.biocapture.entity.WsqImage w = new com.sf.biocapture.entity.WsqImage();
		w.setCompressionRatio(compressionRatio);
		w.setFinger(finger);
		w.setNfiq(nfiq);
		if(reasonCode != null){
			w.setReasonCode(com.sf.biocapture.entity.enums.FingerReasonCodes.fromString(reasonCode.getValue()));
		}else{
			w.setReasonCode(com.sf.biocapture.entity.enums.FingerReasonCodes.NA);
		}
		w.setWsqData(wsqData);
		return w;
	}

	@Override
	public String toString() {
		return "FingerPrint{finger: " + finger + ", nfiq: " + nfiq + ", reason: " + reasonCode + "}";
	}

}
