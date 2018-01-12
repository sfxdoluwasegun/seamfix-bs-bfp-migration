package com.seamfix.kyc.bfp.units;

import java.util.UUID;

import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.contract.IProcess;

/**
*
* @author dawuzi
*
*/
@SuppressWarnings("PMD")
public class ProcessorUnit extends BsClazz implements Runnable{

	private IProcess unit;
	private String id;
	private boolean stopProcess = false;
	private Long sleepTime = 6000L;
	
	public ProcessorUnit(IProcess unit) {
		this.unit = unit;
		this.id = UUID.randomUUID().toString();
	}

	@Override
	public void run() {
		logger.debug(id + " Running processor Thread " + unit.getClass());
		
		while(true){
			try {
				
				unit.process();
				
				if(stopProcess){
					break;
				}
				
				sleep();
				
			} catch (Exception e) {
				logger.error(id + " Exception from: " + unit.getClass(), e);
			}
		}
		
		logger.debug(id + " processor Exited: " + unit.getClass());
	}

	private void sleep() {
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			logger.error(id +  " Interrupted Exception while sleeping process: ", e);
		}
	}

	public IProcess getUnit() {
		return unit;
	}

	public void setUnit(IProcess unit) {
		this.unit = unit;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Long getSleepTime() {
		return sleepTime;
	}

	public void setSleepTime(Long sleepTime) {
		this.sleepTime = sleepTime;
	}
	
	public void stopProcess(){
		this.stopProcess = true;
	}
}
