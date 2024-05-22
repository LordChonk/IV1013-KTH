import java.util.Random;

public class MyRandom extends Random {

    private long seed;  // This is our X0
    private final long m = 2_147_483_647; // Prime
    private final long a = 16_807; // Primitive root of m


    public MyRandom(long seed) {
        setSeed(seed);
    }

    @Override
    protected int next(int bits) {
        if (bits <= 0 || bits > 32) {
            System.err.println("Invalid bits: " + bits + "Bits must be between 1 and 32");
            System.exit(1);
        }
        seed = (a * seed) % m;
        return (int)(seed >>> (31 - bits));
    }

    @Override
    public void setSeed(long seed) {
        if(seed == 0){
            System.err.println("Invalid seed: " + seed + "Seed must be strictly greater than 0");
            System.exit(1);
        }
        //System.out.println("Before setSeed: seed=" + seed + ", a=" + a + ", m=" + m);
        this.seed = (seed ^ a) % m;
        //System.out.println("After setSeed: computed seed=" + this.seed);
    }
}
