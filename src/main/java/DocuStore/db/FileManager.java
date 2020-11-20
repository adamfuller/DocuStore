package DocuStore.db;

import DocuStore.App;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class FileManager {
    final private static String BASE_PATH = System.getProperty("user.home") + File.separator + "Desktop" + File.separator +"test";
    private static final SafeSet<String> fileStores = new SafeSet<>();
    private static final SafeMap<String, Integer> fileReads = new SafeMap<>();

    private static String makeSafe(String filePath){
        String sep = File.separator;
        // Replace all current filepath separators with current filesystem ones
        filePath = filePath.replaceAll("[/\\\\]", sep);
        // Remove any . to prevent dot notation funny business
        return filePath.replace(".", "");
    }

    private static String makeSafe(String id, String path){
        String safeFilePath = makeSafe(path);
        if (!safeFilePath.endsWith(File.separator)){
            safeFilePath += File.separator;
        }
        if (!safeFilePath.startsWith(File.separator)){
            safeFilePath = File.separator + safeFilePath;
        }
        String safeId = makeSafe(id);
        return BASE_PATH + safeFilePath + safeId + ".svbl";
    }

    public static boolean store(String id, String path, byte[] data){
        try{
            String filePath = makeSafe(id, path);
            System.out.println("Storing at: "  + filePath);

            App.printBytes("in FileManager.store: ", data);

            while (true){
                if ((!fileReads.containsKey(filePath) || fileReads.get(filePath) == 0 ) && !fileStores.add(filePath)){
                    Thread.sleep(50);
                    continue;
                }
                File f = new File(filePath);
                if (!f.getParentFile().exists()){
                    if (!f.getParentFile().mkdirs()){
                        return false;
                    }
                }
                FileOutputStream fileOutputStream = new FileOutputStream(f);
                fileOutputStream.write(data);
                fileOutputStream.flush();
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

    public static byte[] fetch(String id, String path){
        // TODO: From id and path
        String filePath = makeSafe(id, path);
        System.out.println("Fetch request from " + filePath);
        File f = new File(filePath);
        byte[] output = new byte[0];
        if (!f.exists()) {
            return null;
        }
        while (true) {
            if (fileStores.contains(filePath)){
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
                output = fileInputStream.readAllBytes();

                App.printBytes("in FileManager.fetch: ", output);

                fileInputStream.close();
                // Decrement the value
                fileReads.apply(filePath, (val) -> val--, 1);
                // Remove the value if it's zero now
                fileReads.removeIf(filePath, (v) -> v == 0);
                return output;
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
