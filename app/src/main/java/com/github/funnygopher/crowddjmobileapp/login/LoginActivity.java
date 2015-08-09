package com.github.funnygopher.crowddjmobileapp.login;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.github.funnygopher.crowddjmobileapp.R;
import com.github.funnygopher.crowddjmobileapp.SessionManager;
import com.github.funnygopher.crowddjmobileapp.playlist.PlaylistActivity;

public class LoginActivity extends AppCompatActivity implements AnybodyHomeable, CanLocateServer {

    SessionManager sessionManager;
    EditText input_name, input_address1, input_address2;
    AppCompatButton btn_join;

    private String ipAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(getApplicationContext());

        input_name = (EditText) findViewById(R.id.input_name);
        input_address1 = (EditText) findViewById(R.id.input_address1);
        input_address2 = (EditText) findViewById(R.id.input_address2);
        btn_join = (AppCompatButton) findViewById(R.id.btn_join);

        btn_join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = input_name.getText().toString();
                String hashedIp = input_address1.getText().toString();
                String hashedPort = input_address2.getText().toString();

                ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                if (!wifi.isConnected()) {
                    showMessage("No Wifi!", "You must be on the same wifi network as the music player!");
                    return;
                }

                if (name.trim().length() <= 0) {
                    showMessage("No Name", "Why don't you have a name?");
                    return;
                }

                if(hashedIp.length() <= 0 || hashedPort.length() <= 0) {
                    showMessage("Missing Server Code", "You have to tell me what server to connect to!");
                    return;
                }

                ipAddress = unhashServerCode(hashedIp, hashedPort);
                checkIfAnybodyIsHome(ipAddress);
            }
        });

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null && !extras.isEmpty()) {
            String name = extras.getString("name", "");
            //String hashedIp = extras.getString("hashedIp", "");
            //String hashedPort = extras.getString("hashedPort", "");
            input_name.setText(name);
            //input_address1.setText(hashedIp);
            //input_address2.setText(hashedPort);
        }
    }

    private String unhashServerCode(String hashedIp, String hashedPort) {
        String unhashedIp = String.valueOf(Long.valueOf(hashedIp, 36));
        String octets = unhashedIp.substring(0, unhashedIp.length() - 4);
        String lengthData = unhashedIp.substring(unhashedIp.length() - 4, unhashedIp.length());

        String[] baseAddress = getBaseAddress().split("\\.");
        int totalLength = 0;
        for(int i = 3; i >= 0; i--) {
            int length = Integer.valueOf(Character.toString(lengthData.charAt(i)));
            if(length == 0)
                continue;

            totalLength += length;
            String octet = octets.substring(octets.length() - totalLength, octets.length() - totalLength + length);
            baseAddress[i] = octet;
        }

        String address = "";
        for(int i = 0; i < 4; i++) {
            address += baseAddress[i];
            if(i < 3) {
                address += ".";
            }
        }
        int port = Integer.valueOf(hashedPort, 36);

        String ipAddress = address + ":" + port;
        return ipAddress;
    }

    private String getBaseAddress() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

        int newNumber = dhcpInfo.ipAddress & dhcpInfo.netmask;

        String something = (newNumber & 0xFF) + "." +
                ((newNumber >> 8 ) & 0xFF) + "." +
                ((newNumber >> 16 ) & 0xFF) + "." +
                ((newNumber >> 24 ) & 0xFF) ;
        Log.i("LocateServerTask", something);

        return something;
    }

    public void showMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void checkIfAnybodyIsHome(String ipAddress) {
        //LocateServerTask locateTask = new LocateServerTask(LoginActivity.this, this);
        //locateTask.execute();
        AnybodyHomeTask anybodyTask = new AnybodyHomeTask(LoginActivity.this, this, ipAddress);
        anybodyTask.execute();
    }

    public void nobodyIsHome() {
        showMessage("Nobody Was Home", "There wasn't a server at that IP Address!");
    }

    public void somebodyIsHome() {
        // Get the phone number of the phone the app is running on. This acts as the UUID.
        TelephonyManager tMgr = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber = tMgr.getLine1Number();

        String name = input_name.getText().toString();
        //String ipAddress = input_address.getText().toString();
        sessionManager.createLoginSession(name, ipAddress, phoneNumber);

        Intent intent = new Intent(LoginActivity.this, PlaylistActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void serverLocated(boolean located, String ipAddress) {
        if(located) {
            // Get the phone number of the phone the app is running on. This acts as the UUID.
            TelephonyManager tMgr = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            String phoneNumber = tMgr.getLine1Number();

            String name = input_name.getText().toString();
            sessionManager.createLoginSession(name, ipAddress, phoneNumber);

            Intent intent = new Intent(LoginActivity.this, PlaylistActivity.class);
            startActivity(intent);
            finish();
        } else {
            showMessage("Nobody Was Home", "Could not find a server nearby");
        }
    }
}