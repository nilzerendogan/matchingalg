import java.util.ArrayList;

/**
 * Core system managing all operations for GigMatch Pro.
 * Handles freelancer/customer registration, job matching, employment lifecycle,
 * and monthly simulations including burnout and loyalty systems.
 */
public class GigMatchSystem {

    // Maps to store all users by ID
    private CustomHashMap<String, Freelancer> freelancers;
    private CustomHashMap<String, Customer> customers;

    // Max heaps for each service type to efficiently rank freelancers
    private CustomHashMap<String, MaxHeap> serviceHeaps;

    // Predefined skill profiles for each service type
    private CustomHashMap<String, int[]> serviceProfiles;

    // Queue for service change requests to be applied at month end
    private CustomHashMap<String, ServiceChangeRequest> pendingServiceChanges;

    public GigMatchSystem() {
        freelancers = new CustomHashMap<>();
        customers = new CustomHashMap<>();
        serviceHeaps = new CustomHashMap<>();
        serviceProfiles = new CustomHashMap<>();
        pendingServiceChanges = new CustomHashMap<>();
        initializeServiceProfiles();
        initializeServiceHeaps();
    }

    /**
     * Initialize skill requirement profiles for all 10 service types.
     * Format: [Technical, Communication, Creativity, Efficiency, Attention to Detail]
     */
    private void initializeServiceProfiles() {
        serviceProfiles.put("paint", new int[]{70, 60, 50, 85, 90});
        serviceProfiles.put("web_dev", new int[]{95, 75, 85, 80, 90});
        serviceProfiles.put("graphic_design", new int[]{75, 85, 95, 70, 85});
        serviceProfiles.put("data_entry", new int[]{50, 50, 30, 95, 95});
        serviceProfiles.put("tutoring", new int[]{80, 95, 70, 90, 75});
        serviceProfiles.put("cleaning", new int[]{40, 60, 40, 90, 85});
        serviceProfiles.put("writing", new int[]{70, 85, 90, 80, 95});
        serviceProfiles.put("photography", new int[]{85, 80, 90, 75, 90});
        serviceProfiles.put("plumbing", new int[]{85, 65, 60, 90, 85});
        serviceProfiles.put("electrical", new int[]{90, 65, 70, 95, 95});
    }

    /**
     * Create a max heap for each service type to maintain ranked freelancer lists.
     */
    private void initializeServiceHeaps() {
        ArrayList<String> services = serviceProfiles.keySet();
        for (int i = 0; i < services.size(); i++) {
            String service = services.get(i);
            serviceHeaps.put(service, new MaxHeap(serviceProfiles.get(service)));
        }
    }

    /**
     * Register a new customer with unique ID.
     */
    public String registerCustomer(String id) {
        if (customers.containsKey(id) || freelancers.containsKey(id)) {
            return "Some error occurred in register_customer.";
        }
        customers.put(id, new Customer(id));
        return "registered customer " + id;
    }

    /**
     * Register a new freelancer with service type, price, and skill profile.
     * Validates all parameters and adds freelancer to appropriate service heap.
     */
    public String registerFreelancer(String id, String service, int price,
                                     int t, int c, int r, int e, int a) {
        try {
            // Check for ID conflicts
            if (freelancers.containsKey(id) || customers.containsKey(id)) {
                return "Some error occurred in register_freelancer.";
            }

            // Validate service exists, price is positive, and all skills are in [0,100]
            if (!serviceProfiles.containsKey(service) || price <= 0 ||
                    t < 0 || t > 100 || c < 0 || c > 100 || r < 0 || r > 100 ||
                    e < 0 || e > 100 || a < 0 || a > 100) {
                return "Some error occurred in register_freelancer.";
            }

            Freelancer freelancer = new Freelancer(id, service, price, t, c, r, e, a);
            freelancers.put(id, freelancer);
            serviceHeaps.get(service).insert(freelancer);

            return "registered freelancer " + id;
        } catch (Exception ex) {
            return "Some error occurred in register_freelancer.";
        }
    }

