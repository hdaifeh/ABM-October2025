/**
 * Household agent class for biowaste management ABM - Phase 1
 * 
 * Phase 1 Class Design:
 * - Households are "containers" for disaggregated flows
 * - NO independent calculations (territory calculates everything)
 * - All households are identical (homogeneous agents)
 * - Territory assigns flows after completing SD calculations
 * - Ensures exact mathematical equivalence with original SD model
 * 
 * Phase 2 (Future):
 * - Add heterogeneity in waste production
 * - Add variation in adoption timing
 * - Add social network effects
 * - Households calculate their own behaviors
 * 
 * @author: [HDAIFEH Ammar]
 * @version: Phase 1 - Homogeneous agents
 * @date: [14/10/2025]
 */
public class Household {
    
    // ============== IDENTITY ==============
    
    /**
     * Unique identifier for this household (0, 1, 2, ...)
     */
    private final int householdId;
    
    /**
     * Reference to parent collection territory
     */
    private final CollectionTerritory myTerritory;
    
    /**
     * Number of persons in this household (e.g., 2.1)
     * Read from input file parameter
     */
    private final double householdSize;
    
        // ============== 15 October ==============
    
    // ============== NEW: Social norm attributes ==============
/**
 * Adoption threshold: household adopts when this fraction of territory has adopted
 * Example: 0.3 means "I'll adopt when 30% of others have adopted"
 */
private double threshold_foodComposting;     // When to start composting food
private boolean hasAdopted_foodComposting;   // Have I adopted? (true/false)

// For simplicity, start with just ONE behavior (food composting)
// Later I can add: threshold_greenComposting, threshold_foodSorting, etc.
    
    
    // ============== WASTE PRODUCTION (assigned by territory) ==============
    
    /**
     * Food waste produced by this household (tonnes/year)
     * Assigned by territory after calculating aggregate Bpf
     */
    private double foodWasteProduced;
    
    /**
     * Green waste produced by this household (tonnes/year)
     * Assigned by territory after calculating aggregate Bpg
     */
    private double greenWasteProduced;
    
    // ============== FOOD WASTE DESTINATIONS (assigned by territory) ==============
    
    /**
     * Food waste actually composted at home (tonnes/year)
     * After applying home composting capacity constraints
     */
    private double foodWaste_homeComposted;
    
    /**
     * Food waste collected via dedicated collection system (tonnes/year)
     * After applying collection capacity constraints
     */
    private double foodWaste_dedicatedCollection;
    
    /**
     * Food waste going to residual household waste bin (tonnes/year)
     * What remains after composting and collection
     */
    private double foodWaste_residualBin;//classical bin
    
    // ============== GREEN WASTE DESTINATIONS (assigned by territory) ==============
    
    /**
     * Green waste actually composted at home (tonnes/year)
     * After applying home composting capacity constraints
     */
    private double greenWaste_homeComposted;
    
    /**
     * Green waste collected via dedicated collection system (tonnes/year)
     * After applying collection capacity constraints
     */
    private double greenWaste_dedicatedCollection;
    
    /**
     * Green waste taken to recycling/valorisation center (tonnes/year)
     * Includes surplus from composting and collection
     */
    private double greenWaste_valorisationCenter;//dechitrie 
    
    // ============== CONSTRUCTOR ==============
    
    /**
     * Create a new household agent
     * 
     * Phase 1: All households in same territory are identical
     * Phase 2: Will add variation parameters here
     * 
     * @param id Unique household identifier (0-based index)
     * @param territory Parent collection territory
     * @param hhSize Number of persons per household (from input file)
     */
    public Household(int id, CollectionTerritory territory, double hhSize, double thresholdFoodCompost) { // NEW PARAMETER
        this.householdId = id;
        this.myTerritory = territory;
        this.householdSize = hhSize;
        
        // NEW: Set threshold
    this.threshold_foodComposting = thresholdFoodCompost; // new addition 15 october 2025
    this.hasAdopted_foodComposting = false; // Start as not adopted
        
        // Initialize all flows to zero
        this.foodWasteProduced = 0.0;
        this.greenWasteProduced = 0.0;
        this.foodWaste_homeComposted = 0.0;
        this.foodWaste_dedicatedCollection = 0.0;
        this.foodWaste_residualBin = 0.0;
        this.greenWaste_homeComposted = 0.0;
        this.greenWaste_dedicatedCollection = 0.0;
        this.greenWaste_valorisationCenter = 0.0;
    }
    
