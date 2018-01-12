package com.seamfix.kyc.client;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

public class KsAuthFilter implements ClientRequestFilter{

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		requestContext.getHeaders().add("sc-auth-key", "#D8CK>HIGH<LOW>#");
		requestContext.getHeaders().add("User-Agent", "MK-BFP");
		requestContext.getHeaders().add("User-UUID", "8c247ac9-d722-496c-b346-9cb405a6bc5d");
		requestContext.getHeaders().add("Client-ID", "smartclient");
	}


}
