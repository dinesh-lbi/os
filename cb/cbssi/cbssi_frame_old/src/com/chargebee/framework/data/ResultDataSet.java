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
public interface ResultDataSet
{
    public int getSize();
    public Object getNextData();
    public Object getCurrentData();
    public boolean isEmpty();
    public List<Object> getList();
}
