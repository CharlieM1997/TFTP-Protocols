/*
 * To change this license header, choose License Headers sntFile Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template sntFile the editor.
 */
package Application;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Scanner;
import java.io.File;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * An instance of the client that will communicate with the server and send or
 * receive datagram packets. Extends UDPBuild to get access to several methods
 * that help with this.
 *
 * @author 164776
 */
public class UDPSocketClient extends UDPBuild {

    private DatagramSocket cSocket; //The client-side socket that is used to communicate to the server. Unique for each instance
    private static final int SERVPORT = 1025; //Port has to be >1024 as stated in assessment

    /**
     * An instance of the client. Created by the main class. Creates a unique
     * socket.
     */
    public UDPSocketClient() {
        try {
            cSocket = new DatagramSocket();
        } catch (SocketException e) {
            System.err.println("Error creating socket.");
            System.exit(1);
        }
    }

    /**
     * The main method. Creates an instance of the client, and then pings the
     * user in the console for input on whether they wish to read or write a
     * file. (Note that both options do the same thing so long as the client and
     * server have the same working directory.) It then goes into one of two
     * switch case statements, and code to start the transfer with the server
     * begins.
     *
     * @param args
     */
    public static void main(String[] args) {
        final UDPSocketClient client = new UDPSocketClient();

        System.out.println("Press 1 to start a read request, or 2 to start a write request.");
        Scanner sc = new Scanner(System.in);
        int req = Integer.parseInt(sc.nextLine());
        //System.out.println("Working Directory = " + System.getProperty("user.dir"));
        switch (req) {
            case 1: //read
                /**
                 * Gets the filename from the user, checks the file's existence
                 * and makes and sends an initial request packet.
                 */
                System.out.println("Please enter the name of the file to read.");
                String rFilename = sc.nextLine();
                File rF = new File(rFilename);
                DatagramPacket reqPacket = client.makeReqPacket(rFilename, RRQ);
                if (rF.exists() && !rF.isDirectory()) { //if the file exists
                    client.sendPacket(reqPacket, client.cSocket);
                } else {
                    //System.out.println(System.getProperty("user.dir"));
                    System.err.println("Could not find file.");
                    System.exit(1);
                }
                /**
                 * Receives a reply packet and then creates the first
                 * acknowledgement packet. It then goes into a loop, checking
                 * the two packets match each time, writing the file with the
                 * data packet each time.
                 */
                DatagramPacket receivedPacket = client.receivePacket(client.cSocket);
                System.out.println("Successfully received ACK packet");
                byte[] dataBlock = receivedPacket.getData();
                byte[] ackBlock = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(0).array();
                ackBlock[1] = ACK;
                for (int i = 0; dataBlock.length > 512; i++) {

                    if (!(dataBlock[0] == 0 && dataBlock[1] == DATA && dataBlock[2] == ackBlock[2] && dataBlock[3] == ackBlock[3])) {
                        System.err.println("Error: Received packet is corrupted.");
                        //System.out.println(Arrays.toString(dataBlock));
                        //System.out.println(Arrays.toString(ackBlock));
                        return;
                    }
                    client.writeFile(rFilename, dataBlock);
                    try {
                        DatagramPacket ack = new DatagramPacket(ackBlock, ackBlock.length, InetAddress.getLocalHost(), receivedPacket.getPort());
                        client.sendPacket(ack, client.cSocket);
                    } catch (UnknownHostException e) {
                        System.err.println("Could not create packet.");
                        return;
                    }
                    receivedPacket = client.receivePacket(client.cSocket);

                    dataBlock = receivedPacket.getData();
                    if (receivedPacket.getLength() < 516) {
                        dataBlock = Arrays.copyOf(dataBlock, receivedPacket.getLength());
                    }
                    ackBlock = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(i + 1).array();
                    ackBlock[1] = ACK;
                }

                /**
                 * The file is successfully written to after the last data
                 * packet (signified by its length being < 512 bytes), and the
                 * client finishes.
                 */
                if (!(dataBlock[0] == 0 && dataBlock[1] == DATA && dataBlock[2] == ackBlock[2] && dataBlock[3] == ackBlock[3])) {
                    System.err.println("Error: Received packet is corrupted.");
                    return;
                }

                client.writeFile(rFilename, dataBlock);
                System.out.println("File transfer completed.");
                break;

            case 2: //write
                /**
                 * Gets the filename, checks the file exists and makes the
                 * request packet.
                 */
                System.out.println("Please enter the name of the file to write.");
                String wFilename = sc.nextLine();
                File wF = new File(wFilename);
                reqPacket = client.makeReqPacket(wFilename, WRQ);
                if (!(wF.exists() && !wF.isDirectory())) {
                    //System.out.println(System.getProperty("user.dir"));
                    System.err.println("Could not find file.");
                    System.exit(1);

                }
                /**
                 * Sends the request packet to the server, receives the
                 * acknowledgement packet, gets the server port from the packet
                 * to use for future transmissions (since the server create a
                 * unique thread to deal with each client, the port is needed
                 * from the first ack packet that this thread sends), and then
                 * tests the ack packet isn't corrupted.
                 */
                int sPort;
                do {
                    client.sendPacket(reqPacket, client.cSocket);
                    DatagramPacket ack = client.receivePacket(client.cSocket);
                    sPort = ack.getPort();
                    ackBlock = ack.getData();
                    if (ackBlock[0] == 0 && ackBlock[1] == ACK && ackBlock[2] == reqPacket.getData()[2] && ackBlock[3] == reqPacket.getData()[3]) {
                        break;
                    }
                } while (true);
                /**
                 * Transmits the first data packet and updates the block number.
                 */
                byte[] data = client.makePacket(wFilename, 0);
                client.transmit(1, data, sPort);
                int i = 2;
                /**
                 * This do-while loop makes and transmits data packets, stopping
                 * when the last data packet is made.
                 */
                do {
                    data = client.makePacket(wFilename, i);
                    client.transmit(i, data, sPort);
                    i++;
                    if (data.length < 512) {
                        break;
                    }
                } while (true);

                break;

            default:
                System.err.println("Error: could not recognise input.");
                System.exit(1);
        }
    }