    /**
     * Manually employ a specific freelancer for a customer.
     * Checks availability, blacklist status, and updates employment records.
     */
    public String employ(String custId, String freelId) {
        if (!customers.containsKey(custId) || !freelancers.containsKey(freelId)) {
            return "Some error occurred in employ.";
        }

        Customer customer = customers.get(custId);
        Freelancer freelancer = freelancers.get(freelId);

        // Verify freelancer is available and not blacklisted
        if (!freelancer.available || freelancer.platformBlacklisted ||
                customer.isBlacklisted(freelId)) {
            return "Some error occurred in employ.";
        }

        // Mark freelancer as employed and remove from available pool
        freelancer.available = false;
        serviceHeaps.get(freelancer.service).remove(freelancer);
        freelancer.currentCustomer = custId;

        // Update customer employment records
        customer.currentEmployments.add(freelId);
        customer.totalEmployments++;

        return custId + " employed " + freelId + " for " + freelancer.service;
    }

    /**
     * Request a job for a service type, display top candidates, and auto-employ the best.
     * Filters by availability and blacklist, ranks by composite score.
     */
    public String requestJob(String custId, String service, int numCandidates) {
        if (!customers.containsKey(custId) || !serviceProfiles.containsKey(service)) {
            return "Some error occurred in request_job.";
        }

        Customer customer = customers.get(custId);
        ArrayList<Freelancer> candidates = getEligibleFreelancers(service, customer, numCandidates);

        if (candidates.isEmpty()) {
            return "no freelancers available";
        }

        // Build output showing top candidates
        StringBuilder result = new StringBuilder();
        result.append("available freelancers for ").append(service)
                .append(" (top ").append(numCandidates).append("):\n");

        int displayCount = Math.min(numCandidates, candidates.size());
        for (int i = 0; i < displayCount; i++) {
            Freelancer f = candidates.get(i);
            result.append(f.id).append(" - composite: ").append(f.lastCompositeScore)
                    .append(", price: ").append(f.price)
                    .append(", rating: ").append(String.format("%.1f", f.getAverageRating()));
            if (i < displayCount - 1) result.append("\n");
        }

        // Auto-employ the best freelancer
        Freelancer best = candidates.get(0);
        best.available = false;
        serviceHeaps.get(service).remove(best);
        best.currentCustomer = custId;
        customer.currentEmployments.add(best.id);
        customer.totalEmployments++;

        result.append("\nauto-employed best freelancer: ").append(best.id)
                .append(" for customer ").append(custId);

        return result.toString();
    }

    /**
     * Get eligible freelancers from heap, filtering out blacklisted ones.
     */
    private ArrayList<Freelancer> getEligibleFreelancers(String service, Customer customer, int needed) {
        MaxHeap heap = serviceHeaps.get(service);
        return (ArrayList<Freelancer>) heap.getTopEligibleFreelancers(needed, customer);
    }

    /**
     * Complete a job with a rating. Updates freelancer stats, applies skill gains
     * if rating >= 4, processes payment with loyalty discount, and marks freelancer available.
     */
    public String completeAndRate(String freelId, int rating) {
        if (!freelancers.containsKey(freelId) || rating < 0 || rating > 5) {
            return "Some error occurred in complete_and_rate.";
        }

        Freelancer freelancer = freelancers.get(freelId);
        if (freelancer.currentCustomer == null || freelancer.available) {
            return "Some error occurred in complete_and_rate.";
        }

        String custId = freelancer.currentCustomer;
        Customer customer = customers.get(custId);

        // Update average rating using weighted formula
        double oldAvg = freelancer.getAverageRating();
        int n = freelancer.completedJobs + freelancer.cancelledJobs;
        double newAvg = (oldAvg * (n+1) + rating) / (n + 2);
        freelancer.avrRating = newAvg;

        freelancer.completedJobs++;
        freelancer.jobsThisMonth++;

        // Apply skill gains for high-quality work
        if (rating >= 4) {
            applySkillGains(freelancer);
        }

        // Process payment with loyalty discount
        int payment = calculateCustomerPayment(customer, freelancer.price);
        customer.totalSpent += payment;

        // Mark freelancer available and update heap
        freelancer.available = true;
        freelancer.currentCustomer = null;
        customer.currentEmployments.remove(freelId);

        serviceHeaps.get(freelancer.service).insert(freelancer);

        return freelId + " completed job for " + custId + " with rating " + rating;
    }

