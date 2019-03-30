package sample;

import java.io.File;

public class Utils {
    public static short extractOpcode(String str) {
        int opIndex = str.indexOf(':');
        return Short.parseShort(str.substring(0, opIndex));
    }

    public static int getFileSizeKiloBytes(File file) {
        return (int) file.length() / 1024;
    }
}
