package org.example;

import com.fazecast.jSerialComm.SerialPort;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class USBConnection {

    private volatile boolean running = true;

    public void Serialport() {
        // Get all available serial ports
        SerialPort[] comPorts = SerialPort.getCommPorts();

        // Find the ELM327 port
        SerialPort elm327Port = null;
        for (SerialPort port : comPorts) {
            if (port.getDescriptivePortName().contains("ELM327") ||
                    port.getSystemPortName().contains("ttyUSB") ||
                    port.getSystemPortName().contains("COM")) {
                elm327Port = port;
                break;
            }
        }

        final SerialPort finalElm327Port = elm327Port;  // Make elm327Port effectively final

        if (finalElm327Port != null) {
            System.out.println("ELM327 device found on port: " + finalElm327Port.getSystemPortName());
            if (finalElm327Port.openPort()) {
                System.out.println("Port opened successfully.");
                InputStream inputStream = finalElm327Port.getInputStream();
                OutputStream outputStream = finalElm327Port.getOutputStream();

                Thread readerThread = new Thread(() -> {
                    try {
                        // Initialize ELM327
                        initializeELM327(outputStream, inputStream);

                        // Here you can add commands to send and read responses
                        // Example: sendCommand(outputStream, "010D\r");  // Request vehicle speed
                        // readResponse(inputStream);  // Read and print the response

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        finalElm327Port.closePort();
                        System.out.println("Port closed.");
                    }
                });

                readerThread.start();
                // Add a shutdown hook to properly close resources when the program exits
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    running = false;
                    readerThread.interrupt();
                    try {
                        readerThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    finalElm327Port.closePort();
                    System.out.println("Shutdown hook: Port closed.");
                }));
            } else {
                System.out.println("Failed to open the port.");
            }
        } else {
            System.out.println("ELM327 device not found.");
        }
    }

    private void initializeELM327(OutputStream outputStream, InputStream inputStream)
            throws Exception {
        sendCommand(outputStream, "ATZ\r");  // Reset
        Thread.sleep(500);
        readResponse(inputStream);

        sendCommand(outputStream, "ATL0\r");  // Turn off linefeeds
        Thread.sleep(500);
        readResponse(inputStream);

        sendCommand(outputStream, "ATE1\r");  // Turn on echo
        Thread.sleep(500);
        readResponse(inputStream);

        sendCommand(outputStream, "ATH1\r");  // Turn on headers
        Thread.sleep(500);
        readResponse(inputStream);

        sendCommand(outputStream, "ATAT1\r");  // Enable Adaptive Timing Auto1
        Thread.sleep(500);
        readResponse(inputStream);

        sendCommand(outputStream, "ATSTFF\r");  // Set a long timeout (255 × 4 ms)
        Thread.sleep(500);
        readResponse(inputStream);

        sendCommand(outputStream, "ATI\r");  // Get the current version ID
        Thread.sleep(500);
        readResponse(inputStream);

        sendCommand(outputStream, "ATDP\r");  // Describe the current protocol
        Thread.sleep(500);
        readResponse(inputStream);

        sendCommand(outputStream, "ATSP0\r");  // Set Protocol to Auto
        Thread.sleep(500);
        readResponse(inputStream);
    }

    private void sendCommand(OutputStream outputStream, String command) throws Exception {
        outputStream.write(command.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private void readResponse(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[512];
        int bytesRead = inputStream.read(buffer);
        if (bytesRead > 0) {
            String response = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8).trim();
            System.out.println("Response: " + response);
        }
    }

    public static void main(String[] args) {
        USBConnection usbConnection = new USBConnection();
        usbConnection.Serialport();
    }
}
