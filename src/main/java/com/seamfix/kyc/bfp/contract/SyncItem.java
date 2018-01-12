package com.seamfix.kyc.bfp.contract;

import java.io.File;
import java.util.List;

/**
 * A generic contract for objects that needs to be
 * pushed to 3rd party systems
 *
 * @author Ogwara O. Rowland
 *
 */
public interface SyncItem {

	public String getItemId();

	public File getFile();

	public boolean delete();

	public List<String> recordMsisdns();

	public void addMsisdn(String msisdn);

	public boolean endMarker();

}
