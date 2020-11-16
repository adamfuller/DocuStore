package DocuStore.db;

import DocuStore.data.Record;
import DocuStore.data.RecordRequest;

import java.io.*;

public class FileManager {

    private static SafeSet<String> fileStores = new SafeSet<>();
    private static SafeSet<String> fileReads = new SafeSet<>();

    public static boolean store(Record record){
        try{
            String filePath = record.getFullPath();
            while (true){
                if (!fileReads.contains(filePath) && !fileStores.add(filePath)){
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

            fileReads.add(filePath);

            try (FileInputStream fileInputStream = new FileInputStream(f)) {
                byte[] bytes = SocketHandler.readInputStream(fileInputStream);
                output = Record.fromBytes(bytes);
                System.out.println("FiMa Read " + new String(bytes));
                fileReads.remove(filePath);
                break;
            } catch (IOException e) {
                e.printStackTrace();
                fileReads.remove(filePath);
                break;
            }
        }
        return output;
    }
}
