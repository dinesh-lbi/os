/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.query;

/**
 *
 * @author maris
 */
public class GetQueryObject implements QueryObject
{
    String qry;
    
    public GetQueryObject(String qry)
    {
        this.qry = qry;
    }

    @Override
    public Object get()
    {
        return qry;
    }
    
}
