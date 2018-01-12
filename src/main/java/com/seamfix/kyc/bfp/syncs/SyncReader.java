package com.seamfix.kyc.bfp.syncs;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.contract.IBlockingBuffer;
import com.seamfix.kyc.bfp.contract.IReader;
import com.seamfix.kyc.bfp.contract.SyncItem;

public class SyncReader extends BsClazz implements IReader{

	private IBlockingBuffer<SyncItem> buffer;

	protected BioCache cache;

	protected File source;

	protected String syncFileSuffix = "\\_bio/" + "receivedfiles/syncfiles";

	protected Integer cacheTimeout;
	protected String queueKey = "BFPT3YXM";

	public SyncReader(BioCache cache) {
		this.cache = cache;
		cacheTimeout = appProps.getInt("sync-cache-timeout", 60);
	}

	@Override
	public void sourceLocation(String src) {
		if(src.contains(":")){
			source = new File(src);
		}else{
			source = new File("\\" + "\\"+ src + syncFileSuffix);
		}
	}

	@Override
	public boolean queueItem(SyncItem item) {
		if(buffer.put(item)){
			return cache.setItem(queueKey + item.getItemId(), item.getItemId(), cacheTimeout);
		}
		return false;
	}

	@Override
	public boolean isQueued(String itemId) {
		return cache.getItem(queueKey + itemId) != null;
	}

	@Override
	public boolean isProcessable() {
		return source != null && source.exists() && source.canRead();
	}

	@Override
	public List<SyncItem> readSyncs() {
		File[] files = source.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("sync");
			}
		});
		List<SyncItem> sis = new ArrayList<>();
		for (File file : files) {
			SyncFile sf = new SyncFile();
			sf.setFile(file);
			sis.add(sf);
		}

		return sis;
	}

	@Override
	public void setBuffer(IBlockingBuffer<SyncItem> buffer) {
		this.buffer = buffer;
	}

	@Override
	public File getLocation() {
		return source;
	}

	public static void main(String[] args) {
		SyncReader sr = new SyncReader(null);
		sr.sourceLocation("Z:\\_bio/receivedfiles");
		System.out.println(sr.getLocation());
	}

}
