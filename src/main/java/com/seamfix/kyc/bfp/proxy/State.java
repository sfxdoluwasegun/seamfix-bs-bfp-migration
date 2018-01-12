package com.seamfix.kyc.bfp.proxy;

import java.io.Serializable;

public class State implements Serializable{

	private static final long serialVersionUID = -7202659526700338363L;

	private String name;

	private String code;

	private Integer stateId;

	public State() {

	}

	public String getName() {
		return this.code != null ? this.name.toUpperCase().replace("STATE", "").trim(): this.code;
	}

	public void setName(String name) {
		this.name = name;
	}
	public String getCode() {
		return this.code != null ? this.code.toUpperCase().replace("STATE", "").trim(): this.code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Integer getStateId() {
		return stateId;
	}

	public void setStateId(Integer stateId) {
		this.stateId = stateId;
	}

}
