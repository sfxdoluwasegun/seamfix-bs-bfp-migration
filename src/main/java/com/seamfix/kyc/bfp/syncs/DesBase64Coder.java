/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.seamfix.kyc.bfp.syncs;

/**
 * @since 23-Aug-2016, 15:54:14
 * @author Dawuzi
 * @author Marcel Ugwu
 */
public class DesBase64Coder extends Base64Coder {

    @Override
    protected void enableMapping() {
        int t = 0;
        for (char c = 'A'; c <= 'Z'; c++) {
            map1[t++] = c;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            map1[t++] = c;
        }
        for (char c = '0'; c <= '9'; c++) {
            map1[t++] = c;
        }
        map1[t++] = '+';
        map1[t++] = '/';

        for (int i = 0; i < map2.length; i++) {
            map2[i] = -1;
        }
        for (int i = 0; i < 64; i++) {
            map2[map1[i]] = (byte) i;
        }
    }

    public DesBase64Coder() {
        this.enableMapping();
    }
}
