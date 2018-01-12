/**
 * 
 */
package com.seamfix.kyc.bfp.util;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;

import com.seamfix.kyc.bfp.BioCache;
import com.seamfix.kyc.bfp.service.AppDS;
import com.sf.biocapture.entity.Outlet;
import com.sf.location.geofence.Coordinate;
import com.sf.location.geofence.Geofence;

import nw.commons.NeemClazz;

/**
 * @author Charles
 *
 */
@Stateless
public class LocationUtil extends NeemClazz {
	
	@Inject
	BioCache cache;
		
	@Inject
	AppDS appDs;
	
	private Geofence geofence = new Geofence();

	public Boolean isWithinGeofence(Double lat, Double lng, String mac, String deviceId) {
		if (lat == null || lng == null) {
			return null;
		}
		
		if(StringUtils.isBlank(mac) && StringUtils.isBlank(deviceId)) {
			return null;
		}
		
		try {
			Coordinate coordinate = getOutletCoordinate(mac, deviceId);
			if(coordinate == null) {
				return null;
			}

			double radius = Double.parseDouble(appDs.getSettingValue("GEO-FENCE-RADIUS", "50", "Geofence radius in meters, outside which a notification will be sent.", true));
			return geofence.isWithinCircleBoundary(lat, lng, radius, coordinate.getLat(), coordinate.getLng());
		} catch (Exception e) {
			logger.error("Error while checking circle geofence: ", e);
			return null;
		}
	}
	
	public Coordinate getOutletCoordinate(String mac, String deviceId) {
		String cacheName = null;
		if(StringUtils.isNotBlank(deviceId)) {
			cacheName = ("OUTLET_COORDINATE" + deviceId).replace(" ", "");
		} else {
			cacheName = ("OUTLET_COORDINATE" + mac).replace(" ", "");
		}
				
		Coordinate coordinate = cache.getItem(cacheName, Coordinate.class);
		if(coordinate == null) {
            //check db
			Outlet outlet = appDs.getOutlet(mac, deviceId);
			if(outlet != null && outlet.getLatitude() != null && outlet.getLongitude() != null) {
				coordinate = new Coordinate(outlet.getLatitude(), outlet.getLongitude());
				cache.setItem(cacheName, coordinate, 60 * 60 * 2); //2 hrs
			}
		}
		return coordinate;
	}
}
