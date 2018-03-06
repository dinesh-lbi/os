/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.framework.exception;

/**
 *
 * @author maris
 */
public class SyncStopException extends CBIntegException
{

    public SyncStopException(ErrorCode errCode, Throwable cause, boolean needLogging)
    {
        super(errCode, cause, needLogging);
    }
    
    public String getErrorString()
    {
       return "Sync Stopped here due to the following error : "+errCode.toString();
    }
    
}
