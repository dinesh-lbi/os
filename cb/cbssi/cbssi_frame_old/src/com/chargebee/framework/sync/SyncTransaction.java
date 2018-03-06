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
public abstract class SyncTransaction
{
    public BaseObject baseObject;
    
    public SyncTransaction(BaseObject baseObject)
    {
        this.baseObject = baseObject;
        baseObject.getDepSyncTransactions().add(this);
    }
    public void executeBaseObject(BaseObject obj) throws Exception
    {
        if(!baseObject.getStatus().equals(BaseObject.SYNC_STATUS.DEPENDING_FAILED))
        {
            execute(obj);
        }
    }
    
    public abstract boolean execute(BaseObject obj) throws Exception;  
    
    public void handleException(BaseObject obj,Exception ex)
    {
        baseObject.metaObj.setDependentFailed(obj.getId(), obj.metaObj.type, ex.getMessage());
        baseObject.setStatus(BaseObject.SYNC_STATUS.DEPENDING_FAILED);
        baseObject.handleException(ex);
    }
    
    public void endTransaction()
    {
        baseObject.getDepSyncTransactions().remove(this);
    }
}
