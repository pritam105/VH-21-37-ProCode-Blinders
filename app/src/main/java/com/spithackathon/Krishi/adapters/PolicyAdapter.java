package com.spithackathon.Krishi.adapters;


import android.content.Context;
import android.content.Intent;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.spithackathon.Krishi.R;
import com.spithackathon.Krishi.activities.WebActivity;

import java.util.ArrayList;


public class PolicyAdapter extends RecyclerView.Adapter<PolicyAdapter.viewHolder> {
    private String[] links;
    private String[] colors;
    private ArrayList<String> list=new ArrayList<>();
    private Context context;

    public PolicyAdapter(Context context,ArrayList<String> list,String[] links, String[] colors) {
        this.links=links;
        this.context = context;
        this.list=list;
        this.colors=colors;
    }

    public  void  refresh(ArrayList<String> list){
        this.list=list;
        notifyItemRangeChanged(0,list.size());
    }
    @Override
    public viewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category,parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(viewHolder holder, final int position) {
        holder.textView.setText(list.get(position));
        holder.itemView.setBackgroundColor(Color.parseColor(colors[position]));


        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browser =new Intent(context, WebActivity.class);
                browser.putExtra("link",links[position]);
                context.startActivity(browser);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class viewHolder extends RecyclerView.ViewHolder{
        TextView textView;
        CardView card;
        public viewHolder(View itemView) {
            super(itemView);
            textView= (TextView) itemView.findViewById(R.id.title);
            card= (CardView) itemView.findViewById(R.id.card);
        }
    }
}
