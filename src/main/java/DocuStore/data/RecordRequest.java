package DocuStore.data;

import DocuStore.db.EncryptionHelper;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class RecordRequest implements Serializable {
    final public static long serialVersionUID = 11L;
    final private String user,key, path, id;
    private static EncryptionHelper encryptionHelper = EncryptionHelper.getInstance();

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

    public byte[] getBytes(){
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] splitBytes = "::".getBytes();
        byte[] endBytes = ":::".getBytes();

        try {
            bytes.write(id.getBytes());
            bytes.write(splitBytes);
            bytes.write(path.getBytes());
            bytes.write(endBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bytes.toByteArray();
    }

    public String toString(){
        return "id:" + this.id
                + ", path:" + this.path;
    }

    public String getId() {
        return this.id;
    }

    public String getPath() {
        return this.path;
    }

}
