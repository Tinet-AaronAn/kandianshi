package com.cantv.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cantv.R;
import com.cantv.model.Series;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * 电视剧列表适配器 - 大卡片（纯 SDK，无外部依赖）
 */
public class SeriesAdapter extends BaseAdapter {

    private Context context;
    private List<Series> seriesList;

    public SeriesAdapter(Context context, List<Series> seriesList) {
        this.context = context;
        this.seriesList = seriesList;
    }

    @Override
    public int getCount() {
        return seriesList.size();
    }

    @Override
    public Series getItem(int position) {
        return seriesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_series, parent, false);
            holder = new ViewHolder();
            holder.ivCover = convertView.findViewById(R.id.iv_cover);
            holder.tvName = convertView.findViewById(R.id.tv_series_name);
            holder.tvCount = convertView.findViewById(R.id.tv_episode_count);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Series series = seriesList.get(position);
        holder.tvName.setText(series.getName());
        holder.tvCount.setText(series.getEpisodeCount() + "集");

        // 封面图 - 采样加载，防止大图 OOM
        if (series.getCoverPath() != null) {
            try {
                Bitmap bmp = decodeSampledBitmap(series.getCoverPath(), 300, 300);
                if (bmp != null) {
                    holder.ivCover.setImageBitmap(bmp);
                } else {
                    holder.ivCover.setImageResource(R.drawable.default_cover);
                }
            } catch (Exception e) {
                holder.ivCover.setImageResource(R.drawable.default_cover);
            }
        } else {
            holder.ivCover.setImageResource(R.drawable.default_cover);
        }

        return convertView;
    }

    /**
     * 采样加载 Bitmap，避免大图 OOM
     */
    private static Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight) {
        // 第一次：只读尺寸
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (FileInputStream fis = new FileInputStream(path)) {
            BitmapFactory.decodeStream(fis, null, options);
        } catch (IOException e) {
            return null;
        }

        // 计算 inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        // 第二次：采样加载
        try (FileInputStream fis = new FileInputStream(path)) {
            return BitmapFactory.decodeStream(fis, null, options);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 计算采样率
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    static class ViewHolder {
        ImageView ivCover;
        TextView tvName;
        TextView tvCount;
    }
}
