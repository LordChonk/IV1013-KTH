import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;


//Much of this code is borrowed from Hiddec.java
public class Hidenc {

    //Initialize some important variables
    static byte[] KEY = null;
    static byte[] CTR = null;
    static int OFFSET = -1;
    static String INPUT_PATH = null;
    static String OUTPUT_PATH = null;
    static String TEMPLATE_PATH = null;
    static int SIZE = -1;
    static byte[] INPUT_DATA = null;
    static byte[] CONTAINER_DATA = null;
    static Cipher cipher;

    //Parse and validate command line arguments
    private static void parseValArgs(String[] args) {
        if (args.length < 4 || args.length > 7) {
            System.err.println("Usage: java Hidenc --key=KEY [--ctr=CTR] --input=INPUT_FILE --output=OUTPUT_FILE --offset=OFFSET [--template=TEMPLATE | --size=SIZE]");
            System.exit(1);
        }
            for (String arg : args) {
                String[] parts = arg.split("=");
                switch (parts[0]) {
                    case "--key":
                        KEY = hexToByte(parts[1]);
                        break;
                    case "--ctr":
                        CTR = hexToByte(parts[1]);
                        break;
                    case "--offset":
                        OFFSET = Integer.parseInt(parts[1]);
                        if (OFFSET < 0 || OFFSET % 16 != 0) {
                            System.err.println("Invalid offset value. Offset must be a non-negative integer divisible by 16.");
                            System.exit(1);
                        }
                        break;
                    case "--input":
                        INPUT_PATH = parts[1];
                        break;
                    case "--output":
                        OUTPUT_PATH = parts[1];
                        break;
                    case "--template":
                        TEMPLATE_PATH = parts[1];
                        break;
                    case "--size":
                        SIZE = Integer.parseInt(parts[1]);
                        if (SIZE <= 0 || SIZE % 16 != 0) {
                            System.err.println("Invalid size value. Size must be a positive integer divisible by 16.");
                            System.exit(1);
                        }
                        break;
                    default:
                        System.err.println("Argument read error: " + arg);
                        System.exit(1);
                }
            }
            if (KEY == null || INPUT_PATH == null || OUTPUT_PATH == null) {
                System.out.println("Please provide required arguments.");
                System.out.println("Usage: java Hidenc --key=KEY [--ctr=CTR] --offset=OFFSET --input=INPUT_FILE --output=OUTPUT_FILE [--template=TEMPLATE_FILE | --size=SIZE]");
                System.exit(1);
            }
            if (TEMPLATE_PATH != null && SIZE != -1) {
                System.out.println("Only one of --template and --size can be specified.");
                System.exit(1);
            }
        }


    // Read input file
    private static void readInFile(String path) {
        try {
            INPUT_DATA = Files.readAllBytes(Paths.get(path));
        } catch (Exception e) {
            System.err.println("Input file read error " + e.getMessage());
            System.exit(1);
        }
    }

    // Initialize the cipher so we can use ECB or CTR mode, however this time we have ENCRYPT_MODE
    // instead of DECRYPT_MODE
    private static void initCipher(byte[] key) {
        try {
            cipher = Cipher.getInstance(CTR != null ? "AES/CTR/NoPadding" : "AES/ECB/NoPadding");
            SecretKeySpec secKey = new SecretKeySpec(key, "AES");

            if (CTR != null) {
                IvParameterSpec ivSpec = new IvParameterSpec(CTR);
                cipher.init(Cipher.ENCRYPT_MODE, secKey, ivSpec);
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, secKey);
            }
        } catch (Exception e) {
            System.err.println("Cipher initialization failed: " + e.getMessage());
            System.exit(1);
        }
    }

    //Compute MD% hash
    private static byte[] computeMD5Hash(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(data);
        } catch (Exception e) {
            System.err.println("MD5 hash computation failed: " + e.getMessage());
            System.exit(1);
        }
        return null;
    }

    //Create blob
    private static byte[] createBlob(byte[] data, byte[] hash){
        byte[] dataHash = computeMD5Hash(data);
        byte[] blob = new byte[data.length + 2 * hash.length + dataHash.length];
        System.arraycopy(hash, 0, blob, 0, hash.length);
        System.arraycopy(data, 0, blob, hash.length, data.length);
        System.arraycopy(hash, 0, blob, hash.length + data.length, hash.length);
        System.arraycopy(dataHash, 0, blob, hash.length + data.length + hash.length, dataHash.length);
        return blob;
    }

    //Encrypt blob
    private static byte[] encryptBlob(byte[] blob){
        try{
            return cipher.doFinal(blob);
        } catch (Exception e){
            System.err.println("Blob encryption failed: " + e.getMessage());
            System.exit(1);
        } return null;
    }

    //Generate container data
    private static byte[] genContainerData(int size){
        byte[] container = new byte[size];
        SecureRandom rand = new SecureRandom();
        rand.nextBytes(container);
        return container;
    }

    //Embed blob into container
    private static void embedBlob(byte[] blob, byte[] container, int offset){
        System.arraycopy(blob, 0, container, offset, blob.length);
    }

    //Write container data to output file
    private static void writeData(byte[] container, String path){
        try(FileOutputStream out = new FileOutputStream(path)){
            out.write(container);
        } catch (Exception e){
            System.err.println("File write error: " + e.getMessage());
            System.exit(1);
        }
    }

    // Converts our hex string to byte array
    private static byte[] hexToByte(String hexString) {
        int length = hexString.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    public static void main(String[] args) {
        parseValArgs(args);
        readInFile(INPUT_PATH);
        initCipher(KEY);

        byte[] computedHash = computeMD5Hash(KEY);
        byte[] blob = createBlob(INPUT_DATA, computedHash);
        byte[] encryptedBlob = encryptBlob(blob);

        if (OFFSET == -1) {
            SecureRandom rand = new SecureRandom();
            int maxOffset = SIZE - encryptedBlob.length;
            OFFSET = rand.nextInt(maxOffset / 16) * 16;
        }

        if(TEMPLATE_PATH != null){
            try {
                CONTAINER_DATA = Files.readAllBytes(Paths.get(TEMPLATE_PATH));
            } catch (Exception e){
                System.err.println("Template file read error: " + e.getMessage());
                System.exit(1);
            }
        } else {
            CONTAINER_DATA = genContainerData(SIZE);
        }

        embedBlob(encryptedBlob, CONTAINER_DATA, OFFSET);
        writeData(CONTAINER_DATA, OUTPUT_PATH);
    }
}
