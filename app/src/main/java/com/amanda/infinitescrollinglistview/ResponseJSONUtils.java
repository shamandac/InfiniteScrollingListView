package com.amanda.infinitescrollinglistview;

import java.util.HashMap;

/**
 * Created by amanda on 2016/3/9.
 */
public class ResponseJSONUtils {

    final static String JSON_KEY_ID = "id";
    final static String JSON_KEY_CREATED = "created";
    final static String JSON_KEY_SOURCE = "source";
    final static String JSON_KEY_DESTINATION = "destination";

    final static String JSON_KEY_SENDER = "sender";
    final static String JSON_KEY_NOTE = "note";
    final static String JSON_KEY_RECIPIENT = "recipient";
    final static String JSON_KEY_AMOUNT = "amount";
    final static String JSON_KEY_CURRENCY = "currency";

    private HashMap<Integer, ItemInfo> mItemList = new HashMap<Integer, ItemInfo>();

    public class ItemInfo {
        String id;
        String created;
        String sender;
        String note;
        String recipient;
        String amount;
        String currency;
    }

    public void newItem(int index, String id, String created, String sender, String note, String recipient, String amount, String currency) {
        ItemInfo iteminfo = new ItemInfo();
        iteminfo.id = id;
        iteminfo.created = created;
        iteminfo.sender = sender;
        iteminfo.note = note;
        iteminfo.recipient = recipient;
        iteminfo.amount = amount;
        iteminfo.currency = currency;
        if (mItemList != null) {
            mItemList.put(index, iteminfo);
        }
    }

    public ItemInfo getItemFromList(int index) {
        return mItemList.get(index);
    }

    public boolean removeItemFromList(int index) {
        if (index >= 0) {
            mItemList.remove(index);
            return true;
        }
        return false;
    }

    public HashMap<Integer, ItemInfo> getItemList() {
        return mItemList;
    }

    public int getItemListSize() {
        if (mItemList != null) {
            return mItemList.size();
        }
        return 0;
    }
}
