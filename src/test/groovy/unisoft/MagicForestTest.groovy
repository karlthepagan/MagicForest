package unisoft

import spock.lang.Unroll

import java.util.concurrent.TimeUnit

/**
 * Created by karl on 6/7/14.
 */
class MagicForestTest extends spock.lang.Specification {
    static def SECONDS_IN_NANOS = 1.0 / TimeUnit.SECONDS.toNanos(1)
    @Unroll
    def "FindStableForests #goats,#wolves,#lions -> #count #winner in < #time seconds"() {
        when:
        def initialForest = new MagicForest.Forest(goats, wolves, lions)

        then:
        long t0 = System.nanoTime()
        def stableForests = MagicForest.findStableForests(initialForest)
        double elapsed = (System.nanoTime() - t0) * SECONDS_IN_NANOS
        stableForests[0] != 0
        stableForests[0][winner] == count
        elapsed < time // TODO caliper
        println("${goats},${wolves},${lions} -> ${count} ${winner} win in ${elapsed}s")

        where:
        goats   | wolves    | lions     || winner   | count | time
        17      | 55        | 6         || 'lions'  | 23    | 0.11
        117     | 155       | 106       || 'lions'  | 223   | 0.32
        217     | 255       | 206       || 'lions'  | 423   | 1.1
        83      | 454       | 690       || 'goats'  | 537   | 6.8
    }
}
