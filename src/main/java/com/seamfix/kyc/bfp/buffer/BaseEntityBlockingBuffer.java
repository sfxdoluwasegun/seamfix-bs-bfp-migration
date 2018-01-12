package com.seamfix.kyc.bfp.buffer;

import com.seamfix.kyc.bfp.contract.IBlockingBuffer;
import com.sf.biocapture.entity.base.BaseEntity;

public class BaseEntityBlockingBuffer<T extends BaseEntity> extends AbstractBlockingBuffer<T> implements IBlockingBuffer<T>  {

	public BaseEntityBlockingBuffer() {
		super();
	}

	public BaseEntityBlockingBuffer(Integer capacity) {
		super(capacity);
	}

	@Override
	public String getId(BaseEntity item) {
		return item.getId().toString();
	}

}
