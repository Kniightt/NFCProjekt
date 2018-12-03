package com.example.temalabor.nfcapp.data;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root
public class TokenItem {

    @ElementList
    private List<String> tokenData;
    @Element
    private String token;
    @Element
    private boolean selected;

    public TokenItem(){
        tokenData = new ArrayList<>();
    }

    public TokenItem(List<String> _tokenData, String _token){
        tokenData = _tokenData;
        token = _token;
        selected = false;
    }

    public List<String> getTokenData() {
        return tokenData;
    }

    public String getToken() {
        return token;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
