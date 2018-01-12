package com.seamfix.kyc.bfp.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.hibernate.criterion.Restrictions;

import com.seamfix.kyc.bfp.BsClazz;
import com.sf.biocapture.entity.EnrollmentRef;
import com.sf.biocapture.entity.Lga;
import com.sf.biocapture.entity.Setting;
import com.sf.biocapture.entity.SmsActivationRequest;
import com.sf.biocapture.entity.State;
import com.sf.biocapture.entity.UserId;

import nw.orm.core.service.Nworm;

public class DataService extends BsClazz{


	protected Nworm dbService;
	
	protected static final String NAME_FIELD = "name";
	protected static final String RECORD_UNIQUE_ID_FIELD = "uniqueId";
        
	@PostConstruct
	public void init() {
		dbService = Nworm.getInstance();
	}

	public Nworm getDbService() {
		return dbService;
	}

	/**
	 * Only use for small tables
	 * @param clazz
	 * @return
	 */
	public <T> List<T> everything(Class<T> clazz){
		return dbService.getAll(clazz);
	}

	public EnrollmentRef getEnrollmentRef(String code){
		return dbService.getByCriteria(EnrollmentRef.class, Restrictions.eq("code", code).ignoreCase());
	}

	public State getState(String name){
		return dbService.getByCriteria(State.class, Restrictions.eq(NAME_FIELD, name));
	}
        
	public List<Lga> getLgasByName(String name){
		return dbService.getListByCriteria(Lga.class, Restrictions.eq(NAME_FIELD, name).ignoreCase());
	}

	public SmsActivationRequest getSmsActivationRequest(String phone){
		 List<SmsActivationRequest> list = dbService.getListByCriteria(SmsActivationRequest.class, Restrictions.eq("isInitiator", true), Restrictions.eq("phoneNumber", phone));
		 if(list.isEmpty()){
			 return null;
		 }
		 return list.get(0);
	}
	
	public boolean userIdExists(String uniqueId) {
		return dbService.getByCriteria(UserId.class, Restrictions.eq(RECORD_UNIQUE_ID_FIELD, uniqueId)) != null;
	}

	public Setting getFtpServers() {
		return dbService.getByCriteria(Setting.class, Restrictions.eq(NAME_FIELD, "SFTP_SERVERS"));
	}

	/**
	 * @param name
	 * @return the setting with that name in the DB
	 */
	public Setting getSetting(String name) {
		return dbService.getByCriteria(Setting.class, Restrictions.eq(NAME_FIELD, name.trim()).ignoreCase());
	}
}
