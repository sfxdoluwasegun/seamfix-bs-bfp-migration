/**
 * 
 */
package com.seamfix.kyc.bfp.missingmsisdn;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;

import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.buffer.BaseEntityBlockingBuffer;
import com.seamfix.kyc.bfp.contract.IBlockingBuffer;
import com.seamfix.kyc.bfp.enums.BfpProperty;
import com.seamfix.kyc.bfp.service.AppDS;
import com.sf.biocapture.entity.SmsActivationRequest;

/**
 * @author dawuzi
 *
 */

@Singleton
public class MissingMsisdnRunnable extends BsClazz implements Runnable {

	
	@Inject
	private BioCache cache;

	@Inject
	private AppDS appDs;
	
	@Resource
    private ManagedScheduledExecutorService managedScheduledExecutorService;
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		logger.debug("MissingMsisdnRunnable run start");
		IBlockingBuffer<SmsActivationRequest> buffer;
		Integer queueSize = appProps.getInt("missing-msisdn-queue-size", 20000);
		buffer = new BaseEntityBlockingBuffer<>(queueSize);
		MissingMsisdnLoader loader = new MissingMsisdnLoader(appDs, cache, buffer);
		
		logger.debug("about to schedule");
		ScheduledFuture<?> loaderScheduledFuture = managedScheduledExecutorService.schedule(loader, 1, TimeUnit.SECONDS);
		
		logger.debug("after schedule");
		
		int missingMsisdnWorkerSize = appProps.getInt("missing-msisdn-worker-size", 1);
		
		logger.debug("missingMsisdnWorkerSize : "+missingMsisdnWorkerSize);
		
		MissingMsisdnWorker[] workers = new MissingMsisdnWorker[missingMsisdnWorkerSize];
		
		for(int x=0; x<missingMsisdnWorkerSize; x++){
			workers[x] = new MissingMsisdnWorker(appDs, cache, buffer);
			managedScheduledExecutorService.schedule(workers[x], 2, TimeUnit.SECONDS);
		}
		
		logger.debug("after scheduling workers");
		
		int sleepTime = getInt(BfpProperty.MISSING_MSISDN_LOADER_SLEEP_TIME);
		
		while(!loaderScheduledFuture.isDone()){
			logger.debug("in while loop");
			try {
				logger.debug("loader not done. about to sleep");
				Thread.sleep(sleepTime*1000);
			} catch (InterruptedException e) {
				logger.error("Error Sleeping", e);
			}
		}
		
		logger.debug("loader is done");
		
		for(int x=0; x<missingMsisdnWorkerSize; x++){
			MissingMsisdnWorker missingMsisdnWorker = workers[x];
			missingMsisdnWorker.setTerminate(true); 
		}
		
		logger.debug("MissingMsisdnRunnable run done");
		
	}

}
