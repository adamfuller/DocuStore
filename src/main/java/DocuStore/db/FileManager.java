package DocuStore.db;

import DocuStore.data.Record;
import DocuStore.data.RecordRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileManager {

    private static final SafeSet<String> fileStores = new SafeSet<>();
    private static final SafeMap<String, Integer> fileReads = new SafeMap<>();

    public static boolean store(Record record){
        try{
            String filePath = record.getFullPath();
            while (true){
                if ((!fileReads.containsKey(filePath) || fileReads.get(filePath) == 0 ) && !fileStores.add(filePath)){
                    System.out.println("FiMa Waiting to write");
                    Thread.sleep(100);
                    continue;
                }
                File f = new File(filePath);
//                System.out.println("Going to write to: "+ record.getFullPath());
                if (!f.getParentFile().exists()){
                    f.getParentFile().mkdirs();
                }
                System.out.println("FiMa About to write to file: " + filePath);
                FileOutputStream fileOutputStream = new FileOutputStream(f);
                fileOutputStream.write(record.getBytes());
                fileOutputStream.flush();
                fileOutputStream.close();
                System.out.println("FiMa Wrote : " + new String(record.getBytes()));
                fileStores.remove(filePath);
                break;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static Record fetch(RecordRequest request){
        String filePath = request.getFullPath();
        File f = new File(filePath);
        Record output = null;
        if (!f.exists()) {
            return null;
        }
        System.out.println("FiMa Going to fetch: " + request.getFullPath());
        while (true) {
            if (fileStores.contains(filePath)){
                System.out.println("FiMa Waiting to read");
                try {
                    Thread.sleep(100);
                    continue;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }

            // If the key is present tick it up by 1, if not set it to 1
            fileReads.apply(filePath, (val) -> val++, 1);

            try (FileInputStream fileInputStream = new FileInputStream(f)) {
                byte[] bytes = SocketHandler.readInputStream(fileInputStream);
                output = Record.fromBytes(bytes);
                System.out.println("FiMa Read " + new String(bytes));

                // Decrement the value
                fileReads.apply(filePath, (val) -> val--, 1);
                // Remove the value if it's zero now
                fileReads.removeIf(filePath, (v) -> v == 0);
                break;
            } catch (IOException e) {
                e.printStackTrace();
                // Decrement the value
                fileReads.apply(filePath, (val) -> val--, 1);
                // Remove the value if it's zero now
                fileReads.removeIf(filePath, (v) -> v == 0);
                break;
            }
        }
        return output;
    }
}
