package unisoft

import spock.lang.Unroll

import java.util.concurrent.TimeUnit

/**
 * Created by karl on 6/7/14.
 */
class MagicForestTest extends spock.lang.Specification {
    static SECONDS_IN_NANOS = 1.0 / TimeUnit.SECONDS.toNanos(1)

    static PARALLEL = [parallel: true, pipelined: false]
    static NORMAL = [parallel: false, pipelined: false]
    static PIPELINED = [parallel: false, pipelined: true]

    @Unroll
    def "FindStableForests #goats,#wolves,#lions -> #count #winner in < #time seconds #mode"() {
        when:
        def universe = new MagicForest(
                [[-1,-1,1],
                [-1,1,-1],
                [1,-1,-1]] as int[][]
        )
        def initialForest = universe.makeForest([goats, wolves, lions] as int[])

        then:
        long t0 = System.nanoTime()
        def stableForests = universe.findStableForests(initialForest,mode.parallel,mode.pipelined)
        double elapsed = (System.nanoTime() - t0) * SECONDS_IN_NANOS
        stableForests[0] != 0
        stableForests[0][winner] == count
        elapsed < time // TODO caliper
        println("${goats},${wolves},${lions} -> ${count} ${winner} win in ${elapsed}s")
        System.out.flush()

        where:
        goats   | wolves    | lions     | mode      || winner   | count | time
        17      | 55        | 6         | NORMAL    || 'lions'  | 23    | 0.10
        117     | 155       | 106       | NORMAL    || 'lions'  | 223   | 0.44
        217     | 255       | 206       | NORMAL    || 'lions'  | 423   | 1.49
        83      | 454       | 690       | NORMAL    || 'goats'  | 537   | 8.8
        83      | 454       | 690       | PARALLEL  || 'goats'  | 537   | 390 // spectacular failure!
        83      | 454       | 690       | PIPELINED || 'goats'  | 537   | 19.5 // much improved, but still 2x slower
        10      | 10        | 11        | NORMAL    || 'lions'  | 21    | 0.09
    }

    @Unroll
    def "FindStableForests #animals -> #winners in < #time seconds"() {
        when:
        def meals = [[-1] * animals] * animals as int[][]
        meals.eachWithIndex{ int[] entry, int i -> entry[i] = 1}
        def universe = new MagicForest(meals)
        def initialPopulation = [40] * animals as int[]
        initialPopulation[-1] += 1
        def initialForest = universe.makeForest(initialPopulation)

        then:
        long t0 = System.nanoTime()
        def stableForests = universe.findStableForests(initialForest,mode.parallel,mode.pipelined)
        double elapsed = (System.nanoTime() - t0) * SECONDS_IN_NANOS
        stableForests.size() == winners
        stableForests.collect{it.sum()}.max() == count
        elapsed < time // TODO caliper
        println("${initialForest} -> ${stableForests.size()} Forests in ${elapsed}s")
        System.out.flush()

        where:
        animals | mode      || winners  | count | time
        3       | NORMAL    || 1        | 81    | 0.13
        3       | PIPELINED || 1        | 81    | 0.22
        4       | NORMAL    || 121      | 81    | 0.17
        4       | PIPELINED || 121      | 81    | 2.64
        5       | NORMAL    || 4841     | 81    | 1.8
        5       | PIPELINED || 4841     | 81    | 3.0
        6       | NORMAL    || 106801   | 81    | 21.6
        6       | PIPELINED || 106801   | 81    | 35.8
    }
}
