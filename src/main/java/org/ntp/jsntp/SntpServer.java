/**
 * This code is copyright (c) Artem Khvastunov 2013
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.  A HTML version of the GNU General Public License can be
 * seen at http://www.gnu.org/licenses/gpl.html
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 */
package org.ntp.jsntp;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

/**
 * @author Artem Khvastunov
 */
public class SntpServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SntpServer.class);

    private static final long WAIT_TIMEOUT = TimeUnit.SECONDS.toMillis(1);

    private int port;
    private boolean started;
    private Dispatcher dispatcher;
    private Thread thread;

    public SntpServer(int port) {
        this.port = port;
    }

    public void start() {
        Validate.isTrue(!started, "Server already started");
        dispatcher = new Dispatcher();
        thread = new Thread(dispatcher);
        thread.start();
        started = true;
        LOGGER.info("SntpServer was started on port {}", port);
    }

    public void stop() {
        Validate.isTrue(started, "Server already stopped");
        dispatcher.stop();
        try {
            Thread.currentThread().wait(WAIT_TIMEOUT);
        } catch (InterruptedException ignored) {
        }
        thread.interrupt();
        started = false;
        LOGGER.info("SntpServer was stopped");
    }

    class Dispatcher implements Runnable {

        private boolean finished;

        @Override
        public void run() {
            while (!finished) {
                try {
                    LOGGER.debug("Start listening for socket on port {}", port);
                    DatagramSocket socket = new DatagramSocket(port);
                    byte[] buf = new byte[48];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    LOGGER.debug("--- //// Input ---");
                    LOGGER.debug(new NtpMessage(packet.getData()).toString());
                    NtpMessage ntpMessage = new NtpMessage(packet.getData(), (System.currentTimeMillis() / 1000.0) + 2208988800.0);
                    LOGGER.debug("--- //// Output ---");
                    LOGGER.debug(ntpMessage.toString());

                    buf = ntpMessage.toByteArray();
                    NtpMessage.encodeTimestamp(buf, 40, (System.currentTimeMillis() / 1000.0) + 2208988800.0);
                    packet = new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort());
                    socket.send(packet);

                    socket.close();
                    LOGGER.debug("Listening successfully done");
                } catch (SocketException e) {
                    System.err.println(e);
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        }

        public void stop() {
            finished = true;
        }
    }
}
