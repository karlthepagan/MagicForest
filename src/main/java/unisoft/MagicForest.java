package unisoft;

// See http://unriskinsight.blogspot.com/2014/06/fast-functional-goats-lions-and-wolves.html
// Sascha Kratky (kratky@unrisk.com), uni software plus GmbH & MathConsult GmbH
//
// compilation requires Java 8.
//
// compile with Oracle JDK 8:
// javac MagicForest.java
//
// run with Oracle JVM 8:
// java MagicForest 117 155 106

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MagicForest {

    static final class Forest {
        private final int goats;
        private final int wolves;
        private final int lions;

        public Forest(int goats, int wolves, int lions) {
            this.goats = goats;
            this.wolves = wolves;
            this.lions = lions;
        }

        static public Forest makeForest(int goats, int wolves, int lions) {
            return new Forest(goats, wolves, lions);
        }

        public boolean isWolfEatingGoat() {
            return this.goats > 0 && this.wolves > 0;
        }

        public Optional<Forest> wolfDevoursGoat() {
            if (isWolfEatingGoat())
                return Optional.of(makeForest(this.goats - 1, this.wolves - 1, this.lions + 1));
            return Optional.empty();
        }

        public boolean isLionEatingGoat() {
            return this.goats > 0 && this.lions > 0;
        }

        public Optional<Forest> lionDevoursGoat() {
            if (isLionEatingGoat())
                return Optional.of(makeForest(this.goats - 1, this.wolves + 1, this.lions - 1));
            return Optional.empty();
        }

        public boolean isLionEatingWolf() {
            return this.lions > 0 && this.wolves > 0;
        }

        public Optional<Forest> lionDevoursWolf() {
            if (isLionEatingWolf())
                return Optional.of(makeForest(this.goats + 1, this.wolves - 1, this.lions - 1));
            return Optional.empty();
        }

        public Stream<Forest> meal() {
            Stream.Builder<Forest> nextForests = Stream.builder();
            this.wolfDevoursGoat().ifPresent(nextForests);
            this.lionDevoursGoat().ifPresent(nextForests);
            this.lionDevoursWolf().ifPresent(nextForests);
            return nextForests.build();
        }

        public boolean isStable() {
            if (this.goats == 0) return (this.wolves == 0) || (this.lions == 0);
            return (this.wolves == 0) && (this.lions == 0);
        }

        @Override
        public int hashCode() {
            final int magic = 0x9e3779b9;
            int seed = 0;
            seed ^= this.goats + magic + (seed << 6) + (seed >> 2);
            seed ^= this.lions + magic + (seed << 6) + (seed >> 2);
            seed ^= this.wolves + magic + (seed << 6) + (seed >> 2);
            return seed;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (this.getClass() != obj.getClass())
                return false;
            Forest other = (Forest) obj;
            if (this.goats != other.goats)
                return false;
            if (this.lions != other.lions)
                return false;
            if (this.wolves != other.wolves)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Forest [goats=" + this.goats + ", wolves=" + this.wolves +
                    ", lions=" + this.lions + "]";
        }

    }

    static List<Forest> meal(List<Forest> forests) {
        return forests.stream().flatMap(Forest::meal).distinct().collect(Collectors.toList());
    }

    static boolean devouringPossible(List<Forest> forests) {
        return !forests.isEmpty() && !forests.stream().anyMatch(Forest::isStable);
    }

    static List<Forest> stableForests(List<Forest> forests) {
        return forests.stream().filter(Forest::isStable).collect(Collectors.toList());
    }

    static public List<Forest> findStableForests(Forest forest) {
        List<Forest> initialForests = Collections.singletonList(forest);
        Optional<List<Forest>> solution =
                Stream.iterate(initialForests, MagicForest::meal).filter(
                        forests->!devouringPossible(forests)).findFirst();
        return solution.isPresent()? stableForests(solution.get()) : Collections.emptyList();
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("USAGE: " + MagicForest.class.getSimpleName() +
                    " <goats> <wolves> <lions>");
            System.exit(-1);
        }
        try {
            Forest initialForest = Forest.makeForest(Integer.parseInt(args[0]),
                    Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            List<Forest> stableForests = findStableForests(initialForest);
            if (stableForests.isEmpty()) {
                System.out.println("no stable forests found.");
            }
            else {
                stableForests.forEach(forest -> System.out.println(forest));
            }
        } catch (Exception ex) {
            System.err.println("ERROR: " + ex.toString());
            System.exit(-1);
        }
    }

}