    // ============== FLOW ASSIGNMENT (called by territory) ==============
    
    /**
     * Territory assigns this household's share of aggregate flows
     * 
     * Called once per year AFTER territory completes all SD calculations:
     * 1. Territory calculates aggregate flows (Bpf, Bpg, Bcf, Bsf, Br, etc.)
     * 2. Territory applies capacity constraints (home composting, collection)
     * 3. Territory divides final flows by numberOfHouseholds
     * 4. Territory calls this method for each household
     * 
     * This ensures: sum(household flows) = territory totals (exact equivalence)
     * 
     * @param foodProduced Food waste produced (tonnes/year)
     * @param greenProduced Green waste produced (tonnes/year)
     * @param foodCompost Food waste actually home composted (tonnes/year)
     * @param foodCollection Food waste actually collected (tonnes/year)
     * @param foodResidual Food waste to residual bin (tonnes/year)
     * @param greenCompost Green waste actually home composted (tonnes/year)
     * @param greenCollection Green waste actually collected (tonnes/year)
     * @param greenValor Green waste to valorisation center (tonnes/year)
     */
    public void assignFlows(double foodProduced, double greenProduced,
                           double foodCompost, double foodCollection, double foodResidual,
                           double greenCompost, double greenCollection, double greenValor) {
        // Production
        this.foodWasteProduced = foodProduced;
        this.greenWasteProduced = greenProduced;
        
        // Food waste destinations
        this.foodWaste_homeComposted = foodCompost;
        this.foodWaste_dedicatedCollection = foodCollection;
        this.foodWaste_residualBin = foodResidual;
        
        // Green waste destinations
        this.greenWaste_homeComposted = greenCompost;
        this.greenWaste_dedicatedCollection = greenCollection;
        this.greenWaste_valorisationCenter = greenValor;
    }
    
    // ============== BASIC GETTERS ==============
    
    /**
     * Get unique household identifier
     * @return Household ID (0-based)
     */
    public int getHouseholdId() {
        return householdId;
    }
    
    /**
     * Get household size (number of persons)
     * @return Persons per household (e.g., 2.1)
     */
    public double getHouseholdSize() {
        return householdSize;
    }
    
    /**
     * Get reference to parent territory
     * @return CollectionTerritory this household belongs to
     */
    public CollectionTerritory getTerritory() {
        return myTerritory;
    }
    
    // ============== WASTE PRODUCTION GETTERS ==============
    
    /**
     * Get food waste produced by this household
     * @return Food waste in tonnes/year
     */
    public double getFoodWasteProduced() {
        return foodWasteProduced;
    }
    
    /**
     * Get green waste produced by this household
     * @return Green waste in tonnes/year
     */
    public double getGreenWasteProduced() {
        return greenWasteProduced;
    }
    
    /**
     * Get total biowaste produced (food + green)
     * @return Total biowaste in tonnes/year
     */
    public double getTotalBiowasteProduced() {
        return foodWasteProduced + greenWasteProduced;
    }
    
    // ============== FOOD WASTE DESTINATION GETTERS ==============
    
    /**
     * Get food waste composted at home
     * @return Food composted in tonnes/year
     */
    public double getFoodWaste_homeComposted() {
        return foodWaste_homeComposted;
    }
    
    /**
     * Get food waste collected via dedicated collection
     * @return Food collected in tonnes/year
     */
    public double getFoodWaste_dedicatedCollection() {
        return foodWaste_dedicatedCollection;
    }
    
    /**
     * Get food waste going to residual bin
     * @return Food to residual waste in tonnes/year
     */
    public double getFoodWaste_residualBin() {
        return foodWaste_residualBin;
    }
    
    // ============== GREEN WASTE DESTINATION GETTERS ==============
    
    /**
     * Get green waste composted at home
     * @return Green composted in tonnes/year
     */
    public double getGreenWaste_homeComposted() {
        return greenWaste_homeComposted;
    }
    
