package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ShareCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.google.common.base.Splitter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_ITEM_ID = "item_id";

    private static final String TAG = ArticleDetailFragment.class.getSimpleName();
    private static final String SAVED_LAYOUT_MANAGER = "saved_layout_manager";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private SimpleDateFormat serverDateFormat;

    private ImageView mPhotoView;
    private TextView titleView;
    private TextView bylineView;
    private RecyclerView bodyRecyclerView;
    private Parcelable layoutManagerSavedState;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
        serverDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);

        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String shareTitle = getString(R.string.default_share);
                if(titleView.getText() != null){
                    shareTitle = titleView.getText().toString();
                }

                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(shareTitle)
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();
        return mRootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        Parcelable state = bodyRecyclerView.getLayoutManager().onSaveInstanceState();
        outState.putParcelable(SAVED_LAYOUT_MANAGER, state);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            layoutManagerSavedState = savedInstanceState.getParcelable(SAVED_LAYOUT_MANAGER);
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        titleView = mRootView.findViewById(R.id.article_title);
        bylineView = mRootView.findViewById(R.id.article_byline);
        bodyRecyclerView = mRootView.findViewById(R.id.detail_recycler_view);
        ImageButton upButton = mRootView.findViewById(R.id.action_up);

        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
    }

    private void showData() {
        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            String titleText = mCursor.getString(ArticleLoader.Query.TITLE);
            if (titleView != null && titleText != null && titleText.length() > 0) {
                titleView.setText(titleText);

                //If title gets too long, shrink it a bit.
                if (titleText.length() > 20) {
                    titleView.setTextSize(22);
                }
            }

            String publishedDateString = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);

            Date publishedDateAsDate = new Date();
            try {
                publishedDateAsDate = serverDateFormat.parse(publishedDateString);
            } catch (ParseException e) {
                Log.e(TAG, "showData parsing date failed: ", e);
            }
            long publishedDate = publishedDateAsDate.getTime();

            String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
            if (bylineView != null && author != null) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate,
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + author
                                + "</font>"));
            }

            String contextText = mCursor.getString(ArticleLoader.Query.BODY);
            if (contextText != null && contextText.length() > 0) {

                //Mark all paragraphs with custom character
                String markParagraphs = contextText.replace("\r\n\r\n", "||");
                //replace all newlines
                String removedNewLines = markParagraphs.replace("\r\n", " ");
                //SPlit all paragraphs into a list of chinks
                List<String> items = Splitter.on("||").splitToList(removedNewLines);

                Log.d(TAG, "showData: " + items.size());

                //Make the chunks into an adapter. This is way more performant.
                AdapterBodyText adapterBodyText = new AdapterBodyText(items);

                bodyRecyclerView.setVisibility(View.VISIBLE);
                bodyRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                bodyRecyclerView.setAdapter(adapterBodyText);
                if (layoutManagerSavedState != null) {
                    bodyRecyclerView.getLayoutManager().onRestoreInstanceState(layoutManagerSavedState);
                }
            }
            
            String imageUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            if (imageUrl != null && imageUrl.length() > 0) {
                ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                        .get(imageUrl, new ImageLoader.ImageListener() {
                            @Override
                            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                                Bitmap bitmap = imageContainer.getBitmap();
                                if (bitmap != null) {
                                    mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                }
                            }

                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                                Log.e(TAG, "onErrorResponse: ", volleyError);
                            }
                        });
            }
        } else {
            Log.d(TAG, "Cursor null");
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A");
            bodyRecyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        showData();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
    }
}