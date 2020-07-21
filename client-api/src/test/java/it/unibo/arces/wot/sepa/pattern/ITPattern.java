package it.unibo.arces.wot.sepa.pattern;

import it.unibo.arces.wot.sepa.AggregatorTestUnit;
import it.unibo.arces.wot.sepa.ConsumerTestUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.HashMap;

public class ITPattern implements ISubscriptionHandler{

	protected final Logger logger = LogManager.getLogger();
	
	protected static JSAP app = null;
	protected static ConfigurationProvider provider;
	protected static ClientSecurityManager sm = null;

	protected static ConsumerTestUnit consumerAll;
	protected static Producer randomProducer;
	protected static AggregatorTestUnit randomAggregator;
	protected static ConsumerTestUnit consumerRandom1;
	
	protected static GenericClient genericClient;
	protected static HashMap<String,String> subscriptions = new HashMap<>();
	
	private int genericClientNotifications;
	private int genericClientSubscriptions;
	
	public void setOnSemanticEvent(String spuid) {
		genericClientNotifications++;
	}
	
	public int getNotificationsCount() {
		return genericClientNotifications;
	}

	public void setOnSubscribe(String spuid, String alias) {
		genericClientSubscriptions++;
	}
	
	public int getSubscriptionsCount() {
		return genericClientSubscriptions;
	}

	public void setOnUnsubscribe(String spuid) {
		genericClientSubscriptions--;
	}
	
	@BeforeAll
	public static void init() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		try {
			provider = new ConfigurationProvider();
			app = provider.getJsap();
		} catch (SEPAPropertiesException | SEPASecurityException e) {
			assertFalse(true,"Configuration not found");
		}

		if (app.isSecure()) {
			sm = provider.getSecurityManager();
			Response ret = sm.register("SEPATest");
			ret = sm.refreshToken();
			assertFalse(ret.isError());
		}
	}
	
	@BeforeEach
	public void beginTest() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		consumerAll = new ConsumerTestUnit(app, "ALL", sm);
		randomProducer = new Producer(app, "RANDOM", sm);
		randomAggregator = new AggregatorTestUnit(app, "RANDOM", "RANDOM1", sm);
		consumerRandom1 = new ConsumerTestUnit(app, "RANDOM1", sm);
		genericClient = new GenericClient(app, sm, this);
	}

	@AfterEach
	public void afterTest() throws IOException, SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		consumerAll.unsubscribe();
		consumerAll.close();
		
		randomAggregator.unsubscribe();
		randomAggregator.close();
		
		consumerRandom1.unsubscribe();
		consumerRandom1.close();
		
		randomProducer.close();
	}
	
	@Test 
	//(timeout = 1000)
	public void subscribe() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		consumerAll.syncSubscribe();
	}

	@Test
	//(timeout = 1000)
	public void produce() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		Response ret = randomProducer.update();
		
		assertFalse(ret.isError());
	}

	@Test 
	//(timeout = 1000)
	public void subscribeAndResults() throws InterruptedException, SEPASecurityException, IOException,
			SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
		consumerAll.syncSubscribe();
		consumerAll.waitFirstNotification();
	}

	@Test 
	//(timeout = 1000)
	public void notification() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		consumerAll.syncSubscribe();

		randomProducer.update();

		consumerAll.waitNotification();
	}
	
	@Test 
	//(timeout = 1000)
	//@RepeatedTest(value = 200)
	public void aggregation() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {		
		logger.debug("Aggregator");
		consumerRandom1.syncSubscribe();

		logger.debug("Aggregator first subscribe ok");

		randomAggregator.syncSubscribe();

		logger.debug("Aggregator second subscribe ok");

		randomProducer.update();
		logger.debug("Aggregator Update Done");

		randomAggregator.waitNotification();
		consumerRandom1.waitNotification();
		logger.debug("Aggregator stop");
	}
	
	@Test
	//(timeout =  5000)
	public void genericClientSingleSubscribe() {
		try {
			genericClient.subscribe("ALL", null, "first");
			
			if (getSubscriptionsCount() != 1) {
				synchronized(this) {
					wait(1000);
				}
				assertFalse(getSubscriptionsCount()!=1,"Failed to subscribe");
			}
			
			genericClient.update("RANDOM", null);
			
			if (getNotificationsCount() != 2) {
				synchronized(this) {
					wait(1000);
				}
				assertFalse(getNotificationsCount()!=2,"Failed to notify");
			}
			
			genericClient.unsubscribe(subscriptions.get("first"));
			
			if (getSubscriptionsCount() != 0) {
				synchronized(this) {
					wait(1000);
				}
				assertFalse(getSubscriptionsCount()!=0,"Failed to unsubscribe");
			}
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | SEPABindingsException
				| InterruptedException | IOException e) {
			e.printStackTrace();
			assertFalse(true,e.getMessage());
		}
	}
	
	@Test
	//(timeout =  5000)
	public void genericClientDoubleSubscribe() {
		try {
			genericClient.subscribe("RANDOM", null, "first");
			genericClient.subscribe("RANDOM1", null, "second");
			
			if (getSubscriptionsCount() != 2) {
				synchronized(this) {
					wait(1000);
				}
				assertFalse(getSubscriptionsCount()!=2,"Failed to subscribe");
			}
			
			genericClient.update("RANDOM", null);
			genericClient.update("RANDOM1", null);
			
			if (getNotificationsCount() != 4) {
				synchronized(this) {
					wait(1000);
				}
				assertFalse(getNotificationsCount()!=2,"Failed to notify");
			}
			
			genericClient.unsubscribe(subscriptions.get("first"));
			genericClient.unsubscribe(subscriptions.get("second"));
					
			if (getSubscriptionsCount() != 0) {
				synchronized(this) {
					wait(1000);
				}
				assertFalse(getSubscriptionsCount()!=0,"Failed to unsubscribe");
			}
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | SEPABindingsException
				| InterruptedException | IOException e) {
			e.printStackTrace();
			assertFalse(true,e.getMessage());
		}
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		logger.debug(notify);
		setOnSemanticEvent(notify.getSpuid());
	}

	@Override
	public void onBrokenConnection(ErrorResponse err) {
		logger.debug("onBrokenConnection "+err);
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.debug(errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.debug("onSubscribe "+spuid+" "+alias);
		subscriptions.put(alias, spuid);
		setOnSubscribe(spuid,alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.debug("onUnsubscribe "+spuid);
		setOnUnsubscribe(spuid);
	}
}
