package DocuStore.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class RecordRequest implements Serializable {
    final public static long serialVersionUID = 11L;
    final private String user,key, path, id;

    public RecordRequest(String user, String key, String path, String id){
        this.user = user;
        this.key = key;
        this.path = path;
        this.id = id;
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

    public static RecordRequest fromBytes(byte[] data) {
        String dataString = new String(data);
        String[] splitData = dataString.replace(":::", "").split("::");
        if (splitData.length != 2){
            return null;
        }
        String id = splitData[0];
        String path = splitData[1];
        return new RecordRequest("user", "key", path, id);
    }

    public String getFullPath(){
        return Record.BASE_PATH + File.separator + this.path + File.separator + this.id + ".svbl";
    }

    public byte[] getBytes(){
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] splitBytes = "::".getBytes();
        byte[] endBytes = ":::".getBytes();

        try {
            bytes.write(id.getBytes());
            bytes.write(splitBytes);
            bytes.write(path.getBytes());
            bytes.write(endBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytes.toByteArray();
    }

    public String toString(){
        return "id:" + this.id
                + ", path:" + this.path;
    }
}
