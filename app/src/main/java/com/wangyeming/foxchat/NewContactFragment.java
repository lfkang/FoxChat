package com.wangyeming.foxchat;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.gc.materialdesign.views.ButtonFloat;
import com.wangyeming.Help.ContactEdit;
import com.wangyeming.Help.HanZiToPinYin;
import com.wangyeming.custom.adapter.AccountFilterAdapter;
import com.wangyeming.custom.adapter.ContactListAdapter;
import com.wangyeming.custom.widget.NewToast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 读取联系人列表信息的Fragment
 *
 * @author 王小明
 * @date 2015/01/11
 */
public class NewContactFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    //ContactsContract.Contacts
    private static final String[] CONTACT_PROJECTION = new String[]{
            "_id",                  //raw contact id 考虑使用lookup代替,不会改变
            "lookup",               //一个opaque值，包含当name_raw id改变时如何查找联系人的暗示
//            "name_raw_contact_id",  //name_raw_contact_id，随姓名改变
            "display_name",         //联系人姓名 DISPLAY_NAME_PRIMARY
            "display_name_alt",     //两者选一的展现display_name的方式，姓在前或名在前，如果选择不可用，则以DISPLAY_NAME_PRIMARY
            "display_name_source",  //用于产生联系人姓名的数据种类（EMAIL, PHONE, ORGANIZATION, NICKNAME, STRUCTURED_NAME）
            "phonetic_name",        //姓名发音
            "phonetic_name_style",  //姓名发音样式（包括JAPANESE， KOREAN， PINYIN和UNDEFINED）
            "sort_key",             //排序方式（对中国是拼音，对日本是五十音顺序）
            "sort_key_alt",         //可选的排序，对西方，名优先
            "photo_id",             //头像id，为null时查找 PHOTO_URI 或 PHOTO_THUMBNAIL_URI
            "photo_uri",            //全尺寸头像uri
            "photo_thumb_uri",      //缩略图头像uri
            "in_visible_group",     //反映群组课件状态的标识
            "has_phone_number",     //是否有手机号，1有，0没有
            "times_contacted",      //联系人的联系次数
            "last_time_contacted",  //最后联系时间
            "starred",              //是否被收藏
            "custom_ringtone",      //手机铃声uri，如果为null或missing，则为默认
            "send_to_voicemail",    //是否总是向该联系人发送声音邮件，0不是，1是
            "contact_presence",     //联系存在状态
            "contact_status",       //联系人最新的状态更新
            "contact_status_ts",    //联系人被新建或更新距今的时间长度
            "contact_status_res_package",  //包含label和icon的资源包
            "contact_status_label", //联系人状态icon资源的id
            "contact_status_icon",  //联系人状态label资源的id,如“Google Talk”
            ContactsContract.RawContacts.ACCOUNT_NAME,
            ContactsContract.RawContacts.ACCOUNT_TYPE,
    };

    //ContactsContract.RawContacts
    private static final String[] RAWCONTACT_PROJECTION = new String[]{
            ContactsContract.RawContacts.ACCOUNT_NAME,
            ContactsContract.RawContacts.ACCOUNT_TYPE,
    };

    //联系人数据操作
    protected ContentResolver cr;
    //滑动时toast姓提示
    protected Toast nameToast;
    private String mParam1;
    private String mParam2;
    private OnFragmentInteractionListener mListener;
    // 当前activity
    private Activity currentActivity;
    // 当前view
    private View currentView;
    //联系人数据
    private List<Map<String, Object>> contactList = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private ContactListAdapter mAdapter;
    //private RecyclerView.LayoutManager mLayoutManager;
    private LinearLayoutManager mLayoutManager;
    //收藏联系人数目
    private int starNum;
    //总联系人数目
    private int totalNum;
    //通讯录账号--Map
    private Map<String, Integer> accountMap = new HashMap<>();
    //通讯录账号--List
    private List<Map<String, Object>> accountList = new ArrayList<>();
    private AccountFilterAdapter accountFilterAdapter;
    public NewContactFragment() {
        // Required empty public constructor
    }

    public static NewContactFragment newInstance(String param1, String param2) {
        NewContactFragment fragment = new NewContactFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_contact, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * 初始化
     */
    public void init() {
        getPhoneMessage();
        currentActivity = getActivity();  //获取当前activity
        currentView = getView();  //获取当前view
        setNewContact(); //设置新建联系人按钮
        cr = currentActivity.getContentResolver();
        setRecyclerView();  //设置recyclerView
        setSpinner();
        new Thread(new Runnable() {

            @Override
            public void run() {
                getPhoneContacts();  //获取手机联系人信息
                Message message = Message.obtain();
                message.obj = "ok";
                NewContactFragment.this.handler1.sendMessage(message);
            }
        }).start();
    }
    /*
    *   获取手机信息
     */
    public void getPhoneMessage() {
        int targetSDK = Build.VERSION.SDK_INT;
        Log.d("wym", "targetSDK " + targetSDK);
    }

    private Handler handler1 = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            mAdapter.notifyDataSetChanged();
            setSpinnerData();
            accountFilterAdapter.notifyDataSetChanged();
            setOnScrollListener();  //设置滑动监听
        }
    };

    /**
     * 联系人账户过滤
     */
    public void setSpinner() {
        Toolbar toolbar = (Toolbar) currentActivity.findViewById(R.id.toolbar);
        Spinner spinner = (Spinner) toolbar.findViewById(R.id.filterAccount);
        accountFilterAdapter = new AccountFilterAdapter(accountList, currentActivity);
        spinner.setAdapter(accountFilterAdapter);
        //spinner
    }

    /**
    *
     */
    public void setSpinnerData() {
        //添加All-所有联系人
        Map<String, Object> map1 = new HashMap<>();
        map1.put("accountName",getString(R.string.all));
        map1.put("accountNum",totalNum);
        accountList.add(map1);
        for(String key: accountMap.keySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("accountName", key);
            map.put("accountNum", accountMap.get(key));
            accountList.add(map);
        }
    }

    /**
     * 设置新建联系人按钮
     */
    public void setNewContact() {
        ButtonFloat button = (ButtonFloat) currentView.findViewById(R.id.addNewContact);
        button.setBackgroundColor(Color.parseColor("#9C27B0"));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newContact();
            }
        });
    }

    /**
     * 新建联系人
     */
    public void newContact() {
        Intent intent = new Intent(currentActivity, AddNewContactActivity.class);
        startActivity(intent);
    }

    /**
     * 得到手机通讯录联系人信息
     */
    public void getPhoneContacts() {
        Log.d(this.getTag(), "对汉字联系人拼音正确标记");
        //setPinYin();
        Log.d(this.getTag(), "读取收藏联系人。。。");
        starNum = readContact("starred=?", new String[]{"1"}); //读取星标联系人
        Log.d(this.getTag(), "读取未收藏联系人。。。");
        totalNum = readContact("starred=?", new String[]{"0"}); //读取非星标联系人
    }

    /**
     * 为汉字联系人设置正确的拼音
     */
     public void setPinYin() {
         Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, CONTACT_PROJECTION,
                 null, null , null);
         while (cursor.moveToNext()) {
             long _id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
             String lookUpKey = cursor.getString(cursor.getColumnIndex(
                     ContactsContract.Contacts.LOOKUP_KEY));
             String displayName = cursor.getString(cursor.getColumnIndex(
                     ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
             //ContactEdit contactEdit = new ContactEdit(lookUpKey, cr);
             ContactEdit contactEdit = new ContactEdit(_id, cr);
             String sortKey = HanZiToPinYin.getPinyin(displayName);
             //Log.d("wym", "拼音 " + sortKey);
             if(displayName.equals("ZTE客服2")) {
                 contactEdit.updateContactPinyin(sortKey);
             }
         }
         cursor.close();
     }

    /**-
     * 读取联系人基本信息
     */
    public int readContact(String selection, String[] selectionArgs) {
        String isStarred = selectionArgs[0];
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, CONTACT_PROJECTION,
                selection, selectionArgs, "sort_key");
        String flag = "first";
        while (cursor.moveToNext()) {
            //获取基本信息
            long _id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            String accountName = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.RawContacts.ACCOUNT_NAME));
            String accountType = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.RawContacts.ACCOUNT_TYPE));
            String lookUpKey = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.LOOKUP_KEY));
