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
public interface ResultDateWrapper
{
    public Object getInvoice(Object invoiceObj);
    public Object getCreaditNote(Object creditNoteObj);
    public Object getTax(Object taxObj);
    public Object getLineItem(Object lineItemObj);
    public Object getCustomerObj(Object customerObj);
    public Object getProduct(Object productObj);
    public Object getPayment(Object paymentObj);
    public Object getSubscription(Object subscriptionObj);
}
