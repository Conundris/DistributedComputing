package sample;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings("Duplicates")
public class SSLStuff {

    /*
     * The following is to set up the keystores.
     */
    private static final String pathToStores = "C:\\";
    private static final String keyStoreFile = "nanithefuck.jks";
    private static final String trustStoreFile = "public.jks";
    private static final String passwd = "ittralee";

    private static final int MAX_HANDSHAKE_LOOPS = 200;
    private static final int MAX_APP_READ_LOOPS = 60;
    private static final int SOCKET_TIMEOUT = Integer.getInteger(
            "socket.timeout", 3 * 1000); // in millis
    private static final int BUFFER_SIZE = 1024;
    private static final int MAXIMUM_PACKET_SIZE = 1024;
    private static final boolean IS_SERVER = true;
    private static final boolean IS_CLIENT = false;

    private static final String keyFilename =
            //System.getProperty("test.src", ".") + "/" +
            pathToStores + "\\" + keyStoreFile;
    private static final String trustFilename =
            //System.getProperty("test.src", ".") + "/" +
            pathToStores + "\\" + trustStoreFile;
    private static Exception clientException = null;
    private static Exception serverException = null;

    // get DTSL context
    public static SSLContext getDTLSContext() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        char[] passphrase = passwd.toCharArray();

        try (FileInputStream fis = new FileInputStream(keyFilename)) {
            ks.load(fis, passphrase);
        }

        try (FileInputStream fis = new FileInputStream(trustFilename)) {
            ts.load(fis, passphrase);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        SSLContext sslCtx = SSLContext.getInstance("DTLS");

        sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslCtx;
    }

    public static SSLEngine createSSLEngine(boolean isClient) throws Exception {
        SSLContext context = getDTLSContext();
        SSLEngine engine = context.createSSLEngine();

        SSLParameters paras = engine.getSSLParameters();
        paras.setMaximumPacketSize(MAXIMUM_PACKET_SIZE);

        engine.setUseClientMode(isClient);
        engine.setSSLParameters(paras);

        return engine;
    }

