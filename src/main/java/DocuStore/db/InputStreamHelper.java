package DocuStore.db;

import DocuStore.App;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;


public class InputStreamHelper {

//    private static byte[] PUBLIC_KEY_REQUEST = "PUBLIC_KEY:::".getBytes();

    static private byte[] readInputStream(InputStream is) throws IOException {
        int count;
        byte[] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while((count = is.read(buffer)) > 0){
            output.write(buffer, 0, count);

            sb.append(new String(Arrays.copyOf(buffer, count)));
            if (sb.toString().contains(":::")){
                break;
            }
        }
        return output.toByteArray();
    }

    public static Optional<byte[]> process(InputStream stream){
        //NOTE: THis is where encryption/decryption will occur
        try {
            byte[] input = readInputStream(stream);
//            System.out.println("InSH process output: " + new String(input));
            TripleByte splitData = TripleByte.split(input);

            String id = new String(splitData.id);
            String path = new String(splitData.path);
            byte[] data = splitData.data;

            App.printBytes("in InputStreamHelper.process: ", data);

//            System.out.println("ID: " + id + ", PATH: " + path + ", DATA: " + new String(data).replace("\n", ""));

            if (data.length <= 3){
                System.out.println("Fetch Request");
                // Input was a record request
                return Optional.ofNullable(FileManager.fetch(id, path));
            } else {
                System.out.println("Store Request");
                // Store request
                if (!FileManager.store(id, path, data)){
                    System.out.println("Failed to store file!");
                }
            }

        }catch (Exception e){
            // Do nothing
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private static class TripleByte {
        byte[] id, path, data;
        private TripleByte(byte[] id, byte[]path, byte[] data){
            this.id = id;
            this.path = path;
            this.data = data;
        }
        public static TripleByte split(byte[] bytes){

            App.printBytes("in TripleByte.split all: ", bytes);

            byte[] id = new byte[0];
            byte[] path = new byte[0];
            byte[] data = new byte[0];
            int lastSplit = 0;
            int splitIndex = 0;
            int index;

            byte lastByte = ' ';
            for (index = 0; index < bytes.length; index++){
                if (bytes[index] == ':' && lastByte == ':'){
                    if (splitIndex == 0){
                        id = Arrays.copyOfRange(bytes, 0, index-1);
                        lastSplit = index+1;
                    } else if (splitIndex == 1){
                        path = Arrays.copyOfRange(bytes, lastSplit, index-1);
                        lastSplit = index+1;
                    } else if (splitIndex == 2){
                        if (lastSplit < (index-1)){
                            data = Arrays.copyOfRange(bytes, lastSplit, index-1);
                        }
                        break;
                    }
                    splitIndex++;
                }
                lastByte = bytes[index];
            }

            App.printBytes("in TripleByte.split data: ", data);

            return new TripleByte(id, path, data);
        }
    }
}
