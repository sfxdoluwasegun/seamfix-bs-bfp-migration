/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.contract;

import com.seamfix.kyc.bfp.validator.ValidatorContext;

/**
 *
 * @author Marcel
 * @since 27-Oct-2016, 16:08:11
 */
public interface IValidator {
	
	/**
	 * This is first method invoked when a new instance of the validator is created. 
	 * All initialization that depend on the values in the context should be done here
	 * @param validatorContext
	 */
	public void init(ValidatorContext validatorContext);

    public SyncItem validate(SyncItem syncItem);

    public boolean isPrerequisite();

    public boolean skip();
    
}
