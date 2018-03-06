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
public class CBIntegException extends RuntimeException
{
    public ErrorCode errCode;
    
    public CBIntegException(ErrorCode errCode,Throwable cause,boolean needLogging)
    {
        super(errCode.errorName, cause);
        this.errCode = errCode;
    }
    
    public String getErrorString()
    {
        return errCode.toString();
    }
    
}
