package com.example.ashishrmehta.searchapp;

import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class WikiSearchAdapter extends BaseAdapter {
	private ArrayList<String> mWikiImageList = new ArrayList<String>();
	private LayoutInflater mInflator;
	private int mWidth;
	private int refresh = 0;

	public WikiSearchAdapter(Context ctx, int width) {
		mInflator = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mWidth = width;
	}

	public void setData(ArrayList<String> wikiImageList) {
		mWikiImageList = wikiImageList;
	}

	@Override
	public int getCount() {
		return mWikiImageList.size();
	}

	@Override
	public String getItem(int position) {
		return mWikiImageList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		String data=getItem(position);

		if (convertView == null) {
			refresh = 1;
			convertView = mInflator.inflate(R.layout.grid_item_wiki_image_list, null);
			holder = new ViewHolder();
			holder.img = (ImageView) convertView.findViewById(R.id.img);
			convertView.setLayoutParams(new GridView.LayoutParams(mWidth, mWidth));
			convertView.setTag(holder);
		}
		else {
			holder = (ViewHolder) convertView.getTag();
		}

		if(refresh == 0) {
			return convertView;
		}
		try{
			refresh = 0;
			LoadAsyncTask las = new LoadAsyncTask();
			las.execute(data, holder);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return convertView;
	}

	public class LoadAsyncTask extends AsyncTask<Object, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Object... objects) {
			String urlStr = (String) objects[0];
			ViewHolder holder= (ViewHolder) objects[1];
			Bitmap bmp = null;
			try{
				URL url = new URL(urlStr);
				bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
				holder.img.setImageBitmap(bmp);
			} catch(Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	private static class ViewHolder {
		public ImageView img;
	}
}
