import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;

public class Hiddec {
    static byte[] KEY = null;
    static byte[] CTR = null;
    static String INPUT_PATH = null;
    static String OUTPUT_PATH = null;
    static byte[] INPUT_DATA = null;
    static byte[] DECRYPTED_DATA = null;
    static Cipher cipher;

    // Parse and validate command line arguments
    private static void parseValArgs(String[] args) {
        if (args.length != 3 && args.length != 4) {
            System.err.println("Usage: java Hiddec --key=KEY [--ctr=CTR] --input=INPUT_FILE --output=OUTPUT_FILE");
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
                case "--input":
                    INPUT_PATH = parts[1];
                    break;
                case "--output":
                    OUTPUT_PATH = parts[1];
                    break;
                default:
                    System.err.println("Argument read error: " + arg);
                    System.exit(1);
            }
        }
        if (KEY == null || INPUT_PATH == null || OUTPUT_PATH == null) {
            System.out.println("Please provide required arguments. Note that arguments inside [] are optional");
            System.out.println("Usage: java Hiddec --key=KEY [--ctr=CTR] --input=INPUT_FILE --output=OUTPUT_FILE");
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

    // Initialize the cipher so we can use ECB or CTR mode
    private static void initCipher(byte[] key) {
        try {
            cipher = Cipher.getInstance(CTR != null ? "AES/CTR/NoPadding" : "AES/ECB/NoPadding");
            SecretKeySpec secKey = new SecretKeySpec(key, "AES");

            if (CTR != null) {
                IvParameterSpec ivSpec = new IvParameterSpec(CTR);
                cipher.init(Cipher.DECRYPT_MODE, secKey, ivSpec);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, secKey);
            }
        } catch (Exception e) {
            System.err.println("Cipher initialization failed: " + e.getMessage());
            System.exit(1);
        }
    }

    // Compute MD5 Hash
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

    // Find and decrypt data
    public static byte[] findBlob(byte[] key, byte[] input, byte[] hash) throws Exception {
        byte[] data;
        initCipher(key);
        for (int i = 0; i < input.length; i += 16) {
            data = cipher.doFinal(Arrays.copyOfRange(input, i, input.length));
            if (matchCheck(data, hash, 0))
                return dataIntegrity(data, hash);
        } throw new Exception("Data not found");
    }

    // Verify data Integrity
    private static byte[] dataIntegrity(byte[] data, byte[] hash) throws Exception {
        int hashLength = hash.length;
        byte[] extractedData;

        for (int offset = hashLength; offset < data.length; offset++) {
            if (matchCheck(data, hash, offset)) {
                extractedData = Arrays.copyOfRange(data, hashLength, offset);
                int start = offset += hashLength;
                int end = start + hashLength;
                byte[] hashedData = Arrays.copyOfRange(data, start, end);

                if (Arrays.equals(computeMD5Hash(extractedData), hashedData)) {
                    return extractedData;
                } else {
                    System.err.println("Extracted data does not match verification data");
                    System.exit(-1);
                }
            }
        }
        throw new Exception("No data found");
    }

    private static boolean matchCheck(byte[] data, byte[] hash, int offset) {
        for (int i = 0; i < hash.length; i++) {
            if (data[offset + i] != hash[i]) {
                return false;
            }
        }
        return true;
    }

    // Write decrypted data to output file
    private static void writeData(byte[] data, String path) {
        try {
            Files.write(Paths.get(path), data);
        } catch (Exception e) {
            System.err.println("Error writing to file: " + e.getMessage());
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

    // Converts byte array to hex string for debugging purposes
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void main(String[] args){
        parseValArgs(args);
        readInFile(INPUT_PATH);
        byte[] computedHash = computeMD5Hash(KEY);
        try {
            DECRYPTED_DATA = findBlob(KEY, INPUT_DATA, computedHash);
        } catch (Exception e) {
            System.err.println("Decryption failed");
            System.exit(1);
        }
        writeData(DECRYPTED_DATA, OUTPUT_PATH);
    }

}




