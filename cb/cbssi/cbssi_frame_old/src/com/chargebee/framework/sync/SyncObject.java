/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.sync;

import com.chargebee.models.ThirdPartyEntityMapping;

/**
 *
 * @author maris
 */
public interface SyncObject
{
    public BaseObject convert(BaseObject bObj)throws Exception;
    public BaseObject syncData(ThirdPartyEntityMapping tpm)throws Exception;
    public BaseObject syncData(String id,ThirdPartyEntityMapping.EntityType eType,boolean needUpdate, String currencyCode)throws Exception;
    public BaseObject syncData(Object obj)throws Exception;  
    public void prerequest() throws Exception;
}
