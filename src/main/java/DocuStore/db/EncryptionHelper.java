package DocuStore.db;

import DocuStore.data.Record;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class EncryptionHelper {
    private static final String RSA = "RSA";
    private KeyPair KEY_PAIR;
    private final String PRI_KEY_LOCATION;
    private final String PUB_KEY_LOCATION;
    public static EncryptionHelper instance = new EncryptionHelper();
    private static final int KEY_SIZE = 4096;

    private EncryptionHelper(){
        PRI_KEY_LOCATION = Record.BASE_PATH + File.separator + "RSA_PRI_KEY.txt";
        PUB_KEY_LOCATION = Record.BASE_PATH + File.separator + "RSA_PUB_KEY.txt";

//        init();
    }

    private void init(){
        File privateKeyFile = new File(PRI_KEY_LOCATION);
        File publicKeyFile = new File(PUB_KEY_LOCATION);

        if (privateKeyFile.exists() && publicKeyFile.exists()){
//            initFromFiles(publicKeyFile, privateKeyFile);
//            return;
        }

        // Generate new key
        KEY_PAIR = generateRSAKeyPair();
        FileOutputStream fos = null;
        try{
            if (!privateKeyFile.getParentFile().exists()){
                privateKeyFile.getParentFile().mkdirs();
            }
            fos = new FileOutputStream(privateKeyFile);
            fos.write(KEY_PAIR.getPrivate().getEncoded());
            fos.flush();
            fos.close();

            fos = new FileOutputStream(publicKeyFile);
            fos.write(KEY_PAIR.getPublic().getEncoded());
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initFromFiles(File publicKeyFile, File privateKeyFile) {
        try {
            FileInputStream pubKeyStream = new FileInputStream(publicKeyFile);
            byte[] publicKeyBytes = pubKeyStream.readAllBytes();

            FileInputStream privateKeyStream = new FileInputStream(privateKeyFile);
            byte[] privateKeyBytes = pubKeyStream.readAllBytes();

            PublicKey publicKey = getPublicKeyFromBytes(publicKeyBytes);
            PrivateKey privateKey = getPrivateKey(privateKeyBytes);

            this.KEY_PAIR = new KeyPair(publicKey, privateKey);

        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    public synchronized static EncryptionHelper getInstance(){
        if (instance == null){
            instance = new EncryptionHelper();
        }
        return instance;
    }

    private KeyPair generateRSAKeyPair() {
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(RSA);
            keyPairGen.initialize(KEY_SIZE, new SecureRandom());
            return keyPairGen.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private PrivateKey getPrivateKey(byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes, "RSA");
        KeyFactory fact = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) fact.generatePrivate(keySpec);
    }

    private PublicKey getPublicKeyFromBytes(byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes, "RSA");
        KeyFactory fact = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) fact.generatePublic(keySpec);
    }

    public byte[] decrypt(byte[] data) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        return this.do_RSADecryption(data, KEY_PAIR.getPrivate());
    }

    public byte[] encrypt(byte[] data) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        return this.do_RSAEncryption(data, KEY_PAIR.getPublic());
    }

    public byte[] getPublicKey() {
        return KEY_PAIR.getPublic().getEncoded();
    }

    // Encryption function which converts
    // the plainText into a cipherText
    // using private Key.
    public byte[] do_RSAEncryption(byte[] data, PublicKey publicKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(RSA);

        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            int i = 0;
            for (i = 0; i < data.length-(KEY_SIZE/8); i += KEY_SIZE / 8) {
                int end = Math.min(i + KEY_SIZE / 8, data.length);
                bos.write(cipher.doFinal(Arrays.copyOfRange(data, i, end)));
            }
            // Copy remaining bytes
            if (i < data.length-(KEY_SIZE/8)){
                bos.write(cipher.doFinal(Arrays.copyOfRange(data, i, data.length)));
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    // Decryption function which converts
    // the ciphertext back to the
    // orginal plaintext.

    /**
     * Perform RSA decryption
     * @param data
     * @param privateKey
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public byte[] do_RSADecryption(byte[] data, PrivateKey privateKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(RSA);

        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            int i = 0;
            for (i = 0; i < data.length-(KEY_SIZE/8); i += KEY_SIZE / 8) {
                int end = Math.min(i + KEY_SIZE / 8, data.length);
                bos.write(cipher.doFinal(Arrays.copyOfRange(data, i, end)));
            }
            // Copy remaining bytes
            if (i < data.length-(KEY_SIZE/8)){
                bos.write(cipher.doFinal(Arrays.copyOfRange(data, i, data.length)));
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    public String convertToHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

}