    /**
     * Get green waste collected via dedicated collection
     * @return Green collected in tonnes/year
     */
    public double getGreenWaste_dedicatedCollection() {
        return greenWaste_dedicatedCollection;
    }
    
    /**
     * Get green waste taken to valorisation center
     * @return Green to valorisation in tonnes/year
     */
    public double getGreenWaste_valorisationCenter() {
        return greenWaste_valorisationCenter;
    }
    
    // ============== BEHAVIORAL INTENTION GETTERS ==============
    // Note: Households don't store behavioral intentions in Phase 1
    // They query territory for current values
    
    /**
     * Get food composting behavioral intention for given year
     * Households query territory's sigmoid curve (don't store locally)
     * 
     * @param year Simulation year
     * @return αcf value (0.0 to 1.0)
     */
    public double getAlphaCf(int year) {
        return myTerritory.getAlphaCf(year);
    }
    
    /**
     * Get green composting behavioral intention for given year
     * 
     * @param year Simulation year
     * @return αcg value (0.0 to 1.0)
     */
    public double getAlphaCg(int year) {
        return myTerritory.getAlphaCg(year);
    }
    
    /**
     * Get food sorting behavioral intention for given year
     * 
     * @param year Simulation year
     * @return αsf value (0.0 to 1.0)
     */
    public double getAlphaSf(int year) {
        return myTerritory.getAlphaSf(year);
    }
    
    /**
     * Get green sorting behavioral intention for given year
     * 
     * @param year Simulation year
     * @return αsg value (0.0 to 1.0)
     */
    public double getAlphaSg(int year) {
        return myTerritory.getAlphaSg(year);
    }
    
    // ============== DERIVED METRICS ==============
    
    /**
     * Calculate total waste managed "sustainably"
     * (composted at home + collected for treatment)
     * 
     * @return Tonnes/year of diverted waste
     */
    public double getTotalDivertedWaste() {
        return foodWaste_homeComposted + foodWaste_dedicatedCollection +
               greenWaste_homeComposted + greenWaste_dedicatedCollection +
               greenWaste_valorisationCenter;
    }
    
    /**
     * Calculate waste diversion rate
     * (diverted / total produced)
     * 
     * @return Fraction diverted (0.0 to 1.0)
     */
    public double getDiversionRate() {
        double total = getTotalBiowasteProduced();
        if (total == 0.0) return 0.0;
        return getTotalDivertedWaste() / total;
    }
    
    /**
     * Calculate per-capita food waste production
     * 
     * @return Tonnes per person per year
     */
    public double getFoodWastePerCapita() {
        if (householdSize == 0.0) return 0.0;
        return foodWasteProduced / householdSize;
    }
    
    /**
     * Calculate per-capita green waste production
     * 
     * @return Tonnes per person per year
     */
    public double getGreenWastePerCapita() {
        if (householdSize == 0.0) return 0.0;
        return greenWasteProduced / householdSize;
    }
    
    /**
     * Calculate per-capita residual waste (what goes to regular trash/incinerator)
     * 
     * @return Tonnes per person per year
     */
    public double getResidualWastePerCapita() {
        if (householdSize == 0.0) return 0.0;
        return foodWaste_residualBin / householdSize;
    }
    
    // ============== MASS BALANCE VALIDATION ==============
    
    /**
     * Verify mass balance for food waste
     * Production should equal sum of destinations
     * 
     * @return true if balanced (within tolerance)
     */
    public boolean validateFoodWasteBalance() {
        double tolerance = 0.000001; // 1 gram
        double produced = foodWasteProduced;
        double allocated = foodWaste_homeComposted + 
                          foodWaste_dedicatedCollection + 
                          foodWaste_residualBin;
        return Math.abs(produced - allocated) < tolerance;
    }
    
    /**
     * Verify mass balance for green waste
     * Production should equal sum of destinations
     * 
     * @return true if balanced (within tolerance)
     */
    public boolean validateGreenWasteBalance() {
        double tolerance = 0.000001; // 1 gram
        double produced = greenWasteProduced;
        double allocated = greenWaste_homeComposted + 
                          greenWaste_dedicatedCollection + 
                          greenWaste_valorisationCenter;
        return Math.abs(produced - allocated) < tolerance;
    }
    
