package DocuStore.db;

import DocuStore.App;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;


public class InputStreamHelper {

//    private static byte[] PUBLIC_KEY_REQUEST = "PUBLIC_KEY:::".getBytes();
    private static final String[] labels = new String[]{"id", "path", "data"};
    private static final int ID_INDEX = 0;
    private static final int PATH_INDEX = 1;
    private static final int DATA_INDEX = 2;

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

            ArrayList<byte[]> splitData = splitBytes(input);

            String id = splitData.size() > ID_INDEX ? new String(splitData.get(ID_INDEX)) : null;
            String path = splitData.size() > PATH_INDEX ? new String(splitData.get(PATH_INDEX)) : null;
            byte[] data =  splitData.size() > DATA_INDEX ? splitData.get(DATA_INDEX) : new byte[0];

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

    public static String getByteString(byte[] data){
//        return;
        StringBuilder dataBuilder = new StringBuilder();
        for (byte b : data) {
            dataBuilder.append(Integer.toHexString(0xff & b)).append(" ");
        }

        return dataBuilder.toString();
    }

    private static void printArray(ArrayList<byte[]> list){
        StringBuilder sb = new StringBuilder("{ ");
        for (byte[] data : list){
            sb.append(getByteString(data));
            sb.append(", ");
        }
        sb.append("}");
        System.out.println(sb.toString());
    }

    private static ArrayList<byte[]> splitBytes(byte[] bytes){
        int lastSplit = 0;
        byte lastByte = '!';
        ArrayList<byte[]> output = new ArrayList<>();

        for (int index = 0; index < bytes.length; index++){
            if (bytes[index] == ':' && lastByte == ':'){
                output.add(Arrays.copyOfRange(bytes, lastSplit, index-1));
                lastSplit=index+1;
                index += 1;
            }
            if (index < bytes.length -1){
                lastByte = bytes[index];
            }
        }

        printArray(output);

        return output;
    }

}
