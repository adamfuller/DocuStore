package DocuStore.data;

import DocuStore.App;
import DocuStore.db.EncryptionHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class Record implements Serializable {
    final public static long serialVersionUID = 11L;
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

        try {
            bos.close();
            bytes.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes.toByteArray();
    }

    public boolean setData(Object object){
        if (object == null){
            return false;
        }
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
        byte[] formattedData = this.getData();

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(formattedData);

            App.printBytes("in Record.getObjectFromData: ", formattedData);

            ObjectInputStream ois = new ObjectInputStream(bis);
            ois.close();
            bis.close();
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            App.printBytes("Failed to parse object from data: ", formattedData);
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

        try {
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bos.toByteArray();
    }

    public static class InvalidRecordException extends Throwable {
    }
}
