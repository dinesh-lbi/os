/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.sync;

/**
 *
 * @author maris
 */
public interface SyncListener
{
    public void addEndSyncTransaction(SyncTransaction obj);
    public void endSyncExecutor();
}
