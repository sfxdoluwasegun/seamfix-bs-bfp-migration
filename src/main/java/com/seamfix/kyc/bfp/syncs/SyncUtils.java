/**
 * 
 */
package com.seamfix.kyc.bfp.syncs;

import com.seamfix.kyc.bfp.BsClazz;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

import com.sf.biocapture.entity.EnrollmentLog;
import com.sf.biocapture.entity.SmsActivationRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

/**
 * @author dawuzi
 *
 *This class just puts the logic for determining the actual time of registration in one place for ease of maintenance and other utility methods
 */
public class SyncUtils extends BsClazz {
        SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm:ss SS");
        SimpleDateFormat concatenatedSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SS");
        
	public void setRegistrationTimestamp(SmsActivationRequest smsActivationRequest, EnrollmentLog log){
		Timestamp registrationTimestamp = getRegistrationTimestamp(log);
		smsActivationRequest.setRegistrationTimestamp(registrationTimestamp); 
	}
	
	public Timestamp getRegistrationTimestamp(EnrollmentLog log){
		
		String code = log.getEnrollmentRef().getCode();

		Timestamp timestamp = null;
		
		if (code != null && code.toUpperCase().startsWith("DROID")) {
			timestamp = new Timestamp(log.getDate().getTime());
		} else {
                        String concatenated = dateSdf.format(log.getDate()) + " " + timeSdf.format(log.getTime());
                        try {
                            /**
                             * //timestamp = new Timestamp(log.getDate().getTime() + log.getTime().getTime());
                             * the above was removed because it introduced an hour difference less than the actual time.
                             * thus this is just a workaround for now.
                             */                            
                            timestamp = new Timestamp(concatenatedSdf.parse(concatenated).getTime());
                        } catch (ParseException ex) {
                            logger.error("", ex);
                        }
		}
		return timestamp;
	}
	
	public boolean isWithinTimeRange(Date time, LocalTime startTime, LocalTime endTime){
		
		Calendar registrationTime = Calendar.getInstance();
		
		registrationTime.setTime(time);
		
		LocalTime registrationLocalTime = LocalTime.of(registrationTime.get(Calendar.HOUR_OF_DAY), 
				registrationTime.get(Calendar.MINUTE), registrationTime.get(Calendar.SECOND));
		
		return isWithinTimeRange(registrationLocalTime, startTime, endTime);
	}
	
	public boolean isWithinTimeRange(LocalTime time, LocalTime startTime, LocalTime endTime){
		return time.isAfter(startTime) && time.isBefore(endTime);
	}
        
        public String getFilteredWindowsAppVersion (String appVersion) {  
            if (appVersion == null) {
                return appVersion;
            }
            String prefix = "Smart Client for KYC [Build: ";
            int indexOf = appVersion.indexOf("Smart Client for KYC [Build: ");
            int indexOf2 = appVersion.indexOf(", Install Date");
            if (indexOf == -1 || indexOf2 == -1) {
                return appVersion;
            } else {
                String version = appVersion.substring(prefix.length() + indexOf, indexOf2);
                appVersion = version.trim();
            }
            return appVersion;
        }
        
    @SuppressWarnings("PMD")
    public String log(Object entity) {
        String result = "";
        if (entity == null) {
            return null;
        }
        try {

            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            result = ow.writeValueAsString(entity);
        } catch (Exception e) {
            logger.error("", e);
        }
        return result;
    }
}
