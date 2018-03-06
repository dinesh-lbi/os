/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.data.connector;

import com.chargebee.Environment;
import com.chargebee.framework.auth.CBCredencials;
import com.chargebee.framework.auth.Credentials;
import com.chargebee.framework.data.ListResultDataSet;
import com.chargebee.framework.data.ResultDataSet;
import com.chargebee.framework.data.connector.batch.BatchExecutor;
import com.chargebee.framework.query.ExecuteUpdateQueryObject;
import com.chargebee.framework.query.QueryObject;
import java.util.List;
import java.util.Map;

/**
 *
 * @author maris
 * 
 */


public class CBDataConnector extends SDKDataConnector
{
    List resultData;
    int i=0;
    
    public CBDataConnector(Credentials cred)
    {
        super(cred);
    }

    @Override
    public void connectDataSpace()
    {
        Map<String,String> credMap = cred.getCredentials();
        Environment.configure(credMap.get(CBCredencials.CB_SITE_NAME),credMap.get(CBCredencials.CB_AUTH_TOKEN));
    }

    @Override
    public ResultDataSet getResultData(QueryObject qry) throws Exception
    {
        List result = (List) qry.get();
        return new ListResultDataSet(result);
    }

    @Override
    public Object UpdateData(ExecuteUpdateQueryObject qry) throws Exception
    {
        return null;
        //
    }

    @Override
    public BatchExecutor getBatchExecutor() throws Exception
    {
        return null;
    }

    @Override
    public Object UpdateDataTax(ExecuteUpdateQueryObject qry) throws Exception
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    public int getResultCount(QueryObject qry) throws Exception
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
