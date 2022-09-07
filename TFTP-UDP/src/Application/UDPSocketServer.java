/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Application;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * The server. Waits to receive request packets from a client. When it does, it
 * creates an instance of the transfer slave to communicate further.
 *
 * @author 164776
 */
public class UDPSocketServer extends UDPBuild implements Runnable {

    private DatagramSocket listenerSocket;
    private static final int REQPORT = 1025; //Port has to be >1024 as stated in assessment

    /**
     * An instance of the server object. Instantiates the listener socket with
     * the request port, that will listen for request packets.
     */
    public UDPSocketServer() {
        try {
            listenerSocket = new DatagramSocket(REQPORT);
            listenerSocket.setSoTimeout(6000);
        } catch (SocketException e) {
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
        UDPSocketServer server = new UDPSocketServer();
        server.run();

    }
    /**
     * The run method which is invoked by the main method. Listens for request
     * packets, and starts a new thread when one is received.
     */
    @Override
    public void run() {
        while (true) {
            DatagramPacket reqPacket = receivePacket(listenerSocket);
            if (validateRequestPacket(reqPacket.getData(), RRQ)) {
                Thread thread = new Thread(new UDPTransferSlave(reqPacket));
                thread.start();
            } else if (validateRequestPacket(reqPacket.getData(), WRQ)) {
                Thread thread = new Thread(new UDPTransferSlave(reqPacket));
                thread.start();
            } else {
                System.err.println("Waiting for a request packet.");
            }
        }
    }
}
