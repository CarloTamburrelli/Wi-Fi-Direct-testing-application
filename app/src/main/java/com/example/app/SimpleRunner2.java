package com.example.app;

import android.os.Bundle;
import android.os.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Created by tamburrelli on 22/12/14.
 */

class SimpleRunner2 extends Thread
{
    public static int ferma = 0;
    int p;
    String[] matrix;
    DeviceDetailFragment.FileServerAsyncTask _thread1;
    WiFiDirectActivity.MyHandler handler;
    SimpleRunner2(DeviceDetailFragment.FileServerAsyncTask c,String[] matr,WiFiDirectActivity.MyHandler u){
        _thread1=c;
        p=0;
        matrix=matr;
        handler=u;
    }

    public void run()
    {
        int f=0;
//sono il thread 2 del server , mi devo occupare di leggere e riinviare i pacchetti.
        for(;p < 6; p=p+1){
            try {
                WiFiDirectActivity.serverSocket.setSoTimeout(10);
            } catch (SocketException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            String[] parts = matrix[p].split(";");
            String part3 = parts[2]; //nome del ciclo

            while(true){
                try {
                    WiFiDirectActivity.serverSocket.receive(WiFiDirectActivity.receivePacket);    //ricevi pacchetti
                    if(f==0){
                        synchronized(this){
                            //questo sblocca il thread1 solo la prima volta....!! !!
                            notify();
                        }
                        f=1;
                    }
                    String sentence = new String( WiFiDirectActivity.receivePacket.getData());    //preleva valore
                    String[] pa = sentence.split(";"); //funzione per prelevare i dati contenuti tra i ";"
                    if(pa[1].equals(part3)){ //blocco di codice dedicato all'esaminamento del pacchetto appena ricevuto.
                        InetAddress IPAddress = WiFiDirectActivity.receivePacket.getAddress();
                        int port = WiFiDirectActivity.receivePacket.getPort();
                        byte [] sum = sentence.getBytes(); //invio con lo stesso contenuto
                        DatagramPacket sendPacket = new DatagramPacket(sum, sum.length, IPAddress, port);
                        WiFiDirectActivity.serverSocket.send(sendPacket); // Restuisci pacchetto al client (Round trip Time)
                        WiFiDirectActivity.i=WiFiDirectActivity.i+1; // Calcolo del Packet Delivery Ratio
                        WiFiDirectActivity.cont = WiFiDirectActivity.cont+ sum.length; // Calcolo del Throughput
                    }
                } catch (SocketTimeoutException e) {
                    //
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(ferma==1){ //il thread 1 mi avverte di terminare la lettura
                    break;
                }
            } //fine while

            //thread2
            synchronized(_thread1) {
                try {
                    _thread1.wait(); //in attesa del thread1
                } catch (InterruptedException e) {
                   //
                }
            }

            ferma=0;
//sono di nuovo libero !!


        } //fine del for

        notifyMessage("fine del thread2!");
    } // fine metodo run


    private void notifyMessage(String arr) {
        Message msg = handler.obtainMessage(); //SI USA PER OTTENERE UN'ISTANZA DELLA CLASSE MESSAGE
        Bundle b = new Bundle();
        b.putString("innuendo", arr);
        msg.setData(b);
        handler.sendMessage(msg);
    }
} //fine classe del Thread

