/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.syncs;

import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.client.ActivationResponse;
import com.seamfix.kyc.client.KSClient;
import com.seamfix.kyc.client.KsService;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Clement
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/bio/queue/mockActivation")})
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class MockActivationRequestListener extends BsClazz implements MessageListener{
    
    private final KsService kservice = new KsService();
    
    @Override
    public void onMessage(Message message) {
        try {
            ObjectMessage om = (ObjectMessage) message;

            if (om.getObject() instanceof MockActivationRequest) {
                MockActivationRequest req = (MockActivationRequest) om.getObject();
                processRequest(req);
            }
        } catch (JMSException e) {
            logger.error("Exception ", e);
        }
    }
    
    private void processRequest(MockActivationRequest req) {
        
            if (StringUtils.isEmpty(req.getMsisdn()) ) {
                logger.debug("Msisdn is empty");
                return;
            }
            
            if (StringUtils.isEmpty(req.getUniqueId()) ) {
                logger.debug("Unique Id is empty");
                return;
            }
        
            doActivation(req.getUsecase(),req.getUniqueId(), req.getMsisdn());
            
    }
    
    private ActivationResponse doActivation(String usecase, String uniqueId, String msisdn) {
        KSClient client = kservice.getServiceClient();
        ActivationResponse response = new ActivationResponse();
        try{
            response = client.doActivation(usecase,uniqueId, msisdn);
            return response;
        }
        catch(Exception e){
           response.setCode(-1);
           response.setDescription("Activation Service failed");
           logger.error("Activation Service failed", e);
        }
        return response;
    }
}