//            long nameRawContactId = cursor.getLong(cursor.getColumnIndex(
//                    ContactsContract.Contacts.NAME_RAW_CONTACT_ID));
            String displayName = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
            String displayNameAlt = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE));
            String displayNameSource = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.DISPLAY_NAME_SOURCE));
            String phoneticName = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.PHONETIC_NAME));
            String phoneticNameStyle = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.PHONETIC_NAME_STYLE));
            String sortKey = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.SORT_KEY_PRIMARY));
            String sortkeyAlt = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.SORT_KEY_ALTERNATIVE));
            long photoId = cursor.getLong(cursor.getColumnIndex(
                    ContactsContract.Contacts.PHOTO_ID));
            String photoUri = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.PHOTO_URI));
            String photoThumbUri = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));
            int inVisibleGroup = cursor.getInt(cursor.getColumnIndex(
                    ContactsContract.Contacts.IN_VISIBLE_GROUP));
            int hasPhoneNumber = cursor.getInt(cursor.getColumnIndex(
                    ContactsContract.Contacts.HAS_PHONE_NUMBER));
            int timesContacted = cursor.getInt(cursor.getColumnIndex(
                    ContactsContract.Contacts.TIMES_CONTACTED));
            long lastTimeContacted = cursor.getLong(cursor.getColumnIndex(
                    ContactsContract.Contacts.LAST_TIME_CONTACTED));
            int starred = cursor.getInt(cursor.getColumnIndex(
                    ContactsContract.Contacts.STARRED));
            String customRingtone = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.CUSTOM_RINGTONE));
            int sendToVoicemail = cursor.getInt(cursor.getColumnIndex(
                    ContactsContract.Contacts.SEND_TO_VOICEMAIL));
            int contactPresence = cursor.getInt(cursor.getColumnIndex(
                    ContactsContract.Contacts.CONTACT_PRESENCE));
            String contactStatus = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.CONTACT_STATUS));
            long contactStatusTs = cursor.getLong(cursor.getColumnIndex(
                    ContactsContract.Contacts.CONTACT_STATUS_TIMESTAMP));
            String contactStatusResPackage = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.CONTACT_STATUS_RES_PACKAGE));
            long contactStatusLabel = cursor.getLong(cursor.getColumnIndex(
                    ContactsContract.Contacts.CONTACT_STATUS_LABEL));
            long contactStatusIcon = cursor.getLong(cursor.getColumnIndex(
                    ContactsContract.Contacts.CONTACT_STATUS_ICON));
            //提取信息
            String mark = null;
            String firstPinyin = sortKey.substring(0, 1);
            switch (isStarred) {
                //如果是收藏联系人
                case "1":
                    if (flag.equals("first")) {
                        mark = "star";
                    } else {
                        mark = "none";
                    }
                    flag = "none";
                    break;
                //如果是非收藏联系人
                case "0":
                    //如果是汉字，转化为PinYin
                    firstPinyin = HanZiToPinYin.getFirstPinyin(firstPinyin);
                    Log.d("wym", displayName + " " + firstPinyin);
                    if (firstPinyin.equals(flag)) {
                        mark = "none";
                    } else {
                        mark = firstPinyin;
                    }
                    flag = firstPinyin;
                    break;
            }
            //存进Map
            Map<String, Object> contactMap = new HashMap<>();
            contactMap.put("_id", _id);
            contactMap.put("accountName",accountName);
            contactMap.put("accountType",accountType);
            contactMap.put("LookUpKey", lookUpKey);
