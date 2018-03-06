/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.data.connector;

import com.chargebee.framework.auth.Credentials;
import com.chargebee.framework.data.ResultDataHanduler;
import com.chargebee.framework.data.ResultDateWrapper;
import com.chargebee.framework.data.connector.batch.BatchExecutor;
import com.chargebee.framework.query.ExecuteUpdateQueryObject;
import com.chargebee.framework.query.QueryObject;

/**
 *
 * @author maris
 */
public abstract class RestAPIDataConnector implements DataConnector
{

    Credentials cred;
    
    public RestAPIDataConnector(Credentials cred)
    {
        this.cred = cred;
    }    

    @Override
    public void preLoadProps()
    {
        
    }

    @Override
    public Object getAuthObject()
    {
        return null;
    }

    @Override
    public void handleExceptions(Exception e) throws Exception
    {
        
    }

    @Override
    public Object fetchData(QueryObject qry, ResultDataHanduler rdh) throws Exception
    {
        return null;
    }

    @Override
    public int getResultCount(QueryObject qry) throws Exception
    {
        return 0;
    }

    @Override
    public void connectDataSpace() throws Exception
    {
        
    }

    @Override
    public ResultDateWrapper getDataWrapper()
    {
        return null;
    }

    @Override
    public Object UpdateData(ExecuteUpdateQueryObject qry) throws Exception
    {
        return null;
    }

    @Override
    public Object UpdateDataTax(ExecuteUpdateQueryObject qry) throws Exception
    {
        return null;
    }

    @Override
    public BatchExecutor getBatchExecutor() throws Exception
    {
        return null;
    }
    
}
