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

import com.chargebee.models.ThirdPartyEntityMapping;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author maris
 */
public class BaseObject implements SyncOperations, SyncListener
{  
    public static enum SYNC_STATUS
    {
        TP_FAILED,SYNC_FAILED,SUCCESS,PENDING,DEPENDING_FAILED,PARTIALLY_SYNCD
    }
    
    SYNC_STATUS status;
        
    List<SyncTransaction> syncTransList = new ArrayList<>();
    
    List<SyncTransaction> dependentSyncTransactions =  new ArrayList<>();
    
    public SyncMetaObject metaObj;
    
    public OperationEnum operation;
    
    public boolean startListen = true;
    
    public boolean exceptionHandled = false;
    

    
    public BaseObject(SyncMetaObject metaObject)
    {
        this.metaObj = metaObject;
        status = SYNC_STATUS.PENDING;
    }
    
    public BaseObject(String id,ThirdPartyEntityMapping.EntityType type) throws Exception
    {
        this.metaObj = new SyncMetaObject(id, type);
        status = SYNC_STATUS.PENDING;
    }
    
    public List<SyncTransaction> getDepSyncTransactions()
    {
        return dependentSyncTransactions;
    }    
    
    public String getKey()
    {
        return null;//ObjectFactory.getKey(metaObj.id,metaObj.type);
    }
    
    public void constructObject() throws Exception
    {
        
    }
    
    @Override
    public Object createExternalData() throws Exception
    {
        return null;
    }

    @Override
    public void updateExternalData()
    {
        //
    }
    
    public boolean isWaitForDependentSync()
    {
        return startListen && !dependentSyncTransactions.isEmpty();
    }

    @Override
    public void deleteExternalData()
    {
        //
    }

    @Override
    public Object get()
    {
        return null;
    }

    @Override
    public void setVoid()
    {
        //
    }
    
    public void setStatus(SYNC_STATUS status)
    {
        this.status = status;
    }
    
    public SYNC_STATUS getStatus()
    {
        return status;
    }    

    @Override
    public void updateThirdPartyResourceMap() throws Exception
    {
        //
    }

    @Override
    public String getId()
    {
        return null;
    }

    @Override
    public BaseObject getConvertObject() throws Exception
    {
        return null;
    }

    @Override
    public void addEndSyncTransaction(SyncTransaction obj)
    {
        syncTransList.add(obj);
    }

    @Override
    public void endSyncExecutor() 
    {
        for(int i=0;i<syncTransList.size();i++)
        {
            SyncTransaction syncTrans = syncTransList.get(i);
            try
            {
                syncTrans.executeBaseObject(this);
            } 
            catch (Exception ex)
            {
                syncTrans.handleException(this,ex);
            }
            
            syncTrans.endTransaction();
        }
    }
    
    @Override
    public Object getRefObject()throws Exception
    {
        return null;
    }
    
    public void set(Object obj)
    {
        //
    }
    

    @Override
    public boolean isBatched()
    {
        return false;
    }
    
    public boolean isPending()
    {
        return status.equals(SYNC_STATUS.PENDING);
    }
    
    public void handleException(Exception ex)
    {
        //
    }
    
    
}

