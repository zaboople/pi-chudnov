package org.tmotte.chudnov;
import java.math.BigInteger;

public class Triple {
    BigInteger p, q, r;
    public Triple(BigInteger p, BigInteger q, BigInteger r) {
        this.p=p;
        this.q=q;
        this.r=r;
    }
    public String toString() {
        return p.toString() + " ---- " + q.toString() + " ---- "+ r.toString();
    }
}