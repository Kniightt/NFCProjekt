package com.example.temalabor.nfcapp.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.nfc.NdefMessage;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.auth0.android.jwt.JWT;
import com.example.temalabor.nfcapp.R;
import com.example.temalabor.nfcapp.TokenClass;
import com.example.temalabor.nfcapp.data.TokenItem;
import com.example.temalabor.nfcapp.data.TokenList;
import com.example.temalabor.nfcapp.utility.NFCHelper;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TokenAdapter extends RecyclerView.Adapter<TokenAdapter.TokenViewHolder> {

    private final TokenList items;
    private TokenItem currentlySelected;
    private Context myContext;
    private NFCHelper nfcHelper;
    private boolean pushComplete = false;

    public TokenAdapter(TokenList _items, Context context, NFCHelper _nfcHelper) {
        items = _items;
        myContext = context;
        nfcHelper = _nfcHelper;
        nfcHelper.setTokenAdapter(this);
    }

    @NonNull
    @Override
    public TokenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_token, parent, false);
        return new TokenViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TokenViewHolder holder, int position) {
        TokenItem item = items.getTokens().get(position);

        Resources myResources = myContext.getResources();

        holder.textViews[0].setText(myResources.getString(R.string.tv_0, item.getTokenData().get(0)));
        holder.textViews[1].setText(myResources.getString(R.string.tv_1, item.getTokenData().get(1)));
        holder.textViews[2].setText(myResources.getString(R.string.tv_2, item.getTokenData().get(2)));
        holder.textViews[3].setText(myResources.getString(R.string.tv_3, item.getTokenData().get(3)));
        holder.textViews[4].setText(myResources.getString(R.string.tv_4, item.getTokenData().get(4)));
        holder.textViews[5].setText(myResources.getString(R.string.tv_5, item.getTokenData().get(5)));


        holder.radioButton.setChecked(item.isSelected());

        if (item.isSelected())
            currentlySelected = item;

        holder.item = item;
    }

    public void addItem(String token){
        TokenItem newItem = new TokenItem(getTokenData(token), token);
        items.getTokens().add(newItem);
        notifyItemInserted(items.getTokens().size() - 1);
        serialize();
    }

    public void changeItem(String token){

        if (currentlySelected != null) {
            try {
                TokenItem newItem = new TokenItem(getTokenData(token), token);
                newItem.setSelected(true);

                int indexToChange = items.getTokens().indexOf(currentlySelected);
                items.getTokens().set(indexToChange, newItem);
                currentlySelected = newItem;
                notifyItemChanged(indexToChange);
                serialize();

                pushComplete = false;
                ndefPush(newItem);
            } catch (Exception e){
                Toast.makeText(myContext, token,
                        Toast.LENGTH_LONG).show();
                pushComplete = false;
            }
        }
    }

    private void deleteItem(TokenItem item){
        if (item.isSelected()) {
            pushComplete = false;
            nfcHelper.pushMessage(null);
            currentlySelected = null;
        }

        items.getTokens().remove(item);
        notifyDataSetChanged();
        serialize();
    }

    private void serialize(){
        File xmlFile = new File(myContext.getExternalFilesDir(null) + "/tokens.xml");
        Serializer serializer = new Persister();
        try {
            serializer.write(items, xmlFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> getTokenData(String token){

        List<String> tokenData = new ArrayList<>();
        JWT jwt = new JWT(token);
        tokenData.add(jwt.getClaim("id").asString());
        tokenData.add(jwt.getClaim("useremail").asString());
        tokenData.add(jwt.getClaim("sid").asString());
        tokenData.add(jwt.getClaim("count").asString());
        tokenData.add(getDate(jwt.getClaim("iat").asDate()));
        tokenData.add(getDate(jwt.getClaim("exp").asDate()));

        return tokenData;
    }

    private String getDate(Date date){
        if (date != null) {
            Integer year = date.getYear() + 1900;
            Integer month = date.getMonth() + 1;
            Integer day = date.getDate();
            return year.toString() + "." + month.toString() + "." + day.toString() + ".";
        }
        else
            return "Error.";
    }

    @Override
    public int getItemCount() {
        return items.getTokens().size();
    }

    class TokenViewHolder extends RecyclerView.ViewHolder {

        RadioButton radioButton;
        ImageButton deleteButton;
        TextView[] textViews;

        TokenItem item;

        TokenViewHolder(View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.radioButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);

            textViews = new TextView[6];
            textViews[0] = itemView.findViewById(R.id.tidView);
            textViews[1] = itemView.findViewById(R.id.uemView);
            textViews[2] = itemView.findViewById(R.id.sidView);
            textViews[3] = itemView.findViewById(R.id.countView);
            textViews[4] = itemView.findViewById(R.id.iatView);
            textViews[5] = itemView.findViewById(R.id.expView);

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteItem(item);
                }
            });

            radioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!pushComplete) {
                        if (currentlySelected != null) currentlySelected.setSelected(false);
                        item.setSelected(true);
                        currentlySelected = item;
                        ndefPush(item);
                        notifyDataSetChanged();
                    } else {
                        Toast.makeText(myContext, "Answer needed first.",
                                Toast.LENGTH_LONG).show();
                        notifyDataSetChanged();
                    }
                }
            });
        }
    }

    private void ndefPush(TokenItem item) {
        TokenClass.Token token;
        token = TokenClass.Token.newBuilder()
                .setToken(item.getToken())
                .build();
        NdefMessage message = nfcHelper.createTextMessage(token);
        nfcHelper.pushMessage(message);
    }

    public void setPushComplete(boolean pushComplete) {
        this.pushComplete = pushComplete;
    }
}
