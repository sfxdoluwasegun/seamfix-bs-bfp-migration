package com.seamfix.kyc.bfp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;

import org.keyczar.Crypter;
import org.keyczar.exceptions.KeyczarException;

import com.seamfix.kyc.bfp.buffer.BaseEntityBlockingBuffer;
import com.seamfix.kyc.bfp.contract.IBlockingBuffer;
import com.seamfix.kyc.bfp.missingmsisdn.MissingMsisdnLoader;
import com.seamfix.kyc.bfp.missingmsisdn.MissingMsisdnWorker;
import com.seamfix.kyc.bfp.queue.JmsQueue;
import com.seamfix.kyc.bfp.resyncs.ResyncReader;
import com.seamfix.kyc.bfp.resyncs.ResyncWorker;
import com.seamfix.kyc.bfp.service.AppDS;
import com.seamfix.kyc.bfp.syncs.SyncReader;
import com.seamfix.kyc.bfp.syncs.SyncWorker;
import com.seamfix.kyc.bfp.telecommaster.TelecomMasterLoader;
import com.seamfix.kyc.bfp.telecommaster.TelecomMasterWorker;
import com.seamfix.kyc.bfp.units.ProductionUnit;
import com.seamfix.kyc.bfp.units.WorkerUnit;
import com.sf.biocapture.entity.Setting;
import com.sf.biocapture.entity.SmsActivationRequest;
import com.sf.biocapture.entity.TelecomMasterRecords;

import sfx.crypto.CryptoReader;

/**
 * Application module launcher
 *
 * @author Ogwara O. Rowland
 *
 */
@Startup
@Singleton
@DependsOn("KycDataProcessor")
public class AppLauncher extends BsClazz {

	private Integer workerSize = 8;
	
	@Resource
	private ManagedScheduledExecutorService mes;

	@Inject
	private BioCache cache;

	@Inject
	private AppDS appDs;
        
        @Inject
        private JmsQueue jmsQueue;

	private Crypter kycCrypter;

	private BlockingBuffer buffer;
	private BlockingBuffer resyncBuffer;
	private List<ScheduledFuture<?>> pus;

	@PostConstruct
	protected void init() {
		
		workerSize = appProps.getInt("sync-worker-size", 10);
		
		Integer syncQueueSize = appProps.getInt("sync-queue-size", 2000);
		
		buffer = new BlockingBuffer(syncQueueSize);
		resyncBuffer = new BlockingBuffer(syncQueueSize);
		
		pus = new ArrayList<>();

		try {
			kycCrypter = new Crypter(new CryptoReader("map"));
			launch();
		} catch (KeyczarException e) {
			logger.error("Exception initializing crypto", e);
		}
		logger.debug("Done Lauching Application Modules");
	}

	@PreDestroy
	protected void close() {
		for (ScheduledFuture<?> pu : pus) {
			pu.cancel(true);
		}
	}

	@Asynchronous
	protected void launch() {
		workers();
		launchMsisdnProcesses();
		launchTelecomMasterProcesses();
		
//		we now read the sftp server locations setting name from property file. The default being the previous name
		String sftpSettingName = appProps.getProperty("SFTP_SERVERS_SETTING_NAME", "SFTP_SERVERS");
		
		logger.debug("sftpSettingName : "+sftpSettingName);
		
		Setting ftpSettings = appDs.getSetting(sftpSettingName);
		if (ftpSettings != null && null != ftpSettings.getValue()) {
			String[] servers = ftpSettings.getValue().split(",");
		
			logger.debug("sftpSettingName value : "+ftpSettings.getValue());
		
			for (String server : servers) {
				logger.debug("server : "+server);
				
				SyncReader sr = new SyncReader(cache);
				sr.sourceLocation(server);
				sr.setBuffer(buffer);
				ProductionUnit pu = new ProductionUnit(sr);
				ScheduledFuture<?> s = mes.schedule(pu, 3, TimeUnit.SECONDS);

				// Resync Processor
				ResyncReader rr = new ResyncReader(cache);
				rr.sourceLocation(server);
				rr.setBuffer(resyncBuffer);
				ProductionUnit rpu = new ProductionUnit(rr);
				ScheduledFuture<?> rs = mes.schedule(rpu, 3, TimeUnit.SECONDS);
				pus.add(s);
				pus.add(rs);
			}
		} else {
			logger.debug("No Sync Location has been set up");
		}
		
	}

