/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Application;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * UDP Build. Holds methods that are used by both the client and the transfer
 * slave.
 *
 * @author 164776
 */
public class UDPBuild {

    /**
     * The opcodes used.
     */
    public static final int RRQ = 1, WRQ = 2, DATA = 3, ACK = 4, ERROR = 5;
    /**
     * Used to get the byte streams for a file that is sent or received.
     */
    private BufferedInputStream sntFile;
    private BufferedOutputStream rcvFile;

    /**
     * The IP of the client. Would be more useful if the client and server were
     * being ran on separate machines.
     */
    protected InetAddress clientIP;

    /**
     * The filenames of a file that is being sent or received.
     */
    public String rFileName, wFileName;

    /**
     * An instant of the build. This is sometimes instantiated in main methods
     * to get access to non-static methods.
     */
    public UDPBuild() {
        try {
            clientIP = InetAddress.getLocalHost();
            clientIP.isReachable(100);
            //System.out.println(clientIP);
        } catch (UnknownHostException e) {
            System.err.println("Could not get local IP address.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Could not reach client");
            System.exit(1);
        }
    }

    /**
     * Makes a byte array and populates it with data.
     *
     * @param file The filename, used to get access to the file.
     * @param offset Used to keep track of how far along data should be read
     * from the file.
     * @return The data byte array.
     */
    public byte[] makePacket(String file, int offset) {
        try {
            sntFile = new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            System.err.println("Error: Could not find file.");
        }
        byte[] bytestream = null;
        try {
            sntFile.skip(offset * 512);
            if (sntFile.available() < 512) {
                bytestream = new byte[sntFile.available()];
                int end = sntFile.available() - 1;
                sntFile.read(bytestream, 0, end);
                sntFile.close();
            } else {
                bytestream = new byte[512];
                sntFile.read(bytestream, 0, 512);
                sntFile.close();
            }
        } catch (IOException e) {
            System.out.println("error");
        }
        return bytestream;
    }

    /**
     * Writes a data packet into the new file.
     *
     * @param file The name of the new file.
     * @param bytestream The data packet.
     */
    public void writeFile(String file, byte[] bytestream) {
        /**
         * this code saves the new file to a folder 'out' in the working
         * directory; it's crude code, but it's the easiest way to show that the
         * program transfers files, especially since both the server and client
         * have the same working directory.
         */
        File dir = new File("out");
        if (!dir.exists()) {
            dir.mkdir();
        }
        file = (dir + "/" + file);
        try {
            rcvFile = new BufferedOutputStream(new FileOutputStream(file, true));
        } catch (FileNotFoundException e) {
            System.err.println("Error: Could not find file.");
        }
        //Ensures the header isn't also written into the file.
        bytestream = Arrays.copyOfRange(bytestream, 4, bytestream.length);
        try {
            rcvFile.write(bytestream, 0, bytestream.length);
            rcvFile.close();
        } catch (IOException e) {
            System.out.println("error");
        }
    }

    /**
     * Sends a packet.
     * 
     * @param packet The packet to be sent.
     * @param socket The socket that sends the packet.
     * @return Whether the packet was successfully sent or not.
     */
    public boolean sendPacket(DatagramPacket packet, DatagramSocket socket) {
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Packet was not sent successfully.");
            return false;
        }
        return true;
    }

    /**
     * Attempts to receive a packet within the timeout period.
     * 
     * @param socket The socket that receives the packet.
     * @return The received packet.
     */
    public DatagramPacket receivePacket(DatagramSocket socket) {
        try {
            socket.setSoTimeout(6000);
        } catch (SocketException e) { //this should only happen if the socket is already in use
            System.err.println("Unknown socket exception.");
            System.exit(1);
        }
        byte[] buffer = new byte[516];
        DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);

        try {
            socket.receive(pkt);
        } catch (IOException e) {

        }
        return pkt;
    }

    /**
     * Checks if the request packet is valid by looking at the byte data.
     * @param reqBlock The request packet.
     * @param opcode Used to check if the request packet has the right opcode.
     * @return Whether the request packet is valid or not.
     */
    public boolean validateRequestPacket(byte[] reqBlock, int opcode) {
        if (reqBlock[0] == 0 && reqBlock[1] == 0 && reqBlock[2] == 0 || (reqBlock[0] != 0 || reqBlock[1] != opcode)) {
            return false; //empty or incorrect packet
        } else {
            System.out.println("Successfully received request packet.");
        }
        
        int i = 0;
        do {
            i++;
        } while (i < reqBlock.length && reqBlock[i] != 0);
        return i != reqBlock.length - 1;
    }

    /**
     * Gets the filename from the request packet and writes it to the build
     * read or write filename variable.
     * @param data The request packet.
     * @param length The length of the request packet.
     * @param opcode Used to check which variable the filename should be written to.
     */
    public void findFilename(byte[] data, int length, int opcode) {
        int nameLength = length - 3;
        if (nameLength > 0) {
            byte[] filename = new byte[nameLength];
            System.arraycopy(data, 2, filename, 0, nameLength);
            String name = new String(filename).trim();
            if (opcode == 1) {
                rFileName = name;
            } else {
                wFileName = name;
            }
        }
    }

    /**
     *
     * @return The read filename.
     */
    public String getrFileName() {
        return rFileName;
    }

    /**
     *
     * @param rFileName The read filename.
     */
    public void setrFileName(String rFileName) {
        this.rFileName = rFileName;
    }

    /**
     *
     * @return The write filename.
     */
    public String getwFileName() {
        return wFileName;
    }

    /**
     *
     * @param wFileName The write filename.
     */
    public void setwFileName(String wFileName) {
        this.wFileName = wFileName;
    }

}
