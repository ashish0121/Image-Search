package com.example.ashishrmehta.searchapp;

import android.app.SearchableInfo;
import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class CustomSearchView extends LinearLayout {

	private OnQueryTextListener mOnQueryChangeListener;
	private OnCloseListener mOnCloseListener;
	private OnFocusChangeListener mOnQueryTextFocusChangeListener;
	private OnClickListener mOnSearchClickListener;

	private boolean mIconifiedByDefault;
	private boolean mIconified;
	private View mSearchButton;
	private ImageView mCloseButton;
	private View mSearchEditFrame;
	private EditText mQueryTextView;
	private ImageView mSearchHintIcon;
	private CharSequence mQueryHint;
	private boolean mQueryRefinement;
	private boolean mClearingFocus;
	private int mMaxWidth;
	private CharSequence mOldQueryText;

	private SearchableInfo mSearchable;
	
	public void setTextColor(int resColorId) {
		mQueryTextView.setTextColor(resColorId);
	}
	
	public void setHintTextColor(int resColorId) {
		mQueryTextView.setHintTextColor(resColorId);
	}

	private Runnable mShowImeRunnable = new Runnable() {
		public void run() {
			InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

			if (imm != null) {
				imm.showSoftInput(mQueryTextView, 0);
			}
		}
	};

	private Runnable mUpdateDrawableStateRunnable = new Runnable() {
		public void run() {
			updateFocusedState();
		}
	};

	public interface OnQueryTextListener {

		boolean onQueryTextChange(String newText);
	}

	public interface OnCloseListener {

		boolean onClose();
	}

	public CustomSearchView(Context context) {
		this(context, null);
	}

	public CustomSearchView(Context context, AttributeSet attrs) {
		super(context, attrs);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.layout_custom_search_view, this, true);

		mSearchButton = findViewById(R.id.search_button);
		mQueryTextView = (EditText) findViewById(R.id.search_src_text);

		mSearchEditFrame = findViewById(R.id.search_edit_frame);
		mCloseButton = (ImageView) findViewById(R.id.search_close_btn);
		mSearchHintIcon = (ImageView) findViewById(R.id.search_mag_icon);

		mSearchButton.setOnClickListener(mOnClickListener);
		mCloseButton.setOnClickListener(mOnClickListener);
		mQueryTextView.setOnClickListener(mOnClickListener);

		mQueryTextView.addTextChangedListener(mTextWatcher);
		mQueryTextView.setOnFocusChangeListener(new OnFocusChangeListener() {

			public void onFocusChange(View v, boolean hasFocus) {
				if (mOnQueryTextFocusChangeListener != null) {
					mOnQueryTextFocusChangeListener.onFocusChange(CustomSearchView.this, hasFocus);
				}
			}
		});

		setQueryHint("input something");

		boolean focusable = true;
		setFocusable(focusable);

		updateViewsVisibility(mIconifiedByDefault);
		updateQueryHint();
	}
	
	@Override
	public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
		if (mClearingFocus)
			return false;
		if (!isFocusable())
			return false;
		if (!isIconified()) {
			boolean result = mQueryTextView.requestFocus(direction, previouslyFocusedRect);
			if (result) {
				updateViewsVisibility(false);
			}
			return result;
		}
		else {
			return super.requestFocus(direction, previouslyFocusedRect);
		}
	}

	@Override
	public void clearFocus() {
		mClearingFocus = true;
		setImeVisibility(false);
		super.clearFocus();
		mQueryTextView.clearFocus();
		mClearingFocus = false;
	}

	public void setQueryHint(CharSequence hint) {
		mQueryHint = hint;
		updateQueryHint();
	}

	public boolean isIconified() {
		return mIconified;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (isIconified()) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			return;
		}

		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int width = MeasureSpec.getSize(widthMeasureSpec);

		switch (widthMode) {
			case MeasureSpec.AT_MOST:
				if (mMaxWidth > 0) {
					width = Math.min(mMaxWidth, width);
				}
				else {
					width = Math.min(getPreferredWidth(), width);
				}
				break;
			case MeasureSpec.EXACTLY:
				if (mMaxWidth > 0) {
					width = Math.min(mMaxWidth, width);
				}
				break;
			case MeasureSpec.UNSPECIFIED:
				width = mMaxWidth > 0 ? mMaxWidth : getPreferredWidth();
				break;
		}
		widthMode = MeasureSpec.EXACTLY;
		super.onMeasure(MeasureSpec.makeMeasureSpec(width, widthMode), heightMeasureSpec);
	}

	private int getPreferredWidth() {
		return 300;
	}

	private void updateViewsVisibility(final boolean collapsed) {
		mIconified = collapsed;
		final int visCollapsed = collapsed ? VISIBLE : GONE;

		mSearchButton.setVisibility(visCollapsed);
		mSearchEditFrame.setVisibility(collapsed ? GONE : VISIBLE);
		mSearchHintIcon.setVisibility(mIconifiedByDefault ? GONE : VISIBLE);
		updateCloseButton();
	}

	private void updateCloseButton() {
		final boolean hasText = !TextUtils.isEmpty(mQueryTextView.getText());
		final boolean showClose = hasText;
		mCloseButton.setVisibility(showClose ? VISIBLE : GONE);
		mCloseButton.getDrawable().setState(hasText ? ENABLED_STATE_SET : EMPTY_STATE_SET);
	}

	private void postUpdateFocusedState() {
		post(mUpdateDrawableStateRunnable);
	}

	private void updateFocusedState() {
		invalidate();
	}

	@Override
	protected void onDetachedFromWindow() {
		removeCallbacks(mUpdateDrawableStateRunnable);
		super.onDetachedFromWindow();
	}

	private void setImeVisibility(final boolean visible) {
		if (visible) {
			post(mShowImeRunnable);
		}
		else {
			removeCallbacks(mShowImeRunnable);
			InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

			if (imm != null) {
				imm.hideSoftInputFromWindow(getWindowToken(), 0);
			}
		}
	}

	private final OnClickListener mOnClickListener = new OnClickListener() {

		public void onClick(View v) {
			if (v == mSearchButton) {
				onSearchClicked();
			}
			else if (v == mCloseButton) {
				onCloseClicked();
			}
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mSearchable == null) {
			return false;
		}

		return super.onKeyDown(keyCode, event);
	}

	private CharSequence getDecoratedHint(CharSequence hintText) {
		if (!mIconifiedByDefault)
			return hintText;

		return hintText;
	}

	private void updateQueryHint() {
		if (mQueryHint != null) {
			mQueryTextView.setHint(getDecoratedHint(mQueryHint));
		}
		else if (mSearchable != null) {
			CharSequence hint = null;
			int hintId = mSearchable.getHintId();
			if (hintId != 0) {
				hint = getContext().getString(hintId);
			}
			if (hint != null) {
				mQueryTextView.setHint(getDecoratedHint(hint));
			}
		}
		else {
			mQueryTextView.setHint(getDecoratedHint(""));
		}
	}

	private void onTextChanged(CharSequence newText) {
		updateCloseButton();
		if (mOnQueryChangeListener != null && !TextUtils.equals(newText, mOldQueryText)) {
			mOnQueryChangeListener.onQueryTextChange(newText.toString());
		}
		mOldQueryText = newText.toString();
	}

	private void onCloseClicked() {
		CharSequence text = mQueryTextView.getText();
		if (TextUtils.isEmpty(text)) {
			if (mIconifiedByDefault) {
				if (mOnCloseListener == null || !mOnCloseListener.onClose()) {
					clearFocus();
					updateViewsVisibility(true);
				}
			}
		}
		else {
			mQueryTextView.setText("");
			mQueryTextView.requestFocus();
			setImeVisibility(true);
		}

	}

	private void onSearchClicked() {
		updateViewsVisibility(false);
		mQueryTextView.requestFocus();
		setImeVisibility(true);
		if (mOnSearchClickListener != null) {
			mOnSearchClickListener.onClick(this);
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);

		postUpdateFocusedState();
	}

	private TextWatcher mTextWatcher = new TextWatcher() {

		public void beforeTextChanged(CharSequence s, int start, int before, int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before, int after) {
			CustomSearchView.this.onTextChanged(s);
		}

		public void afterTextChanged(Editable s) {
		}
	};
	
	public EditText getQueryTextView() {
		return mQueryTextView;
	}
	
	public void makeSearchUIForWiki(){
		int padding = 12;
		mSearchButton.setPadding(padding, padding, padding, padding);
		mSearchButton.setVisibility(View.GONE);
		setQueryHint(getResources().getString(R.string.general_search));
		mSearchHintIcon.setVisibility(View.VISIBLE);
		int paddingSearchFrame =5;
		mSearchEditFrame.setPadding(paddingSearchFrame, paddingSearchFrame, paddingSearchFrame, paddingSearchFrame);
		((LayoutParams) mSearchEditFrame.getLayoutParams()).setMargins(0, 0, 0, 0);
		mSearchEditFrame.setBackgroundColor(getResources().getColor(R.color.green));
		mCloseButton.setPressed(false);
		invalidate();
	}

	public ImageView getSearchHintButton() {
		return (ImageView) mSearchHintIcon;
	}
}