    /**
     * Calculate customer payment after applying loyalty discount.
     * Uses floor function to ensure integer result.
     */
    private int calculateCustomerPayment(Customer customer, int price) {
        double discount = customer.getLoyaltyDiscount();
        return (int) Math.floor(price * (1.0 - discount));
    }

    /**
     * Sort service skill indices by value (descending), with tie-breaking by index (ascending).
     * Returns indices in priority order: [primary, secondary1, secondary2, ...]
     */
    private ArrayList<Integer> getSortedServiceIndices(int[] serviceProfile) {
        ArrayList<Integer> indices = new ArrayList<>();

        // Initialize indices 0-4
        for (int i = 0; i < serviceProfile.length; i++) {
            indices.add(i);
        }

        // Selection sort - sufficient for small array
        for (int i = 0; i < indices.size() - 1; i++) {
            int best = i;

            for (int j = i + 1; j < indices.size(); j++) {
                int i1 = indices.get(j);
                int i2 = indices.get(best);

                // Sort by value descending, then index ascending for tie-break
                if (serviceProfile[i1] > serviceProfile[i2]) {
                    best = j;
                }
                else if (serviceProfile[i1] == serviceProfile[i2] && i1 < i2) {
                    best = j;
                }
            }

            // Swap
            int temp = indices.get(i);
            indices.set(i, indices.get(best));
            indices.set(best, temp);
        }

        return indices;
    }

    /**
     * Apply skill gains after completing a job with rating >= 4.
     * Primary skill: +2, secondary skills (top 2 after primary): +1 each.
     */
    private void applySkillGains(Freelancer f) {
        int[] serviceProfile = serviceProfiles.get(f.service);

        ArrayList<Integer> sortedIndices = getSortedServiceIndices(serviceProfile);

        int primary = sortedIndices.get(0);
        int secondary1 = sortedIndices.get(1);
        int secondary2 = sortedIndices.get(2);

        // Apply gains with cap at 100
        f.skills[primary] = Math.min(100, f.skills[primary] + 2);
        f.skills[secondary1] = Math.min(100, f.skills[secondary1] + 1);
        f.skills[secondary2] = Math.min(100, f.skills[secondary2] + 1);

        f.updateTotalSkill();
    }

    /**
     * Customer cancels an active employment. Freelancer becomes available,
     * customer loses loyalty points (penalty applied).
     */
    public String cancelByCustomer(String custId, String freelId) {
        if (!customers.containsKey(custId) || !freelancers.containsKey(freelId)) {
            return "Some error occurred in cancel_by_customer.";
        }

        Customer customer = customers.get(custId);
        Freelancer freelancer = freelancers.get(freelId);

        // Verify active employment exists
        if (freelancer.currentCustomer == null || !freelancer.currentCustomer.equals(custId)) {
            return "Some error occurred in cancel_by_customer.";
        }

        // Release freelancer and apply loyalty penalty
        freelancer.available = true;
        serviceHeaps.get(freelancer.service).insert(freelancer);
        freelancer.currentCustomer = null;
        customer.currentEmployments.remove(freelId);
        customer.loyaltyPenalty += 250;

        return "cancelled by customer: " + custId + " cancelled " + freelId;
    }