    /**
     * Validate both food and green waste balances
     * 
     * @return true if both balanced
     */
    public boolean validateMassBalance() {
        return validateFoodWasteBalance() && validateGreenWasteBalance();
    }
    
    // ============== DEBUGGING/REPORTING METHODS ==============
    
    /**
     * Get human-readable summary of household state//Generate a human-readable summary report of this household's waste flows * This method creates a detailed text report showing all waste production, * destinations, behavioral intentions, and validation status.
     * 
     * @param year Current simulation year
     * @return Multi-line string with household details
     */
    public String getSummary(int year) { // // StringBuilder is like a "text builder" - better than using + for many strings
    // its like a notepad where we write line by line
        StringBuilder sb = new StringBuilder();// Write the household ID as a header // Example output: "=== Household 42 ==="
        sb.append("=== Household ").append(householdId).append(" ===\n");
        sb.append("Territory: ").append(myTerritory.getIdent()).append("\n"); // Show which territory this household belongs to // Example: "Territory: 0"
        sb.append("Size: ").append(householdSize).append(" persons\n");
        sb.append("\nWaste Production (tonnes/year):\n");
        sb.append("  Food: ").append(String.format("%.6f", foodWasteProduced)).append("\n"); // String.format("%.6f", ...) means: format as decimal number with 6 digits after the dot // Example: 0.002430 tonnes
        sb.append("  Green: ").append(String.format("%.6f", greenWasteProduced)).append("\n");
        sb.append("  Total: ").append(String.format("%.6f", getTotalBiowasteProduced())).append("\n");
        
        sb.append("\nFood Waste Destinations:\n");
        sb.append("  Home compost: ").append(String.format("%.6f", foodWaste_homeComposted));
        sb.append(" (").append(String.format("%.1f%%", 100.0 * foodWaste_homeComposted / foodWasteProduced)).append(")\n");
        sb.append("  Collection: ").append(String.format("%.6f", foodWaste_dedicatedCollection));
        sb.append(" (").append(String.format("%.1f%%", 100.0 * foodWaste_dedicatedCollection / foodWasteProduced)).append(")\n");
        sb.append("  Residual: ").append(String.format("%.6f", foodWaste_residualBin));
        sb.append(" (").append(String.format("%.1f%%", 100.0 * foodWaste_residualBin / foodWasteProduced)).append(")\n");
        
        sb.append("\nGreen Waste Destinations:\n");
        sb.append("  Home compost: ").append(String.format("%.6f", greenWaste_homeComposted));
        sb.append(" (").append(String.format("%.1f%%", 100.0 * greenWaste_homeComposted / greenWasteProduced)).append(")\n");
        sb.append("  Collection: ").append(String.format("%.6f", greenWaste_dedicatedCollection));
        sb.append(" (").append(String.format("%.1f%%", 100.0 * greenWaste_dedicatedCollection / greenWasteProduced)).append(")\n");
        sb.append("  Valorisation: ").append(String.format("%.6f", greenWaste_valorisationCenter));
        sb.append(" (").append(String.format("%.1f%%", 100.0 * greenWaste_valorisationCenter / greenWasteProduced)).append(")\n");
        
        sb.append("\nBehavioral Intentions (Year ").append(year).append("):\n");
        sb.append("  αcf: ").append(String.format("%.3f", getAlphaCf(year))).append("\n");
        sb.append("  αcg: ").append(String.format("%.3f", getAlphaCg(year))).append("\n");
        sb.append("  αsf: ").append(String.format("%.3f", getAlphaSf(year))).append("\n");
        sb.append("  αsg: ").append(String.format("%.3f", getAlphaSg(year))).append("\n");
        
        sb.append("\nDiversion Rate: ").append(String.format("%.1f%%", 100.0 * getDiversionRate())).append("\n");
        sb.append("Mass Balance: ").append(validateMassBalance() ? "✓ Valid" : "✗ INVALID").append("\n");
        
        return sb.toString();
    }
    
    /**
     * Print household summary to console
     * 
     * @param year Current simulation year
     */
    public void printSummary(int year) {
        System.out.println(getSummary(year));
    }
    