//            contactMap.put("nameRawContactId",nameRawContactId);
            contactMap.put("displayName", displayName);
            contactMap.put("displayNameAlt", displayNameAlt);
            contactMap.put("displayNameSource", displayNameSource);
            contactMap.put("phoneticName", phoneticName);
            contactMap.put("phoneticNameStyle", phoneticNameStyle);
            contactMap.put("sortKey", sortKey);
            contactMap.put("sortkeyAlt", sortkeyAlt);
            contactMap.put("photoId", photoId);
            contactMap.put("photoUri", photoUri);
            contactMap.put("photoThumbUri", photoThumbUri);
            contactMap.put("inVisibleGroup", inVisibleGroup);
            contactMap.put("hasPhoneNumber", hasPhoneNumber);
            contactMap.put("timesContacted", timesContacted);
            contactMap.put("lastTimeContacted", lastTimeContacted);
            contactMap.put("starred", starred);
            contactMap.put("customRingtone", customRingtone);
            contactMap.put("sendToVoicemail", sendToVoicemail);
            contactMap.put("contactPresence", contactPresence);
            contactMap.put("contactStatus", contactStatus);
            contactMap.put("contactStatusTs", contactStatusTs);
            contactMap.put("contactStatusResPackage", contactStatusResPackage);
            contactMap.put("contactStatusLabel", contactStatusLabel);
            contactMap.put("contactStatusIcon", contactStatusIcon);
            contactMap.put("mark", mark);
            String logString = "";
            for (String key : contactMap.keySet()) {
                logString = logString + " " + key + " " + contactMap.get(key);
            }
            Log.d("NewContactFragment", logString);
            contactList.add(contactMap);
            if(accountMap.containsKey(accountName)) {
                accountMap.put(accountName, accountMap.get(accountName) + 1);
            } else {
                accountMap.put(accountName, 1);
            }
        }
        cursor.close();
        return contactList.size();
    }

    /**
     * 设置recyclerview
     */
    public void setRecyclerView() {
        mRecyclerView = (RecyclerView) currentView.findViewById(R.id.contact_list_recycler);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(currentActivity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new ContactListAdapter(currentActivity, contactList, starNum);
        mRecyclerView.setAdapter(mAdapter);
    }

    /**
     * 设置RecyclerView滑动监听---根据滑动位置Toast提示
     */
    public void setOnScrollListener() {
        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView view, int scrollState) {
                if (nameToast != null) {
                    nameToast.cancel();
                }
                //GridLayoutManager layoutManager = ((GridLayoutManager)view.getLayoutManager());
                //int firstPos = layoutManager.findFirstVisibleItemPosition() + 1;
                int firstPos = mLayoutManager.findFirstVisibleItemPosition() + 1;
                String name = (String) contactList.get(firstPos).get("displayName");
                Log.d("wym", "firstPos " + firstPos + " name " + name);
                if (name == null) {
                    return;
                }
                String surname = name.substring(0, 1);
                nameToast = NewToast.makeText(currentActivity, surname, Toast.LENGTH_SHORT);
                nameToast.show();
            }
        });
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(Uri uri);
    }

}
