package com.seamfix.kyc.client;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

public interface KSClient {

	@POST
	@Path("serial/status")
	public ServerResponse verifySimSerial(@FormParam("serialNumber") String serialNo, @FormParam("puk") String puk);

	@POST
	@Path("astatus/check")
	public MsisdnResponse verifyMsisdn(@FormParam("msisdn") String msisdn);

	@POST
    @Path("simswap/simchange")
    @Produces({MediaType.APPLICATION_JSON})
    public SimSwapResponse doSwap(@FormParam("msisdn") String msisdn, @FormParam("orderNumber") String orderNumber, 
    		@FormParam("newPUK") String newPUK, @FormParam("newSimSerial") String newSimSerial);
}
