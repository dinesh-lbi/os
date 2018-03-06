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
public class DefaultResultDataWrapper implements ResultDateWrapper
{
   @Override
    public Object getInvoice(Object invoiceObj)
    {
        return invoiceObj;
    }

    @Override
    public Object getCreaditNote(Object creditNoteObj)
    {
        return creditNoteObj;
    }

    @Override
    public Object getTax(Object taxObj)
    {
        return taxObj;
    }

    @Override
    public Object getLineItem(Object lineItemObj)
    {
        return lineItemObj;
    }

    @Override
    public Object getCustomerObj(Object customerObj)
    {
        return customerObj;
    }

    @Override
    public Object getProduct(Object productObj)
    {
       return productObj;
    }

    @Override
    public Object getPayment(Object paymentObj)
    {
        return paymentObj;
    }    

    @Override
    public Object getSubscription(Object subscriptionObj)
    {
        return subscriptionObj;
    }
    
}
