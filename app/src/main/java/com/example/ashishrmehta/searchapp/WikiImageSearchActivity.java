package com.example.ashishrmehta.searchapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class WikiImageSearchActivity extends ActionBarActivity {
	private final static int GRID_MARGIN = 10;
	private final static int GRID_HORIZONTALSPACING = 10;
	private final static int GRID_VERTICALSPACING = 10;
	private static final String WIKI_URL = "https://en.wikipedia.org/w/api.php?%20action=query&prop=pageimages&format=json&piprop=thumbnail&pithumbsize=50&%20pilimit=50&generator=prefixsearch&gpssearch=";
	private GroupMemberGridView gvImageList;
	protected CustomSearchView mSearchView;
	private WikiSearchAdapter mAdapter;
	private String mGroupName;
	private Uri mImageOutUri;
	private int offset = 0;
	private int mNumOfResults = 50;
	private boolean mShowLoadMore = false;
	private TextView txtLoadMore;
	private ArrayList<String> mWikiSearchResults;
	private TextView txtEmptyView;
	private View mFooter;
	private ProgressBar mProgressBar;
	private SearchAsyncTask mSearchTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getLayoutId());
		findViewById();
		initData(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	protected int getLayoutId() {

		return R.layout.activity_wiki_image_search;
	}

	protected void findViewById() {

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		int width = metrics.widthPixels;
		int margin = GRID_MARGIN;
		int horizontalSpacing = GRID_HORIZONTALSPACING;
		int verticalSpacing = GRID_VERTICALSPACING;
		int gridWidth = (width - margin * 2 - horizontalSpacing * 2) / 3;

		mSearchView = (CustomSearchView) findViewById(R.id.search_view);
		gvImageList = (GroupMemberGridView) findViewById(R.id.image_list_gridview);
		txtEmptyView = (TextView) findViewById(R.id.txtEmptyView);

		mFooter = getLayoutInflater().inflate(R.layout.layout_load_more, null);
		txtLoadMore = (TextView) mFooter.findViewById(R.id.txtLoadMore);
		mProgressBar = (ProgressBar) mFooter.findViewById(R.id.progressBar);

		txtEmptyView.setGravity(Gravity.CENTER);
		gvImageList.setEmptyView(txtEmptyView);

		gvImageList.setHorizontalSpacing(horizontalSpacing);
		gvImageList.setVerticalSpacing(verticalSpacing);
		gvImageList.setPadding(margin, margin, margin, margin);
		gvImageList.addFooterView(mFooter, null, false);

		mAdapter = new WikiSearchAdapter(this, gridWidth);

		txtLoadMore.setText(R.string.more);
		txtLoadMore.setVisibility(View.GONE);
		mProgressBar.setVisibility(View.INVISIBLE);

		gvImageList.setAdapter(mAdapter);

	}

	public void prepareForWikiSearch(){
		mSearchView.makeSearchUIForWiki();
	}

	protected void initData(Bundle savedInstanceState) {

		mWikiSearchResults = new ArrayList<String>();

		gvImageList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (isConnected() && mAdapter.getCount() > position) {
				}
				else {
					Toast.makeText(WikiImageSearchActivity.this, R.string.internet_unavailable, Toast.LENGTH_SHORT);
				}
			}
		});

		mSearchView.getSearchHintButton().setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mWikiSearchResults.clear();
				mAdapter.setData(mWikiSearchResults);
				mAdapter.notifyDataSetChanged();
				mSearchTask = new SearchAsyncTask(mSearchView.getQueryTextView().getText().toString());
				mSearchTask.execute();
			}
		});

		txtLoadMore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean flag = isConnected();
				if (!flag) {
					Toast.makeText(WikiImageSearchActivity.this, R.string.internet_unavailable, Toast.LENGTH_SHORT);
					return;

				}
				mSearchTask = new SearchAsyncTask(mSearchView.getQueryTextView().getText().toString());
				mSearchTask.execute();

			}
		});

		mSearchView.getQueryTextView().setText(mGroupName);
		mSearchView.getQueryTextView().setSelection(mSearchView.getQueryTextView().getText().length());
		if (mGroupName != null && mGroupName.length() > 0 && isConnected()) {
			mSearchTask = new SearchAsyncTask(mGroupName);
			mSearchTask.execute();
		}
		else if (!isConnected()) {
			Toast.makeText(WikiImageSearchActivity.this, R.string.internet_unavailable, Toast.LENGTH_SHORT);
			if (mGroupName != null && mGroupName.length() > 0) {
				txtEmptyView.setText(String.format(getResources().getString(R.string.chat_search_noresults), mGroupName));
			}
		}
	}

	public class SearchAsyncTask extends AsyncTask<Void, Void, Boolean> {
		private String mSearchStr;
		private boolean adult = false;
		private ProgressDialog dialog;

		public SearchAsyncTask(String searchStr) {
			mSearchStr = searchStr;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			if (txtLoadMore.getVisibility() == View.GONE) {
				dialog = new ProgressDialog(WikiImageSearchActivity.this);
				dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog.setCancelable(false);
				dialog.show();
				dialog.setContentView(R.layout.layout_progress_bar);
			}
			else {
				mProgressBar.setVisibility(View.VISIBLE);
				txtLoadMore.setVisibility(View.GONE);
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				String searchStr = URLEncoder.encode(mSearchStr, "UTF-8");
				byte[] utf8Bytes = searchStr.getBytes("UTF-8");
				searchStr = new String(utf8Bytes, "UTF8");

				String wikiUrl;
				wikiUrl = WIKI_URL +searchStr;

				URL url1 = null;
				url1 = new URL(wikiUrl);

				URLConnection urlConnection1 = url1.openConnection();
				InputStream response1 = urlConnection1.getInputStream();
				String res1 = readStream(response1);

				JsonElement jsonElement = new JsonParser().parse(res1);
				JsonElement pages = jsonElement.getAsJsonObject().get("query").getAsJsonObject().get("pages");

				Set<Map.Entry<String, JsonElement>> entrySet = pages.getAsJsonObject().entrySet();

				JsonElement yourDesiredElement = null;
				String value = null;
				for(Map.Entry<String,JsonElement> entry : entrySet){
					yourDesiredElement = entry.getValue();
					if(yourDesiredElement.getAsJsonObject().get("thumbnail") != null) {
						value = yourDesiredElement.getAsJsonObject().get("thumbnail").getAsJsonObject().get("source").getAsString();
						mWikiSearchResults.add(value);
					}
				}

				if (mWikiSearchResults.size() == mNumOfResults) {
					offset = offset + mNumOfResults;
					mShowLoadMore = true;
				}
				else {
					mShowLoadMore = false;
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				return false;
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (dialog != null)
				dialog.dismiss();

			if (!result) {
				Toast.makeText(WikiImageSearchActivity.this, R.string.scan_loadfail, Toast.LENGTH_SHORT);
				return;
			}
			mAdapter.setData(mWikiSearchResults);
			mAdapter.notifyDataSetChanged();

			if (mShowLoadMore) {
				mFooter.setVisibility(View.VISIBLE);
				txtLoadMore.setVisibility(View.VISIBLE);
				mProgressBar.setVisibility(View.INVISIBLE);
			}
			else {
				mFooter.setVisibility(View.GONE);
				txtLoadMore.setVisibility(View.GONE);
				mProgressBar.setVisibility(View.INVISIBLE);
			}

			if (mWikiSearchResults.size() == 0) {
				txtEmptyView.setText(String.format(getResources().getString(R.string.chat_search_noresults), mSearchStr));
			}

		}

		private String readStream(InputStream in) {
			BufferedReader reader = null;
			StringBuilder sb = new StringBuilder();
			try {
				reader = new BufferedReader(new InputStreamReader(in));
				String line = "";
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			finally {
				if (reader != null) {
					try {
						reader.close();
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return sb.toString();

		}
	}

	private boolean isConnected() {
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mSearchTask != null && !mSearchTask.isCancelled()) {
			mSearchTask.cancel(true);
		}
	}
}
