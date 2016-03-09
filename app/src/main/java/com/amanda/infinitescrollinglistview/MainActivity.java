package com.amanda.infinitescrollinglistview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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

    EditText mStartIndexEditText;
    EditText mNumEditText;
    Button mQueryBtn;

    String STARTINDEX_DEFAULT_VALUE = "0";
    String NUM_DEFAULT_VALUE = "10";
    int mItemCount;

    RequestQueue mRequestQueue;
    JsonArrayRequest mJsonArrayRequest;
    String mRequestURL = "https://hook.io/syshen/infinite-list/";
    String mContentType = "application/json";

    ResponseJSONUtils mResponseJSONUtils = new ResponseJSONUtils();
    ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        if (networkInfo != null) {
            mQueryBtn.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mProgressDialog = new ProgressDialog(MainActivity.this);
                    mProgressDialog.setMessage("Please wait...");
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.show();
                    mResponseJSONUtils.cleanItemList();
                    startQuery();
                }
            });
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

    private void initUI() {
        mStartIndexEditText = (EditText) findViewById(R.id.startIndexEditText);
        mNumEditText = (EditText) findViewById(R.id.numEditText);
        mQueryBtn = (Button) findViewById(R.id.queryBtn);
    }

    private void startQuery() {
        mRequestQueue = Volley.newRequestQueue(this);
        JSONArray jsonRequest = new JSONArray();

        ////////
        String startIndex = STARTINDEX_DEFAULT_VALUE;
        String num = NUM_DEFAULT_VALUE;
        if (mStartIndexEditText.getText() != null && mStartIndexEditText.getText().toString() != null) {
            startIndex = mStartIndexEditText.getText().toString();
            if (TextUtils.isEmpty(startIndex)) {
                startIndex = STARTINDEX_DEFAULT_VALUE;
            }
        }
        if (mNumEditText.getText() != null && mNumEditText.getText().toString() != null) {
            num = mNumEditText.getText().toString();
            if (TextUtils.isEmpty(num)) {
                num = NUM_DEFAULT_VALUE;
            }
        }
        mItemCount = Integer.parseInt(num);
        mRequestURL = mRequestURL + "?startIndex=" + startIndex + "&num=" + num;
        ////////


        mJsonArrayRequest = new JsonArrayRequest(Request.Method.POST, mRequestURL, jsonRequest, mJSONReponseListener, mErrorListener) {
            //TODO: why doesn't work??
//            @Override
//            protected Map<String, String> getParams() {
//                Map<String, String> params = new HashMap<String, String>();
//                String startIndex = STARTINDEX_DEFAULT_VALUE;
//                String num = NUM_DEFAULT_VALUE;
//                if (mStartIndexEditText.getText() != null && mStartIndexEditText.getText().toString() != null) {
//                    startIndex = mStartIndexEditText.getText().toString();
//                    if (TextUtils.isEmpty(startIndex)) {
//                        startIndex = STARTINDEX_DEFAULT_VALUE;
//                    }
//                }
//                if (mNumEditText.getText() != null && mNumEditText.getText().toString() != null) {
//                    num = mNumEditText.getText().toString();
//                    if (TextUtils.isEmpty(num)) {
//                        num = NUM_DEFAULT_VALUE;
//                    }
//                }
//                mItemCount = Integer.parseInt(num);
//                params.put("startIndex", startIndex);
//                params.put("num", num);
//                return params;
//            }

            public String getBodyContentType() {
                return mContentType;
            }
        };

        mRequestQueue.add(mJsonArrayRequest);
        Log.v(TAG, "mJsonArrayRequest: " + mJsonArrayRequest);
    }


    Response.Listener<JSONArray> mJSONReponseListener = new Response.Listener<JSONArray>() {
        @Override
        public void onResponse(JSONArray response) {
            if (response != null) {
                Log.v(TAG, "JSONArray: " + response.toString());
                try {
                    parseJSONResponse(response);
                    ListView listView = (ListView) findViewById(R.id.mainListView);
                    ResponseJSONCodeAdapter adapter = new ResponseJSONCodeAdapter(MainActivity.this);
                    listView.setAdapter(adapter);
                    dismissProgressDialog();
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

    private void parseJSONResponse(JSONArray response) throws JSONException {
        for (int i = 0; i < mItemCount; i++) {
            JSONObject resultObject = response.getJSONObject(i);
            String id = resultObject.getString(ResponseJSONUtils.JSON_KEY_ID);
            String created = resultObject.getString(ResponseJSONUtils.JSON_KEY_CREATED);
            JSONObject sourceObject = resultObject.getJSONObject(ResponseJSONUtils.JSON_KEY_SOURCE);
            String sender = sourceObject.getString(ResponseJSONUtils.JSON_KEY_SENDER);
            String note = sourceObject.getString(ResponseJSONUtils.JSON_KEY_NOTE);
            JSONObject destinationObject = resultObject.getJSONObject(ResponseJSONUtils.JSON_KEY_DESTINATION);
            String recipient = destinationObject.getString(ResponseJSONUtils.JSON_KEY_RECIPINET);
            String amount = destinationObject.getString(ResponseJSONUtils.JSON_KEY_AMOUNT);
            String currency = destinationObject.getString(ResponseJSONUtils.JSON_KEY_CURRENCY);
            Log.v(TAG, "[parseJSONResponse] id:" + id + ", createdTime: " + created + "" +
                    ", sender: " + sender + ", note: " + note +
                    ", recipient: " + recipient + ", amount: " + amount + ", currency: " + currency);
            mResponseJSONUtils.newItem(id, created, sender, note, recipient, amount, currency);
        }
    }

    private class ResponseJSONCodeAdapter extends BaseAdapter {

        LayoutInflater inflater;

        public ResponseJSONCodeAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            if (mResponseJSONUtils != null && mResponseJSONUtils.getItemList() != null) {
                return mResponseJSONUtils.getItemList().size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (mResponseJSONUtils != null) {
                return mResponseJSONUtils.getItemFromList(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder textHolder;

            if (view == null) {
                view = inflater.inflate(R.layout.item_layout, parent, false);
                textHolder = new ViewHolder();
                textHolder.idTextView = (TextView) view.findViewById(R.id.id);
                textHolder.createdTextView = (TextView) view.findViewById(R.id.created);
                textHolder.senderTextView = (TextView) view.findViewById(R.id.sender);
                textHolder.noteTextView = (TextView) view.findViewById(R.id.note);
                textHolder.recipientTextView = (TextView) view.findViewById(R.id.recipient);
                textHolder.amountTextView = (TextView) view.findViewById(R.id.amount);
                textHolder.currencyTextView = (TextView) view.findViewById(R.id.currency);
                view.setTag(textHolder);
            } else {
                textHolder = (ViewHolder) convertView.getTag();
            }
            ResponseJSONUtils.ItemInfo itemInfo = (ResponseJSONUtils.ItemInfo) getItem(position);
            if (textHolder != null && itemInfo != null) {
                if (textHolder.idTextView != null) {
                    textHolder.idTextView.setText(itemInfo.id);
                }
                if (textHolder.createdTextView != null) {
                    textHolder.createdTextView.setText(itemInfo.created);
                }
                if (textHolder.senderTextView != null) {
                    textHolder.senderTextView.setText(itemInfo.sender);
                }
                if (textHolder.noteTextView != null) {
                    textHolder.noteTextView.setText(itemInfo.note);
                }
                if (textHolder.recipientTextView != null) {
                    textHolder.recipientTextView.setText(itemInfo.recipient);
                }
                if (textHolder.amountTextView != null) {
                    textHolder.amountTextView.setText(itemInfo.amount);
                }
                if (textHolder.currencyTextView != null) {
                    textHolder.currencyTextView.setText(itemInfo.currency);
                }
            }
            return view;
        }

        class ViewHolder {
            TextView idTextView;
            TextView createdTextView;
            TextView senderTextView;
            TextView noteTextView;
            TextView recipientTextView;
            TextView amountTextView;
            TextView currencyTextView;
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
