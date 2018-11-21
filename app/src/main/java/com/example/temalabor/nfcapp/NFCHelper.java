package com.example.temalabor.nfcapp;

import android.content.Context;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;

import java.io.ByteArrayOutputStream;

import hu.bme.aut.myapplication.TokenClass;

public class NFCHelper {

    private NfcAdapter nfcAdapter;

    public NFCHelper(Context context) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context);
    }

    public NdefMessage createTextMessage(TokenClass.Token token) {
        try {
           NdefMessage message;
            NdefRecord record = null;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                record = NdefRecord.createExternal("com.example.temalabor","externaltype", token.toByteArray());
            }
            message = new NdefMessage(new NdefRecord[]{ record });
            return message;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public NfcAdapter getAdapter(){
        return nfcAdapter;
    }
}