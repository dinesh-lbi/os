package com.cb.ss;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.chargebee.APIException;
import com.chargebee.exceptions.*;
import com.chargebee.Environment;
import com.chargebee.ListResult;
import com.chargebee.Result;
import com.chargebee.exceptions.InvalidRequestException;
import com.chargebee.models.CreditNote;
import com.chargebee.models.CreditNote.CreditNoteListRequest;
import com.chargebee.models.Customer;
import com.chargebee.models.Customer.CustomerListRequest;
import com.chargebee.models.Invoice;
import com.chargebee.models.Order;
import com.chargebee.models.Subscription;
import com.chargebee.models.Subscription.SubscriptionListRequest;
import com.chargebee.models.Invoice.BillingAddress;
import com.chargebee.models.Invoice.InvoiceListRequest;
import com.chargebee.models.Invoice.LineItem;
import com.chargebee.models.Invoice.ShippingAddress;
import com.chargebee.models.enums.BillingPeriodUnit;

public class CBSSIntegration
{

	private static final Logger logger = Logger.getLogger(CBSSIntegration.class.getName());

	String ssApiKey;
	JSONObject thirdPartyMapping;	
	private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	public static void main(String[] args) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : Main");
		CBSSIntegration cbssi = new CBSSIntegration(CBSSIUserConfig.ssApiKey);
		cbssi.initSync();
		logger.log(Level.INFO, "\n\tInitial Fetch Finished");
		logger.log(Level.INFO, "\n\tEnter 'c' To Continue or 'any_other_key' To Stop : ");
		Scanner scan = new Scanner(System.in);
		String cont = scan.next();
		if(cont.equals("c"))
		{
			cbssi.sync();
		}
		scan.close();
	}

