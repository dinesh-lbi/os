/*
 * Copyright (c) 2016 Chargebee Inc
 * All Rights Reserved.
 */
package com.chargebee.framework.data.connector.batch;

import com.chargebee.framework.IntegProps;
import com.chargebee.framework.data.connector.DataConnector;
import com.chargebee.framework.sync.BaseObject;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author maris
 */
public class BatchSyncExecutor
{
    DataConnector dc;
    //List<BatchExecutor> executor_in_progress;
    BatchExecutor executor;
    List<BaseObject> baseObjList;
    int batchSize;
    boolean isFinished;
    public BatchSyncExecutor(DataConnector dc,int batchSize) throws Exception
    {
        this.dc = dc;
        this.executor = dc.getBatchExecutor();
        this.batchSize = batchSize;
        baseObjList = new ArrayList<>();
        isFinished = false;
        //executor_in_progress = Collections.synchronizedList(new LinkedList());
  
    }
    
    public void cleanQueue()
    {
        log("Queue cleanup starts");
           List<BaseObject> temp = new ArrayList<>();
           for(int i =0;i<baseObjList.size();i++)
           {
               if(!baseObjList.get(i).isWaitForDependentSync() || baseObjList.get(i).getStatus().equals(BaseObject.SYNC_STATUS.DEPENDING_FAILED))
               {
                   temp.add(baseObjList.get(i));
               }
           }
           baseObjList.removeAll(temp);
           for(int i=0;i<temp.size();i++)
           {
               if(!temp.get(i).getStatus().equals(BaseObject.SYNC_STATUS.DEPENDING_FAILED))
               {
                    addNotExecute(temp.get(i));
               }
           }  
           log("Queue cleanup End");
    }
    
    public void addNotExecute(BaseObject baseObj)
    {
        if(executor.getSize()>=batchSize-1)
        {
            executor.addElement(baseObj);
        }
        else
        {
            baseObjList.add(baseObj);
        }
    }    
    
    public void add(BaseObject baseObj)
    {
        if(baseObj.isWaitForDependentSync())
        {
            log("Had dependency so Added into Queue"+baseObj.getKey());
            baseObjList.add(baseObj);
            if(baseObjList.size()>30)
            {
                execute();
                cleanQueue();
            }
        }
        else
        {
            log("No dependency so Added into Batch Executor"+baseObj.getKey());            
            executor.addElement(baseObj);
            if(executor.getSize()>=batchSize)
            {
                execute();
            }
        }
    }
    
    public void execute()
    {
        //executor_in_progress.add(executor);
        log("-------Batch Execution starts-----");
        executor.executeBatch();
        long startTime = System.currentTimeMillis();
        while(!executor.isFinished)
        {
            long timeDiff = (System.currentTimeMillis() - startTime)/1000;
            if(executor.isFinished())
            {
                log("-------Batch Success Process start-----");         
                executor.handleBatchResponse(executor.get());
                executor.handleBatchError(executor.get());
                executor.isFinished = true;
                try{
                IntegProps.getMetaClient().sendRunningSyncMessage();
                }catch(Exception e)
                {
                    e.printStackTrace();
                }
                break;
            }
            else if(timeDiff>=300)
            {
                log("-------Batch Timeout-----");
                executor.handleError(new Throwable("Batch execution takes more than 5 mins"));
                break;
            }
        }
        try{
        executor = dc.getBatchExecutor();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        log("-------Batch Execution Ends-----");
    }
    
    public void executeRemaining() throws Exception
    {
        while(isFinished && !baseObjList.isEmpty())
        {
           cleanQueue();
           if(executor.getSize()>0)
           {
              execute();
           }
        }
        if(executor.getSize()>0)
        {
            execute();
        }
    }
    
    public void setFinished() throws Exception
    {
       isFinished = true; 
       executeRemaining();
    }
    
    public void remove(BaseObject obj)
    {
        baseObjList.remove(obj);
    }
  
    public void log(String str)
    {
        IntegProps.printNLog(str);
    }
    
}
