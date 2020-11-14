package DocuStore.data;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Record<E> implements Serializable {
    final public static long serialVersionUID = 10L;
    final public static String HOST = "localhost";
    final public static int PORT = 8081;
    final public static String BASE_PATH = System.getProperty("user.home") + File.separator + "Desktop" + File.separator +"test";
    private static String USER;
    private static String KEY;
    private String id;
    private String path;
    private String user;
    private String key;
    private Map<String, Object> data;
    private E object;

    public Record(String id, String path, Map<String,Object> data, E object) {
        this.user = USER;
        this.key = KEY;
        this.id = id;
        this.path = path;
        this.data = data;
        this.object = object;
    }

    public Record(String id, String path, Map<String,Object> data){
        this(id, path, data, null);
    }

    public Record(String id, String path, E object){
        this(id, path, new HashMap<>(), object);
    }

    public Record(String id, String path){
        this(id, path, new HashMap<>(), null);
    }

    public String getFullPath(){
        return BASE_PATH + File.separator + this.path + File.separator + this.id + ".svbl";
    }

    public E getObject(){
        return this.object;
    }

    public void put(String key, Object value) {
        this.data.put(key, value);
    }

    public Object get(String key){
        return this.data.get(key);
    }

    public String toString(){
        return "id:" + this.id
                + ", path:" + this.path
                +", DocuStore.data:" + this.data.toString()
                + ", obj: " + String.valueOf(this.object);
    }
}