//	private String getCBTimeZone(Date date)
//	{
//		logger.log(Level.INFO, "\n\tMethod : getCBTimeZone");
//		formatter.setTimeZone(TimeZone.getTimeZone("Europe/London"));
//		return formatter.format(date);
//	}

	private static String getSSTimeZone(Date date)
	{
		logger.log(Level.INFO, "\n\tMethod : getSSTimeZone");
		formatter.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		return formatter.format(date);
	}

	public CBSSIntegration(String ssApiKey) throws Exception
	{
		logger.log(Level.INFO, "\n\tCBSSIntegration : Constructor");
		this.ssApiKey = ssApiKey;
		thirdPartyMapping = getThirdPartyMapping();
	}

	public void initSync() throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : initSync");
		processSync(CBSSIConstants.INIT_FETCH);
	}

	private void processSync(int mode) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : processSync");
		long cbSyncTime = System.currentTimeMillis();
		ArrayList<String> cbDelInvList = new ArrayList<String>();
		HashMap<String, Invoice> invoices = new HashMap<String, Invoice>();
		HashMap<String, Customer> customers = new HashMap<String, Customer>();
		HashMap<String, Subscription> subscriptions = new HashMap<String, Subscription>();
		HashMap<String, ArrayList<CreditNote>> invoiceVsCreditNotes = new HashMap<String, ArrayList<CreditNote>>(); 
		try
		{
			fetchCBInvoices(mode, cbDelInvList, invoices, customers, subscriptions, invoiceVsCreditNotes);
			constructSSOrders(mode, cbSyncTime, invoices, customers, subscriptions, invoiceVsCreditNotes);
			if(!isFailedInvProcess(mode))
			{
				createCBOrders(mode, cbDelInvList);
			}
			
		}catch (OperationFailedException e)
		{
			logger.log(Level.SEVERE, "\n\t" + e.httpStatusCode + " : " + e.type + " : " + e.apiErrorCode + " : " + e.getMessage() + ", Param : " + e.param, e);
			throw e;

		} catch (InvalidRequestException e)
		{
			logger.log(Level.SEVERE, "\n\t" + e.httpStatusCode + " : " + e.type + " : " + e.apiErrorCode + " : " + e.getMessage() + ", Param : " + e.param, e);
			throw e;

		} catch (APIException e)
		{
			logger.log(Level.SEVERE, "\n\t" + e.httpStatusCode + " : " + e.type + " : " + e.apiErrorCode + " : " + e.getMessage() + ", Param : " + e.param, e);
			throw e;
		} catch (Exception e)
		{
			logger.log(Level.SEVERE, "\n\t"   + e.getLocalizedMessage(), e);
			throw e;
		}
		updThirdPartyMapping("Third Party Mapping : ");
	}

	private void fetchCBInvoices(int mode, ArrayList<String> cbDelInvList, HashMap<String, Invoice> invoices, HashMap<String, Customer> customers, HashMap<String, Subscription> subscriptions, HashMap<String, ArrayList<CreditNote>> invoiceVsCreditNotes) throws OperationFailedException, InvalidRequestException, APIException, Exception
	{
		String nextOffset = null;
		CreditNoteListRequest creditNoteRequest = CreditNote.list().limit(100);
		InvoiceListRequest request = Invoice.list().limit(100).status().in(CBSSIUserConfig.getInvoiceStatus());
		if(isInitialFetch(mode))
		{
			logger.log(Level.INFO, "\n\tInitial Fetch");
			invoices.putAll(getCBInvoices(request, nextOffset, cbDelInvList, customers, subscriptions));
			logger.log(Level.INFO, "\n\tInvoices : " + invoices);
			invoiceVsCreditNotes.putAll(getCreditNotes(creditNoteRequest, nextOffset));
			logger.log(Level.INFO, "\n\tCredit Notes : " + invoiceVsCreditNotes);
		}
		else if (isSync(mode))
		{
			logger.log(Level.INFO, "\n\tSynchronization Process");
			Timestamp lastSynTime = getCBLastSyncTime();
			logger.log(Level.INFO, "\n\tCBLastSynTime : " + lastSynTime);
			if(lastSynTime != null)
			{
				invoices.putAll(getCBInvoices(request.includeDeleted(true).updatedAt().after(lastSynTime), nextOffset, cbDelInvList, customers, subscriptions));
				updateCBInvoices(invoices, cbDelInvList, customers, subscriptions);
				invoiceVsCreditNotes.putAll(getCreditNotes(creditNoteRequest.updatedAt().after(lastSynTime), nextOffset));
			}
			else // if ss_order creation failed in initial fetch last_sync_time won't be updated
			{
				invoices.putAll(getCBInvoices(request.includeDeleted(true), nextOffset, cbDelInvList, customers, subscriptions));
				invoiceVsCreditNotes.putAll(getCreditNotes(creditNoteRequest, nextOffset));
			}
			logger.log(Level.INFO, "\n\tInvoices : " + invoices);
			logger.log(Level.INFO, "\n\tCredit Notes : " + invoiceVsCreditNotes);
		}
		else if (isFailedInvProcess(mode))
		{
			logger.log(Level.INFO, "\n\tFailed Invoice Process");
			JSONObject failedInvoices = thirdPartyMapping.getJSONObject(CBSSIConstants.FailedInvDetails);
			if (failedInvoices.length() == 0) // to skip failed invoices in sync mode
			{
				return;
			}
			invoices.putAll(getCBInvoices(request.id().in(listAsArray(failedInvoices.names())), nextOffset, cbDelInvList, customers, subscriptions));
			logger.log(Level.INFO, "\n\tInvoices : " + invoices);
			Set<String> invList = invoices.keySet();
			if(invList != null) // if "all failed_invoices" has been deleted invoices will be empty => invoices.keySet() = null
			{
				invoiceVsCreditNotes.putAll(getCreditNotes(creditNoteRequest.referenceInvoiceId().in(listAsArray(invList)), nextOffset));
			}
			logger.log(Level.INFO, "\n\tCredit Notes : " + invoiceVsCreditNotes);
		}
	}

	private void constructSSOrders(int mode, long cbSyncTime, HashMap<String, Invoice> invoices, HashMap<String, Customer> customers, HashMap<String, Subscription> subscriptions, HashMap<String, ArrayList<CreditNote>> invoiceVsCreditNotes) throws OperationFailedException, InvalidRequestException, APIException, Exception
	{
		int billingPeriod, count = 0;
		Entry<String, Invoice> entry;
		Boolean isDelInv, hasNoMoreInv, invHasSSOrder;
		JSONArray orders = new JSONArray();
		JSONObject order = new JSONObject();
		JSONArray ssOrderNos = new JSONArray();
		String invoiceId, customerId, subscrpId;
		JSONObject orderVsInvoice = new JSONObject();
		Iterator<Entry<String, Invoice>> invIterator = invoices.entrySet().iterator();
		Invoice invoice; Customer customer; Subscription subscription; JSONObject cbInvIdVsSSOrdNo;
		
		while(invIterator.hasNext())
		{
			invHasSSOrder = false; // is invoice of already created ss_order
			entry = invIterator.next();
			invoiceId = entry.getKey();
			invoice = entry.getValue();
			isDelInv = invoice.deleted(); // deleted invoice ?
			customerId = invoice.customerId();
			customer = customers.get(customerId);
			subscrpId = invoice.subscriptionId();
			subscription = subscriptions.get(subscrpId);
			logger.log(Level.INFO, "\n\tHandling Invoice : " + invoiceId + " of Subscription " + subscrpId + " of Customer : " + customerId);
			billingPeriod = subscription != null ? subscription.billingPeriod() : 1; // handling of recurring and non_recurring invoices
			cbInvIdVsSSOrdNo = thirdPartyMapping.getJSONObject(CBSSIConstants.CBInvIdVsSSOrdNo);
			if(cbInvIdVsSSOrdNo.has(invoiceId))  // is old invoice or invoice of already created ss_order
			{
				invHasSSOrder = true;
				logger.log(Level.INFO, "\n\tInvoice Has SS Order : " + invHasSSOrder);
				ssOrderNos = cbInvIdVsSSOrdNo.getJSONArray(invoiceId); // ss_order no's of single invoice split into multiple orders
				billingPeriod = cbInvIdVsSSOrdNo.getJSONArray(invoiceId).length(); // to avoid conflicts when updating ss_orders of cb_invoices when "subscription plan changes"
			}
			for (int j = 0; j < billingPeriod; j++)
			{
				if(j == 0) // to avoid recurring processing of recurring data
				{
					order = new JSONObject();
					if(!hasValidOrderDetails(isDelInv, invHasSSOrder, invoice, customer, subscription, order, invoiceVsCreditNotes))
					{
						j = billingPeriod; // to avoid recurring processing for failed invoices
						continue;
					}
				}
				fillOrderDetails(j, invoice, invHasSSOrder, ssOrderNos, subscription, order, orderVsInvoice);
				orders.put(new JSONObject(order.toString()));
				count++;
				if (count == 100)
				{
					logger.log(Level.INFO, "\n\tcount : " + count);
					logger.log(Level.INFO, "\n\torders : " + orders);
					createSSOrders( orders, orderVsInvoice);
					hasNoMoreInv = (j == billingPeriod-1 && !invIterator.hasNext());
					updCBLastSyncTime(mode, hasNoMoreInv, cbSyncTime);
					orders = new JSONArray();
					count = 0;
				}
			}
		}
		if (count > 0)
		{
			logger.log(Level.INFO, "\n\tcount : " + count);
			logger.log(Level.INFO, "\n\torders : " + orders);
			createSSOrders(orders, orderVsInvoice);
			updCBLastSyncTime(mode, true, cbSyncTime);
			count = 0;
		}
	}

	private void createCBOrders(int mode, ArrayList<String> cbDelInvList) throws OperationFailedException, InvalidRequestException, APIException, Exception
	{
		logger.log(Level.INFO, "\n\tgetting shipstation orders");
		Client client = ClientBuilder.newClient();
		String lastSyncTime = getSSLastSyncTime();
		int nextPage, currPage = 0, totalPages = 0;
		long ssSyncTime = System.currentTimeMillis();
		String invoiceId, ssOrdId, ssOrdNo, ssOrderKey;
		String batchId = null, trackingId = null, orderStatus = null, customerNote = null;
		JSONObject ssOrdNoVsSSOrdKey = thirdPartyMapping
				.getJSONObject(CBSSIConstants.SSOrdNoVsSSOrdKey);
		JSONObject ssOrdKeyVsCBInvId = thirdPartyMapping
				.getJSONObject(CBSSIConstants.SSOrdKeyVsCBInvId);
		StringBuilder url = new StringBuilder(CBSSIConstants.ordersUrl);
		if(lastSyncTime != null)
		{
			url.append(CBSSIConstants.ssResultAfter).append(lastSyncTime);
		}
		url.append(CBSSIConstants.ssPage);
		do
		{
			nextPage = ++currPage;
			logger.log(Level.INFO, "\n\tSSOrders URL : " + url.toString() + nextPage);
			Response response = client.target(url.toString() + nextPage).request(MediaType.TEXT_PLAIN_TYPE)
					.header("Authorization", ssApiKey).get();
			if (response.getStatus() == HttpStatus.SC_OK)
			{
				JSONObject object = new JSONObject(response.readEntity(String.class));
				totalPages = object.getInt(CBSSIConstants.ssTotalPages);
				currPage = object.getInt(CBSSIConstants.ssCurrentPage);
				JSONArray ssOrders = object.getJSONArray(CBSSIConstants.ssOrders);
				JSONObject orderShipInfo;
				for (int i = 0; i < ssOrders.length(); i++)
				{
					JSONObject ssOrder = ssOrders.getJSONObject(i);
					ssOrdId = ssOrder.getString(CBSSIConstants.ssOrdId);
					ssOrdNo = ssOrder.getString(CBSSIConstants.ssOrdNum);
					if (isOrderFromCB(ssOrdNo))
					{
						ssOrderKey = ssOrdNoVsSSOrdKey.getString(ssOrdNo);
						invoiceId = ssOrdKeyVsCBInvId.getString(ssOrderKey);
						if(cbDelInvList.contains(invoiceId)) // to avoid conflict of updating deleted order
						{
							logger.log(Level.INFO, "\n\tCBOrder Cannot Be Updated (CBOrder Corresponding to this SSOrder has been Deleted)");
							continue;
						}
						orderShipInfo = getShipmentInfo(ssOrdNo);
						if (ssOrder.has(CBSSIConstants.ssOrdStatus))
						{
							orderStatus = ssOrder.getString(CBSSIConstants.ssOrdStatus);
						}
						if (ssOrder.has(CBSSIConstants.ssCusNote))
						{
							customerNote = ssOrder.getString(CBSSIConstants.ssCusNote);
						}
						if (ssOrder.has(CBSSIConstants.ssShipTrackNum))
						{
							trackingId = orderShipInfo.getString(CBSSIConstants.ssShipTrackNum);
						}
						if (ssOrder.has(CBSSIConstants.ssShipBatchNum))
						{
							batchId = orderShipInfo.getString(CBSSIConstants.ssShipBatchNum);
						}
						replicateInCBOrders(invoiceId, ssOrdId, orderStatus, batchId, trackingId, customerNote);
					}
				}
				if(currPage == totalPages)
				{
					updSSLastSyncTime(ssSyncTime);
				}
			}
			else
			{
				handleErrorResponse(response, null, null);
			}
		}
		while(currPage < totalPages);
	}

	private void fillOrderDetails(int j, Invoice invoice, Boolean invHasSSOrder, JSONArray ssOrderNos, Subscription subscription, JSONObject order, JSONObject orderVsInvoice) throws Exception
	{
		String orderKey;
		String invoiceId = invoice.id();
		String orderNo = invoiceId + "_" + UUID.randomUUID();
		Date orderDate = subscription != null ? getOrderDate(j, subscription.billingPeriodUnit(), invoice) : invoice.date();
		if (invHasSSOrder)
		{
			orderNo = ssOrderNos.getString(j);
			orderKey = getSSOrderKey(orderNo);
			order.put(CBSSIConstants.ssOrdKey, orderKey);
		}
		order.put(CBSSIConstants.ssOrdNum, orderNo);
		order.put(CBSSIConstants.ssOrderDate, getSSTimeZone(orderDate));
		orderVsInvoice.put(orderNo, invoiceId);
		logger.log(Level.INFO, "\n\torder : " + order);
	}

	private Boolean hasValidOrderDetails(Boolean isDelInv, Boolean invHasSSOrder, Invoice invoice, Customer customer, Subscription subscription, JSONObject order, HashMap<String, ArrayList<CreditNote>> invoiceVsCreditNotes) throws Exception
	{
		String invoiceId = invoice.id();
		fillCustomerInfo(customer, order);
		if (!hasValidBillingAddress(isDelInv, invoice, customer, order))
		{
			logger.log(Level.INFO, "\n\tInvoice : " + invoiceId
					+ " is failed due to invalid billing address");
			updateFailedInvoice(invHasSSOrder, invoiceId, "Invalid Billing Address");
			return false;
		}
		if (!hasValidShippingAddress(isDelInv, invoice, subscription, order))
		{
			logger.log(Level.INFO, "\n\tInvoice " + invoiceId
					+ " is failed due to invalid shipping address");
			updateFailedInvoice(invHasSSOrder, invoiceId, "Invalid Shipping Address");
			return false;
		}
		if(!hasValidOrderItems(isDelInv, invHasSSOrder, invoice, order, invoiceVsCreditNotes))
		{
			return false;
		}
		return true;
	}

	private void createSSOrders(JSONArray orders, JSONObject orderVsInvoice) throws OperationFailedException, InvalidRequestException, APIException, Exception
	{
		 logger.log(Level.INFO, "\n\tcreating shipstation orders");
		 Client client = ClientBuilder.newClient();
		 Entity<String> payload = Entity.json(orders.toString());
		 Response response = client.target(CBSSIConstants.createOrdersUrl)
				 .request(MediaType.APPLICATION_JSON_TYPE).header("Authorization", ssApiKey)
				 .post(payload);
		 handleResponse(response, orders, orderVsInvoice);
	}

	private void getCreditNotesLineItems(String invoiceId, ArrayList<String> creditNotesLineIems,
			ArrayList<CreditNote> creditNotes)
	{
		String item;
		CreditNote creditNote;
		List<CreditNote.LineItem> lineItems;
		for(int i = 0; i < creditNotes.size(); i++)
		{
			creditNote = creditNotes.get(i);
			lineItems = creditNote.lineItems();
			for(CreditNote.LineItem lineItem : lineItems)
			{
				item = lineItem.entityId();
				if(!creditNotesLineIems.contains(item))
				{
					creditNotesLineIems.add(item);
				}
			}
		}
	}

	private HashMap<String, ArrayList<CreditNote>> getCreditNotes(CreditNoteListRequest request, String nextOffset) throws OperationFailedException, InvalidRequestException, APIException, Exception
	{
		logger.log(Level.INFO, "\n\tMethod : getCreditNotes");
		HashMap<String, ArrayList<CreditNote>> creditNotes = new HashMap<String, ArrayList<CreditNote>>();
		ArrayList<CreditNote> array;
		CreditNote creditNote;
		String invoiceId;
		ListResult result;
		try
		{
			do
			{
				CreditNoteListRequest creditNoteRequest = request;
				result = creditNoteRequest.offset(nextOffset).request(getEnvironment());
				nextOffset = result.nextOffset();
				logger.log(Level.INFO, "\n\tresult size : " + result.size());
				for (int i = 0; i < result.size(); i++)
				{
					creditNote = result.get(i).creditNote();
					invoiceId = creditNote.referenceInvoiceId();
					if(creditNotes.containsKey(invoiceId))
					{
						array = creditNotes.get(invoiceId);
						if(array.contains(creditNote))
						{
							continue;
						}
						array.add(creditNote);
						creditNotes.put(invoiceId, array);
					}
					else
					{
						array = new ArrayList<CreditNote>();
						array.add(creditNote);
						creditNotes.put(invoiceId, array);
					}
					
				}
			}
			while(nextOffset != null);
		}
		catch(OperationFailedException e)
		{
			if(e.apiErrorCode.equals(CBSSIConstants.cbRateLimitExceeded))
			{
				logger.log(Level.SEVERE, "\n\t" + e.httpStatusCode + " : " + e.type + " : " + e.apiErrorCode + " : " + e.getMessage() + ", Param : " + e.param, e);
				Thread.sleep(60000);
				getCreditNotes(request, nextOffset);
			}
			else
			{
				throw e;
			}
		}
		return creditNotes;
	}

	private HashMap<String, Invoice> getCBInvoices(InvoiceListRequest request, String nextOffset, ArrayList<String> cbDelInvList, HashMap<String, Customer> customers, HashMap<String, Subscription> subscriptions) throws OperationFailedException, InvalidRequestException, APIException, Exception
	{
		logger.log(Level.INFO, "\n\tMethod : getInvoices");
		Invoice invoice;
		String invoiceId;
		ListResult result;
		String customerId;
		String subscriptionId;
		Boolean isDelInv, invHasSSOrder;
		ArrayList<String> cusList = new ArrayList<String>();
		ArrayList<String> subList = new ArrayList<String>();
		HashMap<String, Invoice> invoiceList = new HashMap<String, Invoice>();
		JSONObject cbInvIdVsSSOrdNo = thirdPartyMapping.getJSONObject(CBSSIConstants.CBInvIdVsSSOrdNo);
		try
		{
			do
			{
				result = request.offset(nextOffset).request(getEnvironment());
				nextOffset = result.nextOffset();
				logger.log(Level.INFO, "\n\tresult size : " + result.size());
				for (int i = 0; i < result.size(); i++)
				{
					invoice = result.get(i).invoice();
					invoiceId = invoice.id();
					isDelInv = invoice.deleted();
					invHasSSOrder = cbInvIdVsSSOrdNo.has(invoiceId);
					if(isDelInv)
					{
						cbDelInvList.add(invoiceId);
						removeFromFailedInvoices(invoiceId);
					}
					if(!isDelInv || (isDelInv && invHasSSOrder)) // to avoid update of "new but deleted_invoice" which has no corresponding ss_order "SYNC MODE"
					{
						invoiceList.put(invoiceId, invoice);
						customerId = invoice.customerId();
						if(!cusList.contains(customerId) && customerId != null)
						{
							cusList.add(customerId);
						}
						subscriptionId = invoice.subscriptionId();
						if(!subList.contains(subscriptionId) && subscriptionId != null)
						{
							subList.add(subscriptionId);
						}
					}
//					updCustomerVsInvoice(invoiceId, customerId); // for "merging invoices of same subscription" case
//					updSubscriptionVsInvoice(invoiceId, subscriptionId);
				}
			}
			while(nextOffset != null);
			if(cusList.size() != 0 && customers != null)
			{
				CustomerListRequest  cutomerRequest = Customer.list().limit(100).includeDeleted(true).id().in(listAsArray(cusList));
				customers.putAll(getCBCustomers(cutomerRequest, null));
				logger.log(Level.INFO, "\n\tCustomers : " + customers);
			}
			if(subList.size() != 0 && subscriptions != null)
			{
				SubscriptionListRequest subscriptionRequest = Subscription.list().limit(100).includeDeleted(true).id().in(listAsArray(subList));
				subscriptions.putAll(getCBSubscriptions(subscriptionRequest, null));
				logger.log(Level.INFO, "\n\tSubscriptions : " + subscriptions);
			}
		}
		catch(OperationFailedException e)
		{
			if(e.apiErrorCode.equals(CBSSIConstants.cbRateLimitExceeded))
			{
				logger.log(Level.SEVERE, "\n\t" + e.httpStatusCode + " : " + e.type + " : " + e.apiErrorCode + " : " + e.getMessage() + ", Param : " + e.param, e);
				Thread.sleep(60000);
				getCBInvoices(request, nextOffset, cbDelInvList, customers, subscriptions);
			}
			else
			{
				throw e;
			}
		}
		return invoiceList;
	}

	private Environment getEnvironment()
	{
		Environment.configure(CBSSIUserConfig.cbSite, CBSSIUserConfig.cbApiKey);
		Environment env = Environment.defaultConfig();
		return env;
	}

	private HashMap<String, Customer> getCBCustomers(CustomerListRequest request, String nextOffset) throws OperationFailedException, InvalidRequestException, APIException, Exception
	{
		logger.log(Level.INFO, "\n\tMethod : getCustomers");
		Customer customer;
		String customerId;
		ListResult result;
		HashMap<String, Customer> updCustomers = new HashMap<String, Customer>();
		try
		{
			do
			{
				result = request.offset(nextOffset).request(getEnvironment());
				nextOffset = result.nextOffset();
				logger.log(Level.INFO, "\n\tresult size : " + result.size());
				for (int i = 0; i < result.size(); i++)
				{
					customer = result.get(i).customer();
					customerId = customer.id();
					updCustomers.put(customerId, customer);
				}
			}
			while(nextOffset != null);
		}
		catch(OperationFailedException e)
		{
			if(e.apiErrorCode.equals(CBSSIConstants.cbRateLimitExceeded))
			{
				logger.log(Level.SEVERE, "\n\t" + e.httpStatusCode + " : " + e.type + " : " + e.apiErrorCode + " : " + e.getMessage() + ", Param : " + e.param, e);
				Thread.sleep(60000);
				getCBCustomers(request, nextOffset);
			}
			else
			{
				throw e;
			}
		}
		return updCustomers;
	}

	private HashMap<String, Subscription> getCBSubscriptions(SubscriptionListRequest request, String nextOffset) throws OperationFailedException, InvalidRequestException, APIException, Exception
	{
		logger.log(Level.INFO, "\n\tMethod : getSubscriptions");
		ListResult result;
		String subscriptionId;
		Subscription subscription;
		HashMap<String, Subscription> updSubscriptions = new HashMap<String, Subscription>();
		try
		{
			do
			{
				result = request.offset(nextOffset).request(getEnvironment());
				nextOffset = result.nextOffset();
				logger.log(Level.INFO, "\n\tresult size : " + result.size());
				for (int i = 0; i < result.size(); i++)
				{
					subscription = result.get(i).subscription();
					subscriptionId = subscription.id();
					updSubscriptions.put(subscriptionId, subscription);
				}
			}
			while(nextOffset != null);
		}
		catch(OperationFailedException e)
		{
			if(e.apiErrorCode.equals(CBSSIConstants.cbRateLimitExceeded))
			{
				logger.log(Level.SEVERE, "\n\t" + e.httpStatusCode + " : " + e.type + " : " + e.apiErrorCode + " : " + e.getMessage() + ", Param : " + e.param, e);
				Thread.sleep(60000);
				getCBSubscriptions(request, nextOffset);
			}
			else
			{
				throw e;
			}
		}
		return updSubscriptions;
	}

	private void updateCBInvoices(HashMap<String, Invoice> invoices, ArrayList<String> cbDelInvList, HashMap<String, Customer> customers,
			HashMap<String, Subscription> subscriptions) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : updateInvoices");
		Invoice invoice;
		String invoiceId, customerId, subscriptionId, nextOffset = null;
		Timestamp lastSyncTime = getCBLastSyncTime();
		CustomerListRequest customerRequest = Customer.list().limit(100).includeDeleted(true).updatedAt().after(lastSyncTime);
		SubscriptionListRequest subscriptionReqest = Subscription.list().limit(100).includeDeleted(true).updatedAt().after(lastSyncTime);
		HashMap<String, Customer> updCustomers = getCBCustomers(customerRequest, nextOffset);
		logger.log(Level.INFO, "\n\tUpdated Customers : " + updCustomers);
		HashMap<String, Subscription> updSubscriptions = getCBSubscriptions(subscriptionReqest, nextOffset);
		logger.log(Level.INFO, "\n\tUpdated Subscriptions : " + updSubscriptions);
		//getting invoices of updated customers and subscriptions
		if(updCustomers.size() != 0)
		{
			InvoiceListRequest invoiceRequest = Invoice.list().limit(100).includeDeleted(false).status().in(CBSSIUserConfig.getInvoiceStatus()).customerId().in(listAsArray(updCustomers.keySet()));
			Set<Entry<String, Invoice>> updCusInvoices = getCBInvoices(invoiceRequest, nextOffset, cbDelInvList, null, null).entrySet();
			for(Entry<String, Invoice> entry : updCusInvoices)
			{
				invoiceId = entry.getKey();
				invoice = entry.getValue();
				customerId = invoice.customerId();
				if(!isSameAddress(invoice.billingAddress(), updCustomers.get(customerId).billingAddress()))
				{
					invoices.put(invoiceId, invoice);
					continue;
				}
			}
			customers.putAll(updCustomers);
		}
		if(updSubscriptions.size() != 0)
		{
			InvoiceListRequest invoiceRequest = Invoice.list().limit(100).includeDeleted(false).status().in(CBSSIUserConfig.getInvoiceStatus()).subscriptionId().in(listAsArray(updSubscriptions.keySet()));
			Set<Entry<String, Invoice>> updSubInvoices = getCBInvoices(invoiceRequest, nextOffset, cbDelInvList, null, null).entrySet();
			for(Entry<String, Invoice> entry : updSubInvoices)
			{
				invoiceId = entry.getKey();
				invoice = entry.getValue();
				subscriptionId = invoice.subscriptionId();
				if(!isSameAddress(invoice.shippingAddress(), updSubscriptions.get(subscriptionId).shippingAddress()))
				{
					invoices.put(invoiceId, invoice);
				}
			}
			subscriptions.putAll(updSubscriptions);
		}	
	}

	private String[] listAsArray(Set<String> keys)
	{
		logger.log(Level.INFO, "\n\tMethod : listAsArray");
		int length = keys.size();
		String[] array = new String[length];
		Iterator<String> it = keys.iterator();
		for(int i = 0; i < length; i++)
		{
			array[i] = it.next();
		}
		logger.log(Level.INFO, "\n\tList Size : " + length);
		return array;
	}

	private String[] listAsArray(ArrayList<String> keys) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : listAsArray");
		int length = keys.size();
		String[] array = new String[length];
		for (int i = 0; i < length; i++)
		{
			array[i] = keys.get(i);
		}
		return array;
	}

	private String[] listAsArray(JSONArray keys) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : listAsArray");
		int length = keys.length();
		String[] array = new String[length];
		for (int i = 0; i < length; i++)
		{
			array[i] = keys.getString(i);
		}
		return array;
	}

