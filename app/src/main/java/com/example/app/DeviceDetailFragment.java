/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import static com.example.app.WiFiDirectActivity.TAG;
import static com.example.app.WiFiDirectActivity.cont;
import static com.example.app.WiFiDirectActivity.i;
import static com.example.app.WiFiDirectActivity.receivePacket;
import static com.example.app.WiFiDirectActivity.serverSocket;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener, WifiP2pManager.GroupInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private View mpun = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;
    public static WiFiDirectActivity main;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        main=(WiFiDirectActivity) activity;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //lui inizialmente inserisce tutte le varie view del file device_detail.xml , ma subito dopo eseguirà una funzione che renderà
        //invisibile tutto il fragment
        mpun = inflater.inflate(R.layout.device_list, null);
        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                        );
                ((DeviceListFragment.DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceListFragment.DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                        //poi ci penserà onActivityResult() a prelevare il file e inviarlo !
                    }
                });


        return mContentView;
    }

    /**
     * Recupero dei risultati dall'upload dei file
     *
     * @param requestCode è il codice che identifica quale Intent stà restituendo informazioni
     * @param resultCode è un codice restituito dall'Intent chiamato
     * @param data è l'Intent che è stato eseguito tramite la chiamata startActivityForResult
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //bisognava inserire un if con requestCode che identificasse l'intent associato all'upload dei file
        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uri);
        Log.d(TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        getActivity().startService(serviceIntent);
    }

    @Override
    public void onGroupInfoAvailable(final WifiP2pGroup group) {
        // if you do a requestGroupInfo(), you will get the GO details on the callback
        //quando creerò il gruppo verro' informato qui
        //main.qualcosam("HAI AVUTO LE TUE INFORMAZIONI" + group.getClientList().toString());
        String a  = group.getPassphrase();

        TextView view = (TextView)main.findViewById(R.id.ps);
        view.setText(a);

    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        this.info = info;
        this.getView().setVisibility(View.VISIBLE);
        TextView qua =(TextView) main.findViewById(R.id.fulmi);
        qua.setText(info.groupOwnerAddress.getHostAddress());
        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                        : getResources().getString(R.string.no)));
        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());
        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
            //in quest'app si può solamente inviare file da un client a un owner, non viceversa.
           // new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text),main).execute();
            main.formation("P2P GO",""+(System.currentTimeMillis()));
        } else if (info.groupFormed) {
            main.formation("P2P Client",""+(System.currentTimeMillis()));
            // The other device acts as the client. In this case, we enable the
            // get file button.
            //mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            //((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources().getString(R.string.client_text));
        }

        // hide the connect button
       // mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }


    /**
     * Updates the UI with device data
     * 
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        //all'inizio il broadcast receiver rileverà un intent che chiamerà successivamente questa funzione per ripulire il fragment
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }



    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class FileServerAsyncTask extends AsyncTask<Void, String, String> {

        private Context context;
        private TextView statusText;
        private Activity lolli;
        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText,Activity jo) {
            this.context = context;
            this.statusText = (TextView) statusText;
            this.lolli = jo;
        }
        @Override
        protected void onProgressUpdate(String[] values) {
            String a=values[0];
            if(a!=null){
            TextView mytext=(TextView)lolli.findViewById(R.id.viliri);
                String ai = (String) mytext.getText();
            mytext.setText(ai+a);
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                String[] matrix = new String[6];
                matrix[0]= "993;100;a";  //ms e numero di pacchetti
                matrix[1]= "977;500;b";
                matrix[2]= "940;1000;c";
                matrix[3]= "740;5000;d";
                matrix[4]= "540;9000;e";
                matrix[5]= "160;15000;f";
                serverSocket = new DatagramSocket(9958);
                serverSocket.setSoTimeout(1);
                byte[] receiveData = new byte[1000];
                cont=0; //conta i byte in arrivo
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                int sap=0;

                SimpleRunner2 r = new SimpleRunner2(this,matrix, (WiFiDirectActivity.MyHandler) WiFiDirectActivity.handler);
                Thread v = new Thread(r);
                v.start(); //parte il thread 2 !


                synchronized(r) {
                    try {
                        publishProgress("thread1 BLOCCATO");
                        // Calling wait() will block this thread until another thread
                        // calls notify() on the object.
                        r.wait();
                    } catch (InterruptedException e) {
                        // Happens if someone interrupts your thread.
                    }
                }

                publishProgress("thread1 sbloccato: iniziero' ad aspettare");
                while(true){
                    try {
                        Thread.sleep(50500);  //il thread 1 aspettera' 50 sec
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    SimpleRunner2.ferma=1; //blocco il thread 2 !!
                    //se passa un secondo bisogna avvertire il client
                    publishProgress("Con un totale di " + i + " pacchetti (circa " + cont + " byte)");
                    InetAddress IPAddress = receivePacket.getAddress(); //PRIMA RECEIVEPACKET ERA VUOTO, ADESSO E' STATO RIEMPITO DA RECEIVE() DAI DATI DELL'UTENTE
                    int port = receivePacket.getPort();
                    String bul = "BULBASAUR;"+sap+";";  //il server dall'ok al client, cosicchè il client possa informare il server con maggiori informazioni
                    sap=sap+1;
                    byte[] b = bul.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(b, b.length, IPAddress, port);
                    serverSocket.send(sendPacket);
                    int luck=0;
                    int bu=0;
                    String n_pk="";
					/* questo while è stato necessario per evitare che il servente_rivisto sia in ricezione su pacchetti
				    di formato diverso */
                    int kk=0;
                    String[] parts = matrix[sap-1].split(";");
                    n_pk = parts[1]; //numero di pacchetti di questo ciclo in arrivo
                    if(sap==6){
                        sap=sap-1;
                        bu=1;
                    }
                    String[] parts2 = matrix[sap].split(";");
                    String part3 = parts2[2]; //nome ciclo

                    double ww = i;
                    long mm = (Integer.parseInt(n_pk))*50;
                    publishProgress("numero di pacc = " + n_pk);
                    double p_d_r = (ww/(mm)); //packet delivery ratio
                    int soommm= (Integer.parseInt(n_pk))*50;
                    publishProgress("--->Il client ha inviato " + (soommm) + " con un Packet Delivery Ratio % di " + p_d_r + " (" + (ww) + " / " + (mm) + ")");
                    // Calcoleremo il Throughput in questo modo: ((cont * 8) bits )/ 1 secondo = (cont * 8) bits
                    publishProgress("--->Con Throughput = " + BigDecimal.valueOf((cont * 8) / 50).toPlainString() + " bit/s \n");
                    //resettare di nuovo le variabili per il prossimo ciclo
                    i=0;  //resetta i pacchetti inviati
                    cont=0;

                    serverSocket.setSoTimeout(1000);

                    if(bu==1) {

                        synchronized(this){
                            //notify sbloccherà il thread2
                            notify();
                        }

                        break;
                    }
                    while(luck==0){
                        String at="";
                        kk=0;
                        try {
                            serverSocket.receive(receivePacket);    //ricevi pacchetti?
                        } catch (SocketTimeoutException e) {
                            kk=1;
                            serverSocket.send(sendPacket); //rinvia ogni 2secondi se non riceve pacchetti
                        }
                        if(kk==0) {
                            String ans = new String( receivePacket.getData());
                            String[] dividi = ans.split(";");
                            at = dividi[1];  //vedi nome del ciclo
                            if((at.equals(part3))){
                                //e' arrivato un pacchetto del nuovo ciclo !!!
                                String sentence = new String( receivePacket.getData());
                                byte [] sum = sentence.getBytes(); //invio con lo stesso contenuto
                                cont = cont+ sum.length;
                                DatagramPacket sendPacke = new DatagramPacket(sum, sum.length, IPAddress, port);
                                serverSocket.send(sendPacke);
                                i= i+1;
                                luck=1;
                            }
                        }
                    }

                    //thread1
                    synchronized(this){
                        //notify sbloccherà il thread2
                        this.notify();
                    }



                }



            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return "uc";
        }
        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {

        }


        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
/*
            DatagramSocket serverSocket = null;
            while(true){
                try {
                    serverSocket = new DatagramSocket(9888);
                    byte[] receiveData = new byte[20];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);    //ricevi pacchetti col protocollo UDP
                    //pacchetto ricevuto !!!
                    sentence = new String( receivePacket.getData());
                    if(sentence!= null){
                        TextView view = (TextView) lolli.findViewById(R.id.viliri);
                        view.setText(sentence);
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            */

        }

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        long startTime=System.currentTimeMillis();
        
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
            long endTime=System.currentTimeMillis()-startTime;
            Log.v("","Time taken to transfer all bytes is : "+endTime);
            
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return false;
        }
        return true;
    }

}
