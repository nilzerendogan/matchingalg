import java.util.ArrayList;

/**
 * Represents a customer in the GigMatch Pro system.
 * Customers hire freelancers, accumulate spending for loyalty tiers,
 * and can maintain personal blacklists of freelancers.
 */
public class Customer {
    String id;

    // Total amount customer has paid for completed jobs (after discounts)
    int totalSpent;

    // Penalty from cancellations ($250 per cancellation, affects loyalty tier calculation)
    int loyaltyPenalty;

    // Current loyalty tier: BRONZE, SILVER, GOLD, or PLATINUM
    String loyaltyTier;

    // Personal blacklist - these freelancers won't appear in job requests
    ArrayList<String> blacklistedFreelancers;

    // Currently active employments (freelancer IDs)
    ArrayList<String> currentEmployments;

    // Total number of employments initiated (completed or not)
    int totalEmployments;

    /**
     * Create a new customer with default BRONZE tier and empty lists.
     */
    public Customer(String id) {
        this.id = id;
        this.totalSpent = 0;
        this.loyaltyPenalty = 0;
        this.loyaltyTier = "BRONZE";
        this.blacklistedFreelancers = new ArrayList<>();
        this.currentEmployments = new ArrayList<>();
        this.totalEmployments = 0;
    }

    /**
     * Check if a freelancer is in this customer's personal blacklist.
     */
    public boolean isBlacklisted(String freelancerId) {
        return blacklistedFreelancers.contains(freelancerId);
    }

    /**
     * Get the customer's current loyalty tier.
     */
    public String getLoyaltyTier() {
        return loyaltyTier;
    }

    /**
     * Update loyalty tier based on effective spending (total spent minus penalties).
     * Tiers: PLATINUM (≥$5000), GOLD (≥$2000), SILVER (≥$500), BRONZE (<$500)
     * Called during monthly simulation.
     */
    public void updateLoyaltyTier() {
        int effectiveSpending = Math.max(0, totalSpent - loyaltyPenalty);
        if (effectiveSpending >= 5000) {
            loyaltyTier = "PLATINUM";
        } else if (effectiveSpending >= 2000) {
            loyaltyTier = "GOLD";
        } else if (effectiveSpending >= 500) {
            loyaltyTier = "SILVER";
        } else {
            loyaltyTier = "BRONZE";
        }
    }

    /**
     * Get the discount rate for the current loyalty tier.
     * Returns: 0.15 (PLATINUM), 0.10 (GOLD), 0.05 (SILVER), 0.0 (BRONZE)
     */
    public double getLoyaltyDiscount() {
        switch (loyaltyTier) {
            case "PLATINUM": return 0.15;
            case "GOLD": return 0.10;
            case "SILVER": return 0.05;
            default: return 0.0;
        }
    }
}