package com.example.temalabor.nfcapp.utility;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Parcelable;

import com.example.temalabor.nfcapp.TokenClass;
import com.example.temalabor.nfcapp.adapter.TokenAdapter;
import com.google.protobuf.InvalidProtocolBufferException;

public class NFCHelper implements NfcAdapter.OnNdefPushCompleteCallback {

    private NfcAdapter nfcAdapter;
    private Activity activity;
    private TokenAdapter tokenAdapter;

    public NFCHelper(Context context, Activity _activity) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        activity = _activity;
        tokenAdapter = null;
    }

    public NdefMessage createTextMessage(TokenClass.Token token) {
            NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], token.toByteArray());
            return new NdefMessage(new NdefRecord[]{record});
    }

    public void pushMessage(NdefMessage message){
        nfcAdapter.setNdefPushMessage(message, activity);
        nfcAdapter.setOnNdefPushCompleteCallback(this, activity);
    }

    public void receiveMessage(Intent intent){
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] receivedArray = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (receivedArray != null) {
                NdefMessage message = (NdefMessage) receivedArray[0];
                NdefRecord[] records = message.getRecords();
                try {
                    TokenClass.Token token = TokenClass.Token.parseFrom(records[0].getPayload());
                    tokenAdapter.changeItem(token.getToken());
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onNdefPushComplete(NfcEvent nfcEvent) {
        tokenAdapter.getItems().setAnswerNeeded(true);
        tokenAdapter.serialize();
        pushMessage(null);
    }

    public NfcAdapter getAdapter(){
        return nfcAdapter;
    }

    public void setTokenAdapter(TokenAdapter tokenAdapter) {
        this.tokenAdapter = tokenAdapter;
    }
}