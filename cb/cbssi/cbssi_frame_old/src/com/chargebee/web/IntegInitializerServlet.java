/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.web;

import com.chargebee.logging.KVL;
import com.chargebee.web.auth.IntegAuth;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;


/**
 *
 * @author maris
 */


public class IntegInitializerServlet extends HttpServlet {
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            KVL.start("main", false);
            Invoker.init();
            IntegAuth.init();
        } catch (Throwable th) {
            throw new ServletException(th);
        }
    }
    

    @Override
    public void destroy() 
    {
        Invoker.shutDown();
    }    
    
}

