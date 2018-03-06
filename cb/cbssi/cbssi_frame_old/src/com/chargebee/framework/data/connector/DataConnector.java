/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.data.connector;

import com.chargebee.framework.data.ResultDataHanduler;
import com.chargebee.framework.data.ResultDateWrapper;
import com.chargebee.framework.data.connector.batch.BatchExecutor;
import com.chargebee.framework.query.ExecuteUpdateQueryObject;
import com.chargebee.framework.query.QueryObject;

/**
 *
 * @author maris
 */
public interface DataConnector 
{
    public void preLoadProps();
    public Object getAuthObject();
    public void handleExceptions(Exception e)throws Exception;
    public Object fetchData(QueryObject qry,ResultDataHanduler rdh)throws Exception;
    public int getResultCount(QueryObject qry)throws Exception;
    public void connectDataSpace()throws Exception;
    public ResultDateWrapper getDataWrapper();
    public Object UpdateData(ExecuteUpdateQueryObject qry)throws Exception;
    public Object UpdateDataTax(ExecuteUpdateQueryObject qry) throws Exception;
    public BatchExecutor getBatchExecutor()throws Exception;
}
