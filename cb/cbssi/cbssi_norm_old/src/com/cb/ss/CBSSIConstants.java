package com.cb.ss;

import java.util.HashMap;

import com.chargebee.models.Invoice;
import com.chargebee.models.Order.Status;

public class CBSSIConstants
{

	/////////////// CBSSI Modes /////////////////
	
	final static int INIT_FETCH = 1;
	final static int FAILED_INV_PROCESS = 2;
	final static int SYNC = 3;

	////// CB API RESPONSE HEADERS AND ERROR CODES///////

	final static String cbRateLimitExceeded = "api_request_limit_exceeded";

	/////////////// ShipStation Api_End_Points And Other Constants /////////////////
	
							////// SS Api_End_Points ///////
	
	final static String ordersUrl = "https://ssapi.shipstation.com/orders?sortBy=ModifyDate&sortDir=ASC";
	final static String shipmentUrl = "https://ssapi.shipstation.com/shipments?orderNumber=";
	final static String createOrdersUrl = "https://ssapi.shipstation.com/orders/createorders";
	final static String ssWareHousesUrl = "https://ssapi.shipstation.com/warehouses";
	
						////// SS API REQUEST PARAMETERS///////
	
	final static String ssResultAfter = "&modifyDateStart=";
	final static String ssPage = "&page=";
	
						////// SS API RESPONSE FIELDS///////

	final static String ssTotalPages = "pages";
	final static String ssCurrentPage = "page";
	final static int SS_RateLimExceeded = 429;

				////// SS API RESPONSE HEADERS AND ERROR CODES///////

	final static String ssRetryAfter = "X-Rate-Limit-Reset";
	
						////// SS RETRIVE ORDERs RESPONSE Fields ///////

	final static String ssOrders = "orders";
	final static String ssSuccess = "success";
	final static String ssErrorMsg = "errorMessage";
	
							////// SS CREATE ORDER Fields ///////
	
	final static String ssResults = "results";
	final static String ssCusName = "customerUsername";
	final static String ssCusEmail = "customerEmail";
	final static String ssBillTo = "billTo";
	final static String ssShipTo = "shipTo";
	final static String ssOrdId = "orderId";
	final static String ssOrdKey = "orderKey";
	final static String ssOrdNum = "orderNumber";
	final static String ssOrderDate = "orderDate";
	final static String ssPaidDate = "paymentDate";
	final static String ssOrdStatus = "orderStatus";
	final static String ssAmountPaid = "amountPaid";
	final static String ssOrdItems = "items";
	final static String ssCusNote = "customerNote";
	
						////// SS ORDER LINE ITEM Fields ///////
	
	final static String ssItemAdjusted = "adjustment";
	final static String ssItemKey = "lineItemKey";
	final static String ssItemSKU = "sku";
	final static String ssItemName = "name";
	final static String ssItemQntity = "quantity";
	final static String ssItemPrice = "unitPrice";
	final static String ssItemTaxAmnt = "taxAmount";

					////// SS ORDER SHIPMENT Fields ///////
	
	final static String ssShipTrackNum = "trackingNumber";
	final static String ssShipBatchNum = "batchNumber";

					////// SS ORDER ADDRESS Fields ///////

	final static String ssAddCusName = "name";
	final static String ssAddLine1 = "street1";
	final static String ssAddLine2 = "street2";
	final static String ssAddLine3 = "street3";
	final static String ssAddPhone = "phone";
	final static String ssAddCity = "city";
	final static String ssAddState = "state";
	final static String ssAddCountry = "country";
	final static String ssAddPostCode = "postalCode";
	final static String ssAddIsVerified = "addressVerified";
	
	final static HashMap<String, String> SS_CURR_CODES = new HashMap<String, String>()
	{
		{
			put("US", "USD");
			put("AU", "AUD");
			put("CA", "CAD");
			put("GB", "GBP");
		}
	};
	/////////////// Thirdparty_Mapping Fields /////////////////
	
	final static String CBLastSyncTime = "cblastsynctime";
	final static String SSLastSyncTime = "sslastsynctime";
	final static String SSOrdIdVsCBOrdId = "ssordid-vs-cbordid";
	final static String CBOrdIdVsSSOrdId = "cbordid-vs-ssordid";
	final static String CBInvIdVsCBCusId = "cbinvid-vs-cbcusid";
	final static String CBInvIdVsCBSubId = "cbinvid-vs-cbsubid";
	final static String CBCusIdVsCBInvId = "cbcusid-vs-cbinvid";
	final static String CBSubIdVsCBInvId = "cbsubid-vs-cbinvid";
	final static String FailedInvDetails = "failed-inv-details";
	final static String CBInvIdVsSSOrdNo = "cbinvid-vs-ssordno";
	final static String SSOrdNoVsSSOrdKey = "ssordno-vs-ssordkey";
	final static String SSOrdKeyVsCBInvId = "ssordkey-vs-cbinvid";

	final static HashMap<String, Status> cbOrdStatusForSSOrdStatus = new HashMap<String, Status>()
	{
		{
			put("awaiting_payment", Status.PROCESSING);
			put("awaiting_shipment", Status.PROCESSING);
			put("on_hold", Status.PROCESSING);
			put("shipped", Status.COMPLETE);
			put("cancelled", Status.CANCELLED);
		}
	};

	final static HashMap<Invoice.Status, String> ssOrdStatusOfCBInvStatus = new HashMap<Invoice.Status, String>()
	{
		{
			put(Invoice.Status.NOT_PAID, "awaiting_payment");
			put(Invoice.Status.PAYMENT_DUE, "awaiting_payment");
			put(Invoice.Status.PAID, "awaiting_shipment");
			put(Invoice.Status.PENDING, "on_hold");
			put(Invoice.Status.POSTED, "awaiting_payment");
			put(Invoice.Status.VOIDED, "cancelled");
		}
	};
}
