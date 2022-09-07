/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Application;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 164776
 */
public class TCPSocketClient {

    private Socket cSocket; //The client-side socket that is used to communicate to the server. Unique for each instance
    private static final int SERVPORT = 1025; //Port has to be >1024 as stated in assessment
    static InputStream input;
    static OutputStream output;

    public TCPSocketClient() {
        try {
            cSocket = new Socket(InetAddress.getLocalHost(), SERVPORT);
        } catch (UnknownHostException e) {
            System.err.println("Could not find server.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Could not initalise client socket.");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        final TCPSocketClient client = new TCPSocketClient();

        System.out.println("Press 1 to start a read request, or 2 to start a write request.");
        Scanner sc = new Scanner(System.in);
        int req = Integer.parseInt(sc.nextLine());
        //System.out.println("Working Directory = " + System.getProperty("user.dir"));
        switch (req) {
            case 1: //read
                /**
                 * Gets the filename from the user, checks the file's existence
                 * and... *ADD TO THIS*
                 */
                System.out.println("Please enter the name of the file to read.");
                String rFilename = sc.nextLine();
                File rF = new File(rFilename);
                if (!(rF.exists() && !rF.isDirectory())) {
                    System.err.println("File does not exist.");
                    System.exit(1);
                }

                try {
                    output = client.cSocket.getOutputStream();
                    output.write(1);
                    output.write(rFilename.length());
                    output.write(rFilename.getBytes("UTF-8"));
                } catch (IOException e) {
                    System.err.println("Cannot write data.");
                    System.exit(1);
                }
        {
            try {
                input = new BufferedInputStream(new FileInputStream(rF));
            } catch (FileNotFoundException ex) {
                System.err.println("Error: Could not find file.");
            }
        }
            client.writeFile(rFilename, output, input);
            System.out.println("File successfully written.");
            break;
            case 2: //write
                /**
                 * Gets the filename from the user, checks the file's existence
                 * and... *ADD TO THIS*
                 */
                System.out.println("Please enter the name of the file to read.");
                rFilename = sc.nextLine();
                rF = new File(rFilename);
                if (!(rF.exists() && !rF.isDirectory())) {
                    System.err.println("File does not exist.");
                    System.exit(1);
                }

                try {
                    output = client.cSocket.getOutputStream();
                    input = client.cSocket.getInputStream();
                    output.write(2);
                    output.write(rFilename.length());
                    output.write(rFilename.getBytes("UTF-8"));
                } catch (IOException e) {
                    System.err.println("Cannot write data.");
                    System.exit(1);
                }
                byte[] data = client.readFile(rF);

                for (byte i : data) {
                    try {
                        output.write(i);
                    } catch (IOException e) {
                        System.err.println("Cannot write data.");
                        System.exit(1);
                    }
                }
                {
                    try {
                        TimeUnit.SECONDS.sleep(1);

                        output.close();
                    } catch (InterruptedException | IOException e) {
                    }
                }
                System.out.println("File successfully written.");
                break;
            default:
                System.err.println("Error: could not recognise input.");
                System.exit(1);
        }
    }

    private byte[] readFile(File file) {
        byte[] data = new byte[(int) file.length()];
        try {
            input = new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            System.err.println("Error: Could not find file.");
        }

        int readBytes = 0;
        int bytes;

        try {
            while ((bytes = input.read()) != -1) {
                data[readBytes++] = (byte) bytes;
            }
            input.close();
        } catch (IOException e) {
            System.err.println("Could not read from file.");
        }

        return data;
    }

    private void writeFile(String filename, OutputStream output, InputStream input) {
        File dir = new File("out");
        if (!dir.exists()) {
            dir.mkdir();
        }
        filename = (dir + "/" + filename);
        try {
            output = new BufferedOutputStream(new FileOutputStream(filename, true));
        } catch (FileNotFoundException e) {
            System.err.println("Could not find file.");
        }
        int b;
        try {
            while ((b = input.read()) != -1) {
                output.write(b);
            }
            output.close();
        } catch (IOException e) {
            System.err.println("Could not write file.");
        }
    }
}
