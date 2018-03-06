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
public abstract class ExecuteQueryObject implements QueryObject
{

    @Override
    public Object get()
    {
        return executeQuery();
    }
    
    public abstract Object executeQuery();
    
}
