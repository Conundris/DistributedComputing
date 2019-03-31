package sample.SSL;

import sample.SSLStuff;

import javax.net.ssl.SSLEngine;

public class SSLClient {

    public SSLClient() {
        try {
            SSLEngine engine = SSLStuff.createSSLEngine(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
