package DocuStore.data;

import DocuStore.App;
import DocuStore.db.EncryptionHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class Record implements Serializable {
    final public static long serialVersionUID = 11L;
    final public static String HOST = "localhost";
    final public static int PORT = 8081;
    final public static byte[] NULL_RECORD_BYTES = ":::".getBytes();
    private static EncryptionHelper encryptionHelper = EncryptionHelper.getInstance();
    /**
     * Base path for storing records, ~/Desktop/test
     */
    final public static String BASE_PATH = System.getProperty("user.home") + File.separator + "Desktop" + File.separator +"test";
    private static String USER;
    private static String KEY;
    private String id;
    private String path;
    private String user;
    private String key;
    private byte[] data;

    private Record(){
    }

    public Record(String id, String path, byte[] data) throws InvalidRecordException {
        if (id == null || path == null){
            throw new InvalidRecordException();
        }
        this.user = USER;
        this.key = KEY;
        this.id = id;
        this.path = path;
        this.data = data;
    }

    public static Record getRecord(String id, String path, byte[] data){
        Record r = new Record();
        r.path = path;
        r.id = id;
        r.data = data;
        return r;
    }

    public static String makeFilePathSafe(String str){
        return str.replace("\\", "").replace(".","").replace("/", "");
    }

    public String getFullPath(){
        String safePath = makeFilePathSafe(this.path);
        String safeId = makeFilePathSafe(this.id);

        return BASE_PATH + File.separator + safePath + File.separator + safeId + ".svbl";
    }

    static String[] split(String s, String sep){
        ArrayList<String> separated = new ArrayList<>();
        int lastParse = 0;
        for (int i = sep.length(); i<s.length()+sep.length(); i++){
            if (s.startsWith(sep, i-sep.length())){
                separated.add(s.substring(lastParse, i-sep.length()));
                lastParse = i;
                i+=sep.length()-1;
            }
        }
        String[] output = new String[separated.size()];
        separated.toArray(output);
        return output;
    }

//    /**
//     * Get a record from a byte array.
//     * Byte array should be formatted id::path::data:::
//     * Any : in data should be replaced with a _:_
//     * @param data
//     * @return
//     */
//    public static Record fromBytes(byte[] data){
//        String dataString = new String(data);
//        String[] splitData = dataString.replace(":::", "").split("::");
//        if (splitData.length != 3){
//            return null;
//        }
//        String id = splitData[0];
//        String path = splitData[1];
//        byte[] recordData = splitData[2].getBytes();
//        try {
//            id = new String(encryptionHelper.decrypt(splitData[0].getBytes()));
//            path = new String(encryptionHelper.decrypt(splitData[1].getBytes()));
//            recordData = encryptionHelper.decrypt(splitData[2].replaceAll("_:_", ":").getBytes());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return Record.getRecord(id, path, recordData);
//    }

    public String toString(){
        return "id:" + this.id
                + ", path:" + this.path
                +", data:" + new String(this.data);
    }

    /**
     * Get the bytes, safe for transfer and parsing
     * @return
     */
    public byte[] getBytes() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] splitBytes = "::".getBytes();
        byte[] endBytes = ":::".getBytes();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (byte b : this.data) {
            if (b == ':') {
                bos.write('_');
                bos.write(':');
                bos.write('_');
            } else {
                bos.write(b);
            }
        }

        try {
            bytes.write(id.getBytes());
            bytes.write(splitBytes);
            bytes.write(path.getBytes());
            bytes.write(splitBytes);
            // Replace any : with _:_ to keep formatting when parsing
            bytes.write(bos.toByteArray());
            bytes.write(endBytes);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                bytes.write(endBytes);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        return bytes.toByteArray();
    }

    public boolean setData(Object object){

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(object);
            oos.close();
            bos.close();

            byte[] data = bos.toByteArray();

            App.printBytes("in Record.setData: ", data);

            this.data = data;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public Object getObjectFromData(){
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(this.data);

            App.printBytes("in Record.getObjectFromData: ", this.data);

            ObjectInputStream ois = new ObjectInputStream(bis);
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Get the data, identical to input from setData
     * @return - Bytes for the current data
     */
    public byte[] getData() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        for (int i = 0; i<this.data.length; i++) {
            if (i<this.data.length-2){
                if (this.data[i] == '_' && this.data[i+1] == ':' && this.data[i+2] == '_'){
                    bos.write(':');
                    i+=2;
                    continue;
                }
            }
            bos.write(this.data[i]);
        }

        return bos.toByteArray();
    }

    public static class InvalidRecordException extends Throwable {
    }
}
