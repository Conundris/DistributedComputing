package sample;

import java.io.File;

public class Utils {
    public static short extractResponseCode(String str) {
        int responseIndex = str.indexOf(':');
        return Short.parseShort(str.substring(0, responseIndex));
    }

    public static int getFileSizeKiloBytes(File file) {
        return (int) file.length() / 1024;
    }
}
