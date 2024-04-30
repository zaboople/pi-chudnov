package org.tmotte.chudnov;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;
import java.math.BigDecimal;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Currently does 1,000,000 digits in about 6 seconds on my computer,
 * running at depth 80,000 or so.
 * Does 10,000,000 in about 212 seconds, depth 800,000.
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
            +" - pi file: "+(ip.piFile==null ?"<internal>" :ip.piFile)
        );
        long startTime = System.currentTimeMillis();
        BigDecimal pi = exec(ip.precision, ip.depth, ip.coreCount);
        debug(pi, ip, System.currentTimeMillis() - startTime);
    }

    static class InputParser {
        int precision = 3000;
        int depth = 10000;
        int coreCount = 4;
        boolean quiet = false;
        String piFile = null;

        InputParser(String[] args) {
            for (int i=0; i<args.length; i++) {
                String str = args[i];
                try {
                    switch (str) {
                        case "-p":
                        case "-precision":
                        case "--precision":
                            precision = Integer.parseInt(args[++i]);
                            break;
                        case "-d":
                        case "-depth":
                        case "--depth":
                            depth = Integer.parseInt(args[++i]);
                            break;
                        case "-c":
                        case "-cores":
                        case "--cores":
                            coreCount = Integer.parseInt(args[++i]);
                            break;
                        case "-q":
                        case "-quiet":
                        case "--quiet":
                            quiet = true;
                            break;
                        case "-f":
                        case "-file":
                        case "--file":
                            piFile = args[++i];
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
                    System.err.println("\nUser error: "+e.getMessage());
                    help();
                    System.exit(1);
                }
            }
        }
        private void help() {
            System.out.println(
                "\n"
                +"Usage: \n"
                +"  -p <precision>: Decimal places of precision to try for\n"
                +"  -d <depth>: Depth - larger is more accurate...\n"
                +"  -q: Quiet mode - doesn't print result, just verifies against built-in million-digit reference\n"
                +"      point\n"
                +"  -c <core count>: Defaults to assuming 4 cores \n"
                +"  -f <pi file>: If you have an existing text file containing digits of pi, for comparing \n"
                +"  \n"
                +"  You will typically need to add 10,000 depth for every 141,816 digits of accuracy desired. \n"
                +"  This automatically checks your pi calculation against an internal file of 1,000,000 digits \n"
                +"  of pi. \n"
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
            return new Driver2(tpe, precision + 2).computePI(depth, coreCount);
        } finally {
            tpe.shutdown();
        }
    }

    private static void debug(BigDecimal result, InputParser ip, long time) {
        System.out.println("Took "+(time/1000L)+" seconds, comparing...");
        final String strResult = result.toString();
        if (!ip.quiet)
            System.out.println(strResult);
        final int resultLen = strResult.length();
        int resultPtr = 0;
        final byte[] strBytes = new byte[1024 * 256];
        try (InputStream istr = getPiFile(ip.piFile)) {
            int didRead = 1;
            while ((didRead = istr.read(strBytes, 0, strBytes.length))!=0) {
                final int compareLen = Math.min(didRead, resultLen - resultPtr);
                if (compareLen==0)
                    break;
                final String subOrig = new String(strBytes, StandardCharsets.UTF_8);
                final String subResult = strResult.substring(resultPtr, resultPtr + compareLen);
                for (int i=0; i<compareLen; i++) {
                    char c1 = subOrig.charAt(i), c2 = subResult.charAt(i);
                    if (c1 != c2) {
                        System.out.println("Correct up to: "+(resultPtr+i-2)+" digits");
                        return;
                    }
                }
                resultPtr += compareLen;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed comparing data: "+e, e);
        }
        System.out.println("PERFECT");
    }
    private static InputStream getPiFile(String piFile) throws Exception {
        return piFile==null
            ?new Main().getClass()
                .getResourceAsStream("/org/tmotte/chudnov/pi_million.txt")
            :new FileInputStream(piFile);
    }
}
