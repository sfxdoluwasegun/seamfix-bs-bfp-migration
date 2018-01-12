/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.queue;

import com.seamfix.kyc.bfp.BsClazz;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.DeliveryMode;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Queue;

/**
 *
 * @author Marcel
 * @since Dec 13, 2017 - 5:48:02 PM
 */
@Stateless
public class JmsQueue extends BsClazz {

    @Inject
    protected JMSContext ctx;

    @Resource(lookup = "java:/bio/queue/SubscriberDetail")
    private Queue subscriberDetailQueue;

    public boolean queueSubscriberMsisdn(String msisdn) {
        Message msg = ctx.createObjectMessage(msisdn);
        ctx.createProducer()
                .setDeliveryMode(DeliveryMode.PERSISTENT)
                .send(subscriberDetailQueue, msg);
        return true;
    }
}