    /**
     * Makes a request packet to begin transmission with the server.
     * The request packet is made up of the opcode and the first block number
     * (00) and the filename, in bytes.
     * @param filename The filename, to be converted to bytes.
     * @param opcode Read or Write Request.
     * @return The byte array to be placed into the request packet.
     */
    public DatagramPacket makeReqPacket(String filename, int opcode) {
        byte[] nameBlock = filename.getBytes();
        byte[] reqBlock = new byte[4 + nameBlock.length + 1];
        reqBlock[0] = (byte) ((opcode >> 8) & 0x0f);
        reqBlock[1] = (byte) (opcode & 0x0f);
        System.arraycopy(nameBlock, 0, reqBlock, 4, nameBlock.length); //copies the rFilename into the request block
        reqBlock[reqBlock.length - 1] = 0; //terminates the name
        DatagramPacket reqPacket = new DatagramPacket(reqBlock, reqBlock.length, clientIP, SERVPORT);
        return reqPacket;
    }

    /**
     * This method is only called when writing files. It adds the header info
     * to the data packet, sends it and then receives the acknowledgement packet
     * and checks the two. If the two don't match, the last packet is resent.
     * @param i The block number.
     * @param data The data byte array.
     * @param sPort The port to communicate to the server instance.
     */
    private void transmit(int i, byte[] data, int sPort) {
        byte[] arrayBlock = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(i).array();
        byte[] dataBlock = new byte[data.length + arrayBlock.length + 2];
        dataBlock[0] = 0;
        dataBlock[1] = DATA;
        System.arraycopy(arrayBlock, 2, dataBlock, 2, arrayBlock.length - 2);
        System.arraycopy(data, 0, dataBlock, 4, data.length);
        data = dataBlock;
        DatagramPacket packet = new DatagramPacket(data, data.length, clientIP, sPort);
        sendPacket(packet, cSocket);
        DatagramPacket ack = receivePacket(cSocket);
        byte[] ackBlock = ack.getData();
        if (!(ackBlock[0] == 0 && ackBlock[1] == ACK && ackBlock[2] == arrayBlock[2] && ackBlock[3] == arrayBlock[3])) {
            data = Arrays.copyOfRange(data, 4, data.length);
            transmit(i, data, sPort);
        }
    }
}
