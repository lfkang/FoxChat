package com.wangyeming.custom;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wangyeming.foxchat.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Wang on 2014/11/9.
 * 基础适配器
 */
public class MyAdapter extends BaseAdapter {

    private List<Map<String, String>> data = new ArrayList<Map<String, String>>();
    private LayoutInflater mInflater = null;

    public MyAdapter(List<Map<String, String>> data, Context context){
        this.data = data;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return data == null ? 0: data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.list_item1,
                    null);
            //holder.image = (ImageView) convertView.findViewById(R.id.image);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        //一般按如下方式将数据与UI联系起来
        //holder.image.setImageResource(mData.get(position).getmIcon());
        holder.name.setText(data.get(position).get("name"));
        return convertView;
    }

    class ViewHolder {
        //public ImageView image;
        public TextView name;
    }
}
