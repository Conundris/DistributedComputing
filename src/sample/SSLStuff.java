package sample;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;

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
}
