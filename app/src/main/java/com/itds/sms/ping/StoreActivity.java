package com.itds.sms.ping;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.mobicents.protocols.ss7.map.api.MAPException;
import org.mobicents.protocols.ss7.map.api.smstpdu.ApplicationPortAddressing16BitAddress;
import org.mobicents.protocols.ss7.map.smstpdu.ApplicationPortAddressing16BitAddressImpl;
import org.mobicents.protocols.ss7.map.smstpdu.SmsDeliverTpduImpl;
import org.mobicents.protocols.ss7.map.smstpdu.SmsTpduImpl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import static com.itds.sms.ping.MainActivity.PREF_DATA_SMS_STORE;

public class StoreActivity extends AppCompatActivity {

    public static final String TAG = "SMSPing/StoreActivity";
    public static final DateFormat sdf = SimpleDateFormat.getDateTimeInstance();

    ListView listView;
    TextView description;
    ArrayAdapter<String> storeAdapter;
    ArrayList<String> storeContent = new ArrayList<>();
    SharedPreferences preferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);
        preferences = getSharedPreferences(PREF_DATA_SMS_STORE, Context.MODE_PRIVATE);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        description = findViewById(R.id.storeDescription);
        description.setText(R.string.storage_description);

        listView = findViewById(R.id.listView);
        storeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);
        listView.setAdapter(storeAdapter);

        listView.setOnItemClickListener((parent, view, position, id) -> Log.d(TAG, storeAdapter.getItem(position)));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("SMS PDU", storeAdapter.getItem(position)));
            }
            Toast.makeText(StoreActivity.this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
            return true;
        });

        storeContent.clear();

        storeContent.addAll(parseSmsPDUs(preferences.getString(PREF_DATA_SMS_STORE, "").split(",")));

        storeAdapter.clear();
        storeAdapter.addAll(storeContent);
        storeAdapter.notifyDataSetChanged();
    }

    public byte[] pduHexToByteArray(String PDU) {
        if (PDU.length() % 2 != 0) {
            Log.e(TAG, "wrong number of bytes to pduHexToByteArray");
            return new byte[0];
        }
        byte[] converted = new byte[PDU.length() / 2];
        for (int i = 0; i < (PDU.length() / 2); i++) {
            converted[i] = (byte) ((Character.digit(PDU.charAt(i * 2), 16) << 4)
                    + Character.digit(PDU.charAt((i * 2) + 1), 16));
        }
        return converted;
    }

    public static String formatNumber(String number) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return PhoneNumberUtils.formatNumber(number, Locale.getDefault().getCountry());
        } else {
            //Deprecated method
            //noinspection deprecation
            return PhoneNumberUtils.formatNumber(number);
        }
    }

    /**
     * Notes: PDU will always be SMS
     */
    public ArrayList<String> parseSmsPDUs(String[] PDUs) {
        ArrayList<String> parsedPDUs = new ArrayList<>();
        int counter = 0;

        for (String PDU : PDUs) {
            if (PDU == null || PDU.isEmpty()) {
                continue;
            }

            StringBuilder sb = new StringBuilder();

            ApplicationPortAddressing16BitAddress smsPort = null;
            SmsTpduImpl smsTpdu = null;

            try {
                // PDU should always be SMS-DELIVER prefixed by SMSC info (SCA - Service Centre Address)
                byte[] pduWithSCA = pduHexToByteArray(PDU);
                // Cut off the SCA
                byte[] smsPdu = Arrays.copyOfRange(pduWithSCA, pduWithSCA[0] + 1, pduWithSCA.length);

                smsTpdu = SmsTpduImpl.createInstance(smsPdu, false, null);
                switch (smsTpdu.getSmsTpduType()) {
                    case SMS_DELIVER:
                        ((SmsDeliverTpduImpl) smsTpdu).getUserData().decode();

                        // see https://github.com/RestComm/jss7/issues/275
                        byte[] dtr = ((SmsDeliverTpduImpl) smsTpdu).getUserData().getDecodedUserDataHeader().getAllData().get(5);
                        smsPort = new ApplicationPortAddressing16BitAddressImpl(dtr);
                        break;
                }
            } catch (MAPException | IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                // ignore the exceptions
                e.printStackTrace();
            }

            SmsMessage sms = SmsMessage.createFromPdu(pduHexToByteArray(PDU));
            //sb.append("Raw PDU (hex): ").append(PDU);
            sb.append("Date/Time: ").append(sdf.format(sms.getTimestampMillis()));
            sb.append("\nSMSC: ").append(formatNumber(sms.getServiceCenterAddress()));
            sb.append("\nFrom: ").append(formatNumber(sms.getOriginatingAddress()));
            sb.append("\nFrom (display): ").append(sms.getDisplayOriginatingAddress());
            sb.append("\nFrom port: ").append(smsPort == null ? "N/A" : smsPort.getOriginatorPort());
            sb.append("\nTo port: ").append(smsPort == null ? "N/A" : smsPort.getDestinationPort());
            sb.append("\nData (hex): ");

            for (byte b : sms.getUserData()) {
                sb.append(String.format("%02x", b));
            }

            if (smsTpdu != null) {
                sb.append("\n\nAll info\n\n").append(smsTpdu);
            }

            //sb.append("\nUser Data (ascii):");
            //sb.append(new String(sms.getUserData()));

            parsedPDUs.add(sb.toString());
            parsedPDUs.add(PDU);
            parsedPDUs.add("Message #" + counter);
            counter++;
        }

        Collections.reverse(parsedPDUs);
        return parsedPDUs;
    }

}
