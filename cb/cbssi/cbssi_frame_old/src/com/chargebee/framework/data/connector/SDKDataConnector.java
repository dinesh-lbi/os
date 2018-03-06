/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.data.connector;

import com.chargebee.framework.IntegProps;
import com.chargebee.framework.auth.Credentials;
import com.chargebee.framework.data.DefaultResultDataWrapper;
import com.chargebee.framework.data.ResultDataHanduler;
import com.chargebee.framework.data.ResultDataSet;
import com.chargebee.framework.data.ResultDateWrapper;
import com.chargebee.framework.query.QueryObject;

/**
 *
 * @author maris
 */
public abstract class SDKDataConnector implements DataConnector
{
    Credentials cred;
    
    public SDKDataConnector(Credentials cred)
    {
        this.cred = cred;
    }
    
    @Override
    public void preLoadProps()
    {
        //Nothing
    }
    
    @Override
    public Object fetchData(QueryObject qry,ResultDataHanduler rdh)throws Exception
    {
         try
         {
             getAuthObject();
             connectDataSpace();
             ResultDataSet rds = getResultData(qry);
             rdh.itrateResultDataSet(rds);
         }
         catch(Exception e)
         {
             handleExceptions(e);
         }
         finally
         {
             
         }
         return rdh.getData();
    }
    
    public abstract ResultDataSet getResultData(QueryObject qry)throws Exception;
    
    @Override
    public void handleExceptions(Exception e) throws Exception
    {
        IntegProps.printNLogErr(e);
        throw e;
    }

    @Override
    public Object getAuthObject()
    {
        return cred.getCredentials();
    }
    
    @Override
    public ResultDateWrapper getDataWrapper()
    {
        return new DefaultResultDataWrapper();
    }

 
}
