package DocuStore.data;

import java.io.Serializable;

public class RecordRequest<E> implements Serializable {
    final public static long serialVersionUID = 10L;
    final private String user,key, path, id;
    final private Record<E> record;

    public RecordRequest(String user, String key, String path, String id){
        this.user = user;
        this.key = key;
        this.path = path;
        this.id = id;
        this.record = null;
    }

    public Record<E> getRecord(){
        return record;
    }

    public String getFullPath(){
        return Record.BASE_PATH + "\\" + this.path + "\\" + this.id + ".svbl";
    }

    public String toString(){
        return "id:" + this.id
                + ", path:" + this.path
                + ", obj: " + String.valueOf(this.record);
    }
}
