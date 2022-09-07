/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Application;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The server. Waits to receive request packets from a client. When it does, it
 * creates an instance of the transfer slave to communicate further.
 *
 * @author 164776
 */
public class TCPSocketServer implements Runnable {

    private ServerSocket listenerSocket;
    private static final int REQPORT = 1025; //Port has to be >1024 as stated in assessment

    /**
     * An instance of the server object. Instantiates the listener socket with
     * the request port, that will listen for request packets.
     */
    public TCPSocketServer() {
        try {
            listenerSocket = new ServerSocket(REQPORT);
            listenerSocket.setSoTimeout(6000);
        } catch (IOException e) {
            System.err.println("Could not create socket.");
            System.exit(1);
        }
    }

    /**
     * The main method. Creates a new server instance and calls its run method.
     *
     * @param args
     */
    public static void main(String[] args) {
        TCPSocketServer server = new TCPSocketServer();
        server.run();

    }

    /**
     * The run method which is invoked by the main method. Listens for request
     * packets, and starts a new thread when one is received.
     */
    @Override
    public void run() {
        System.out.println("Waiting to connect to a client.");

        Socket cSocket = null;
        while (true) {
            try {
                cSocket = listenerSocket.accept();
                Thread thread = new Thread(new TCPTransferSlave(cSocket));
                thread.start();
            } catch (IOException e) {
            }
        }
    }
}
