package com.example.dani.nfcapp;

import android.content.Context;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import java.io.ByteArrayOutputStream;

public class NFCHelper {

    private NfcAdapter nfcAdapter;

    public NFCHelper(Context context) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context);
    }

    public NdefMessage createTextMessage(String content) {
        try {
            byte[] text = content.getBytes("UTF-8"); // Content in UTF-8
            int textLength = text.length;

            ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + textLength);
            payload.write(text, 0, textLength);
            NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());

            return new NdefMessage(new NdefRecord[]{record});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public NfcAdapter getAdapter(){
        return nfcAdapter;
    }
}