package com.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClientTest {
    private int port = 8080;
    private String host = "localhost";

    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    private Socket socket;

    @Test
    public void readWriteTest() {
        try {
            connect();
            String result = writeToAndReadFromSocket(socket, "INDEX|cloog|gmp,isl,pkg-config\n");
            assertEquals(PackageManager.FAIL, result);
            disconnect();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Test
    public void emptyPackageTest() {
        try {
            connect();
            String result = writeToAndReadFromSocket(socket, "INDEX|cloog|\n");
            assertEquals(PackageManager.OK, result);
            result = writeToAndReadFromSocket(socket, "REMOVE|cloog|\n");
            assertEquals(PackageManager.OK, result);
            disconnect();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Test
    public void removeUnindexedTest() {
        try {
            connect();
            String result = writeToAndReadFromSocket(socket, "REMOVE|unindexed|\n");
            assertEquals(PackageManager.OK, result);
            disconnect();
        } catch ( Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void errorTest() {
        try {
            connect();
            String result = writeToAndReadFromSocket(socket, "INDEX|invalid message\n");
            assertEquals(PackageManager.ERROR, result);
            disconnect();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Test
    public void twoWayRefs() {
        try {
            connect();
            String result = writeToAndReadFromSocket(socket, "INDEX|a|b\n");
            assertEquals(PackageManager.FAIL, result);
            result = writeToAndReadFromSocket(socket, "INDEX|b|a\n");
            assertEquals(PackageManager.FAIL, result);
            disconnect();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Test
    public void dependentRef() {
        try {
            connect();
            String result = writeToAndReadFromSocket(socket, "INDEX|a|\n");
            assertEquals(PackageManager.OK, result);
            result = writeToAndReadFromSocket(socket, "INDEX|b|a\n");
            assertEquals(PackageManager.OK, result);
            result = writeToAndReadFromSocket(socket, "REMOVE|a|\n");
            assertEquals(PackageManager.FAIL, result);
            result = writeToAndReadFromSocket(socket, "REMOVE|b|\n");
            assertEquals(PackageManager.OK, result);
            result = writeToAndReadFromSocket(socket, "REMOVE|a|\n");
            assertEquals(PackageManager.OK, result);
            disconnect();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void connect() throws Exception {
        socket = openSocket(host, port);
    }

    private void disconnect() throws IOException {
        bufferedReader.close();
        socket.close();
    }

    private String writeToAndReadFromSocket(Socket socket, String writeTo) throws Exception
    {
        try
        {
            bufferedWriter.write(writeTo);
            bufferedWriter.flush();

            String str = bufferedReader.readLine();
            return str;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Open a socket connection to the given server on the given port.
    **/
    private Socket openSocket(String server, int port) throws Exception
    {
        Socket socket = new Socket();

        try
        {
            InetAddress inteAddress = InetAddress.getByName(server);
            SocketAddress socketAddress = new InetSocketAddress(inteAddress, port);
            int timeoutInMs = 10 * 1000;   // 10 seconds
            socket.connect(socketAddress, timeoutInMs);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return socket;
        }
        catch (SocketTimeoutException ste)
        {
            System.err.println("Timed out waiting for the socket.");
            ste.printStackTrace();
            throw ste;
        }
    }

}
