package com.amanda.infinitescrollinglistview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    final String TAG = "infinite_listview";

    RequestQueue mRequestQueue;
    JSONArray mJsonRequest = new JSONArray();

    static int NUM_DEFAULT_VALUE = 20;
    static String NUM_FETCH_VALUE = "10";

    JsonArrayRequest mJsonArrayRequest;
    final String mRequestURL = "https://hook.io/syshen/infinite-list/";
    String mContentType = "application/json";
    ResponseJSONUtils mResponseJSONUtils = new ResponseJSONUtils();

    ProgressDialog mProgressDialog;
    ResponseJSONCodeAdapter mAdapter = null;

    int mCurrentCount = 0;
    int mRemoveIndex = 0;
    boolean isLoadNew = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        if (networkInfo != null) {
            mRequestQueue = Volley.newRequestQueue(this);
            initViewAndAdapter();
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setMessage("Please wait...");
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            startQuery(0);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("No network connection...");
            builder.setTitle("Warning");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    MainActivity.this.finish();
                }
            });
            builder.create().show();
        }
    }

    private void startQuery(int startIndex) {
        startQuery(startIndex, String.valueOf(NUM_DEFAULT_VALUE));
    }

    private void startQuery(int startIndex, String number) {
        String requestURL = mRequestURL + "?startIndex=" + String.valueOf(startIndex) + "&num=" + number;
        mJsonArrayRequest = new JsonArrayRequest(Request.Method.POST, requestURL, mJsonRequest, mJSONResponseListener, mErrorListener) {
            public String getBodyContentType() {
                return mContentType;
            }
        };
        Log.v(TAG, "mJsonArrayRequest: " + mJsonArrayRequest);
        mRequestQueue.add(mJsonArrayRequest);
    }

    Response.Listener<JSONArray> mJSONResponseListener = new Response.Listener<JSONArray>() {
        @Override
        public void onResponse(final JSONArray response) {
            if (response != null) {
//                Log.v(TAG, "JSONArray: " + response.toString());
                try {
                    parseJSONResponseAndSetupData(response);
                    refreshView();
                } catch (JSONException e) {
                    Log.w(TAG, "parseJSONResponse has JSONException: " + e);
                } finally {
                    dismissProgressDialog();
                }
            }
        }
    };

    Response.ErrorListener mErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.v(TAG, "onErrorResponse: " + error.getMessage());
            dismissProgressDialog();
        }
    };

    private void initViewAndAdapter() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        mAdapter = new ResponseJSONCodeAdapter(this, recyclerView);

        recyclerView.setAdapter(mAdapter);
        mAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isLoadNew = true;
                        // add progress item
