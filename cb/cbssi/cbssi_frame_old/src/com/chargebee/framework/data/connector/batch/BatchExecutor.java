/*
 * Copyright (c) 2016 Chargebee Inc
 * All Rights Reserved.
 */
package com.chargebee.framework.data.connector.batch;

import com.chargebee.framework.data.connector.DataConnector;
import com.chargebee.framework.sync.BaseObject;

/**
 *
 * @author maris
 */
public abstract class BatchExecutor
{
    DataConnector dc;
    
    boolean isSyncStarts;
    
    boolean isFinished;

    public BatchExecutor(DataConnector dc) throws Exception
    {
        this.dc = dc;
        dc.connectDataSpace();
    }
    
    public abstract void handleError(Throwable ex);
    
    public abstract Object get();
    
    public abstract boolean isFinished();
    
    public synchronized boolean setSyncStarts()
    {
        if(!isSyncStarts)
        {
            isSyncStarts = true;
            return true;
        }
        return false;
    }
    
    public abstract void handleBatchError(Object obj);
    
    public abstract void handleBatchResponse(Object obj);
                
    public abstract void executeBatch();
    
    public abstract void addElement(BaseObject bObj);
    
    public abstract int getSize();
    
    public abstract void clear();
    
}
