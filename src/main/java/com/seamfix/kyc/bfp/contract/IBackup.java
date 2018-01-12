/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.contract;

/**
 *
 * @author Marcel Ugwu
 * @since 13-Oct-2016 09:45:14
 */
public interface IBackup {

    public boolean isQueued(String itemId);

    public boolean queueItem(SyncItem syncItem);

    public SyncItem getItem();

    public boolean backupItem(SyncItem syncItem);

    public void setBuffer(IBlockingBuffer<SyncItem> buffer);
}