    /**
     * Get CSV header for household data export
     * 
     * @return Semicolon-separated header string
     */
    public static String getCSVHeader() {
        return "householdId;territoryId;householdSize;" +
               "foodProduced;greenProduced;totalProduced;" +
               "foodCompost;foodCollection;foodResidual;" +
               "greenCompost;greenCollection;greenValor;" +
               "diversionRate;foodPerCapita;greenPerCapita;residualPerCapita";
    }
    
    /**
     * Get household data as CSV row
     * 
     * @return Semicolon-separated data string
     */
    public String toCSV() {
        return householdId + ";" +
               myTerritory.getIdent() + ";" +
               householdSize + ";" +
               foodWasteProduced + ";" +
               greenWasteProduced + ";" +
               getTotalBiowasteProduced() + ";" +
               foodWaste_homeComposted + ";" +
               foodWaste_dedicatedCollection + ";" +
               foodWaste_residualBin + ";" +
               greenWaste_homeComposted + ";" +
               greenWaste_dedicatedCollection + ";" +
               greenWaste_valorisationCenter + ";" +
               getDiversionRate() + ";" +
               getFoodWastePerCapita() + ";" +
               getGreenWastePerCapita() + ";" +
               getResidualWastePerCapita();
    }
    
    // ============== OBJECT METHODS ==============
    
    /**
     * String representation of household
     */
    @Override
    public String toString() {
        return "Household[id=" + householdId + 
               ", territory=" + myTerritory.getIdent() + 
               ", size=" + householdSize + 
               ", totalWaste=" + String.format("%.4f", getTotalBiowasteProduced()) + "t/y]";
    }
    
    /**
     * Equality based on household ID and territory
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Household other = (Household) obj;
        return this.householdId == other.householdId && 
               this.myTerritory == other.myTerritory;
    }
    
    /**
     *  * Generate a hash code for this household
     * Hash code based on household ID and territory
     * Hash codes are used by Java collections (like HashMap, HashSet) to quickly
 * find objects. its like a "filing cabinet number" - it helps Java
 * organize and find households .
 * Two households with the same ID in the same territory will have the same hash code.
 * Different households will (usually) have different hash codes.
     */
    @Override
    public int hashCode() {
    // Combine household ID and territory ID into a single number
    // Formula: 31 * householdId + territoryId
    // 
    // Why 31? It's a prime number that:
    // - Reduces hash collisions (different households getting same hash)
    // - Is fast to compute (31 * x = (x << 5) - x, a CPU trick)
    // - Is a Java convention (used by String.hashCode() too)
    //
    // Example:
    // Household 100 in Territory 0: 31 * 100 + 0 = 3100
    // Household 100 in Territory 1: 31 * 100 + 1 = 3101
    // Household 101 in Territory 0: 31 * 101 + 0 = 3131
        // for example  Book: "Harry Potter" >>> Hash code: 47382 >>> Go to shelf 47 >>> Find book quickly
        return 31 * householdId + myTerritory.getIdent();
    }
    // ============== NEW: Social norm methods ============== // 15 october 

/**
 * Check if household should adopt food composting based on territory rate
 * Called each year by territory
 * 
 * @param territoryAdoptionRate Fraction of territory that has adopted (0.0 to 1.0)
 */
public void checkAdoption_foodComposting(double territoryAdoptionRate) {
    // If I haven't adopted yet AND territory rate >= my threshold → ADOPT!
    if (!hasAdopted_foodComposting && territoryAdoptionRate >= threshold_foodComposting) {
        hasAdopted_foodComposting = true;
        System.err.println("  → Household " + householdId + 
                          " adopted food composting (threshold=" + 
                          String.format("%.2f", threshold_foodComposting) + 
                          ", territory_rate=" + 
                          String.format("%.2f", territoryAdoptionRate) + ")");
    }
}

/**
 * Get household's personal intention for food composting
 * Returns 1.0 if adopted, 0.0 if not
 */
public double getPersonalIntention_foodComposting() {
    return hasAdopted_foodComposting ? 1.0 : 0.0;
}

/**
 * Check if this household has adopted food composting
 */
public boolean hasAdoptedFoodComposting() {
    return hasAdopted_foodComposting;
}

/**
 * Get adoption threshold for food composting
 */
public double getThreshold_foodComposting() {
    return threshold_foodComposting;
}
    
}
