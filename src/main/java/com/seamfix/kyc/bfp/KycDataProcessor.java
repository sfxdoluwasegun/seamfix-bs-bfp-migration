package com.seamfix.kyc.bfp;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import nw.commons.NeemClazz;
import nw.orm.core.service.Nworm;

@Startup
@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
public class KycDataProcessor extends NeemClazz{

	@PostConstruct
	public void start(){
            logger.debug("Waking up Biocapture Services");
            Nworm dbService = Nworm.getInstance();
            dbService.enableJTA();
            dbService.enableSessionByContext();
            logger.debug("Biocapture BFP is awake");
	}

	@PreDestroy
	public void stop(){
            Nworm.getInstance().closeFactory();
            logger.debug("Goodbye BFP Services");
	}

	public static void main(String[] args) {
            System.out.println(Boolean.valueOf("true"));
	}

}
