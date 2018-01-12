/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.syncs;

import com.seamfix.kyc.bfp.BsClazz;
import com.seamfix.kyc.bfp.exception.EncryptionException;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.apache.commons.lang.CharUtils;

/**
 *
 * @author Dawuzi, Marcel
 * @since 23-Aug-2016, 15:09:27
 */
public class DesEncrypter extends BsClazz{

    private class SecretKeyImpl implements SecretKey {

        private String encodedKey = "ZPKU/eWJIF4=";

        public SecretKeyImpl() {
        }

        public SecretKeyImpl(String encodedKey) {
            this.encodedKey = encodedKey;
        }

        private static final long serialVersionUID = 7875437463225811419L;

        @Override
        public String getAlgorithm() {
            return "DES";
        }

        @Override
        public String getFormat() {
            return "RAW";
        }

        @Override
        public byte[] getEncoded() {
            return desBase64Coder.decode(encodedKey);
        }

    }

    private Cipher deCipher;
    
    private DesBase64Coder desBase64Coder;

    /**
     * this is used to improve efficiency by defining a reusable pattern instead of recreating this pattern each time a string comparison is made
     */
    private final Pattern SPACE_PATTERN = Pattern.compile(" ");

    public DesEncrypter() {
        try {
            desBase64Coder = new DesBase64Coder();
            deCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            deCipher.init(Cipher.DECRYPT_MODE, new SecretKeyImpl());
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalStateException();
        }
    }

    public void decryptFields(Object entity) throws EncryptionException {
        if (entity == null) {
            return;
        }
        Class<?> clazz = entity.getClass();
        BeanInfo beanInfo = null;

        try {
            beanInfo = Introspector.getBeanInfo(clazz);
        } catch (IntrospectionException e) {
            throw new EncryptionException(e);
        }

        if (beanInfo == null || beanInfo.getPropertyDescriptors() == null) {
            return;
        }
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for (int i = 0; i < propertyDescriptors.length; i++) {
            PropertyDescriptor propertyDescriptor = propertyDescriptors[i];
            decryptField(entity, propertyDescriptor);
        }
    }

    /**
     * Decrypts a given string variable in a given entity.
     * Checks for and decrypts concatenated encryptions that are separated by a white space. this only applies when a space is found
     * @param entity
     * @param propertyDescriptor 
     */
    private void decryptField(Object entity, PropertyDescriptor propertyDescriptor) throws EncryptionException {
        try {
            if (propertyDescriptor == null || !propertyDescriptor.getPropertyType().equals(String.class)) {
                return;
            }

            Method getMethod = propertyDescriptor.getReadMethod();
            Object fieldValue = getMethod.invoke(entity);

            if (fieldValue == null) {
                return;
            }
            if (!fieldValue.getClass().equals(String.class)) {
                //this should not happen but who knows
                return;
            }

            String stringField = (String) fieldValue;
            String plain;

            if (!stringField.contains(" ")) {
                //not white space separated
                plain = decrypt(stringField);
            } else {
                String[] encryptedString = SPACE_PATTERN.split(stringField);
                plain = "";
                for (int i = 0; i < encryptedString.length; i++) {
                    plain += decrypt(encryptedString[i]);
                    if (i < encryptedString.length - 1) {
                        //reintroduce white space to result after decryption
                        plain += " ";
                    }
                }
            }

            //reassign decrypted value to entity variable
            Method setMethod = propertyDescriptor.getWriteMethod();
            setMethod.invoke(entity, plain);
        } catch (ReflectiveOperationException e) {
            throw new EncryptionException(e);
        }
    }

    @SuppressWarnings("PMD")
    public String decrypt(String encryptedText) {
        try {
            if (encryptedText == null || encryptedText.equals("")) {
                return encryptedText;
            }
            byte[] cp = desBase64Coder.decode(encryptedText);
            byte[] plain = deCipher.doFinal(cp);
            String plainText = new String(plain, "UTF8");

//            logger.debug("encrypted: " + encryptedText + ", plain: " + plainText);
            //a recursive call to ensure proper clean up where recursive encryption may have occurred on a string value
            if (isEncrypted(plainText)) {
                return decrypt(plainText);
            }
            return plainText;
//            the following exceptions are expected when a valid String is finally encountered in one of the recursive call stack
//            hence the hiding of the exception stacktrace
//            it simply indicates that a valid non encrypted text has been encountered and so terminate and return the decrypted String
        } catch (IllegalArgumentException | UnsupportedEncodingException | IllegalBlockSizeException | BadPaddingException e) {
//            logger.error("");
        }

        return encryptedText;
    }

    private boolean isEncrypted(String text) {
        try {
            if (text == null || text.isEmpty()) {
                return false;
            }
            if (!isAscii(text)) {
                return true;
            }
            desBase64Coder.decode(text);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isAscii(String plainText) {
        for (int x = 0; x < plainText.length(); x++) {
            char ch = plainText.charAt(x);
            if (!CharUtils.isAsciiAlphanumeric(ch)) {
                return false;
            }
        }
        return true;
    }
}
