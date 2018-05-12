package com.pince.webSocketBitshares;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.common.primitives.UnsignedLong;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.SSLContext;

import de.bitsharesmunich.graphenej.Address;
import de.bitsharesmunich.graphenej.Asset;
import de.bitsharesmunich.graphenej.AssetAmount;
import de.bitsharesmunich.graphenej.Authority;
import de.bitsharesmunich.graphenej.BaseOperation;
import de.bitsharesmunich.graphenej.Transaction;
import de.bitsharesmunich.graphenej.UserAccount;
import de.bitsharesmunich.graphenej.api.TransactionBroadcastSequence;
import de.bitsharesmunich.graphenej.errors.MalformedAddressException;
import de.bitsharesmunich.graphenej.interfaces.WitnessResponseListener;
import de.bitsharesmunich.graphenej.models.BaseResponse;
import de.bitsharesmunich.graphenej.models.WitnessResponse;
import de.bitsharesmunich.graphenej.objects.Memo;
import de.bitsharesmunich.graphenej.operations.TransferOperation;
import de.bitsharesmunich.graphenej.operations.TransferOperationBuilder;
import de.bitsharesmunich.graphenej.test.NaiveSSLContext;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private static final String FULL_NODE_URL = "";
    public static final String sourcePrivateKey = "";
    public static final String publicKey = "";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Very simple funds transfer
        //This is a simple transfer operation of 1 BTS from account **bilthon-15** to **bilthon-5**

        // Creating a transfer operation
        //1：构造BaseGrapheneHandler的子类TransactionBroadcastSequence构造方法中所需的transaction对象
        UserAccount fromUserAccount = new UserAccount("1.2.48");
        HashMap<Address, Long> keyAuths = new HashMap<Address, Long>() {{
            try {
                put(new Address(publicKey), 1l);
            } catch (MalformedAddressException e) {
                e.printStackTrace();
            }
        }};
        Authority activeAuthority = new Authority();
        activeAuthority.setKeyAuthorities(keyAuths);
        fromUserAccount.setActive(activeAuthority);

        TransferOperation transferOperation = new TransferOperationBuilder()
                .setTransferAmount(new AssetAmount(UnsignedLong.valueOf(10), new Asset("1.3.0")))
                .setSource(fromUserAccount)       // bilthon-15
                .setDestination(new UserAccount("1.2.20"))  // bilthon-5
                .setFee(new AssetAmount(UnsignedLong.valueOf(0), new Asset("1.3.0")))
                .setMemo(new Memo())
                .build();
        // Adding operations to the operation list
        ArrayList<BaseOperation> operationList = new ArrayList<>();
        operationList.add(transferOperation);

        // Creating a transaction instance
        Transaction transaction = new Transaction(sourcePrivateKey, null, operationList);

        //From here on, it is just a matter of creating a websocket connection and using a custom handler called
        //TransactionBroadcastSequence``` in order to broadcast it to the witness node.

        // Setting up a secure websocket connection.
        SSLContext context = null;
        try {
            context = NaiveSSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        WebSocketFactory factory = new WebSocketFactory();
        factory.setSSLContext(context);

        WebSocket mWebSocket = null;
        try {
            mWebSocket = factory.createSocket(FULL_NODE_URL);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //2:根据transaction构造BaseGrapheneHandler的子类TransactionBroadcastSequence，并添加到mWebSocket.addListener，之后设置监听回调
        //The provided ```listener``` is an instance of the class ```WitnessResponseListener```.
        mWebSocket.addListener(new TransactionBroadcastSequence(transaction, new Asset("1.3.0"), new WitnessResponseListener() {
            @Override
            public void onSuccess(WitnessResponse response) {
                Log.d(TAG, "response = " + response);
            }

            @Override
            public void onError(BaseResponse.Error error) {
                //missing required active authority
                Log.e(TAG, "error.message = " + error.message);
            }
        }));
            final WebSocket finalMWebSocket = mWebSocket;
            new Thread() {
                @Override
                public void run() {
                    try {
                        finalMWebSocket.connect();
                    } catch (WebSocketException e) {
                        e.printStackTrace();
                    }
                }
            }.start();

    }
}
