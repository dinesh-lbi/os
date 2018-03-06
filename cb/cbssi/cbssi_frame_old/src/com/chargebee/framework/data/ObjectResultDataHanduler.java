/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.data;

/**
 *
 * @author maris
 */
public class ObjectResultDataHanduler implements ResultDataHanduler
{
    Object data;
    @Override
    public void itrateResultDataSet(ResultDataSet rds)
    {
        data = rds.getNextData();
    }

    @Override
    public Object getData()
    {
        return data;
    }
    
}
