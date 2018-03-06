/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.data;

import com.chargebee.framework.data.connector.DataConnector;


/**
 *
 * @author maris
 */
public class DataAPI
{
    private static DataConnector getDefaultCBConnector()
    {
        return null;//IntegProps.getCbClient();
    }
  
}
