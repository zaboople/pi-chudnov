package org.tmotte.chudnov;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
    The original binary-splitting algorithm is recursive; even though that recursion
    is only log2(n), I've de-recursed (for the heck of it) and thus use an in-memory
    stack as a substitute for the recursive call stack. Still, actual memory pressure
    is going to come from very large numbers, not the stack size itself.

    I've also made this multi-threaded, which is... somewhat helpful? If you think of
    the calculations as a binary tree, the leaf nodes are easiest, with things getting
    harder as you go up. The nastiest of all is the final calculation, and guess what?

    We have to find the square root of 10005 *to the desired precision*, and that is
    a hard problem in its own right! Without multi-threading that item, a lot of our time
    gets spent dealing with it, and... argh. I've built an improved sqrt() though, using
    old-timey Heron's Method, takes about 1/3 java's version.
*/
public class Driver2 {
    private final ExecutorService execService;
    private final int precision;

    public Driver2(ExecutorService execService, int precision) {
        this.execService = execService;
        this.precision = precision+2;
    }

    public BigDecimal computePI(int depth, int coreCount) throws Exception {
        final Future<BigDecimal> sqrt10005 = execService.submit(
            ()-> computeSqrt(10005)
        );
        final Triple tr = compute(TreeNode.makeRoot(1, depth), coreCount);
        System.out.println("Is square root ready yet... ?");
        BigDecimal pi = computeFinal(tr, sqrt10005.get(), precision);
        System.out.println("ALL DONE");
        return pi;
    }

    /** This only computes the necessary triple for an arbitrary range.
        It would be useful when running as a network node in some
        sorta distributed super-duper-computing env. */
    public Triple compute(TreeNode topNode, int coreCount) {
        final ArrayList<TreeNode> stack = new ArrayList<>();
        stack.add(topNode);
        while (!stack.isEmpty()) {
            final TreeNode node = stack.remove(stack.size()-1);
            if (isLeaf(node)) {
                node.parent.setTriple(
                    computeLeaf(node.low, node.high), node.isLeft
                );
                node.nullify();
            } else if (node.hasTriples()) {
                final Triple tr = computeNode(node.leftTriple, node.rightTriple);
                if (node.parent==null) {
                    node.nullify();
                    return tr; // EARLY RETURN
                }
                node.parent.setTriple(tr, node.isLeft);
                node.nullify();
            } else {
                // Push node, create its children, and push them too; however,
                // if we have remaining threads in our thread pool, we'll
                // fork them off and wait for them, blocking the current thread.
                stack.add(node);
                final long mid = (node.low + node.high) / 2;
                final TreeNode
                    leftNode = new TreeNode(node.low, mid, node, true),
                    rightNode = new TreeNode(mid, node.high, node, false);
                if (coreCount <= 1) {
                    stack.add(leftNode);
                    stack.add(rightNode);
                } else {
                    final int t = coreCount / 2;
                    coreCount = 0;
                    submit(
                        ()->compute(leftNode, t), ()->compute(rightNode, t)
                    );
                }
            }
        }
        System.out.println("DONE: "+topNode+" ");
        return null;
    }

    private void submit(Runnable a, Runnable b) {
        System.out.println("FORK TWO ");
        Future<?> w1 = execService.submit(a);
        Future<?> w2 = execService.submit(b);
        try {
            w1.get();
            w2.get();
        } catch (Exception e) {
            throw new RuntimeException("Failed on wait ", e);
        }
    }

    private boolean isLeaf(TreeNode t) {
        return t.high == t.low + 1;
    }


    //////////////
    // COMPUTE: //
    //////////////

    private static MathContext mc(int i) {
        return new MathContext(i, RoundingMode.FLOOR);
    }

    public BigDecimal computeSqrt(int number) throws Exception {
        // Heron's iterative method:
        //     N.next = (N + (squared / N)) / 2
        final BigDecimal diffAllowed = BigDecimal.valueOf(1, precision-1);
        final BigDecimal two = new BigDecimal(2, mc(0));
        final BigDecimal squared = new BigDecimal(number, mc(0));
        int prec = 16;
        double remains = precision + 10;
        BigDecimal guess = new BigDecimal(Math.sqrt(number), mc(prec));
        while (remains > 1) {
            System.out.println("Sqrt Remains: "+((long)remains)+" precision: "+prec);
            final MathContext mcprec = mc(prec);
            BigDecimal thisGuess = guess.add(
                squared.divide(guess, mcprec)
            ).divide(two, mcprec);
            if (prec < precision) {
                prec *= 2;
                if (prec > precision)
                    prec = precision;
            } else if (
                thisGuess.subtract(guess).abs().compareTo(diffAllowed)<= 0
                ) {
                remains=1.0d; // Short-circuit
            }
            guess = thisGuess;
            remains /= 2.0d;
        }
        System.out.println("SQRT done ");
        return guess;
    }

    final static BigInteger
        big109 = big(10939058860032000L),
        big545 = big(545140134),
        big135 = big(13591409);
    private Triple computeLeaf(long a, long b) {
        /* Original:
            Pab = -(6*a - 5) * (2*a - 1) * (6*a - 1)
            Qab = 10939058860032000 * a**3
            Rab = Pab * (545140134*a + 13591409)
            return Pab, Qab, Rab
        */
        final long a6 = a * 6L;
        final BigInteger
            one = big(-(a6 - 5L)),
            two = big((2L * a) - 1L),
            three = big(a6 - 1L);
        final BigInteger
            pab =  one.multiply(two).multiply(three),
            abig = big(a);
        final BigInteger
            qab = big109.multiply(abig.pow(3)),
            rab = pab.multiply(
                big545.multiply(abig).add(big135)
            );
        return new Triple(pab, qab, rab);
    }
    private static Triple computeNode(Triple ta, Triple tb) {
        /* Original:
        pa, qa, ra = tripleA
        pb, qb, rb = tripleB
        return (
            pa * pb,
            qa * qb,
            qb * ra + pa * rb
        )
        */
        return new Triple(
            ta.p.multiply(tb.p),
            ta.q.multiply(tb.q),
            tb.q.multiply(ta.r).add(
                ta.p.multiply(tb.r)
            )
        );
    }

    private BigDecimal computeFinal(
            Triple triple, BigDecimal sqrt10005, int precision
        ) {
        /** Original
            (426880 * Q1n * sqrt(10005)) / (13591409 * Q1n + R1n)
        **/
        System.out.println("Final run proceeding...");
        final MathContext m = new MathContext(precision);
        final BigInteger qi = triple.q;
        final BigInteger ri = triple.r;
        triple.p = triple.q = triple.r = null; // Dumb, but, memory!
        System.out.print("Upper/Lower... ");
        final Future<BigDecimal>
            fupper = execService.submit(()->
                new BigDecimal(
                        big(426880).multiply(qi), m
                    ).multiply(sqrt10005)
            ),
            flower = execService.submit(()->
                new BigDecimal(
                    big(13591409).multiply(qi).add(ri), m
                )
            );
        try {
            final BigDecimal upper = fupper.get(), lower = flower.get();
            System.out.println("Here comes... ");
            return upper.divide(lower, precision, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new RuntimeException("Failure at the very end: "+e, e);
        }
    }

    private static BigInteger big(long x) {
        return BigInteger.valueOf(x);
    }

}
