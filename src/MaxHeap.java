import java.util.ArrayList;

/**
 * Max heap for organizing freelancers by composite score for their service.
 * Maintains heap property where parent has higher or equal composite score than children.
 */
public class MaxHeap {
    private ArrayList<Freelancer> heap;
    private int[] serviceProfile;

    public MaxHeap(int[] serviceProfile) {
        this.heap = new ArrayList<>();
        this.serviceProfile = serviceProfile;
    }

    /**
     * Inserts a freelancer into the heap.
     */
    public void insert(Freelancer freelancer) {
        // Calculate and store composite score for this service
        freelancer.lastCompositeScore = calculateCompositeScore(freelancer);
        heap.add(freelancer);
        freelancer.heapIndex = heap.size() - 1;
        heapifyUp(heap.size() - 1);
    }

    /**
     * Removes a freelancer from the heap.
     */
    public void remove(Freelancer freelancer) {
        int index = freelancer.heapIndex;
        if (index < 0 || index >= heap.size()) return;

        int lastIndex = heap.size() - 1;
        swap(index, lastIndex);
        heap.remove(lastIndex);
        freelancer.heapIndex = -1;

        if (index < heap.size()) {
            heapifyDown(index);
            heapifyUp(index);
        }
    }

    public ArrayList<Freelancer> getAll() {
        return heap;
    }

    /**
     * Returns top eligible freelancers in sorted order (highest composite score first).
     */
    public ArrayList<Freelancer> getTopEligibleFreelancers(int needed, Customer customer) {
        ArrayList<Freelancer> result = new ArrayList<>();

        // Custom priority queue implementation using ArrayList
        ArrayList<Integer> pq = new ArrayList<>();

        if (!heap.isEmpty()) pq.add(0);

        while (!pq.isEmpty() && result.size() < needed) {
            // Find and remove max element from pq
            int maxIdx = 0;
            for (int i = 1; i < pq.size(); i++) {
                if (compare(heap.get(pq.get(i)), heap.get(pq.get(maxIdx))) > 0) {
                    maxIdx = i;
                }
            }
            int idx = pq.remove(maxIdx);

            Freelancer f = heap.get(idx);

            if (f.available && !f.platformBlacklisted && !customer.isBlacklisted(f.id)) {
                result.add(f);
            }

            int left = 2 * idx + 1;
            int right = 2 * idx + 2;

            if (left < heap.size()) pq.add(left);
            if (right < heap.size()) pq.add(right);
        }

        return result;
    }

    /**
     * Updates a freelancer's position in the heap after skill changes.
     */
    public void updateFreelancer(Freelancer freelancer) {
        int index = freelancer.heapIndex;
        if (index < 0 || index >= heap.size()) return;

        // Recalculate composite score
        freelancer.lastCompositeScore = calculateCompositeScore(freelancer);

        heapifyDown(index);
        heapifyUp(index);
    }

    /**
     * Restores heap property by moving element up.
     */
    private void heapifyUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (compare(heap.get(index), heap.get(parent)) > 0) {
                swap(index, parent);
                index = parent;
            } else {
                break;
            }
        }
    }

    /**
     * Restores heap property by moving element down.
     */
    private void heapifyDown(int index) {
        int size = heap.size();
        while (true) {
            int largest = index;
            int left = 2 * index + 1;
            int right = 2 * index + 2;

            if (left < size && compare(heap.get(left), heap.get(largest)) > 0) {
                largest = left;
            }
            if (right < size && compare(heap.get(right), heap.get(largest)) > 0) {
                largest = right;
            }

            if (largest != index) {
                swap(index, largest);
                index = largest;
            } else {
                break;
            }
        }
    }

    /**
     * Compares two freelancers based on composite score.
     * Returns positive if f1 > f2, negative if f1 < f2, zero if equal.
     * Tie-breaker: lexicographically smaller ID wins (higher priority).
     */
    private int compare(Freelancer f1, Freelancer f2) {
        if (f1.lastCompositeScore != f2.lastCompositeScore) {
            return f1.lastCompositeScore - f2.lastCompositeScore;
        }
        // Lexicographically smaller ID should have higher priority in max heap
        return f2.id.compareTo(f1.id);
    }

    /**
     * Swaps two elements in the heap and updates their indices.
     */
    private void swap(int i, int j) {
        Freelancer temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
        heap.get(i).heapIndex = i;
        heap.get(j).heapIndex = j;
    }

    /**
     * Calculates composite score for a freelancer based on this heap's service profile.
     */
    private int calculateCompositeScore(Freelancer f) {
        double skillScore = calculateSkillScore(f);
        double ratingScore = f.getAverageRating() / 5.0;
        double reliabilityScore = calculateReliabilityScore(f);
        double burnoutPenalty = f.burnout ? 0.45 : 0.0;

        double composite = 10000 * (0.55 * skillScore + 0.25 * ratingScore +
                0.20 * reliabilityScore - burnoutPenalty);
        return (int) Math.floor(composite);
    }

    private double calculateSkillScore(Freelancer f) {
        int dotProduct = f.skills[0] * serviceProfile[0] +
                f.skills[1] * serviceProfile[1] +
                f.skills[2] * serviceProfile[2] +
                f.skills[3] * serviceProfile[3] +
                f.skills[4] * serviceProfile[4];

        int sumService = serviceProfile[0] + serviceProfile[1] +
                serviceProfile[2] + serviceProfile[3] + serviceProfile[4];

        return (double) dotProduct / (100.0 * sumService);
    }

    private double calculateReliabilityScore(Freelancer f) {
        int total = f.completedJobs + f.cancelledJobs;
        if (total == 0) return 1.0;
        return 1.0 - ((double) f.cancelledJobs / total);
    }
}