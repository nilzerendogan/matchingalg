/**
 * Represents a pending service change request for a freelancer.
 * Requests are queued when change_service is called and applied
 * during the next simulate_month operation.
 */
public class ServiceChangeRequest {
    // New service type to switch to
    String service;

    // New price for the service
    int price;

    /**
     * Create a new service change request.
     * @param service The target service type
     * @param price The new price for that service
     */
    public ServiceChangeRequest(String service, int price) {
        this.service = service;
        this.price = price;
    }
}