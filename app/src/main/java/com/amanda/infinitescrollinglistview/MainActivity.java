package com.amanda.infinitescrollinglistview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    final String TAG = "MainActivity+Amanda";

    int START_INDEX_DEFAULT_VALUE = 0;
    String NUM_DEFAULT_VALUE = "10";

    RequestQueue mRequestQueue;
    JsonArrayRequest mJsonArrayRequest;
    final String mRequestURL = "https://hook.io/syshen/infinite-list/";
    String mContentType = "application/json";

    ResponseJSONUtils mResponseJSONUtils = new ResponseJSONUtils();
    ProgressDialog mProgressDialog;

    ResponseJSONCodeAdapter mAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        if (networkInfo != null) {
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setMessage("Please wait...");
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            startQuery(START_INDEX_DEFAULT_VALUE);
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
        mRequestQueue = Volley.newRequestQueue(this);
        JSONArray jsonRequest = new JSONArray();

        ////////
        if (mResponseJSONUtils != null && mResponseJSONUtils.getItemList() != null) {
            startIndex = mResponseJSONUtils.getItemList().size();
        }
        String requestURL = mRequestURL + "?startIndex=" + String.valueOf(startIndex) + "&num=" + NUM_DEFAULT_VALUE;
        ////////

        mJsonArrayRequest = new JsonArrayRequest(Request.Method.POST, requestURL, jsonRequest, mJSONResponseListener, mErrorListener) {
            //TODO: why doesn't work??
//            @Override
//            protected Map<String, String> getParams() {
//                Map<String, String> params = new HashMap<String, String>();
//                params.put("startIndex", String.valueOf(startIndex));
//                params.put("num", NUM_DEFAULT_VALUE);
//                return params;
//            }

            public String getBodyContentType() {
                return mContentType;
            }
        };

        mRequestQueue.add(mJsonArrayRequest);
        Log.v(TAG, "mJsonArrayRequest: " + mJsonArrayRequest);
    }


    Response.Listener<JSONArray> mJSONResponseListener = new Response.Listener<JSONArray>() {
        @Override
        public void onResponse(JSONArray response) {
            if (response != null) {
                Log.v(TAG, "JSONArray: " + response.toString());
                try {
                    parseJSONResponse(response);
                    initViewAndSetupData();
                } catch (JSONException e) {
                    Log.w(TAG, "parseJSONResponse has JSONException: " + e);
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

    private void initViewAndSetupData() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        mAdapter = new ResponseJSONCodeAdapter(MainActivity.this);
        recyclerView.setOnScrollListener(new InfiniteScrollingListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                loadMoreData();
            }
        });
        recyclerView.setAdapter(mAdapter);
        dismissProgressDialog();
    }

    // Append more data into the adapter
    public void loadMoreData() {
        if (mResponseJSONUtils.getItemList() != null) {
            startQuery(mResponseJSONUtils.getItemList().size());
        }
    }

    private void parseJSONResponse(JSONArray response) throws JSONException {
        int count = response.length();
        int curSize = 0;
        if (mAdapter != null) {
            curSize = mAdapter.getItemCount();
        }
        for (int i = 0; i < count; i++) {
            JSONObject resultObject = response.getJSONObject(i);
            String id = resultObject.getString(ResponseJSONUtils.JSON_KEY_ID);
            String created = resultObject.getString(ResponseJSONUtils.JSON_KEY_CREATED);
            JSONObject sourceObject = resultObject.getJSONObject(ResponseJSONUtils.JSON_KEY_SOURCE);
            String sender = sourceObject.getString(ResponseJSONUtils.JSON_KEY_SENDER);
            String note = sourceObject.getString(ResponseJSONUtils.JSON_KEY_NOTE);
            JSONObject destinationObject = resultObject.getJSONObject(ResponseJSONUtils.JSON_KEY_DESTINATION);
            String recipient = destinationObject.getString(ResponseJSONUtils.JSON_KEY_RECIPIENT);
            String amount = destinationObject.getString(ResponseJSONUtils.JSON_KEY_AMOUNT);
            String currency = destinationObject.getString(ResponseJSONUtils.JSON_KEY_CURRENCY);
            Log.v(TAG, "[parseJSONResponse] id:" + id + ", createdTime: " + created + "" +
                    ", sender: " + sender + ", note: " + note +
                    ", recipient: " + recipient + ", amount: " + amount + ", currency: " + currency);
            mResponseJSONUtils.newItem(id, created, sender, note, recipient, amount, currency);
        }
        if (mAdapter != null) {
            mAdapter.notifyItemRangeInserted(curSize, mResponseJSONUtils.getItemList().size() - 1);
        }
    }

    private class ResponseJSONCodeAdapter extends RecyclerView.Adapter {

        LayoutInflater inflater;

        public ResponseJSONCodeAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View itemLayoutView = inflater.inflate(R.layout.item_layout, viewGroup, false);
            MyViewHolder myViewHolder = new MyViewHolder(itemLayoutView);
            return myViewHolder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            MyViewHolder myViewHolder = null;
            ResponseJSONUtils.ItemInfo itemInfo = null;
            if (mResponseJSONUtils != null && position < getItemCount()) {
                itemInfo = mResponseJSONUtils.getItemFromList(position);
            }
            if (viewHolder != null && viewHolder instanceof MyViewHolder) {
                myViewHolder = (MyViewHolder) viewHolder;
            }
            if (myViewHolder != null && itemInfo != null) {
                if (myViewHolder.idTextView != null) {
                    myViewHolder.idTextView.setText(itemInfo.id);
                }
                if (myViewHolder.createdTextView != null) {
                    myViewHolder.createdTextView.setText(itemInfo.created);
                }
                if (myViewHolder.senderTextView != null) {
                    myViewHolder.senderTextView.setText(itemInfo.sender);
                }
                if (myViewHolder.noteTextView != null) {
                    myViewHolder.noteTextView.setText(itemInfo.note);
                }
                if (myViewHolder.recipientTextView != null) {
                    myViewHolder.recipientTextView.setText(itemInfo.recipient);
                }
                if (myViewHolder.amountTextView != null) {
                    myViewHolder.amountTextView.setText(itemInfo.amount);
                }
                if (myViewHolder.currencyTextView != null) {
                    myViewHolder.currencyTextView.setText(itemInfo.currency);
                }
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            if (mResponseJSONUtils != null && mResponseJSONUtils.getItemList() != null) {
                return mResponseJSONUtils.getItemList().size();
            }
            return 0;
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