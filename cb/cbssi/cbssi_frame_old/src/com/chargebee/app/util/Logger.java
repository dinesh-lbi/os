/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chargebee.app.util;

/**
 *
 * @author maris
 */
import com.chargebee.framework.util.*;
import com.google.common.base.*;
import java.io.*;
import java.util.*;
import org.apache.commons.io.*;

/**
 *
 * @author karthi-cb
 */
public class Logger {

public final File outFile;
    public final File errorFile;
    public final File skippedFile;

    public Logger(String domain) {
        String prefix = "output/" + domain + "-" + DateFmts.YYYY_MM_DD_HH_MM_SS.get().format(new Date());
        prefix = prefix.replace(' ', '-').replace(':', '_');
        outFile = new File(prefix + "-out.txt");
        errorFile = new File(prefix + "-error.txt");
        skippedFile = new File(prefix + "-skipped.txt");
    }

    public void printNLog(String content) throws Exception {
        System.out.println(content);
        if (Strings.isNullOrEmpty(content)) {
            FileUtils.writeStringToFile(outFile, "\n", true);
        } else {
            FileUtils.writeStringToFile(outFile, "\n" + content, true);
        }
    }

    public void printNLogErr(Exception e) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Error Message : ").append(e.getMessage()).append("\n");
        sb.append(getStackTraceString(e));
        System.out.println(sb.toString());
        FileUtils.writeStringToFile(outFile, "\n" + sb.toString(), true);
        log("\n" + sb.toString());
        logError("--------------------------------");
        logError("\n" + sb.toString());
        logError("--------------------------------");
        ErrorUtil.logError(e, "error_file", errorFile.getAbsolutePath());
    }

    public void printNLogErr(Throwable th) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Error Message : ").append(th.getMessage()).append("\n");
        sb.append(getStackTraceString(th));
        System.out.println(sb.toString());
        FileUtils.writeStringToFile(outFile, "\n" + sb.toString(), true);
        log("\n" + sb.toString());
        logError("--------------------------------");
        logError("\n" + sb.toString());
        logError("--------------------------------");
        ErrorUtil.logError(th, "error_file", errorFile.getAbsolutePath());
    }

    public static String getStackTraceString(Throwable th) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        th.printStackTrace(pw);
        return sw.toString();
    }

    public void log(String content) throws Exception {
        FileUtils.writeStringToFile(outFile, "\n" + content, true);
    }

    public void logError(String errorMessage) throws Exception {
        FileUtils.writeStringToFile(errorFile, "\n" + errorMessage + "\n", true);
    }

    public void logSkip(String content) throws Exception {
        System.out.println(content);
        FileUtils.writeStringToFile(skippedFile, "\n" + content + "\n", true);
    }

}

