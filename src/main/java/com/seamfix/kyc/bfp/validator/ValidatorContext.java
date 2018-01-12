/**
 *
 */
package com.seamfix.kyc.bfp.validator;

import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.service.AppDS;

/**
 * @author dawuzi
 *
 */
public class ValidatorContext {

    private AppDS appDS;
    private BioCache bioCache;
    private String workerId;

    public AppDS getAppDS() {
        return appDS;
    }

    public void setAppDS(AppDS appDS) {
        this.appDS = appDS;
    }

    public BioCache getBioCache() {
        return bioCache;
    }

    public void setBioCache(BioCache bioCache) {
        this.bioCache = bioCache;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
}
