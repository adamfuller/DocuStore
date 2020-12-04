package DocuStore.db;

import DocuStore.App;

import java.io.*;
import java.util.Arrays;

public class FileManager {
    final private static String BASE_PATH = System.getProperty("user.home") + File.separator + "Desktop" + File.separator +"test";
    private static final SafeSet<String> fileStores = new SafeSet<>();
    private static final SafeMap<String, Integer> fileReads = new SafeMap<>();
    private static final byte[] NONEXISTENT_FILE_CONTENTS = " ".getBytes();

    private static String makeSafe(String filePath){
        if (filePath == null){
            return null;
        }

        System.out.println("Making safe: " + filePath);
        String sep = File.separator;
        // Replace all current filepath separators with current filesystem ones
        filePath = filePath.replace("/", sep).replace("\\", sep);

        // Remove any . to prevent dot notation funny business
        return filePath.replace("..", "");
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
        return BASE_PATH + safeFilePath + (id != null ? (safeId + ".svbl") : "");
    }

    public static boolean store(String id, String path, byte[] data){
        System.out.println("in FileManager.store: " + fileReads + ", " + fileStores);
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

    private static byte[] fetch(File file){
        String filePath = file.getPath();
        byte[] output = new byte[0];

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

            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                output = fileInputStream.readAllBytes();

//                App.printBytes("in FileManager.fetch: ", output);

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

    public static byte[] fetch(String id, String path){
        System.out.println("in FileManager.fetch: " + fileReads + ", " + fileStores);
        if ( (id == null || id.trim().length() == 0) && path != null ){
            File p = new File(makeSafe(null, path));
            System.out.println("Files (" + p.getAbsolutePath() + "): " + Arrays.toString(p.list()));
            String[] files = p.list((dir, name) -> name.endsWith(".svbl"));
            if (files == null){
                System.out.println("Files were null, " + p.getAbsolutePath());
                return NONEXISTENT_FILE_CONTENTS;
            }

            System.out.println("Doing multi-fetch request");
            for (int i=0; i<files.length; i++){
                files[i] = p.getAbsolutePath() + File.separator + files[i];
            }
            return fetchMultiple(files);
        }
        String filePath = makeSafe(id, path);
        System.out.println("Fetch request from " + filePath);
        File f = new File(filePath);
        if (!f.exists()) {
            return NONEXISTENT_FILE_CONTENTS;
        }
        return fetch(f);
    }

    private static byte[] fetchMultiple(String... filePaths) {
        System.out.println("in fetchMultiple " + Arrays.toString(filePaths));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] separator = "::".getBytes();
        for (String path : filePaths){
//            String filePath = makeSafe(null, path);
            File file = new File(path);
            System.out.println("About to parse file: " + file.getAbsolutePath());
            if (!file.exists()){
                try {
                    output.write(NONEXISTENT_FILE_CONTENTS);
                    output.write(separator);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                continue;
            }

            try{
                output.write(fetch(file));
                output.write(separator);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return output.toByteArray();
    }
}
