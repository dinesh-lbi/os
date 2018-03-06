/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.web;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author maris
 */
import com.chargebee.framework.web.*;
import javax.servlet.http.*;


public class IntegAjaxResponse implements AjaxResponse{
    
    HttpServletRequest req;
    
    HttpServletResponse resp;

    public IntegAjaxResponse(HttpServletRequest req, HttpServletResponse resp) {
        this.req = req;
        this.resp = resp;
    }    
    
    public boolean includeSecurityHeaders(){
        return true;
    }
    
    
    @Override
    public HttpServletResponse getResp() {
        return resp;
    }

    @Override
    public HttpServletRequest getReq() {
        return req;
    }

    @Override
    public FlashMap flash() {
        return null;
    }

}

