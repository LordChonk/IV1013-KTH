import java.math.BigInteger;
import java.util.Random;

public class MyRandom extends Random {
    private byte[] Sbox = new byte[256];
    private int i = 0, j = 0;


    public MyRandom(BigInteger key) {
        setKey(key);
    }

    private void setKey(BigInteger key) {
        if (key.equals(BigInteger.ZERO)){
            System.err.println("Key is zero, use a more secure key");
            System.exit(1);
        }
        //S-box init
        for (int index = 0; index < 256; index++) {
            Sbox[index] = (byte) index;
        }

        int j = 0;

        //BigInteger to byte array
        byte[] keyBytes = key.toByteArray();

        for (int index = 0; index < 256; index++) {
            j = (j + Sbox[index] + (keyBytes[index % keyBytes.length] & 0xFF)) % 256;
            if (j < 0){
                j += 256;
            }
            //Swap values
            byte temp = Sbox[index];
            Sbox[index] = Sbox[j];
            Sbox[j] = temp;
        }
    }


    @Override
    protected int next(int bits) {
        if (bits != 8) {
            System.err.println("Invalid bits: " + bits + ". Bits must be exactly 8");
            System.exit(1);
        }

        i = (i + 1) % 256;
        j = (j + Sbox[i] + 256) % 256;

        //Swap values
        byte temp = Sbox[i];
        Sbox[i] = Sbox[j];
        Sbox[j] = temp;

        //Return next word
        int nxtWrd = Sbox[((Sbox[i] & 0xFF) + (Sbox[j] & 0xFF)) % 256] & 0xFF;
        return nxtWrd;
    }


}

