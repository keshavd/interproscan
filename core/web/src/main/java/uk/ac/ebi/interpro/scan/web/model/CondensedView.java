package uk.ac.ebi.interpro.scan.web.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Phil Jones
 *         Date: 24/01/12
 *         Time: 16:27
 *         Comprises the lines for the condensed view and is responsible for
 *         building this structure.
 */
public class CondensedView {

    private final SimpleProtein protein;

    // The CondensedLines in this Set are ordered by their lineNumber,
    // 0 indexed.
    private Set<CondensedLine> lines = new TreeSet<CondensedLine>();

    public CondensedView(final SimpleProtein protein) {
        this.protein = protein;
        // First of all, need to build SuperMatches.
        final List<SimpleSuperMatch> superMatches = buildSuperMatchList();

        // Second, need to build "SuperMatchBucket" objects.  This process also merges
        // matches to entries in the same hierarchy.
        final List<SuperMatchBucket> buckets = buildBuckets(superMatches);

        // Finally, add the buckets to the lines, aiming for the least number of lines possible.
        buildLines(buckets);
    }

    /**
     * Very dumb method - just makes "SimpleSuperMatch" objects out of SimpleEntry
     * objects - however at this point they are not Supermatches - that is the job
     * of the next method (buildBuckets).
     *
     * @return a List of SimpleSuperMatch objects, one for each Entry / location.
     */
    private List<SimpleSuperMatch> buildSuperMatchList() {
        final List<SimpleSuperMatch> superMatchList = new ArrayList<SimpleSuperMatch>();
        // Initially the SimpleSuperMatches are just matches - the merging occurs in the next method call.
        for (final SimpleEntry entry : protein.getAllEntries()) {
            for (final SimpleLocation location : entry.getLocations()) {
                superMatchList.add(new SimpleSuperMatch(
                        entry.getType(),
                        location,
                        entry
                ));
            }
        }
        return superMatchList;
    }

    /**
     * Iterates over the supermatches and merges / buckets them according to their
     * relationships in the hierarchy.
     *
     * @param superMatches to be merged & bucketed.
     * @return a List of SuperMatchBuckets.
     */
    private List<SuperMatchBucket> buildBuckets(final List<SimpleSuperMatch> superMatches) {
        List<SuperMatchBucket> superMatchBucketList = new ArrayList<SuperMatchBucket>();
        for (SimpleSuperMatch superMatch : superMatches) {
            boolean inList = false;
            for (final SuperMatchBucket bucket : superMatchBucketList) {
                // addIfSameHierarchy also merges matches into supermatches.
                inList = bucket.addIfSameHierarchy(superMatch);
                if (inList) break; // Will be only one bucket per hierarchy, so no need to go further.
            }
            if (!inList) {
                // Need a new Bucket.
                superMatchBucketList.add(new SuperMatchBucket(superMatch));
            }
        }
        return superMatchBucketList;
    }

    /**
     * Considering each bucket in turn, attempt to add the buckets to a line, minimising the
     * number of lines and attempting to add the
     *
     * @param buckets
     */
    private void buildLines(List<SuperMatchBucket> buckets) {
        for (SuperMatchBucket bucket : buckets) {
            boolean bucketFoundAHome = false;
            // Check if this bucket can be added to any existing lines
            for (CondensedLine line : lines) {  // This will give the lines in the correct order, as they are in a TreeSet.
                bucketFoundAHome = line.addSuperMatchesWithoutOverlap(bucket);
                if (bucketFoundAHome) {
                    break; // out of the Condensed line loop - stop trying to add this bucket to any more lines.
                }
            }
            // if the bucket has still not found a line to live on, need to create a new line for it.
            if (!bucketFoundAHome) {
                lines.add(new CondensedLine(lines.size(), bucket));
            }
        }
    }

    public Set<CondensedLine> getLines() {
        return lines;
    }
}