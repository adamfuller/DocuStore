package DocuStore.db;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamHelper {
    public static boolean process(OutputStream stream, byte[] data){
        // TODO: Process data and send to output stream
        try {
            stream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
