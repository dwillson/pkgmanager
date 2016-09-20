package com.test;

import java.net.ServerSocket;
import java.net.Socket;


public class Main {
    public static void main(String args[]) throws Exception {
        ServerSocket ssock = new ServerSocket(8080);
        System.out.println("Listening");

        while (true) {
            Socket sock = ssock.accept();
            new Thread(new PackageManager(sock)).start();
        }
    }

}
