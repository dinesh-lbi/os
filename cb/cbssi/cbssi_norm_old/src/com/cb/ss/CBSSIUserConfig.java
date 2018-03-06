package com.cb.ss;

import java.util.Base64;

import com.chargebee.models.CreditNote;
import com.chargebee.models.Invoice;
import com.chargebee.models.Invoice.Status;

public class CBSSIUserConfig
{
	
	final static String cbSite = "raster-test";
	final static String cbApiKey = "test_1LY2Lcd7R2BwwG2PmulgVBu325B87LYDi";
	final static String ssApiKey = "Basic " + Base64.getEncoder().encodeToString(
			("bc9e26b4606546b3a49da7e52547e542:2e96d8639728454dbda1f74971dc0d4e").getBytes());
	
	// same subscription invoices will be added as single order based on user config
	
	public static boolean mergeInvoices()
	{
		return true;
	}

	public static Status[] getInvoiceStatus()
	{
		return new Status[] {Invoice.Status.PAID};
	}

	public static boolean handleCreditNotes()
	{
		return true;
	}

	public static CreditNote.Status[] getCreditNoteStatus()
	{
		return new CreditNote.Status[] {CreditNote.Status.ADJUSTED, CreditNote.Status.REFUNDED, CreditNote.Status.REFUND_DUE, CreditNote.Status.VOIDED};
	}

}