import java.io.*;
import java.util.Locale;

/**
 * Main entry point for GigMatch Pro platform.
 * Handles command-line input/output processing for the freelancing platform.
 *
 * @author Nil Zeren Doğan
 * @date November 19, 2025
 */
public class Main {

    private static GigMatchSystem system = new GigMatchSystem();

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        if (args.length != 2) {
            System.err.println("Usage: java Main <input_file> <output_file>");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = args[1];

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                processCommand(line, writer);
            }

        } catch (IOException e) {
            System.err.println("Error reading/writing files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processCommand(String command, BufferedWriter writer)
            throws IOException {

        String[] parts = command.split("\\s+");
        String operation = parts[0];

        try {
            String result = "";

            switch (operation) {
                case "register_customer":
                    // Creates a new customer account with the given ID
                    result = system.registerCustomer(parts[1]);
                    break;

                case "register_freelancer":
                    // Registers a new freelancer with their service offering, base price,
                    // and initial skill profile (T, C, R, E, A values)
                    result = system.registerFreelancer(
                            parts[1], // freelancerID
                            parts[2], // service type (e.g., paint, web_dev)
                            Integer.parseInt(parts[3]), // base price for the service
                            Integer.parseInt(parts[4]), // T - Technical Proficiency
                            Integer.parseInt(parts[5]), // C - Communication
                            Integer.parseInt(parts[6]), // R - Creativity
                            Integer.parseInt(parts[7]), // E - Efficiency
                            Integer.parseInt(parts[8])  // A - Attention to Detail
                    );
                    break;

                case "request_job":
                    // Finds and ranks available freelancers for a specific service,
                    // displays top K candidates, and auto-employs the best match
                    result = system.requestJob(
                            parts[1], // customerID
                            parts[2], // service type requested
                            Integer.parseInt(parts[3]) // topK - number of candidates to display
                    );
                    break;

                case "employ_freelancer":
                    // Manually employs a specific freelancer for a customer
                    // (used in Type 1 test cases only)
                    result = system.employ(parts[1], parts[2]);
                    break;

                case "complete_and_rate":
                    // Marks a job as completed, updates freelancer's rating average,
                    // applies skill gains if rating >= 4, and makes freelancer available again
                    result = system.completeAndRate(
                            parts[1], // freelancerID
                            Integer.parseInt(parts[2]) // rating (0-5)
                    );
                    break;

                case "cancel_by_freelancer":
                    // Handles freelancer-initiated cancellation: applies 0-star rating,
                    // degrades all skills by 3, and checks for platform blacklist (5+ cancels/month)
                    result = system.cancelByFreelancer(parts[1]);
                    break;

                case "cancel_by_customer":
                    // Handles customer-initiated cancellation: frees up the freelancer
                    // and deducts loyalty points from the customer ($250 penalty)
                    result = system.cancelByCustomer(parts[1], parts[2]);
                    break;

                case "blacklist":
                    // Adds a freelancer to a customer's personal blacklist,
                    // preventing them from appearing in future job requests
                    result = system.blacklist(parts[1], parts[2]);
                    break;

                case "unblacklist":
                    // Removes a freelancer from a customer's personal blacklist,
                    // allowing them to be matched again in future requests
                    result = system.unblacklist(parts[1], parts[2]);
                    break;

                case "change_service":
                    // Queues a service type change for a freelancer to be applied
                    // at the next month simulation (updates service and price)
                    result = system.changeService(
                            parts[1], // freelancerID
                            parts[2], // new service type
                            Integer.parseInt(parts[3]) // new price
                    );
                    break;

                case "simulate_month":
                    // Advances the system by one month: applies queued service changes,
                    // updates burnout status, recalculates loyalty tiers
                    result = system.simulateMonth();
                    break;

                case "query_freelancer":
                    // Retrieves and displays detailed information about a freelancer:
                    // service, price, rating, job counts, skills, availability, burnout status
                    result = system.queryFreelancer(parts[1]);
                    break;

                case "query_customer":
                    // Retrieves and displays customer information:
                    // total spending, loyalty tier, blacklist count, employment count
                    result = system.queryCustomer(parts[1]);
                    break;

                case "update_skill":
                    // Manually updates a freelancer's skill profile with new values
                    // (used in Type 3 test cases only)
                    result = system.updateSkill(
                            parts[1], // freelancerID
                            Integer.parseInt(parts[2]), // T - Technical Proficiency
                            Integer.parseInt(parts[3]), // C - Communication
                            Integer.parseInt(parts[4]), // R - Creativity
                            Integer.parseInt(parts[5]), // E - Efficiency
                            Integer.parseInt(parts[6])  // A - Attention to Detail
                    );
                    break;

                default:
                    result = "Unknown command: " + operation;
            }

            if (result != null && !result.isEmpty()) {
                writer.write(result);
                writer.newLine();
            }

        } catch (Exception e) {
            writer.write("Error processing command: " + command);
            writer.newLine();
        }
    }
}