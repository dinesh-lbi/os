/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.sync;

/**
 *
 * @author maris
 */
public interface SyncOperations
{
    public Object createExternalData() throws Exception;
    public void updateExternalData();
    public void deleteExternalData();
    public Object get();
    public void setVoid();
    public void updateThirdPartyResourceMap() throws Exception;
    public String getId();
    public BaseObject getConvertObject() throws Exception;
    public boolean isBatched();
    public Object getRefObject()throws Exception;
    
    public static enum OperationEnum
    {
        CREATE, UPDATE, REVERT, DELETE, VOID, SEND;
    }
}
