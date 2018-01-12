package com.seamfix.kyc.bfp.units;

import java.util.List;

import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.contract.IReader;
import com.seamfix.kyc.bfp.contract.SyncItem;

/**
 * Manages Startup of producers and associated workers
 * @author Ogwara O. Rowland
 *
 */
public class ProductionUnit extends BsClazz implements Runnable{

	private IReader reader;
	private Long sleepTime = 6000L;

	private boolean run = true;

	public ProductionUnit(IReader reader) {
		this.reader = reader;
	}

        @SuppressWarnings("PMD")
	@Override
	public void run() {

		// start production
		while(isRun()){
                        //it is absolute that exception is caught here to avoid thread from breaking should one record fail for unidentified reason.
                        //thus supression was incorporated above
			try {
                            if(reader.isProcessable()){
                                    List<SyncItem> data = reader.readSyncs();
    //				logger.debug("Sync File  Location : " + reader.getLocation() + " Sync: " + data.size());
                                    for (SyncItem item : data) {
                                            if(!reader.isQueued(item.getItemId())){
                                                    reader.queueItem(item);
                                            }
                                    }
                            }else{
                                    logger.debug(getClass() + ": Sync files location not readable: " + reader.getLocation());
                            }
                            sleepALittle();
                        } catch (Exception e) {
                            logger.error("", e);
                        }
		}
	}

	protected void sleepALittle(){
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			logger.error("Exception ", e);
		}
	}

	public boolean isRun() {
		return run;
	}

	public void setRun(boolean run) {
		this.run = run;
	}

}
