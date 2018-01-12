package com.seamfix.kyc.bfp.resyncs;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.contract.SyncItem;
import com.seamfix.kyc.bfp.syncs.SyncFile;
import com.seamfix.kyc.bfp.syncs.SyncReader;

/**
 * This loader needs to lazily load sync files.
 *
 * For each folder list the files in batches of 1000
 *
 * @author Ogwara O. Rowland
 *
 */
public class ResyncReader extends SyncReader {

	public ResyncReader(BioCache cache) {
		super(cache);
		queueKey = "BFPT3YXRM";
		syncFileSuffix = "\\_bio/" + "receivedfiles/resyncs";
	}

	@Override
	public void sourceLocation(String src) {
		super.sourceLocation(src);
		source = new File(source.getParent() + "/resyncs");
	}

	@Override
	public List<SyncItem> readSyncs() {
		List<String> fileNames = new ArrayList<>();
		String[] list = listSyncNames();
		if(list.length > 1000){
			fileNames = Arrays.asList(list).subList(0, 1000);
		}else{
			fileNames = Arrays.asList(list);
		}

		List<SyncItem> syncs = new ArrayList<>();
		for (String name : fileNames) {
			File sync = new File(source + "/" + name);
			SyncFile sf = new SyncFile();
			sf.setFile(sync);
			syncs.add(sf);
		}
		return syncs;
	}

	protected String[] listSyncNames(){
		return source.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("sync");
			}
		});
	}

}
