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
public class ListResultDataSet implements ResultDataSet
{
    private final List resultSet;
    int i=0,size;
    Object data = null;
    boolean isEmpty = true;
    
    public ListResultDataSet(List resultSet)
    {
        this.resultSet = resultSet;
        if(resultSet!=null)
        {
            size = resultSet.size();
        }
        else
        {
            size = 0;
        }
        if(size>0)
        {
            isEmpty = false;
        }
        
    }
    
    @Override
    public int getSize()
    {
        return size;
    }

    @Override
    public Object getNextData()
    {
        if(!isEmpty && i<size)
        {
            data = resultSet.get(i++);
        }
        else
        {
            return null;
        }        
        return data;
    }

    @Override
    public Object getCurrentData()
    {
        return data;
    }

    @Override
    public boolean isEmpty()
    {
        return isEmpty;
    }

    @Override
    public List<Object> getList()
    {
        return resultSet;
    }


    
}