//                        if (mResponseJSONUtils.getItemList() != null) {
//                            mResponseJSONUtils.getItemList().put(mCurrentCount, null);
//                            mCurrentCount++;
//                            mAdapter.notifyItemInserted(mCurrentCount - 1);
//                        }
                        loadMoreData(mCurrentCount - 1);
                        mAdapter.setLoaded();
                    }
                }, 3000);
            }
        });
        mAdapter.OnLoadLegacyListener(new OnLoadLegacyListener() {
            @Override
            public void onLoadLegacy() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isLoadNew = false;
                        int index = mRemoveIndex - Integer.parseInt(NUM_FETCH_VALUE) - Integer.parseInt(NUM_FETCH_VALUE) + 1;
                        if (index < 0) {
                            index = 0;
                        }
                        loadMoreData(index);
                        mAdapter.setLoaded();
                    }
                }, 3000);
            }
        });
    }

    // Append more data into the adapter
    public void loadMoreData(int startIndex) {
        startQuery(startIndex, NUM_FETCH_VALUE);
    }

    private void removeLegacyData(int index) {
        Log.v(TAG, "removeLegacyData index: " + index);
        mRemoveIndex = index;
        int count = 0;
        for (int i = index - Integer.parseInt(NUM_FETCH_VALUE); i >= 0; i--) {
            mResponseJSONUtils.removeItemFromList(i);
            count++;
        }
        Log.v(TAG, "removeLegacyData size: " + mResponseJSONUtils.getItemListSize());
    }

    private void removeNewData(int index) {
        Log.v(TAG, "removeNewData index: " + index);
        for (int i = index; i >= mRemoveIndex; i--) {
            mResponseJSONUtils.removeItemFromList(i);
            mCurrentCount = i;
        }
        Log.v(TAG, "removeNewData size: " + mResponseJSONUtils.getItemListSize());
    }

    private void parseJSONResponseAndSetupData(JSONArray response) throws JSONException {
//        //  remove progress item
//        if (mCurrentCount > NUM_DEFAULT_VALUE && mResponseJSONUtils.removeItemFromList(mCurrentCount - 1)) {
//            mCurrentCount--;
//            mAdapter.notifyItemRemoved(mCurrentCount);
//        }
        int count = response.length();
        String id = String.valueOf(mAdapter.getItemCount());
        for (int i = 0; i < count; i++) {
            JSONObject resultObject = response.getJSONObject(i);
            id = resultObject.getString(ResponseJSONUtils.JSON_KEY_ID);
            String created = resultObject.getString(ResponseJSONUtils.JSON_KEY_CREATED);
            JSONObject sourceObject = resultObject.getJSONObject(ResponseJSONUtils.JSON_KEY_SOURCE);
            String sender = sourceObject.getString(ResponseJSONUtils.JSON_KEY_SENDER);
            String note = sourceObject.getString(ResponseJSONUtils.JSON_KEY_NOTE);
            JSONObject destinationObject = resultObject.getJSONObject(ResponseJSONUtils.JSON_KEY_DESTINATION);
            String recipient = destinationObject.getString(ResponseJSONUtils.JSON_KEY_RECIPIENT);
            String amount = destinationObject.getString(ResponseJSONUtils.JSON_KEY_AMOUNT);
            String currency = destinationObject.getString(ResponseJSONUtils.JSON_KEY_CURRENCY);
//            Log.v(TAG, "[parseJSONResponse] id:" + id + ", createdTime: " + created + "" +
//                    ", sender: " + sender + ", note: " + note +
//                    ", recipient: " + recipient + ", amount: " + amount + ", currency: " + currency);
            mResponseJSONUtils.newItem(Integer.parseInt(id), id, created, sender, note, recipient, amount, currency);
            if (mCurrentCount <= Integer.parseInt(id)) {
                mCurrentCount = Integer.parseInt(id) + 1;
            }
        }
    }

    private void refreshView() {
        if (isLoadNew) {
            if (mAdapter != null) {
                mAdapter.notifyItemRangeInserted(mAdapter.lastVisibleItem + 1, Integer.parseInt(NUM_FETCH_VALUE));
            }
            removeLegacyData(mAdapter.lastVisibleItem);
        } else {
            if (mAdapter != null) {
                int index = mRemoveIndex - Integer.parseInt(NUM_FETCH_VALUE) - Integer.parseInt(NUM_FETCH_VALUE) + 1;
                if (index < 0) {
                    index = 0;
                }
                mAdapter.notifyItemRangeChanged(index, Integer.parseInt(NUM_FETCH_VALUE));
            }
            removeNewData(mAdapter.lastVisibleItem + Integer.parseInt(NUM_FETCH_VALUE));
        }
    }

    private class ResponseJSONCodeAdapter extends RecyclerView.Adapter {

        LayoutInflater inflater;
        private final int VIEW_ITEM = 0;
        private final int VIEW_PROG = 1;

        private int firstVisibleItem, lastVisibleItem, totalItemCount;
        private boolean loading;
        private OnLoadMoreListener onLoadMoreListener;
        private OnLoadLegacyListener onLoadLegacyListener;

        public ResponseJSONCodeAdapter(Context context, RecyclerView recyclerView) {
            inflater = LayoutInflater.from(context);

            if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        totalItemCount = linearLayoutManager.getItemCount();
                        lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                        firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition();
                        Log.v(TAG, "totalItemCount: " + totalItemCount + ", lastVisibleItem: " + lastVisibleItem);
                        Log.v(TAG, "firstVisibleItem: " + firstVisibleItem + ", mRemoveIndex: " + mRemoveIndex);
                        if (!loading && lastVisibleItem + 1 >= totalItemCount) {//totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                            if (onLoadMoreListener != null) {
                                onLoadMoreListener.onLoadMore();
                            }
                            loading = true;
                        } else if (!loading && firstVisibleItem > 0 && firstVisibleItem <= mRemoveIndex - Integer.parseInt(NUM_FETCH_VALUE) + 1) {
                            if (onLoadLegacyListener != null) {
                                onLoadLegacyListener.onLoadLegacy();
                            }
                            loading = true;
                        }
                    }
                });
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            RecyclerView.ViewHolder viewHolder;
            if (viewType == VIEW_ITEM) {
                View itemLayoutView = inflater.inflate(R.layout.item_layout, viewGroup, false);
                viewHolder = new MyViewHolder(itemLayoutView);
            } else {
                View itemLayoutView = inflater.inflate(R.layout.progressbar_item, viewGroup, false);
                viewHolder = new ProgressViewHolder(itemLayoutView);
            }
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            Log.v(TAG, "viewHolder: " + viewHolder);
            if (viewHolder != null && viewHolder instanceof MyViewHolder) {
                ResponseJSONUtils.ItemInfo itemInfo = null;
                Log.v(TAG, "position: " + position + ", getItemCount: " + getItemCount() + ", size: " + mResponseJSONUtils.getItemListSize());
                if (mResponseJSONUtils != null && position < getItemCount()) {
                    itemInfo = mResponseJSONUtils.getItemFromList(position);
                }
                if (itemInfo != null) {
                    if (((MyViewHolder) viewHolder).idTextView != null) {
                        ((MyViewHolder) viewHolder).idTextView.setText(itemInfo.id);
                    }
                    if (((MyViewHolder) viewHolder).createdTextView != null) {
                        ((MyViewHolder) viewHolder).createdTextView.setText(itemInfo.created);
                    }
                    if (((MyViewHolder) viewHolder).senderTextView != null) {
                        ((MyViewHolder) viewHolder).senderTextView.setText(itemInfo.sender);
                    }
                    if (((MyViewHolder) viewHolder).noteTextView != null) {
                        ((MyViewHolder) viewHolder).noteTextView.setText(itemInfo.note);
                    }
                    if (((MyViewHolder) viewHolder).recipientTextView != null) {
                        ((MyViewHolder) viewHolder).recipientTextView.setText(itemInfo.recipient);
                    }
                    if (((MyViewHolder) viewHolder).amountTextView != null) {
                        ((MyViewHolder) viewHolder).amountTextView.setText(itemInfo.amount);
                    }
                    if (((MyViewHolder) viewHolder).currencyTextView != null) {
                        ((MyViewHolder) viewHolder).currencyTextView.setText(itemInfo.currency);
                    }
                }
            } else if (viewHolder != null && viewHolder instanceof ProgressViewHolder) {
                ((ProgressViewHolder) viewHolder).progressItem.setIndeterminate(true);
            }
        }

        @Override
        public int getItemViewType(int position) {
            int type = mResponseJSONUtils.getItemFromList(position) != null ? VIEW_ITEM : VIEW_PROG;
            Log.v(TAG, "position: " + position + ", type: " + type);
            return type;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return mCurrentCount;
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView idTextView;
            TextView createdTextView;
            TextView senderTextView;
            TextView noteTextView;
            TextView recipientTextView;
            TextView amountTextView;
            TextView currencyTextView;

            public MyViewHolder(View itemView) {
                super(itemView);
                idTextView = (TextView) itemView.findViewById(R.id.id);
                createdTextView = (TextView) itemView.findViewById(R.id.created);
                senderTextView = (TextView) itemView.findViewById(R.id.sender);
                noteTextView = (TextView) itemView.findViewById(R.id.note);
                recipientTextView = (TextView) itemView.findViewById(R.id.recipient);
                amountTextView = (TextView) itemView.findViewById(R.id.amount);
                currencyTextView = (TextView) itemView.findViewById(R.id.currency);
            }
        }

        class ProgressViewHolder extends RecyclerView.ViewHolder {
            ProgressBar progressItem;

            public ProgressViewHolder(View itemView) {
                super(itemView);
                progressItem = (ProgressBar) itemView.findViewById(R.id.progressItem);
            }
        }

        public void setLoaded() {
            loading = false;
        }

        public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
            this.onLoadMoreListener = onLoadMoreListener;
        }

        public void OnLoadLegacyListener(OnLoadLegacyListener onLoadLegacyListener) {
            this.onLoadLegacyListener = onLoadLegacyListener;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(mJsonArrayRequest);
        }
        dismissProgressDialog();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}