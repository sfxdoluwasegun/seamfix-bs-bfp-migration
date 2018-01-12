/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.proxy;
import com.sf.biocapture.entity.enums.FingersEnum;
import java.io.Serializable;
/**
 * This is used to authenticate or identify the user that registered a
 * subscriber
 *
 * @author Marcel
 * @since 01-Sep-2016, 16:37:12
 */
public class RegistrationSignature implements Serializable{
    private static final long serialVersionUID = 4324319610622971811L;
    private String username;
    private String fingerType;
    private byte[] wsqData;
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getFingerType() {
        return fingerType;
    }
    public void setFingerType(String fingerType) {
        this.fingerType = fingerType;
    }
    public byte[] getWsqData() {
        return wsqData;
    }
    public void setWsqData(byte[] wsqData) {
        this.wsqData = wsqData;
    }
    public com.sf.biocapture.entity.RegistrationSignature to() {
        com.sf.biocapture.entity.RegistrationSignature rs = new com.sf.biocapture.entity.RegistrationSignature();
        rs.setFingerType(fingerType == null ? null : FingersEnum.valueOf(fingerType));
        rs.setUsername(username);
        rs.setWsqData(wsqData);
        return rs;
    }
}