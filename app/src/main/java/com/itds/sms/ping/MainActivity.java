package com.itds.sms.ping;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MainActivity extends AppCompatActivity {

    final byte[] payload = new byte[]{0x0A, 0x06, 0x03, (byte) 0xB0, (byte) 0xAF, (byte) 0x82, 0x03, 0x06, 0x6A, 0x00, 0x05};

    final String TAG = "Ping SMS";

    final String SENT = "pingsms.sent";
    final String DELIVER = "pingsms.deliver";

    final IntentFilter sentFilter = new IntentFilter(SENT);
    final IntentFilter deliveryFilter = new IntentFilter(DELIVER);
    final IntentFilter wapDeliveryFilter = new IntentFilter("android.provider.Telephony.WAP_PUSH_DELIVER");

    MenuItem pickContact;
    public final static int MENU_ITEM_PICK_CONTACT = 999;
    MenuItem clearHistory;
    public final static int MENU_ITEM_CLEAR_HISTORY = 998;

    SharedPreferences preferences;
    public final static String PREF_LAST_NUMBER = "pref_last_number";
    public final static String PREF_HISTORY = "pref_history";

    ArrayAdapter<String> historyAdapter;
    ArrayList<String> historyContent = new ArrayList<>();

    PendingIntent sentPI;
    PendingIntent deliveryPI;

    EditText phoneNumber;
    TextView statusText, resultText;
    ListView historyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneNumber = findViewById(R.id.phoneNumber);
        statusText = findViewById(R.id.sendStatus);
        resultText = findViewById(R.id.resultStatus);
        historyList = findViewById(R.id.historyList);

        preferences = getPreferences(Context.MODE_PRIVATE);
        phoneNumber.setText(preferences.getString(PREF_LAST_NUMBER, getString(R.string.phonenumber)));

        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity.this.checkPermissions()) {
                    resultText.setText(null);
                    updateHistory(phoneNumber.getText().toString());
                    SmsManager.getDefault().sendDataMessage(phoneNumber.getText().toString(), null, (short) 9200, payload, sentPI, deliveryPI);
                }
            }
        });

        sentPI = PendingIntent.getBroadcast(this, 0x1337, new Intent(SENT), PendingIntent.FLAG_CANCEL_CURRENT);
        deliveryPI = PendingIntent.getBroadcast(this, 0x1337, new Intent(DELIVER), PendingIntent.FLAG_CANCEL_CURRENT);

        historyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_2, android.R.id.text1);
        historyList.setAdapter(historyAdapter);
        updateHistory(null);

        historyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                phoneNumber.setText(historyAdapter.getItem(position));
            }
        });
    }

    void updateHistory(String current) {
        if (current != null) {
            preferences.edit().putString(PREF_LAST_NUMBER, current).apply();
            preferences.edit().putString(PREF_HISTORY, preferences.getString(PREF_HISTORY, "").concat(current + ",")).apply();
        }

        historyContent.clear();
        historyContent.addAll(Arrays.asList(preferences.getString(PREF_HISTORY, "").split(",")));

        if (historyAdapter != null) {
            historyAdapter.clear();
            historyAdapter.addAll(historyContent);
            historyAdapter.notifyDataSetChanged();
        }
    }

    void clearHistory() {
        preferences.edit().putString(PREF_HISTORY, "").apply();
        updateHistory(null);
    }

    boolean checkPermissions() {
        List<String> missingPermissions = new ArrayList<>();

        int sendSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
        int readPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

        if (sendSmsPermission != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.SEND_SMS);
        }

        if (readPhonePermission != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), 1);
            return false;
        }

        return true;
    }

    String getLogBytesHex(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(String.format("0x%02X ", b));
        }
        return sb.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(br, sentFilter);
        registerReceiver(br, deliveryFilter);
        registerReceiver(br, wapDeliveryFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(br);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        pickContact = menu.findItem(MENU_ITEM_PICK_CONTACT);
        if (pickContact == null) {
            pickContact = menu.add(Menu.NONE, MENU_ITEM_PICK_CONTACT, Menu.NONE, R.string.pick_contact)
                    .setIcon(R.mipmap.ic_menu_invite).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        clearHistory = menu.findItem(MENU_ITEM_CLEAR_HISTORY);
        if (clearHistory == null) {
            clearHistory = menu.add(Menu.NONE, MENU_ITEM_CLEAR_HISTORY, Menu.NONE, "Clear history")
                    .setIcon(android.R.drawable.ic_menu_close_clear_cancel).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_ITEM_PICK_CONTACT) {
            pickContact();
            return true;
        } else if (item.getItemId() == MENU_ITEM_CLEAR_HISTORY) {
            clearHistory();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void pickContact() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        ActivityCompat.startActivityForResult(this, pickIntent, MENU_ITEM_PICK_CONTACT, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == MENU_ITEM_PICK_CONTACT && resultCode == RESULT_OK) {
                Uri contactUri = data.getData();
                String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
                Cursor cursor = getContentResolver().query(contactUri, projection,
                        null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    phoneNumber.setText(cursor.getString(numberIndex));
                }

                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.pick_contact_failed, Toast.LENGTH_SHORT).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "intent: " + ((intent == null || intent.getAction() == null) ? "null" : intent.getAction()));
            Log.e(TAG, "result: " + getResultCode());
            Log.e(TAG, "pdu (if any): " + ((intent != null && intent.hasExtra("pdu")) ? getLogBytesHex((byte[]) intent.getExtras().get("pdu")) : ""));

            if (intent == null) {
                return;
            }

            if (SENT.equalsIgnoreCase(intent.getAction())) {
                statusText.setText((getResultCode() == RESULT_OK ? R.string.sent : R.string.notsent));
                resultText.setText(null);
            } else if (DELIVER.equalsIgnoreCase(intent.getAction())) {
                boolean delivered = false;
                if (intent.hasExtra("pdu")) {
                    byte[] pdu = (byte[]) intent.getExtras().get("pdu");
                    if (pdu != null && pdu.length > 1) {
                        String resultPdu = getLogBytesHex(pdu).trim();
                        delivered = "00".equalsIgnoreCase(resultPdu.substring(resultPdu.length() - 2));
                    }
                }
                resultText.setText(delivered ? R.string.delivered : R.string.offline);
            }

        }
    };
}
