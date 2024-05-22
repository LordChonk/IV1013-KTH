import java.util.Random;
import java.io.*;

public class StreamCipher {
    //Initialize key and files
    static Long key = null; //key set to null so we can check for valid/invalid keys later
    static String inputFile = null;
    static String outputFile = null;

    public static void main(String[] args) {
        parseArgs(args);
        validateArgs();
        logic(inputFile, outputFile, key);
        System.exit(0); //Program completed successfully
    }

    private static void parseArgs(String[] args) {
        //Command-line args parsing
        if (args.length != 6){
            System.err.println("Unexpected number of arguments. Usage: --key <key> --in <inputFile> --out <outputFile>");
            System.exit(1);
        }
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--key":
                    if (i + 1 < args.length) {
                        key = Long.parseLong(args[++i]);
                    }
                    break;
                case "--in":
                    if (i + 1 < args.length) {
                        inputFile = args[++i];
                    }
                    break;
                case "--out":
                    if (i + 1 < args.length) {
                        outputFile = args[++i];
                    }
                    break;
            }
        }
    }

    //Validate arguments
    private static void validateArgs() throws IllegalArgumentException {
        if (key == null) {
            System.err.println("Error: Missing required argument '--key'. Please provide a valid key.");
            System.exit(1);
        }
        if (inputFile == null) {
            System.err.println("Missing required argument '--in'. Please specify input file");
            System.exit(1);
        }
        if (outputFile == null) {
            System.err.println("Missing required argument '--out'. Please specify output file");
            System.exit(1);
        } //If validation succeeds we do not need to take further action
    }

    // Encryption/decryption logic
    private static void logic(String inputFile, String outputFile, Long key) {
        try { //Set up streams and key
            FileInputStream inStream = new FileInputStream(inputFile);
            FileOutputStream outStream = new FileOutputStream(outputFile);
            Random prng = new Random(key);

            //Handle each byte
            int oneByte;
            while ((oneByte = inStream.read()) != -1) {
                int randByte = prng.nextInt(256);
                outStream.write(oneByte ^ randByte);
            }

            //Close streams
            inStream.close();
            outStream.close();
        }
        //Some final error handling
        catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("IO Error: " + e.getMessage());
            System.exit(1);
        }
    }
}


