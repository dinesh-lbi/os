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

public abstract class ExecuteUpdateQueryObject implements QueryObject
{
    public Object exeObj;
    @Override
    public Object get()
    {
        return null;
    }
    public void setExecutionObject(Object exeObj)
    {
        this.exeObj = exeObj;
    }
    public abstract Object executeUpdate()throws Exception;
    
}
