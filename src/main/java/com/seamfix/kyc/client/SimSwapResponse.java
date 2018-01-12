package com.seamfix.kyc.client;

/**
 * 
 * @author Nnanna
 * @since 19/11/2016
 *
 */
public class SimSwapResponse {
	private String code;
	private String message;
	
	public SimSwapResponse(){}
	
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	@Override
	public String toString() {
		return "SimSwapResponse [message=" + message + ", status=" + code + "]";
	}
}