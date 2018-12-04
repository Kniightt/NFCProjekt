package com.example.temalabor.nfcapp.data;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root
public class TokenList {

    @ElementList
    private List<TokenItem> tokens;

    @Element
    private boolean answerNeeded;

    public TokenList(){
        tokens = new ArrayList<>();
    }

    public List<TokenItem> getTokens() {
        return tokens;
    }

    public boolean isAnswerNeeded() {
        return answerNeeded;
    }

    public void setAnswerNeeded(boolean answerNeeded) {
        this.answerNeeded = answerNeeded;
    }

    public TokenItem searchSelectedToken() {
        for (int i = 0; i < tokens.size(); i++)
            if (tokens.get(i).isSelected())
                return tokens.get(i);
        return null;
    }
}
