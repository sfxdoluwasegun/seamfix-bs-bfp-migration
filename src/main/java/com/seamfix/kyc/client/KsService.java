package com.seamfix.kyc.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import com.seamfix.kyc.bfp.BsClazz;

public class KsService extends BsClazz{

	private String serverUrl;

	public KsService() {
		serverUrl = appProps.getProperty("ks-url", "https://simreg.mtnnigeria.net:8443")+"/biocapture";
	}

	public static void main(String[] args) {

		Client client = ClientBuilder.newClient();
//		ResteasyWebTarget target = (ResteasyWebTarget) client.target("http://ojtsimreg02:8990/biocapture");
		ResteasyWebTarget target = (ResteasyWebTarget) client.target("https://simreg.mtnnigeria.net:8443/biocapture");
//		ResteasyWebTarget target = (ResteasyWebTarget) client.target("http://10.1.242.239:7080/biocapture");
		target.register(new KsAuthFilter());
		KSClient proxy = target.proxy(KSClient.class);
//		ServerResponse sim = proxy.verifySimSerial("AAAaa", null);
//		System.out.println(sim);
//		System.out.println("\n\n");
		MsisdnResponse verifyMsisdn;
		
//		verifyMsisdn = proxy.verifyMsisdn("07033333333");
//		System.out.println("verifyMsisdn : "+verifyMsisdn);
		verifyMsisdn = proxy.verifyMsisdn("09074977585");
		System.out.println("verifyMsisdn : "+verifyMsisdn);
	}

	public KSClient getServiceClient(){
		Client client = ClientBuilder.newClient();
		ResteasyWebTarget target = (ResteasyWebTarget) client.target(serverUrl);
		target.register(new KsAuthFilter());
		KSClient proxy = target.proxy(KSClient.class);
		return proxy;
	}

}
