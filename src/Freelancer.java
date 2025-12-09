/**
 * Represents a freelancer in the GigMatch Pro system.
 * Freelancers offer services, maintain skill profiles, receive ratings,
 * and can experience burnout or platform blacklisting.
 */
public class Freelancer {
    String id;

    // Current service type offered (can change via service change requests)
    String service;

    // Price charged for the service
    int price;

    // Skill profile: [Technical, Communication, Creativity, Efficiency, Attention to Detail]
    int[] skills;

    // Cached sum of all skills for efficiency
    int totalSkill;

    // Number of successfully completed jobs
    int completedJobs;

    // Number of jobs cancelled by this freelancer
    int cancelledJobs;

    // Average rating (0.0 to 5.0), starts at 5.0 with one implicit review
    double avrRating;

    // Whether freelancer is currently available for work
    boolean available;

    // Burnout status (triggered by 5+ jobs in a month, affects composite score)
    boolean burnout;

    // Platform-level blacklist (permanent ban after 5+ cancellations in a month)
    boolean platformBlacklisted;

    // ID of customer currently employing this freelancer (null if available)
    String currentCustomer;

    // Position in the max heap for efficient updates (-1 if not in heap)
    int heapIndex;

    // Counter for jobs completed this month (for burnout tracking)
    int jobsThisMonth;

    // Counter for cancellations this month (for platform blacklist check)
    int cancellationsThisMonth;

    // Most recently calculated composite score (cached for display)
    int lastCompositeScore;

    /**
     * Create a new freelancer with specified service, price, and skill profile.
     * Freelancer starts available with 5.0 rating and no burnout.
     */
    public Freelancer(String id, String service, int price, int t, int c, int r, int e, int a) {
        this.id = id;
        this.service = service;
        this.price = price;
        this.skills = new int[]{t, c, r, e, a};
        this.totalSkill = t + c + r + e + a;
        this.completedJobs = 0;
        this.cancelledJobs = 0;
        this.avrRating = 5.0; // Starts with one implicit 5-star review
        this.available = true;
        this.burnout = false;
        this.platformBlacklisted = false;
        this.currentCustomer = null;
        this.heapIndex = -1;
        this.jobsThisMonth = 0;
        this.cancellationsThisMonth = 0;
        this.lastCompositeScore = 0;
    }

    /**
     * Get the freelancer's current average rating.
     * New freelancers start with 5.0 (one implicit review).
     */
    public double getAverageRating() {
        return avrRating;
    }

    /**
     * Recalculate and update the total skill points.
     * Called after any skill modifications (gains, degradation, manual updates).
     */
    public void updateTotalSkill() {
        totalSkill = skills[0] + skills[1] + skills[2] + skills[3] + skills[4];
    }
}