    /**
     * Freelancer cancels an active employment. Applies zero-star rating,
     * skill degradation (-3 to all skills), and checks for platform blacklist.
     */
    public String cancelByFreelancer(String freelId) {
        if (!freelancers.containsKey(freelId)) {
            return "Some error occurred in cancel_by_freelancer.";
        }

        Freelancer freelancer = freelancers.get(freelId);
        if (freelancer.currentCustomer == null || freelancer.available) {
            return "Some error occurred in cancel_by_freelancer.";
        }

        String custId = freelancer.currentCustomer;
        Customer customer = customers.get(custId);

        // Apply zero-star rating to average
        double oldAvg = freelancer.getAverageRating();
        int n = freelancer.completedJobs + freelancer.cancelledJobs;
        double newAvg = (oldAvg * (n+1) + 0) / (n + 2);
        freelancer.avrRating = newAvg;

        freelancer.cancelledJobs++;
        freelancer.cancellationsThisMonth++;

        // Apply skill degradation penalty
        for (int i = 0; i < 5; i++) {
            freelancer.skills[i] = Math.max(0, freelancer.skills[i] - 3);
        }
        freelancer.updateTotalSkill();

        freelancer.available = true;
        freelancer.currentCustomer = null;
        customer.currentEmployments.remove(freelId);

        StringBuilder result = new StringBuilder();
        result.append("cancelled by freelancer: ").append(freelId)
                .append(" cancelled ").append(custId);

        // Platform blacklist if 5+ cancellations this month
        if (freelancer.cancellationsThisMonth >= 5 && !freelancer.platformBlacklisted) {
            freelancer.platformBlacklisted = true;
            result.append("\nplatform banned freelancer: ").append(freelId);
        } else {
            // Re-insert with updated stats
            serviceHeaps.get(freelancer.service).insert(freelancer);
        }

        return result.toString();
    }

    /**
     * Queue a service change request to be applied at month end.
     */
    public String changeService(String freelId, String newService, int newPrice) {
        if (!freelancers.containsKey(freelId) || !serviceProfiles.containsKey(newService) || newPrice <= 0) {
            return "Some error occurred in change_service.";
        }

        Freelancer freelancer = freelancers.get(freelId);
        String oldService = freelancer.service;

        pendingServiceChanges.put(freelId, new ServiceChangeRequest(newService, newPrice));

        return "service change for " + freelId + " queued from " + oldService + " to " + newService;
    }

    /**
     * Simulate one month passing. Processes:
     * 1. Burnout updates (5+ jobs = burnout, <=2 jobs = recovery)
     * 2. Customer loyalty tier updates
     * 3. Queued service changes
     */
    public String simulateMonth() {
        // Process burnout for all freelancers
        ArrayList<Freelancer> allFreelancers = freelancers.values();
        for (int i = 0; i < allFreelancers.size(); i++) {
            Freelancer f = allFreelancers.get(i);
            boolean oldBurnout = f.burnout;

            // Trigger burnout if 5+ jobs this month
            if (!f.burnout && f.jobsThisMonth >= 5) {
                f.burnout = true;
            }
            // Recover from burnout if 2 or fewer jobs
            else if (f.burnout && f.jobsThisMonth <= 2) {
                f.burnout = false;
            }

            // Update heap if burnout status changed (affects composite score)
            if (oldBurnout != f.burnout) {
                serviceHeaps.get(f.service).updateFreelancer(f);
            }

            // Reset monthly counters
            f.jobsThisMonth = 0;
            f.cancellationsThisMonth = 0;
        }

        // Update customer loyalty tiers based on total spending
        ArrayList<Customer> allCustomers = customers.values();
        for (int i = 0; i < allCustomers.size(); i++) {
            allCustomers.get(i).updateLoyaltyTier();
        }

        // Apply all queued service changes
        ArrayList<CustomHashMap.MapEntry<String, ServiceChangeRequest>> entries = new ArrayList<>();
        ArrayList<String> keys = pendingServiceChanges.keySet();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            entries.add(new CustomHashMap.MapEntry<>(key, pendingServiceChanges.get(key)));
        }