//	private void updCustomerVsInvoice(String invoiceId, String customerId) throws Exception
//	{
//		logger.log(Level.INFO, "\n\tMethod : updCBInvIdVsCBCusId");
//		JSONObject invIdVsCusId = thirdPartyMapping.getJSONObject(CBSSIConstants.CBInvIdVsCBCusId);
//		JSONObject cusIdVsInvId = thirdPartyMapping.getJSONObject(CBSSIConstants.CBCusIdVsCBInvId);
//		
//		if(customerId != null)
//		{
//			if (!invIdVsCusId.has(invoiceId))
//			{
//				invIdVsCusId.put(invoiceId, customerId);
//			}
//			
//			if(cusIdVsInvId.has(customerId))
//			{
//				JSONArray invIds = cusIdVsInvId.getJSONArray(customerId);
//				for(int i = 0; i < invIds.length(); i++)
//				{
//					if(invIds.getString(i).equals(invoiceId))
//					{
//						break;
//					}
//					if(i == invIds.length()-1)
//					{
//						invIds.put(invoiceId);
//					}
//
//				}
//				cusIdVsInvId.put(customerId, invIds);
//			}
//			else
//			{
//				JSONArray invIds = new JSONArray();
//				invIds.put(invoiceId);
//				cusIdVsInvId.put(customerId, invIds);
//			}
//		}
//		thirdPartyMapping.put(CBSSIConstants.CBInvIdVsCBCusId, invIdVsCusId);
//		thirdPartyMapping.put(CBSSIConstants.CBCusIdVsCBInvId, cusIdVsInvId);
//	}

