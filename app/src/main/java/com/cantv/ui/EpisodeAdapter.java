package com.cantv.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.cantv.R;
import com.cantv.model.Episode;

import java.util.List;

/**
 * 集数适配器 - 大按钮风格
 */
public class EpisodeAdapter extends BaseAdapter {

    private Context context;
    private List<Episode> episodes;

    public EpisodeAdapter(Context context, List<Episode> episodes) {
        this.context = context;
        this.episodes = episodes;
    }

    @Override
    public int getCount() {
        return episodes.size();
    }

    @Override
    public Episode getItem(int position) {
        return episodes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_episode, parent, false);
            holder = new ViewHolder();
            holder.tvEpisode = convertView.findViewById(R.id.tv_episode);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Episode episode = episodes.get(position);
        holder.tvEpisode.setText(episode.getDisplayTitle());

        return convertView;
    }

    static class ViewHolder {
        TextView tvEpisode;
    }
}