	@Asynchronous
	private void launchTelecomMasterProcesses() {
		
		logger.debug("launchTelecomMasterProcesses entered");
		int workerSize = appProps.getInt("telecom-master-worker-size", 1);
		
		IBlockingBuffer<TelecomMasterRecords> buffer;
		Integer queueSize = appProps.getInt("telecom-master-queue-size", 20000);
		buffer = new BaseEntityBlockingBuffer<>(queueSize);
                if (workerSize > 0) {
                    TelecomMasterLoader loader = new TelecomMasterLoader(appDs, cache, buffer);
                    logger.debug("about to schedule");
                    ScheduledFuture<?> loaderScheduledFuture = mes.schedule(loader, 1, TimeUnit.SECONDS);

                    pus.add(loaderScheduledFuture);
                }
		
		
		logger.debug("after schedule");
		
		
		logger.debug("workerSize : "+workerSize);
		
		for(int x=0; x<workerSize; x++){
			TelecomMasterWorker worker = new TelecomMasterWorker(appDs, cache, buffer);
			ScheduledFuture<?> scheduledFuture = mes.schedule(worker, 3, TimeUnit.SECONDS);
			pus.add(scheduledFuture);
		}
		
		logger.debug("after launchTelecomMasterProcesses");
	}

	@Asynchronous
	protected void launchMsisdnProcesses() {

		logger.debug("launchMsisdnProcesses entered");
		
		IBlockingBuffer<SmsActivationRequest> buffer;
		Integer queueSize = appProps.getInt("missing-msisdn-queue-size", 20000);
		buffer = new BaseEntityBlockingBuffer<>(queueSize);
		MissingMsisdnLoader loader = new MissingMsisdnLoader(appDs, cache, buffer);
		
		logger.debug("about to schedule");
		ScheduledFuture<?> loaderScheduledFuture = mes.schedule(loader, 1, TimeUnit.SECONDS);
		
		pus.add(loaderScheduledFuture);
		
		logger.debug("after schedule");
		
		int missingMsisdnWorkerSize = appProps.getInt("missing-msisdn-worker-size", 1);
		
		logger.debug("missingMsisdnWorkerSize : "+missingMsisdnWorkerSize);
		
		for(int x=0; x<missingMsisdnWorkerSize; x++){
			MissingMsisdnWorker missingMsisdnWorker = new MissingMsisdnWorker(appDs, cache, buffer);
			ScheduledFuture<?> missingMsisdnWorkerscheduledFuture = mes.schedule(missingMsisdnWorker, 3, TimeUnit.SECONDS);
			pus.add(missingMsisdnWorkerscheduledFuture);
		}
		
		logger.debug("after launchMsisdnProcesses");
	}

	@Asynchronous
	protected void workers() {
		logger.debug("Lauching Workers");
		for (int i = 0; i < workerSize; i++) {
			SyncWorker sw = new SyncWorker(appDs, cache, jmsQueue);
			sw.setCrypto(kycCrypter);
			sw.setBuffer(buffer);
			WorkerUnit wu = new WorkerUnit(sw);
			ScheduledFuture<?> s = mes.schedule(wu, 5, TimeUnit.SECONDS);
			pus.add(s);
		}
		for (int i = 0; i < 3; i++) {
			// resync
			ResyncWorker rw = new ResyncWorker(appDs, cache, jmsQueue);
			rw.setCrypto(kycCrypter);
			rw.setBuffer(resyncBuffer);
			WorkerUnit rwu = new WorkerUnit(rw);
			ScheduledFuture<?> rs = mes.schedule(rwu, 5, TimeUnit.SECONDS);
			pus.add(rs);
		}
		
		logger.debug("Done Lauching Workers");
	}

}