//	private void updSubscriptionVsInvoice(String invoiceId, String subscrpId) throws Exception
//	{
//		logger.log(Level.INFO, "\n\tMethod : updCBInvIdVsCBSubId");
//		JSONObject invIdVsSubId = thirdPartyMapping.getJSONObject(CBSSIConstants.CBInvIdVsCBSubId);
//		JSONObject subIdVsInvId = thirdPartyMapping.getJSONObject(CBSSIConstants.CBSubIdVsCBInvId);
//		if(subscrpId != null)
//		{
//			if (!invIdVsSubId.has(invoiceId))
//			{
//				invIdVsSubId.put(invoiceId, subscrpId);
//			}
//			if(subIdVsInvId.has(subscrpId))
//			{
//				JSONArray invIds = subIdVsInvId.getJSONArray(subscrpId);
//				for(int i = 0; i < invIds.length(); i++)
//				{
//					if(invIds.getString(i).equals(invoiceId))
//					{
//						break;
//					}
//					if(i == invIds.length()-1)
//					{
//						invIds.put(invoiceId);
//					}
//
//				}
//				subIdVsInvId.put(subscrpId, invIds);
//			}
//			else
//			{
//				JSONArray invIds = new JSONArray();
//				invIds.put(invoiceId);
//				subIdVsInvId.put(subscrpId, invIds);
//			}
//		}
//		thirdPartyMapping.put(CBSSIConstants.CBInvIdVsCBSubId, invIdVsSubId);
//		thirdPartyMapping.put(CBSSIConstants.CBSubIdVsCBInvId, subIdVsInvId);
//	}

	private void updThirdPartyMapping(String action) throws Exception
	{
		logger.log(Level.INFO, "\n\t" + action + thirdPartyMapping.toString());
	}

	private boolean checkFailedInvoice(String invoiceId) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : checkFailedInvoice");
		JSONObject failedInvoices = thirdPartyMapping.getJSONObject(CBSSIConstants.FailedInvDetails);
		if (failedInvoices.has(invoiceId))
		{
			return true;
		}
		return false;
	}

	private Date getOrderDate(int index, BillingPeriodUnit billingPeriodUnit, Invoice invoice)
			throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : getOrderDate");

		Timestamp ordDate = invoice.date();
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(ordDate.getTime());
		if (billingPeriodUnit.equals(BillingPeriodUnit.MONTH))
		{
			cal.add(Calendar.MONTH, index);
		}
		else if (billingPeriodUnit.equals(BillingPeriodUnit.WEEK))
		{
			cal.add(Calendar.WEEK_OF_MONTH, index);
		}
		else if (billingPeriodUnit.equals(BillingPeriodUnit.YEAR))
		{
			cal.add(Calendar.YEAR, index);
		}
		return cal.getTime();
	}

	private void handleResponse(Response response, JSONArray orders,
			JSONObject orderVsInvoice) throws Exception
	{
		int status = response.getStatus();
		if (status == HttpStatus.SC_OK || status == HttpStatus.SC_ACCEPTED)
		{
			handleSuccessResponse(response, orderVsInvoice);
		}
		else
		{
			handleErrorResponse(response, orders, orderVsInvoice);
		}
	}

	private void updateOrderVsInvoice(JSONObject order, JSONObject orderVsInvoice)
			throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : updOrderVsInvoice");

		JSONObject cbInvIdVsSSOrdNo = thirdPartyMapping
				.getJSONObject(CBSSIConstants.CBInvIdVsSSOrdNo);
		JSONObject ssOrdNoVsSSOrdKey = thirdPartyMapping
				.getJSONObject(CBSSIConstants.SSOrdNoVsSSOrdKey);
		JSONObject ssOrdKeyVsCBInvId = thirdPartyMapping
				.getJSONObject(CBSSIConstants.SSOrdKeyVsCBInvId);
		String orderNo = order.getString(CBSSIConstants.ssOrdNum);
		String orderKey = order.getString(CBSSIConstants.ssOrdKey);
		String invoiceId = orderVsInvoice.getString(orderNo);
		Boolean invHasSSOrder = cbInvIdVsSSOrdNo.has(invoiceId);
		if (!order.getBoolean(CBSSIConstants.ssSuccess))
		{
			updateFailedInvoice(invHasSSOrder, invoiceId, order.getString(CBSSIConstants.ssErrorMsg));
		}
		else
		{
			JSONArray orders;
			if (invHasSSOrder)
			{
				orders = cbInvIdVsSSOrdNo.getJSONArray(invoiceId);
				for (int i = 0; i < orders.length(); i++)
				{
					if (orders.getString(i).equals(orderNo))
					{
						break;
					}
					if(i == orders.length()-1)
					{
						orders.put(orderNo);
					}
				}
				cbInvIdVsSSOrdNo.put(invoiceId, orders);
			}
			else
			{
				orders = new JSONArray();
				orders.put(orderNo);
				cbInvIdVsSSOrdNo.put(invoiceId, orders);
			}
			if (!ssOrdNoVsSSOrdKey.has(orderNo))
			{
				ssOrdNoVsSSOrdKey.put(orderNo, orderKey);
				ssOrdKeyVsCBInvId.put(orderKey, invoiceId);
			}
			thirdPartyMapping.put(CBSSIConstants.CBInvIdVsSSOrdNo, cbInvIdVsSSOrdNo);
			thirdPartyMapping.put(CBSSIConstants.SSOrdNoVsSSOrdKey, ssOrdNoVsSSOrdKey);
			thirdPartyMapping.put(CBSSIConstants.SSOrdKeyVsCBInvId, ssOrdKeyVsCBInvId);
			removeFromFailedInvoices(invoiceId);
		}
	}

	private void handleSuccessResponse(Response response, JSONObject orderVsInvoice)
			throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : handleSuccessResponse");

		JSONObject object = new JSONObject(response.readEntity(String.class));
		JSONArray orders = object.getJSONArray(CBSSIConstants.ssResults);
		JSONObject order;
		for (int i = 0; i < orders.length(); i++)
		{
			order = orders.getJSONObject(i);
			updateOrderVsInvoice(order, orderVsInvoice);
		}
	}

	private void handleErrorResponse(Response response, JSONArray orders,
			JSONObject orderVsInvoice) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : handleErrorResponse");
		String orderNo;
		JSONObject order;
		String invoiceId;
		Boolean invHasSSOrder;
		MultivaluedMap<String, Object> headers = response.getHeaders();
		int status = response.getStatus();
		String messege = response.getStatusInfo().getReasonPhrase();
		JSONObject cbInvIdVsSSOrdNo = thirdPartyMapping.getJSONObject(CBSSIConstants.CBInvIdVsSSOrdNo);
		logger.log(Level.INFO, "\n\t" + status + " : " + messege);
		if ((status == CBSSIConstants.SS_RateLimExceeded))
		{
			logger.log(Level.INFO, "\n\t" + status + " : " + messege);
			if(headers.containsKey(CBSSIConstants.ssRetryAfter))
			{
				Thread.sleep((Integer)headers.get(CBSSIConstants.ssRetryAfter).get(0)*1000);
			}
			else
			{
				Thread.sleep(60000);
			}
			createSSOrders(orders, orderVsInvoice);
		}
		else
		{
			for(int i = 0; i < orders.length(); i++)
			{
				order = orders.getJSONObject(i);
				orderNo = order.getString(CBSSIConstants.ssOrdNum);
				invoiceId = orderVsInvoice.getString(orderNo);
				invHasSSOrder = cbInvIdVsSSOrdNo.has(invoiceId);
				updateFailedInvoice(invHasSSOrder, invoiceId, messege);
			}
			updThirdPartyMapping("Third Party Mapping : ");
			logger.log(Level.INFO, "\n\t" + status + " : " + messege);
			if(status != HttpStatus.SC_INTERNAL_SERVER_ERROR && status != HttpStatus.SC_GATEWAY_TIMEOUT)
			{
				throw new RuntimeException(status + " : " + messege);
			}
		}
	}

	private void updateFailedInvoice(Boolean invHasSSOrder, String invoiceId, String reason) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : updateFailedInvoice");
		JSONObject failedInvoices = thirdPartyMapping.getJSONObject(CBSSIConstants.FailedInvDetails);
		failedInvoices.put(invoiceId, reason);
		thirdPartyMapping.put(CBSSIConstants.FailedInvDetails, failedInvoices);
	}

	private void removeFromFailedInvoices(String invoiceId) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : updateFailedInvoice");
		JSONObject failedInvoices = thirdPartyMapping.getJSONObject(CBSSIConstants.FailedInvDetails);
		if (failedInvoices.has(invoiceId))
		{
			failedInvoices.remove(invoiceId);
			thirdPartyMapping.put(CBSSIConstants.FailedInvDetails, failedInvoices);
		}
	}

	private void updSSLastSyncTime(long time) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : updLastSyncTime");
		thirdPartyMapping.put(CBSSIConstants.SSLastSyncTime, time);
	}

	private void updCBLastSyncTime(int mode, Boolean hasNoMoreInv, long cbSyncTime) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : updLastSyncTime");
		if(!isFailedInvProcess(mode) && hasNoMoreInv)
		{
			thirdPartyMapping.put(CBSSIConstants.CBLastSyncTime, cbSyncTime);
		}
	}

	public void sync() throws Exception
	{
		handlePreviousFailedInvoice();
        updThirdPartyMapping("Third Party Mapping : ");
		logger.log(Level.INFO, "\n\tFailed Invoice Process Finished");
		logger.log(Level.INFO, "\n\tEnter 'c' To Continue or 'any_other_key' To Stop : ");
		Scanner s1 = new Scanner(System.in);
		Scanner s2 = new Scanner(System.in);
		String cont = s1.next();
		if(cont.equals("c"))
		{
			processSync(CBSSIConstants.SYNC);
			logger.log(Level.INFO, "\n\tSync Process Finished");
			logger.log(Level.INFO, "\n\tEnter 'c' To Continue or 'any_other_key' To Stop : ");
			cont = s2.next();
			if(cont.equals("c"))
			{
				sync();
			}
			s2.close();
		}
		s1.close();
		
	}

	private void handlePreviousFailedInvoice() throws Exception
	{
		processSync(CBSSIConstants.FAILED_INV_PROCESS);
	}

	private boolean hasValidBillingAddress(Boolean isDelInv, Invoice invoice, Customer customer, JSONObject order) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : hasValidBillingAddress");
		BillingAddress invBillAdd = invoice != null ? invoice.billingAddress() : null;
		Customer.BillingAddress cusBillAdd = customer != null ? customer.billingAddress() : null;
		JSONObject object = new JSONObject();
		if(invBillAdd == null && cusBillAdd == null)
		{
			return false;
		}
		if(cusBillAdd != null && hasValidCusBillAdd(cusBillAdd))
		{	
			logger.log(Level.INFO, "\n\tFilling Billing address for invoice :: " + invoice.id());
			fillCusBillAdd(cusBillAdd, object);
		}
		else if(invBillAdd != null && hasValidInvBillAdd(invBillAdd))
		{	
			logger.log(Level.INFO, "\n\tFilling Billing address for invoice :: " + invoice.id());
			fillInvBillAdd(invBillAdd, object);
		}
		else
		{
			return false;
		}
		order.put(CBSSIConstants.ssBillTo, object);
		return true;
	}
	
	private boolean isSameAddress(BillingAddress invBillAdd, Customer.BillingAddress cusBillAdd)
	{
		if((invBillAdd.firstName() == null && cusBillAdd.firstName() == null) || !(invBillAdd.firstName() != null && cusBillAdd.firstName() != null && invBillAdd.firstName().equals(cusBillAdd.firstName())))
		{
			return false;
		}
		else if((invBillAdd.lastName() == null && cusBillAdd.lastName() == null) || !(invBillAdd.lastName() != null && cusBillAdd.lastName() != null && invBillAdd.lastName().equals(cusBillAdd.lastName())))
		{
			return false;
		}
		else if((invBillAdd.line1() == null && cusBillAdd.line1() == null) || !(invBillAdd.line1() != null && cusBillAdd.line1() != null && invBillAdd.line1().equals(cusBillAdd.line1())))
		{
			return false;
		}
		else if((invBillAdd.line2() == null && cusBillAdd.line2() == null) || !(invBillAdd.line2() != null && cusBillAdd.line2() != null && invBillAdd.line2().equals(cusBillAdd.line2())))
		{
			return false;
		}
		else if((invBillAdd.line3() == null && cusBillAdd.line3() == null) || !(invBillAdd.line3() != null && cusBillAdd.line3() != null && invBillAdd.line3().equals(cusBillAdd.line3())))
		{
			return false;
		}
		else if(!(invBillAdd.city() != null && cusBillAdd.city() != null && invBillAdd.city().equals(cusBillAdd.city())))
		{
			return false;
		}
		else if(!(invBillAdd.state() != null && cusBillAdd.state() != null && invBillAdd.state().equals(cusBillAdd.state())))
		{
			return false;
		}
		else if(!(invBillAdd.zip() != null && cusBillAdd.zip() != null && invBillAdd.zip().equals(cusBillAdd.zip())))
		{
			return false;
		}
		else if(!(invBillAdd.country() != null && cusBillAdd.country() != null && invBillAdd.country().equals(cusBillAdd.country())))
		{
			return false;
		}
		else if((invBillAdd.phone() == null && cusBillAdd.phone() == null) || !(invBillAdd.phone() != null && cusBillAdd.phone() != null && invBillAdd.phone().equals(cusBillAdd.phone())))
		{
			return false;
		}
		return true;
	}

	private boolean hasValidInvBillAdd(BillingAddress invBillAdd) throws Exception
	{
		if ((invBillAdd.firstName() != null || invBillAdd.lastName() != null)
				&& (invBillAdd.line1() != null || invBillAdd.line2() != null
				|| invBillAdd.line3() != null) && invBillAdd.city() != null
				&& invBillAdd.state() != null && invBillAdd.zip() != null
				&& invBillAdd.country() != null)
		{	
			return true;
		}
		return false;
	}
	
	private void fillInvBillAdd(BillingAddress invBillAdd, JSONObject object) throws Exception
	{
		if (invBillAdd.firstName() != null && invBillAdd.lastName() != null)
		{
			object.put(CBSSIConstants.ssAddCusName, invBillAdd.firstName() + " " + invBillAdd.lastName());
		}
		else if (invBillAdd.firstName() != null)
		{
			object.put(CBSSIConstants.ssAddCusName, invBillAdd.firstName());
		}
		else if (invBillAdd.lastName() != null)
		{
			object.put(CBSSIConstants.ssAddCusName, invBillAdd.lastName());
		}
		if (invBillAdd.line1() != null && invBillAdd.line2() != null && invBillAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, invBillAdd.line1());
			object.put(CBSSIConstants.ssAddLine2, invBillAdd.line2());
			object.put(CBSSIConstants.ssAddLine3, invBillAdd.line3());
		}
		else if(invBillAdd.line1() != null && invBillAdd.line2() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, invBillAdd.line1());
			object.put(CBSSIConstants.ssAddLine2, invBillAdd.line2());
		}
		else if(invBillAdd.line2() != null && invBillAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, invBillAdd.line2());
			object.put(CBSSIConstants.ssAddLine2, invBillAdd.line3());
		}
		else if(invBillAdd.line1() != null && invBillAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, invBillAdd.line1());
			object.put(CBSSIConstants.ssAddLine2, invBillAdd.line3());
		}
		else if(invBillAdd.line1() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, invBillAdd.line1());
		}
		else if(invBillAdd.line2() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, invBillAdd.line2());
		}
		else if(invBillAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, invBillAdd.line3());
		}
		if (invBillAdd.validationStatus() != null)
		{
			object.put(CBSSIConstants.ssAddIsVerified, invBillAdd.validationStatus().toString());
		}
		if (invBillAdd.phone() != null)
		{
			object.put(CBSSIConstants.ssAddPhone, invBillAdd.phone());
		}
		object.put(CBSSIConstants.ssAddCity, invBillAdd.city());
		object.put(CBSSIConstants.ssAddState, invBillAdd.state());
		object.put(CBSSIConstants.ssAddPostCode, invBillAdd.zip());
		object.put(CBSSIConstants.ssAddCountry, invBillAdd.country());
	}

	private boolean hasValidCusBillAdd(Customer.BillingAddress cusBillAdd) throws Exception
	{
		if ((cusBillAdd.firstName() != null || cusBillAdd.lastName() != null)
				&& (cusBillAdd.line1() != null || cusBillAdd.line2() != null
				|| cusBillAdd.line3() != null) && cusBillAdd.city() != null
				&& cusBillAdd.state() != null && cusBillAdd.zip() != null
				&& cusBillAdd.country() != null)
		{	
			return true;
		}
		return false;
	}

	private void fillCusBillAdd(Customer.BillingAddress cusBillAdd, JSONObject object) throws Exception
	{
		if (cusBillAdd.firstName() != null && cusBillAdd.lastName() != null)
		{
			object.put(CBSSIConstants.ssAddCusName, cusBillAdd.firstName() + " " + cusBillAdd.lastName());
		}
		else if (cusBillAdd.firstName() != null)
		{
			object.put(CBSSIConstants.ssAddCusName, cusBillAdd.firstName());
		}
		else if (cusBillAdd.lastName() != null)
		{
			object.put(CBSSIConstants.ssAddCusName, cusBillAdd.lastName());
		}
		if (cusBillAdd.line1() != null && cusBillAdd.line2() != null && cusBillAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, cusBillAdd.line1());
			object.put(CBSSIConstants.ssAddLine2, cusBillAdd.line2());
			object.put(CBSSIConstants.ssAddLine3, cusBillAdd.line3());
		}
		else if(cusBillAdd.line1() != null && cusBillAdd.line2() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, cusBillAdd.line1());
			object.put(CBSSIConstants.ssAddLine2, cusBillAdd.line2());
		}
		else if(cusBillAdd.line2() != null && cusBillAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, cusBillAdd.line2());
			object.put(CBSSIConstants.ssAddLine2, cusBillAdd.line3());
		}
		else if(cusBillAdd.line1() != null && cusBillAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, cusBillAdd.line1());
			object.put(CBSSIConstants.ssAddLine2, cusBillAdd.line3());
		}
		else if(cusBillAdd.line1() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, cusBillAdd.line1());
		}
		else if(cusBillAdd.line2() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, cusBillAdd.line2());
		}
		else if(cusBillAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, cusBillAdd.line3());
		}
		if (cusBillAdd.validationStatus() != null)
		{
			object.put(CBSSIConstants.ssAddIsVerified, cusBillAdd.validationStatus().toString());
		}
		if (cusBillAdd.phone() != null)
		{
			object.put(CBSSIConstants.ssAddPhone, cusBillAdd.phone());
		}
		
		object.put(CBSSIConstants.ssAddCity, cusBillAdd.city());
		object.put(CBSSIConstants.ssAddState, cusBillAdd.state());
		object.put(CBSSIConstants.ssAddPostCode, cusBillAdd.zip());
		object.put(CBSSIConstants.ssAddCountry, cusBillAdd.country());
		
	}

	public Boolean hasValidShippingAddress(Boolean isDelInv, Invoice invoice, Subscription subscription, JSONObject order) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : hasValidShippingAddress");
		ShippingAddress invShippAdd = invoice != null ? invoice.shippingAddress() : null;
		Subscription.ShippingAddress subShippAdd = subscription != null ? subscription.shippingAddress() : null;
		JSONObject object = new JSONObject();
		if (invShippAdd == null && subShippAdd == null)
		{
			return false;
		}
		if(subShippAdd != null && hasValidSubShippAdd(subShippAdd))
		{	
			logger.log(Level.INFO, "\n\tFilling Shipping address for invoice :: " + invoice.id());
			fillSubShippAdd(subShippAdd, object);
		}
		else if(invShippAdd != null && hasValidInvShippAdd(invShippAdd))
		{
			logger.log(Level.INFO, "\n\tFilling Shipping address for invoice :: " + invoice.id());
			fillInvShippAdd(invShippAdd, object);
		}
		else
		{
			return false;
		}
		order.put(CBSSIConstants.ssShipTo, object);
		return true;
	}

	private boolean isSameAddress(ShippingAddress invShippAdd, Subscription.ShippingAddress subShippAdd)
	{
		if((invShippAdd.firstName() == null && subShippAdd.firstName() == null) || !(invShippAdd.firstName() != null && subShippAdd.firstName() != null && invShippAdd.firstName().equals(subShippAdd.firstName())))
		{
			return false;
		}
		else if((invShippAdd.lastName() == null && subShippAdd.lastName() == null) || !(invShippAdd.lastName() != null && subShippAdd.lastName() != null && invShippAdd.lastName().equals(subShippAdd.lastName())))
		{
			return false;
		}
		else if((invShippAdd.line1() == null && subShippAdd.line1() == null) || !(invShippAdd.line1() != null && subShippAdd.line1() != null && invShippAdd.line1().equals(subShippAdd.line1())))
		{
			return false;
		}
		else if((invShippAdd.line2() == null && subShippAdd.line2() == null) || !(invShippAdd.line2() != null && subShippAdd.line2() != null && invShippAdd.line2().equals(subShippAdd.line2())))
		{
			return false;
		}
		else if((invShippAdd.line3() == null && subShippAdd.line3() == null) || !(invShippAdd.line3() != null && subShippAdd.line3() != null && invShippAdd.line3().equals(subShippAdd.line3())))
		{
			return false;
		}
		else if(!(invShippAdd.city() != null && subShippAdd.city() != null && invShippAdd.city().equals(subShippAdd.city())))
		{
			return false;
		}
		else if(!(invShippAdd.state() != null && subShippAdd.state() != null && invShippAdd.state().equals(subShippAdd.state())))
		{
			return false;
		}
		else if(!(invShippAdd.zip() != null && subShippAdd.zip() != null && invShippAdd.zip().equals(subShippAdd.zip())))
		{
			return false;
		}
		else if(!(invShippAdd.country() != null && subShippAdd.country() != null && invShippAdd.country().equals(subShippAdd.country())))
		{
			return false;
		}
		else if((invShippAdd.phone() == null && subShippAdd.phone() == null) || !(invShippAdd.phone() != null && subShippAdd.phone() != null && invShippAdd.phone().equals(subShippAdd.phone())))
		{
			return false;
		}
		return true;
	}

	private boolean hasValidInvShippAdd(ShippingAddress invShippAdd)
	{
		if ((invShippAdd.firstName() != null || invShippAdd.lastName() != null)
				&& (invShippAdd.line1() != null || invShippAdd.line2() != null
				|| invShippAdd.line3() != null) && invShippAdd.city() != null
				&& invShippAdd.state() != null && invShippAdd.zip() != null
				&& invShippAdd.country() != null)
		{
			return true;
		}
		return false;
	}

	private void fillInvShippAdd(ShippingAddress invShippAdd, JSONObject object) throws Exception
	{
		if (invShippAdd.firstName() != null && invShippAdd.lastName() != null)
		{
			object.put(CBSSIConstants.ssAddCusName, invShippAdd.firstName() + " " + invShippAdd.lastName());
		}
		else if (invShippAdd.firstName() != null)
		{
			object.put(CBSSIConstants.ssAddCusName, invShippAdd.firstName());
		}
		else if (invShippAdd.lastName() != null)
		{
			object.put(CBSSIConstants.ssAddCusName, invShippAdd.lastName());
		}
		if (invShippAdd.line1() != null && invShippAdd.line2() != null && invShippAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, invShippAdd.line1());
			object.put(CBSSIConstants.ssAddLine2, invShippAdd.line2());
			object.put(CBSSIConstants.ssAddLine3, invShippAdd.line3());
		}
		else if(invShippAdd.line1() != null && invShippAdd.line2() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, invShippAdd.line1());
			object.put(CBSSIConstants.ssAddLine2, invShippAdd.line2());
		}
		else if(invShippAdd.line2() != null && invShippAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, invShippAdd.line2());
			object.put(CBSSIConstants.ssAddLine2, invShippAdd.line3());
		}
		else if(invShippAdd.line1() != null && invShippAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, invShippAdd.line1());
			object.put(CBSSIConstants.ssAddLine2, invShippAdd.line3());
		}
		else if(invShippAdd.line1() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, invShippAdd.line1());
		}
		else if(invShippAdd.line2() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, invShippAdd.line2());
		}
		else if(invShippAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, invShippAdd.line3());
		}
		if (invShippAdd.validationStatus() != null)
		{
			object.put(CBSSIConstants.ssAddIsVerified, invShippAdd.validationStatus().toString());
		}
		if (invShippAdd.phone() != null)
		{
			object.put(CBSSIConstants.ssAddPhone, invShippAdd.phone());
		}
		object.put(CBSSIConstants.ssAddCity, invShippAdd.city());
		object.put(CBSSIConstants.ssAddState, invShippAdd.state());
		object.put(CBSSIConstants.ssAddPostCode, invShippAdd.zip());
		object.put(CBSSIConstants.ssAddCountry, invShippAdd.country());
	}

	private boolean hasValidSubShippAdd(Subscription.ShippingAddress subShippAdd) throws Exception
	{
		if ((subShippAdd.firstName() != null || subShippAdd.lastName() != null)
				&& (subShippAdd.line1() != null || subShippAdd.line2() != null
				|| subShippAdd.line3() != null) && subShippAdd.city() != null
				&& subShippAdd.state() != null && subShippAdd.zip() != null
				&& subShippAdd.country() != null)
		{
			return true;
		}
		return false;
	}

	private void fillSubShippAdd(Subscription.ShippingAddress subShippAdd, JSONObject object) throws Exception
	{
		if (subShippAdd.firstName() != null && subShippAdd.lastName() != null)
		{
			object.put(CBSSIConstants.ssAddCusName, subShippAdd.firstName() + " " + subShippAdd.lastName());
		}
		else if (subShippAdd.firstName() != null)
		{
			object.put(CBSSIConstants.ssAddCusName, subShippAdd.firstName());
		}
		else if (subShippAdd.lastName() != null)
		{
			object.put(CBSSIConstants.ssAddCusName, subShippAdd.lastName());
		}
		if (subShippAdd.line1() != null && subShippAdd.line2() != null && subShippAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, subShippAdd.line1());
			object.put(CBSSIConstants.ssAddLine2, subShippAdd.line2());
			object.put(CBSSIConstants.ssAddLine3, subShippAdd.line3());
		}
		else if(subShippAdd.line1() != null && subShippAdd.line2() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, subShippAdd.line1());
			object.put(CBSSIConstants.ssAddLine2, subShippAdd.line2());
		}
		else if(subShippAdd.line2() != null && subShippAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, subShippAdd.line2());
			object.put(CBSSIConstants.ssAddLine2, subShippAdd.line3());
		}
		else if(subShippAdd.line1() != null && subShippAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, subShippAdd.line1());
			object.put(CBSSIConstants.ssAddLine2, subShippAdd.line3());
		}
		else if(subShippAdd.line1() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, subShippAdd.line1());
		}
		else if(subShippAdd.line2() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, subShippAdd.line2());
		}
		else if(subShippAdd.line3() != null)
		{
			object.put(CBSSIConstants.ssAddLine1, subShippAdd.line3());
		}
		if (subShippAdd.validationStatus() != null)
		{
			object.put(CBSSIConstants.ssAddIsVerified, subShippAdd.validationStatus().toString());
		}
		if (subShippAdd.phone() != null)
		{
			object.put(CBSSIConstants.ssAddPhone, subShippAdd.phone());
		}
		
		object.put(CBSSIConstants.ssAddCity, subShippAdd.city());
		object.put(CBSSIConstants.ssAddState, subShippAdd.state());
		object.put(CBSSIConstants.ssAddPostCode, subShippAdd.zip());
		object.put(CBSSIConstants.ssAddCountry, subShippAdd.country());	
	}

	private void fillCustomerInfo(Customer customer, JSONObject order) throws Exception
	{
		logger.log(Level.INFO, "\n\tFilling Customer Info "); // name and email not mandatory
		String name = getCustomerName(customer);
		String email = customer != null ? customer.email() : null;
		if(name != null)
		{
			order.put(CBSSIConstants.ssCusName, name);
		}
		if(email != null)
		{
			order.put(CBSSIConstants.ssCusEmail, email);
		}
	}

	private String getCustomerName(Customer customer) throws Exception
	{
		if(customer != null)
		{
			if (customer.firstName() != null && customer.lastName() != null)
			{
				return customer.firstName() + " " + customer.lastName();
			}
			else if (customer.firstName() != null)
			{
				return customer.firstName();
			}
			else if (customer.lastName() != null)
			{
				return customer.lastName();
			}
		}
		return null;
	}

	private String getSSOrderKey(String orderNo) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : getOrderNoForInvoice");
		JSONObject ordNoVsOrdkey = thirdPartyMapping
				.getJSONObject(CBSSIConstants.SSOrdNoVsSSOrdKey);
		return ordNoVsOrdkey.getString(orderNo);
	}

	private Boolean hasValidOrderItems(Boolean isDelInv, Boolean invHasSSOrder, Invoice invoice, JSONObject order, HashMap<String, ArrayList<CreditNote>> invoiceVsCreditNotes) throws Exception
	{
		String invoiceId = invoice.id();
		JSONArray orderItems = new JSONArray();
		List<LineItem> invLineItems = invoice.lineItems();
		if(invLineItems != null)
		{
			for (LineItem item : invLineItems)
			{
				fillOrderItems(invoiceId, item, orderItems, invoiceVsCreditNotes);
			}
		}
		Boolean invHasNoLineIems = orderItems.length() == 0;
		if(invHasNoLineIems && !invHasSSOrder) // for new invoice without line items, ss_order should not be created
		{
			logger.log(Level.INFO, "\n\tline items : 0 or deleted invoice : SSOrder cannot be created");
			return false;
		}
		if(isDelInv || invHasNoLineIems) // for updated_invoices with "all line items removed" & deleted_invoices, corresponding ss_order should be cancelled
		{	
			order.put(CBSSIConstants.ssOrdStatus, getOrderStatus(Invoice.Status.VOIDED));
		}
		else
		{
			order.put(CBSSIConstants.ssOrdStatus, getOrderStatus(invoice.status()));
		}
		order.put(CBSSIConstants.ssPaidDate, getSSTimeZone(invoice.paidAt()));
		order.put(CBSSIConstants.ssAmountPaid, (double)(invoice.amountPaid()/100));
		order.put(CBSSIConstants.ssOrdItems, orderItems);
		
		return true;
	}
	
	private void fillOrderItems(String invoiceId, LineItem item, JSONArray orderItems, HashMap<String, ArrayList<CreditNote>> invoiceVsCreditNotes) throws Exception
	{
		ArrayList<String> creditNotesLineIems = new ArrayList<String>();
		if(invoiceVsCreditNotes.containsKey(invoiceId))
		{
			getCreditNotesLineItems(invoiceId, creditNotesLineIems, invoiceVsCreditNotes.get(invoiceId));
		}
		if(!creditNotesLineIems.contains(item.entityId()))
		{
			JSONObject product = new JSONObject();
			product.put(CBSSIConstants.ssItemAdjusted, false);
			product.put(CBSSIConstants.ssItemKey, item.id());
			product.put(CBSSIConstants.ssItemSKU, item.entityId());
			product.put(CBSSIConstants.ssItemName, item.description());
			product.put(CBSSIConstants.ssItemQntity, item.quantity());
			product.put(CBSSIConstants.ssItemPrice, (double)(item.unitAmount()/100));
			product.put(CBSSIConstants.ssItemTaxAmnt, (double)(item.taxAmount()/100));
			orderItems .put(product);
		}	
	}

	private boolean isOrderFromCB(String ssOrderNum)
			throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : isOrderFromCB");
		JSONObject ssOrdersOfCBInvoices = thirdPartyMapping.getJSONObject(CBSSIConstants.SSOrdNoVsSSOrdKey);
		if (ssOrdersOfCBInvoices.has(ssOrderNum))// ssOrdersOfCBInvoices is a JSON object contains SSORDER NOS, which is updated regularly whenever SSOrders for CBInvoice created.						
		{
			logger.log(Level.INFO, "\n\tYup : OrderNum : " + ssOrderNum);
			return true;
		}
		logger.log(Level.INFO, "\n\tNope : OrderNum : " + ssOrderNum);
		return false;
	}

	private JSONObject getShipmentInfo(String orderNo) throws Exception
	{
		Client client = ClientBuilder.newClient();
		Response response = client.target(CBSSIConstants.shipmentUrl + orderNo)
				.request(MediaType.TEXT_PLAIN_TYPE).header("Authorization", ssApiKey).get();
		return new JSONObject(response.readEntity(String.class));
	}

	private void replicateInCBOrders(String invoiceId, String ssOrdId, String orderStatus, String batchId,
			String trackingId, String customerNote) throws OperationFailedException, InvalidRequestException, APIException, Exception
	{
		logger.log(Level.INFO, "\n\tCreating ChargeBee orders");
		Order order;
		Result result;
		String cbOrdId = getCBOrdId(ssOrdId);
		try
		{
			if (cbOrdId != null) // new order
			{
				result = Order.update(cbOrdId).status(CBSSIConstants.cbOrdStatusForSSOrdStatus.get(orderStatus)).fulfillmentStatus(orderStatus)
						.referenceId(ssOrdId).batchId(batchId).note(customerNote).request();
				order = result.order();
				logger.log(Level.INFO, "\n\tChargebee Order Updated");
			}
			else
			{
				logger.log(Level.INFO, "\n\tStatus : " + orderStatus);
				result = Order.create().id(ssOrdId).invoiceId(invoiceId).status(CBSSIConstants.cbOrdStatusForSSOrdStatus.get(orderStatus)).fulfillmentStatus(orderStatus)
						.referenceId(ssOrdId).batchId(batchId).note(customerNote).request();
				order = result.order();
				updSSOrderVsCBOrder(ssOrdId, order.id());
				logger.log(Level.INFO, "\n\tChargebee Order Created");
			}
		}
		catch(OperationFailedException e)
		{
			if(e.apiErrorCode.equals(CBSSIConstants.cbRateLimitExceeded))
			{
				logger.log(Level.SEVERE, "\n\t" + e.httpStatusCode + " : " + e.type + " : " + e.apiErrorCode + " : " + e.getMessage() + ", Param : " + e.param, e);
				Thread.sleep(60000);
				replicateInCBOrders(invoiceId, ssOrdId, orderStatus, batchId, trackingId, customerNote);
			}
			else
			{
				throw e;
			}
		}

	}

	private String getCBOrdId(String ssOrdId) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : getCBOrdId");
		JSONObject ssOrdVsCBOrd = thirdPartyMapping.getJSONObject(CBSSIConstants.SSOrdIdVsCBOrdId);
		if (ssOrdVsCBOrd.has(ssOrdId))
		{
			return ssOrdVsCBOrd.getString(ssOrdId);
		}
		return null;
	}

	private void updSSOrderVsCBOrder(String ssOrderId, String cbOrderId) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : updCBOrderVsSSOrder");
		JSONObject ssOrdVsCBOrd = thirdPartyMapping.getJSONObject(CBSSIConstants.SSOrdIdVsCBOrdId);
		JSONObject cbOrdVsSSOrd = thirdPartyMapping.getJSONObject(CBSSIConstants.CBOrdIdVsSSOrdId);
		if (ssOrdVsCBOrd.has(ssOrderId))
		{
			return;
		}
		ssOrdVsCBOrd.put(ssOrderId, cbOrderId);
		cbOrdVsSSOrd.put(cbOrderId, ssOrderId);
		thirdPartyMapping.put(CBSSIConstants.SSOrdIdVsCBOrdId, ssOrdVsCBOrd);
		thirdPartyMapping.put(CBSSIConstants.CBOrdIdVsSSOrdId, cbOrdVsSSOrd);
		// updThirdPartyMapping("SSOrdIdVsCBOrdId Updated : ");
	}

	public Timestamp getCBLastSyncTime() throws Exception
	{
		if (thirdPartyMapping.has(CBSSIConstants.CBLastSyncTime))
		{
			long lastSyncTime = thirdPartyMapping.getLong(CBSSIConstants.CBLastSyncTime);
			return new Timestamp(lastSyncTime);
		}
		return null;
	}

	public String getSSLastSyncTime() throws Exception
	{
		if (thirdPartyMapping.has(CBSSIConstants.SSLastSyncTime))
		{
			long lastSyncTime = thirdPartyMapping.getLong(CBSSIConstants.SSLastSyncTime);
			return getSSTimeZone(new Date(lastSyncTime));
		}
		return null;
	}

	private JSONObject getThirdPartyMapping() throws Exception
	{
		JSONObject obj = null;

		if (obj == null)
		{
			obj = new JSONObject();
			obj.put(CBSSIConstants.CBInvIdVsCBSubId, new JSONObject());
			obj.put(CBSSIConstants.CBInvIdVsCBCusId, new JSONObject());
			obj.put(CBSSIConstants.CBCusIdVsCBInvId, new JSONObject());
			obj.put(CBSSIConstants.CBSubIdVsCBInvId, new JSONObject());
			obj.put(CBSSIConstants.FailedInvDetails, new JSONObject());
			obj.put(CBSSIConstants.SSOrdIdVsCBOrdId, new JSONObject());
			obj.put(CBSSIConstants.CBOrdIdVsSSOrdId, new JSONObject());
			obj.put(CBSSIConstants.SSOrdNoVsSSOrdKey, new JSONObject());
			obj.put(CBSSIConstants.SSOrdKeyVsCBInvId, new JSONObject());
			obj.put(CBSSIConstants.CBInvIdVsSSOrdNo, new JSONObject());
		}
		return obj;
	}

	private boolean isInitialFetch(int mode)
	{
		return mode == CBSSIConstants.INIT_FETCH;
	}

	private boolean isSync(int mode)
	{
		return mode == CBSSIConstants.SYNC;
	}

	private boolean isFailedInvProcess(int mode)
	{
		return mode == CBSSIConstants.FAILED_INV_PROCESS;
	}

	private String getOrderStatus(Invoice.Status status) throws Exception
	{
		logger.log(Level.INFO, "\n\tMethod : getOrderStatus");
		return CBSSIConstants.ssOrdStatusOfCBInvStatus.get(status);
	}

}