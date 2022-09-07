/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Application;

import static Application.TCPSocketClient.output;
import java.io.BufferedWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 164776
 */
public class TCPTransferSlave implements Runnable {

    private BufferedInputStream input;
    private BufferedOutputStream output;
    private Socket cSocket;
    private ServerSocket sSocket;

    public TCPTransferSlave(Socket clientSocket) {
        try {
            cSocket = clientSocket;
            input = new BufferedInputStream(cSocket.getInputStream());
            output = new BufferedOutputStream(cSocket.getOutputStream());
        } catch (IOException e) {
            System.err.println("Could not create transfer slave.");
            System.exit(1);
        }
    }

    @Override
    public void run() {
        try {
            input = new BufferedInputStream(cSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("Cannot get client input.");
            System.exit(1);
        }
        int type = 0;
        byte[] nameBytes = null;
        try {
            type = input.read();
            nameBytes = new byte[input.read()];
            input.read(nameBytes, 0, nameBytes.length);
        } catch (IOException e) {
            System.err.println("Did not receive input from the client.");
            System.exit(1);
        }
        switch (type) {
            case 1:
                String filename = new String();
                try {
                    filename = new String(nameBytes, "UTF-8");
                } catch (IOException e) {
                    System.err.println("Cannot get client output.");
                    System.exit(1);
                }
                File rF = new File(filename);
                if (!(rF.exists() && !rF.isDirectory())) {
                    System.err.println("File does not exist.");
                    System.exit(1);
                }

                byte[] data = readFile(rF);

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
            case 2:
                try {
                    filename = new String(nameBytes, "UTF-8");
                    writeFile(filename, input);
                    input.close();
                } catch (IOException e) {
                    System.err.println("Cannot get client output.");
                    e.printStackTrace();
                    System.exit(1);
                }
                break;
            default:
                System.err.println("Did not receive correct input from the client.");
                System.exit(1);
        }
        System.exit(0);
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

    private void writeFile(String filename, BufferedInputStream input) {
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
