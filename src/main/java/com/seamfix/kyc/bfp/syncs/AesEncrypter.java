/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.seamfix.kyc.bfp.syncs;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.seamfix.kyc.bfp.BsClazz;
import java.io.IOException;
import java.security.GeneralSecurityException;



/**
 * This class provides a means to easily encrypt an plaintext from an input stream and output the ciphertext in an output stream using AES algorithm
 * and and vice versa.
 *
 * @author Ezeozue Chidube
 */
public class AesEncrypter extends BsClazz{
    /**
     * Used for logging within this class. Named after the class
     */
    /**
     * Encryption cipher
     */
    private Cipher ecipher;
    /**
     * Decryption cipher
     */
    private Cipher dcipher;

    private Cipher newDCipher;
    
    private Base64Coder base64Coder = new Base64Coder();

    /**
     * Creates an AesEncrypter object.
     */
    public AesEncrypter() {
        try {
        	//The following is the process for starting up encryption and decryption
            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            byte[] seed = "SeamfixBiocapture".getBytes();
            SecureRandom sr = new SecureRandom(seed);
            keygen.init(sr);
            // If you do not initialize the KeyGenerator, each provider supply a default initialization.
            SecretKey aeskey = keygen.generateKey();

            ecipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            dcipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            newDCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

            ecipher.init(Cipher.ENCRYPT_MODE, aeskey);
            dcipher.init(Cipher.DECRYPT_MODE, aeskey);
            newDCipher.init(Cipher.DECRYPT_MODE, getDecryptSecureKey());
//            base64Coder = new Base64Coder();
        } catch (GeneralSecurityException e) {
            logger.error("", e);
        }
    }
    
    protected void encryptDecrypt(InputStream in, OutputStream out, boolean type) {
        try {
            // Buffer used to transport the bytes from one stream to another
            byte[] buf = new byte[1024];
            
            if(type){// encrypt
            	out = new CipherOutputStream(out, ecipher);
            }else{ // decrypt
            	in = new CipherInputStream(in, newDCipher);
            }

            // Read in the cleartext bytes and write to out to encrypt
            int numRead = 0;
            while ((numRead = in.read(buf)) >= 0) {
                out.write(buf, 0, numRead);
            }
            out.close();
        } catch (IOException e) {
            logger.error("", e);
        }
        finally
        {
        	try {
				in.close();
			} catch (IOException e) {
				logger.error("", e);
			}
        	try {
				out.close();
			} catch (IOException e) {
				logger.error("", e);
			}
        }
    }

    /**
     * Encrypts plaintext from an input stream and writes out ciphertext to an output stream
     *
     * @param in
     *            source of plaintext
     * @param out
     *            destination of ciphertext
     */
    public void encrypt(InputStream in, OutputStream out) {
        encryptDecrypt(in, out, Boolean.TRUE);
    }

    /**
     * Decrypts ciphertext from an input stream and writes out plaintext to an output stream
     *
     * @param in
     *            source of ciphertext
     * @param out
     *            destination of plaintext
     */
	public void decrypt(InputStream in, OutputStream out) {
        encryptDecrypt(in, out, Boolean.FALSE);
    }

    /**
     * @author Temitope Faro
     * @return
     */
    private final SecretKey getDecryptSecureKey(){

    	return new SecretKey() {
			/**
			 *
			 */
			private static final long serialVersionUID = 5035361746769477420L;

			@Override
			public String getFormat() {
				return "RAW";
			}

			@Override
			public byte[] getEncoded() {
				return base64Coder.decode("ZPOU-eSJIV-RUJkNWahuDA==");
			}

			@Override
			public String getAlgorithm() {
				return "AES";
			}
		};
    }
}
