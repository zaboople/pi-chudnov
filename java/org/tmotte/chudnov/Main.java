package org.tmotte.chudnov;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;
import java.math.BigDecimal;

/**
 * Currently does 1,000,000 digits in about 6 seconds on my computer,
 * running at depth 80,000 or so.
 * See Driver2.java for more info on calculation.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        InputParser ip = new InputParser(args);
        System.out.println(
            "Precision: "+ip.precision
            +" - depth: "+ip.depth
            +" - quiet: "+ip.quiet
            +" - cores: "+ip.coreCount
        );
        long startTime = System.currentTimeMillis();
        BigDecimal pi = exec(ip.precision, ip.depth, ip.coreCount);
        debug(pi, ip.quiet, System.currentTimeMillis() - startTime);
    }

    static class InputParser {
        int precision = 3000;
        int depth = 10000;
        int coreCount = 4;
        boolean quiet = false;

        InputParser(String[] args) {
            for (int i=0; i<args.length; i++) {
                String str = args[i];
                try {
                    switch (str) {
                        case "-p":
                            precision = Integer.parseInt(args[++i]);
                            break;
                        case "-d":
                            depth = Integer.parseInt(args[++i]);
                            break;
                        case "-c":
                            coreCount = Integer.parseInt(args[++i]);
                            break;
                        case "-q":
                        case "-quiet":
                        case "--quiet":
                            quiet = true;
                            break;
                        case "-h":
                        case "-help":
                        case "--help":
                            help();
                            System.exit(1);
                            return;
                        default:
                            throw new RuntimeException("Don't know what to do with: "+str);
                    }
                } catch (Exception e) {
                    System.err.println("User error: "+e.getMessage());
                    help();
                    System.exit(1);
                }
            }
        }
        private void help() {
            System.out.println(
                "\n"
                +"Usage: \n"
                +"  -q: Quiet mode - doesn't print result, just verifies against built-in million-digit reference point\n"
                +"  -p <precision>: Decimal places of precision to try for\n"
                +"  -d <depth>: Depth - larger is more accurate...\n"
                +"  -c <core count>: Defaults to assuming 4 cores \n"
                +"  You will typically need to add 10,000 depth for every 141,816 digits of accuracy desired. "
            );
        }
    }

    private static BigDecimal exec(int precision, int depth, int coreCount) throws Exception {
        // Running for four cores, meh:
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(
            coreCount, coreCount,
            30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(coreCount * 3)
        );
        try {
            return new Driver2(tpe, precision + 2)
                .computePI(depth, coreCount);
        } finally {
            tpe.shutdown();
        }
    }

    private static void debug(BigDecimal result, boolean quiet, long time) throws Exception {
        // Up to two digits of wrong consistently at end, so chop those.
        String strResult = result.toString();
        if (!quiet)
            System.out.println(strResult);
        String origMillions;
        final byte[] strBytes = new byte[1024 * 1024 * 10];
        try (java.io.InputStream istr = new Main().getClass()
            .getResourceAsStream("/org/tmotte/chudnov/pi_million.txt")
            ) {
            int didRead = istr.read(strBytes, 0, strBytes.length);
            origMillions = new String(strBytes, java.nio.charset.StandardCharsets.UTF_8);
        }

        System.out.println("Took "+(time/1000L)+" seconds");
        final int lenCrawl = Math.min(strResult.length(), origMillions.length());
        for (int i=0; i<lenCrawl; i++) {
            char c1 = origMillions.charAt(i), c2 = strResult.charAt(i);
            if (c1 != c2) {
                System.out.println("Correct up to: "+(i-2)+" digits");
                return;
            }
        }
        System.out.println("PERFECT");
    }
}
