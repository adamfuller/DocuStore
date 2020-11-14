package DocuStore.db;

import DocuStore.data.Record;
import DocuStore.data.RecordRequest;

import java.io.*;

public class FileManager {

    private static SafeSet<String> fileStores = new SafeSet<>();
    private static SafeSet<String> fileReads = new SafeSet<>();

    public static boolean store(Record<?> record){
        try{
            String filePath = record.getFullPath();
            while (true){
                if (!fileReads.contains(filePath) && !fileStores.add(filePath)){
//                    System.out.println("Waiting to write");
                    Thread.sleep(100);
                    continue;
                }
                File f = new File(filePath);
//                System.out.println("Going to write to: "+ record.getFullPath());
                if (!f.getParentFile().exists()){
                    f.getParentFile().mkdirs();
                }
                FileOutputStream fileOutputStream = new FileOutputStream(f);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(record);
                fileOutputStream.close();
                fileStores.remove(filePath);
                break;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static Record<?> fetch(RecordRequest<?> request){
        String filePath = request.getFullPath();
        File f = new File(filePath);
//        System.out.println("Going to fetch: " + request.getFullPath());
        while (true) {
            if (fileStores.contains(filePath)){
//                System.out.println("Waiting to read");
                try {
                    Thread.sleep(100);
                    continue;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }

            fileReads.add(filePath);

            try (FileInputStream fileInputStream = new FileInputStream(f)) {
                ObjectInputStream objectOutputStream = new ObjectInputStream(fileInputStream);
                return (Record<?>) objectOutputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                break;
            } finally {
                fileReads.remove(filePath);
            }
        }
        return null;
    }
}
