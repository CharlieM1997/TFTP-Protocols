/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Application;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * The transfer slave. Instantiated by the server to handle contact with a
 * client.
 *
 * @author 164776
 */
public class UDPTransferSlave extends UDPBuild implements Runnable {

    private DatagramSocket tSocket;
    private DatagramPacket cReqPacket;
    private int opcode;

    /**
     * An instance of the transfer slave. Instantiated with the request packet.
     *
     * @param packet The request packet from the client.
     */
    public UDPTransferSlave(DatagramPacket packet) {
        cReqPacket = packet;
        try {
            tSocket = new DatagramSocket();
        } catch (SocketException e) {
            System.err.println("Could not create Socket.");
            System.exit(1);
        }
    }

    /**
     * The runnable method. Deals with transmission with the client. Gets the
     * client port from the request packet, instantiates a UDPBuild object, and
     * then checks the request packet's opcode and runs appropriate code.
     */
    @Override
    public void run() {
        int cPort = cReqPacket.getPort();
        byte[] data;
        UDPBuild tempBuild = new UDPBuild();
        if (cReqPacket.getData()[1] == 1) {
            /**
             * Stores the filename, makes data packets and runs the transmit
             * method with the block number, data packet and client port.
             */
            opcode = RRQ;
            tempBuild.findFilename(cReqPacket.getData(), cReqPacket.getLength(), opcode);
            data = tempBuild.makePacket(tempBuild.getrFileName(), 0);
            transmit(0, data, cPort);
            int i = 1;
            do {
                data = tempBuild.makePacket(tempBuild.getrFileName(), i);
                transmit(i, data, cPort);
                i++;
                if (data.length < 512) {
                    break;
                }
            } while (true);
        } else {
            /**
             * Stores the filename, and then creates the first acknowledgement
             * packet. It then goes into a loop, checking the two packets match
             * each time, writing the file with the data packet each time.
             */
            opcode = WRQ;
            tempBuild.findFilename(cReqPacket.getData(), cReqPacket.getLength(), opcode);
            data = cReqPacket.getData();
            byte[] ackBlock = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(0).array();
            ackBlock[1] = ACK;
            if (!(data[0] == 0 && data[1] == WRQ && data[2] == ackBlock[2] && data[3] == ackBlock[3])) {
                System.err.println("Error: Received packet is corrupted.");
                //System.out.println(Arrays.toString(data));
                //System.out.println(Arrays.toString(ackBlock));
                return;
            }
            try {
                DatagramPacket ack = new DatagramPacket(ackBlock, ackBlock.length, InetAddress.getLocalHost(), cPort);
                tempBuild.sendPacket(ack, tSocket);
            } catch (UnknownHostException e) {
                System.err.println("Could not create packet.");
                return;
            }
            DatagramPacket receivedPacket = tempBuild.receivePacket(tSocket);

            data = receivedPacket.getData();
            if (receivedPacket.getLength() < 516) {
                data = Arrays.copyOf(data, receivedPacket.getLength());
            }
            ackBlock = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(1).array();
            ackBlock[1] = ACK;

            for (int i = 1; data.length > 512; i++) {
                if (!(data[0] == 0 && data[1] == DATA && data[2] == ackBlock[2] && data[3] == ackBlock[3])) {
                    System.err.println("Error: Received packet is corrupted.");
                    //System.out.println(Arrays.toString(data));
                    //System.out.println(Arrays.toString(ackBlock));
                    return;
                }
                tempBuild.writeFile(tempBuild.getwFileName(), data);
                try {
                    DatagramPacket ack = new DatagramPacket(ackBlock, ackBlock.length, InetAddress.getLocalHost(), cPort);
                    tempBuild.sendPacket(ack, tSocket);
                } catch (UnknownHostException e) {
                    System.err.println("Could not create packet.");
                    return;
                }

                receivedPacket = tempBuild.receivePacket(tSocket);
                System.out.println(Arrays.toString(receivedPacket.getData()));
                data = receivedPacket.getData();
                if (receivedPacket.getLength() < 516) {
                    data = Arrays.copyOf(data, receivedPacket.getLength());
                }
                ackBlock = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(i + 1).array();
                ackBlock[1] = ACK;
            }
            try {
                DatagramPacket ack = new DatagramPacket(ackBlock, ackBlock.length, InetAddress.getLocalHost(), cPort);
                tempBuild.sendPacket(ack, tSocket);
            } catch (UnknownHostException e) {
                System.err.println("Could not create packet.");
                return;
            }

            if (!(data[0] == 0 && data[1] == DATA && data[2] == ackBlock[2] && data[3] == ackBlock[3])) {
                System.err.println("Error: Received packet is corrupted.");
                return;
            }

            tempBuild.writeFile(tempBuild.getwFileName(), data);
            System.out.println("File transfer completed.");
        }
    }

    /**
     * This method is only called when writing files. It's the same method as in
     * the client class. It adds the header info to the data packet, sends it
     * and then receives the acknowledgement packet and checks the two. If the
     * two don't match, the last packet is resent.
     *
     * @param i The block number.
     * @param data The data byte array.
     * @param sPort The port to communicate to the server instance.
     */
    private void transmit(int i, byte[] data, int cPort) {
        byte[] arrayBlock = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(i).array();
        byte[] dataBlock = new byte[data.length + arrayBlock.length + 2];
        dataBlock[0] = 0;
        dataBlock[1] = DATA;
        System.arraycopy(arrayBlock, 2, dataBlock, 2, arrayBlock.length - 2);
        System.arraycopy(data, 0, dataBlock, 4, data.length);
        data = dataBlock;
        DatagramPacket packet = new DatagramPacket(data, data.length, clientIP, cPort);
        UDPBuild tempBuild = new UDPBuild();
        tempBuild.sendPacket(packet, tSocket);
        DatagramPacket ack = tempBuild.receivePacket(tSocket);
        byte[] ackBlock = ack.getData();
        if (!(ackBlock[0] == 0 && ackBlock[1] == ACK && ackBlock[2] == arrayBlock[2] && ackBlock[3] == arrayBlock[3])) {
            data = Arrays.copyOfRange(data, 4, data.length);
            transmit(i, data, cPort);
        }
    }
}
