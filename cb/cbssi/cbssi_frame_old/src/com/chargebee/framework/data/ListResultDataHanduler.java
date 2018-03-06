/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.data;

import java.util.List;

/**
 *
 * @author maris
 */
public class ListResultDataHanduler implements ResultDataHanduler
{
    List<Object> data;

    @Override
    public void itrateResultDataSet(ResultDataSet rds)
    {
        data = rds.getList();
    }

    @Override
    public Object getData()
    {
        return data;
    }
    
}
