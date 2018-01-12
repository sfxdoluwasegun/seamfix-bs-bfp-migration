/**
 *
 */
package com.seamfix.kyc.bfp.validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.seamfix.kyc.bfp.contract.IValidator;

/**
 * @author dawuzi
 *
 */
public class Validators {

    private ValidatorContext validatorContext;
    List<IValidator> validators;

    public Validators(ValidatorContext validatorContext) {
        this.validatorContext = validatorContext;
        init();
    }

    private void init() {
        validatorContext.getAppDS();
        validators = new ArrayList<>();
        addValidators();
        verifyValidators();
        //the list of validators should not be altered from the outside
        validators = Collections.unmodifiableList(validators);
    }

    /**
     *
     */
    private void addValidators() {
        for (ValidatorOrder order : ValidatorOrder.values()) {
            Class<? extends IValidator> validatorClass = order.getValidatorClass();
            IValidator validator;

            try {
                validator = validatorClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("Cannot instantiate validator class : " + validatorClass.getName()
                        + ". Please ensure it has a no argument constructor", e);
            }
            validator.init(validatorContext);
            validators.add(validator);
        }
    }

    private void verifyValidators() {
        Set<Class<? extends IValidator>> seenValidators = new HashSet<>();
        for (IValidator validator : validators) {
            Class<? extends IValidator> clazz = validator.getClass();
            //we should not register the same validator more than once
            if (!seenValidators.add(clazz)) {
                throw new IllegalStateException("Multiple registration for validator class : " + validator.getClass().getName());
            }
        }
    }

    public List<IValidator> getValidators() {
        return validators;
    }
}