        for (int i = 0; i < entries.size(); i++) {
            CustomHashMap.MapEntry<String, ServiceChangeRequest> entry = entries.get(i);
            String freelId = entry.getKey();
            ServiceChangeRequest request = entry.getValue();
            Freelancer f = freelancers.get(freelId);

            // Move freelancer to new service heap
            serviceHeaps.get(f.service).remove(f);
            f.service = request.service;
            f.price = request.price;
            serviceHeaps.get(f.service).insert(f);
        }
        pendingServiceChanges.clear();

        return "month complete";
    }

    /**
     * Query freelancer details: service, price, rating, jobs, skills, availability, burnout.
     */
    public String queryFreelancer(String freelId) {
        if (!freelancers.containsKey(freelId)) {
            return "Some error occurred in query_freelancer.";
        }

        Freelancer f = freelancers.get(freelId);
        return String.format("%s: %s, price: %d, rating: %.1f, completed: %d, cancelled: %d, " +
                        "skills: (%d,%d,%d,%d,%d), available: %s, burnout: %s",
                f.id, f.service, f.price, f.getAverageRating(),
                f.completedJobs, f.cancelledJobs,
                f.skills[0], f.skills[1], f.skills[2], f.skills[3], f.skills[4],
                f.available ? "yes" : "no",
                f.burnout ? "yes" : "no");
    }

    /**
     * Query customer details: total spent, loyalty tier, blacklist count, total employments.
     */
    public String queryCustomer(String custId) {
        if (!customers.containsKey(custId)) {
            return "Some error occurred in query_customer.";
        }

        Customer c = customers.get(custId);
        return String.format("%s: total spent: $%d, loyalty tier: %s, blacklisted freelancer count: %d, " +
                        "total employment count: %d",
                c.id, c.totalSpent, c.getLoyaltyTier(),
                c.blacklistedFreelancers.size(), c.totalEmployments);
    }

    /**
     * Add freelancer to customer's personal blacklist.
     */
    public String blacklist(String custId, String freelId) {
        if (!customers.containsKey(custId) || !freelancers.containsKey(freelId)) {
            return "Some error occurred in blacklist.";
        }

        Customer customer = customers.get(custId);
        if (customer.isBlacklisted(freelId)) {
            return "Some error occurred in blacklist.";
        }

        customer.blacklistedFreelancers.add(freelId);
        return custId + " blacklisted " + freelId;
    }

    /**
     * Remove freelancer from customer's personal blacklist.
     */
    public String unblacklist(String custId, String freelId) {
        if (!customers.containsKey(custId) || !freelancers.containsKey(freelId)) {
            return "Some error occurred in unblacklist.";
        }

        Customer customer = customers.get(custId);
        if (!customer.isBlacklisted(freelId)) {
            return "Some error occurred in unblacklist.";
        }

        customer.blacklistedFreelancers.remove(freelId);
        return custId + " unblacklisted " + freelId;
    }

    /**
     * Manually update freelancer's skill profile. Recalculates composite score
     * and updates position in heap.
     */
    public String updateSkill(String freelId, int t, int c, int r, int e, int a) {
        if (!freelancers.containsKey(freelId) ||
                t < 0 || t > 100 || c < 0 || c > 100 || r < 0 || r > 100 ||
                e < 0 || e > 100 || a < 0 || a > 100) {
            return "Some error occurred in update_skill.";
        }

        Freelancer f = freelancers.get(freelId);
        f.skills[0] = t;
        f.skills[1] = c;
        f.skills[2] = r;
        f.skills[3] = e;
        f.skills[4] = a;
        f.updateTotalSkill();

        // Update heap position with new composite score
        serviceHeaps.get(f.service).updateFreelancer(f);

        return "updated skills of " + freelId + " for " + f.service;
    }
}