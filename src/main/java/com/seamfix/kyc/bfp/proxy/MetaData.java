package com.seamfix.kyc.bfp.proxy;

import com.sf.biocapture.entity.EnrollmentRef;
import java.io.Serializable;
import java.sql.Timestamp;

public class MetaData implements Serializable {

    private static final long serialVersionUID = 265465236123L;

    private Timestamp syncTimestamp;

    private String captureMachineId;

    private Timestamp confirmationTimestamp;

    private String appVersion;

    private String stateOfRegistration;

    private String agentMobile;

    private String realtimeDeviceId;

    private Double latitude;

    private Double longitude;

    private Double locationAccuracy;

    private EnrollmentRef enrollmentRef;
    
    private Boolean mockedCoordinate;

    public Timestamp getSyncTimestamp() {
        return this.syncTimestamp;
    }

    public void setSyncTimestamp(Timestamp syncTimestamp) {
        this.syncTimestamp = syncTimestamp;
    }

    public String getCaptureMachineId() {
        return this.captureMachineId;
    }

    public void setCaptureMachineId(String captureMachineId) {
        this.captureMachineId = captureMachineId;
    }

    public Timestamp getConfirmationTimestamp() {
        return this.confirmationTimestamp;
    }

    public void setConfirmationTimestamp(Timestamp confirmationTimestamp) {
        this.confirmationTimestamp = confirmationTimestamp;
    }

    public String getAppVersion() {
        return this.appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getStateOfRegistration() {
        return this.stateOfRegistration;
    }

    public void setStateOfRegistration(String stateOfRegistration) {
        this.stateOfRegistration = stateOfRegistration;
    }

    public String getAgentMobile() {
        return this.agentMobile;
    }

    public void setAgentMobile(String agentMobile) {
        this.agentMobile = agentMobile;
    }

    public String getRealtimeDeviceId() {
        return realtimeDeviceId;
    }

    public void setRealtimeDeviceId(String realtimeDeviceId) {
        this.realtimeDeviceId = realtimeDeviceId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLocationAccuracy() {
        return locationAccuracy;
    }

    public void setLocationAccuracy(Double locationAccuracy) {
        this.locationAccuracy = locationAccuracy;
    }

    public EnrollmentRef getEnrollmentRef() {
        return enrollmentRef;
    }

    public void setEnrollmentRef(EnrollmentRef enrollmentRef) {
        this.enrollmentRef = enrollmentRef;
    }

    public Boolean getMockedCoordinate() {
        return mockedCoordinate;
    }

    public void setMockedCoordinate(Boolean mockedCoordinate) {
        this.mockedCoordinate = mockedCoordinate;
    }

    public com.sf.biocapture.entity.MetaData toMetaData() {
        com.sf.biocapture.entity.MetaData md = new com.sf.biocapture.entity.MetaData();
        md.setAgentMobile(agentMobile);
        md.setAppVersion(appVersion);
        md.setCaptureMachineId(captureMachineId);
        md.setConfirmationTimestamp(confirmationTimestamp);
        md.setStateOfRegistration(stateOfRegistration != null ? stateOfRegistration.toUpperCase().replace("STATE", "").trim() : stateOfRegistration);
        md.setSyncTimestamp(syncTimestamp);
        md.setRealtimeDeviceId(realtimeDeviceId);
        md.setLatitude(latitude);
        md.setLongitude(longitude);
        md.setLocationAccuracy(locationAccuracy);
        md.setMockedCoordinate(mockedCoordinate);

        return md;
    }

    @Override
    public String toString() {
        return "MetaData{" + "syncTimestamp=" + syncTimestamp + ", captureMachineId=" + captureMachineId + ", confirmationTimestamp=" + confirmationTimestamp
                + ", appVersion=" + appVersion + ", stateOfRegistration=" + stateOfRegistration + ", agentMobile=" + agentMobile + ", realtimeDeviceId="
                + realtimeDeviceId + ", latitude=" + latitude + ", longitude=" + longitude + ", locationAccuracy=" + locationAccuracy + ", enrollmentRef=" + enrollmentRef + '}';
    }
}
