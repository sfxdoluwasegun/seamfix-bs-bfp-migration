/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.syncs;

import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.service.AppDS;
import com.sf.biocapture.entity.temp.SubscriberDetail;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import nw.orm.core.exception.NwormQueryException;
import org.hibernate.HibernateException;

/**
 *
 * @author Marcel Ugwu
 * @since Dec 14, 2017 - 8:39:21 AM
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/bio/queue/SubscriberDetail")})
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SubscriberMessageListener extends BsClazz implements MessageListener {

    @Inject
    private AppDS appDs;

    @Override
    public void onMessage(Message message) {
        try {
            ObjectMessage om = (ObjectMessage) message;

            if (om.getObject() instanceof String) {
                String sp = (String) om.getObject();
                processRequest(sp);
                logger.info("Subscriber detail updated successfully : " + sp);
            }
        } catch (JMSException e) {
            logger.error("Exception ", e);
        }
    }

    private void processRequest(String msisdn) {
        try {
            if (msisdn == null || msisdn.isEmpty()) {
                logger.debug("Provided msisdn is null or empty: " + msisdn);
                return;
            }
            SubscriberDetail subscriberDetail = null;
            try {
                subscriberDetail = appDs.getSubscriberDetail(msisdn);
            } catch (NwormQueryException | HibernateException e) {
                logger.error("Unable to retrieve subscriber detail", e);
            }
            if (subscriberDetail == null) {
                logger.debug("Subscriber details without biometrics update not found for msisdn: " + msisdn);
                return;
            }
            subscriberDetail.setBiometricsUpdated(true);
            appDs.updateEntity(subscriberDetail);
        } catch (NwormQueryException e) {
            logger.error("Unable to save susbscriber detail: " + e);
        }
    }
}
