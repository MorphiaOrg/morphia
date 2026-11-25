package dev.morphia.audits.semantics

/** Multiset matching for the comparators, where equivalence need not be transitive. */
internal object Matching {
    /**
     * Finds a perfect bipartite matching of [lefts] onto [rights] with augmenting paths (a greedy
     * removal can reject a valid assignment) and returns the first left index it cannot place, or
     * null when every one is matched. [spend] is charged for each step.
     */
    fun firstUnmatched(
        lefts: Int,
        rights: Int,
        spend: () -> Unit,
        candidate: (Int, Int) -> Boolean,
    ): Int? {
        val candidates = Array(lefts) { left -> BooleanArray(rights) { candidate(left, it) } }
        val owners = IntArray(rights) { -1 }
        val assigned = IntArray(lefts) { -1 }
        for (root in 0 until lefts) {
            // Iterative augmentation keeps the call stack independent of the input size.
            val previous = IntArray(rights) { -1 }
            val queue = ArrayDeque<Int>()
            queue.add(root)
            var free = -1
            search@ while (queue.isNotEmpty()) {
                val left = queue.removeFirst()
                for (right in 0 until rights) {
                    spend()
                    if (!candidates[left][right] || previous[right] != -1) continue
                    previous[right] = left
                    if (owners[right] == -1) {
                        free = right
                        break@search
                    }
                    queue.add(owners[right])
                }
            }
            if (free == -1) return root
            while (free != -1) {
                spend()
                val left = previous[free]
                val old = assigned[left]
                owners[free] = left
                assigned[left] = free
                free = old
            }
        }
        return null
    }
}
