package com.example.temalabor.nfcapp.data;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root
public class TokenList {

    @ElementList
    private List<TokenItem> tokens;

    public TokenList(){
        tokens = new ArrayList<>();
    }

    public List<TokenItem> getTokens() {
        return tokens;
    }
}
