package com.seamfix.kyc.client;

public class ServerResponse {

	private int status = -1;
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

	@Override
	public String toString() {
		return "ServerResponse [status=" + status + ", message=" + message + "]";
	}
}
