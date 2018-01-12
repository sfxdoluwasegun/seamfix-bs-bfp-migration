/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.validator;

import java.util.List;
import java.util.regex.Pattern;

import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.contract.IValidator;
import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.enums.BfpProperty;
import com.seamfix.kyc.bfp.enums.BfpRejectionReason;
import com.seamfix.kyc.bfp.enums.ProxyKeyEnum;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.service.AppDS;
import com.seamfix.kyc.bfp.syncs.SyncFile;
import com.sf.biocapture.entity.EnrollmentRef;

/**
 *
 * @author Marcel
 * @since 27-Oct-2016, 16:20:56
 */
public class TaggedKitValidator extends AbstractSyncFileValidator implements IValidator {

    private boolean prerequisite = true;
    private boolean skip;
    private AppDS appDS;
    private boolean createNewTag;
    private ValidatorContext validatorContext;
	private final Pattern SPACE_PATTERN = Pattern.compile(" ");

    public TaggedKitValidator() {
        defineConditions();
    }

    private void defineConditions() {
        skip = getBool(BfpProperty.SKIP_TAG_VALIDATION);
        createNewTag = getBool(BfpProperty.CREATE_NON_EXISTING_TAG);
    }

    @Override
    public SyncItem validate(SyncItem syncItem) {
        if (syncItem == null) {
            logger.debug(validatorContext.getWorkerId() + " sync item is null");
            return syncItem;
        }
        SyncFile syncFile = (SyncFile) syncItem;
        if (skip()) {
            syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
            return syncFile;
        }
        String code = getKitTag(syncFile);
        
        if(!createNewTag && !tagExistsInEnrollmentRef(code)){
        	
			logger.debug(validatorContext.getWorkerId()+" code : "+code+" does not exists in enrollment ref");
			
            syncFile.setValidationStatusEnum(ValidationStatusEnum.KIT_NOT_TAGGED);
            @SuppressWarnings("unchecked")
            List<MsisdnDetail> msisdns = (List<MsisdnDetail>) syncFile.getProxyItem(ProxyKeyEnum.MSISDNS);
            
            for (MsisdnDetail msisdnDetail : msisdns) {
                addBfpFailureLog(syncFile, msisdnDetail, BfpRejectionReason.KIT_NOT_TAGGED); 
            }
            
            logger.debug(validatorContext.getWorkerId() + " Failed tag validation. Is prerequisite: " + isPrerequisite());
       } else {
           syncFile.setValidationStatusEnum(ValidationStatusEnum.VALID);
       }

        return syncFile;
    }
    
	/**
	 * @param tag
	 * @return true if the tag exists in enrollment ref
	 */
	private boolean tagExistsInEnrollmentRef(String tag) {
		
		BioCache cache = validatorContext.getBioCache();
		
		String cacheKey = "KIT-TAG-" + SPACE_PATTERN.matcher(tag).replaceAll("_");
		String exists = (String) cache.getItem(cacheKey);
		if(exists == null){
			EnrollmentRef enrollmentRef = appDS.getEnrollmentRef(tag);
			if(enrollmentRef == null){
				exists = "N";
			} else {
				exists = "Y";
			}
			int tagExistCacheTimeout = appProps.getInt("tag-exists-cache-time-out", 120);
			cache.setItem(cacheKey, exists, tagExistCacheTimeout * 60);
			logger.debug(validatorContext.getWorkerId()+" exists not in cache : "+exists+", cache key : "+cacheKey);
		} else {
			logger.debug(validatorContext.getWorkerId()+" exists in cache : "+exists+", cache key : "+cacheKey);
		}
		return exists.equals("Y");
	}
    

    @Override
    public boolean skip() {
        return skip;
    }

    @Override
    public boolean isPrerequisite() {
        return prerequisite;
    }

    @Override
    public void init(ValidatorContext validatorContext) {
        this.validatorContext = validatorContext;
        this.appDS = validatorContext.getAppDS();
    }
}