    // handshake
    public static ByteBuffer handshake(SSLEngine engine, DatagramSocket socket,
                         SocketAddress peerAddr, boolean isServer) throws Exception {

        boolean endLoops = false;
        int loops = MAX_HANDSHAKE_LOOPS;
        List<DatagramPacket> packets = new ArrayList<>();
        String side = isServer ? "Server" : "Client";
        engine.beginHandshake();
        while (!endLoops &&
                (serverException == null) && (clientException == null)) {

            if (--loops < 0) {
                throw new RuntimeException(
                        "Too much loops to produce handshake packets");
            }

            // Handshake statuses
            // NEED_WRAP: must send data to the remote side before handshaking can continue
            // NEED_UNWRAP: needs to receive data from the remote side before handshaking can continue.
            // NEED_UNWRAP_AGAIN: needs to receive data from the remote side before handshaking can continue again.
            SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
            if (hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP ||
                    hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN) {

                log(side, "Need DTLS records, handshake status is " + hs);

                ByteBuffer iNet;
                ByteBuffer iApp;
                if (hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                    byte[] buf = new byte[BUFFER_SIZE];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    try {
                        log(side, "handshake(): wait for a packet");
                        socket.receive(packet);
                        log(side, "handshake(): received a packet, length = "
                                + packet.getLength());
                    } catch (SocketTimeoutException ste) {
                        log(side, "Warning: " + ste);

                        packets.clear();
                        boolean finished = onReceiveTimeout(
                                engine, peerAddr, side, packets);

                        log(side, "handshake(): receive timeout: re-send "
                                + packets.size() + " packets");
                        for (DatagramPacket p : packets) {
                            socket.send(p);
                        }

                        if (finished) {
                            log(side, "Handshake status is FINISHED "
                                    + "after calling onReceiveTimeout(), "
                                    + "finish the loop");
                            endLoops = true;
                        }

                        log(side, "New handshake status is "
                                + engine.getHandshakeStatus());

                        continue;
                    }

                    iNet = ByteBuffer.wrap(buf, 0, packet.getLength());
                    iApp = ByteBuffer.allocate(BUFFER_SIZE);
                } else {
                    iNet = ByteBuffer.allocate(0);
                    iApp = ByteBuffer.allocate(BUFFER_SIZE);
                }

                SSLEngineResult r = engine.unwrap(iNet, iApp);
                SSLEngineResult.Status rs = r.getStatus();
                hs = r.getHandshakeStatus();
                if (rs == SSLEngineResult.Status.OK) {
                    // OK
                } else if (rs == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                    log(side, "BUFFER_OVERFLOW, handshake status is " + hs);

                    // the client maximum fragment size config does not work?
                    throw new Exception("Buffer overflow: " +
                            "incorrect client maximum fragment size");
                } else if (rs == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                    log(side, "BUFFER_UNDERFLOW, handshake status is " + hs);

                    // bad packet, or the client maximum fragment size
                    // config does not work?
                    if (hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                        throw new Exception("Buffer underflow: " +
                                "incorrect client maximum fragment size");
                    } // otherwise, ignore this packet
                } else if (rs == SSLEngineResult.Status.CLOSED) {
                    throw new Exception(
                            "SSL engine closed, handshake status is " + hs);
                } else {
                    throw new Exception("Can't reach here, result is " + rs);
                }

                if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
                    log(side, "Handshake status is FINISHED, finish the loop");
                    endLoops = true;
                }
            } else if (hs == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                packets.clear();
                boolean finished = produceHandshakePackets(
                        engine, peerAddr, side, packets);

                log(side, "handshake(): need wrap: send " + packets.size()
                        + " packets");
                for (DatagramPacket p : packets) {
                    socket.send(p);
                }

                if (finished) {
                    log(side, "Handshake status is FINISHED "
                            + "after producing handshake packets, "
                            + "finish the loop");
                    endLoops = true;
                }
            } else if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                runDelegatedTasks(engine);
            } else if (hs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                log(side, "Handshake status is NOT_HANDSHAKING,"
                        + " finish the loop");
                endLoops = true;
            } else if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
                throw new Exception(
                        "Unexpected status, SSLEngine.getHandshakeStatus() "
                                + "shouldn't return FINISHED");
            } else {
                throw new Exception("Can't reach here, handshake status is "
                        + hs);
            }
        }

        // in case of server, try to receive first application data
        //
        // if a client keeps sending handshake data it may mean
        // that some packets were lost (for example, FINISHED message),
        // and the server needs to re-send last handshake messages
        ByteBuffer appBuffer = null;
        if (isServer) {
            appBuffer = receiveAppData(engine, socket, side, (Callable) () -> {
                // looks like client didn't finish handshaking
                // because it keeps sending handshake messages
                //
                // re-send final handshake packets

                SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
                log(side, "receiveAppData(): handshake status is " + hs);

                packets.clear();
                produceHandshakePackets(engine, peerAddr, side, packets);

                log(side, "receiveAppData(): re-send " + packets.size()
                        + " final packets");
                for (DatagramPacket p : packets) {
                    socket.send(p);
                }

                return null;
            });
        }

        SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
        log(side, "Handshake finished, status is " + hs);

        if (engine.getHandshakeSession() != null) {
            throw new Exception(
                    "Handshake finished, but handshake session is not null");
        }

        SSLSession session = engine.getSession();
        if (session == null) {
            throw new Exception("Handshake finished, but session is null");
        }
        log(side, "Negotiated protocol is " + session.getProtocol());
        log(side, "Negotiated cipher suite is " + session.getCipherSuite());

        // handshake status should be NOT_HANDSHAKING
        //
        // according to the spec,
        // SSLEngine.getHandshakeStatus() can't return FINISHED
        if (hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            throw new Exception("Unexpected handshake status " + hs);
        }

        return appBuffer;
    }

    // produce handshake packets
    static boolean produceHandshakePackets(SSLEngine engine, SocketAddress socketAddr,
                                           String side, List<DatagramPacket> packets) throws Exception {

        boolean endLoops = false;
        int loops = MAX_HANDSHAKE_LOOPS;
        while (!endLoops &&
                (serverException == null) && (clientException == null)) {

            if (--loops < 0) {
                throw new RuntimeException(
                        "Too much loops to produce handshake packets");
            }

            ByteBuffer oNet = ByteBuffer.allocate(32768);
            ByteBuffer oApp = ByteBuffer.allocate(0);
            SSLEngineResult r = engine.wrap(oApp, oNet);
            oNet.flip();

            SSLEngineResult.Status rs = r.getStatus();
            SSLEngineResult.HandshakeStatus hs = r.getHandshakeStatus();
            if (rs == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                // the client maximum fragment size config does not work?
                throw new Exception("Buffer overflow: " +
                        "incorrect server maximum fragment size");
            } else if (rs == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                log(side, "produceHandshakePackets(): BUFFER_UNDERFLOW");
                log(side, "produceHandshakePackets(): handshake status: " + hs);
                // bad packet, or the client maximum fragment size
                // config does not work?
                if (hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                    throw new Exception("Buffer underflow: " +
                            "incorrect server maximum fragment size");
                } // otherwise, ignore this packet
            } else if (rs == SSLEngineResult.Status.CLOSED) {
                throw new Exception("SSLEngine has closed");
            } else if (rs == SSLEngineResult.Status.OK) {
                // OK
            } else {
                throw new Exception("Can't reach here, result is " + rs);
            }

            // SSLEngineResult.Status.OK:
            if (oNet.hasRemaining()) {
                byte[] ba = new byte[oNet.remaining()];
                oNet.get(ba);
                DatagramPacket packet = createHandshakePacket(ba, socketAddr);
                packets.add(packet);
            }

            if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
                log(side, "produceHandshakePackets(): "
                        + "Handshake status is FINISHED, finish the loop");
                return true;
            }

            boolean endInnerLoop = false;
            SSLEngineResult.HandshakeStatus nhs = hs;
            while (!endInnerLoop) {
                if (nhs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                    runDelegatedTasks(engine);
                } else if (nhs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP ||
                        nhs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN ||
                        nhs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {

                    endInnerLoop = true;
                    endLoops = true;
                } else if (nhs == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                    endInnerLoop = true;
                } else if (nhs == SSLEngineResult.HandshakeStatus.FINISHED) {
                    throw new Exception(
                            "Unexpected status, SSLEngine.getHandshakeStatus() "
                                    + "shouldn't return FINISHED");
                } else {
                    throw new Exception("Can't reach here, handshake status is "
                            + nhs);
                }
                nhs = engine.getHandshakeStatus();
            }
        }

        return false;
    }

    // deliver application data
    public static void sendAppData(SSLEngine engine, DatagramSocket socket,
                     ByteBuffer appData, SocketAddress peerAddr, String side)
            throws Exception {

        List<DatagramPacket> packets =
                produceApplicationPackets(engine, appData, peerAddr);
        appData.flip();
        log(side, "sendAppData(): send " + packets.size() + " packets");
        for (DatagramPacket p : packets) {
            socket.send(p);
        }
    }

    static DatagramPacket createHandshakePacket(byte[] ba, SocketAddress socketAddr) {
        return new DatagramPacket(ba, ba.length, socketAddr);
    }

    // produce application packets
    static List<DatagramPacket> produceApplicationPackets(
            SSLEngine engine, ByteBuffer source,
            SocketAddress socketAddr) throws Exception {

        List<DatagramPacket> packets = new ArrayList<>();
        ByteBuffer appNet = ByteBuffer.allocate(32768);
        SSLEngineResult r = engine.wrap(source, appNet);
        appNet.flip();

        SSLEngineResult.Status rs = r.getStatus();
        if (rs == SSLEngineResult.Status.BUFFER_OVERFLOW) {
            // the client maximum fragment size config does not work?
            throw new Exception("Buffer overflow: " +
                    "incorrect server maximum fragment size");
        } else if (rs == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
            // unlikely
            throw new Exception("Buffer underflow during wraping");
        } else if (rs == SSLEngineResult.Status.CLOSED) {
            throw new Exception("SSLEngine has closed");
        } else if (rs == SSLEngineResult.Status.OK) {
            // OK
        } else {
            throw new Exception("Can't reach here, result is " + rs);
        }

        // SSLEngineResult.Status.OK:
        if (appNet.hasRemaining()) {
            byte[] ba = new byte[appNet.remaining()];
            appNet.get(ba);
            DatagramPacket packet =
                    new DatagramPacket(ba, ba.length, socketAddr);
            packets.add(packet);
        }

        return packets;
    }

    // retransmission if timeout
    static boolean onReceiveTimeout(SSLEngine engine, SocketAddress socketAddr,
                                    String side, List<DatagramPacket> packets) throws Exception {

        SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
        if (hs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            return false;
        } else {
            // retransmission of handshake messages
            return produceHandshakePackets(engine, socketAddr, side, packets);
        }
    }

    public static ByteBuffer receiveAppData(SSLEngine engine, DatagramSocket socket,
                              String side) throws Exception {
        return receiveAppData(engine, socket, side, null);
    }

    // receive application data
    // the method returns not-null if data received
    public static ByteBuffer receiveAppData(SSLEngine engine, DatagramSocket socket,
                                     String side, Callable onHandshakeMessage)
            throws Exception {

        int loops = MAX_APP_READ_LOOPS;
        while ((serverException == null) && (clientException == null)) {
            if (--loops < 0) {
                throw new RuntimeException(
                        "Too much loops to receive application data");
            }

            byte[] buf = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            log(side, "receiveAppData(): wait for a packet");
            try {
                socket.receive(packet);
                log(side, "receiveAppData(): received a packet");
            } catch (SocketTimeoutException e) {
                log(side, "Warning: " + e);
                continue;
            }
            ByteBuffer netBuffer = ByteBuffer.wrap(buf, 0, packet.getLength());
            ByteBuffer recBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            SSLEngineResult rs = engine.unwrap(netBuffer, recBuffer);
            log(side, "receiveAppData(): engine status is " + rs);
            recBuffer.flip();
            if (recBuffer.remaining() != 0) {
                return recBuffer;
            }

            if (onHandshakeMessage != null) {
                onHandshakeMessage.call();
            }
        }

        return null;
    }

    // run delegated tasks
    static void runDelegatedTasks(SSLEngine engine) throws Exception {
        Runnable runnable;
        while ((runnable = engine.getDelegatedTask()) != null) {
            runnable.run();
        }

        SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
        if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            throw new Exception("handshake shouldn't need additional tasks");
        }
    }

    static void log(String side, String message) {
        System.out.println(side + ": " + message);
    }
}
