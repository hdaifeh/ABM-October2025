import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CollectionTerritory - Refactored from CollectionTerritories for ABM Phase 1
 * 
 * Phase 1 Strategy: Minimal changes to original SD model
 * - All aggregate calculations remain unchanged
 * - Households created as containers for disaggregated flows
 * - Territory distributes results equally to all households
 * - Exact mathematical equivalence with original SD model
 * 
 * Changes from original:
 * 1. Added household list and management methods
 * 2. Added householdSize parameter (read from input file)
 * 3. Added distributeFlowsToHouseholds() method
 * 4. All original SD methods preserved exactly as-is
 * 
 * @author shuet (original SD model)
 * @author [your name] (ABM refactoring)
 */
public class CollectionTerritory {

    Territory myTerre;
    
    // ============== NEW: HOUSEHOLD MANAGEMENT ==============
    private List<Household> households;
    private int numberOfHouseholds;
    private double householdSize; // NEW PARAMETER: read from params[24]
    private double[] territoryAdoptionRate_foodComposting;  // NEW: Track adoption rate over time 15 October 
    // ============== ORIGINAL SD MODEL VARIABLES (UNCHANGED) ==============
    
    // Define the starting point of the simulation
    int timeBeforeInit_αcf_initial; // time before init for food composting behavioral intention
    int timeBeforeInit_αcg_initial; // time before init for green composting behavioral intention
    int timeBeforeInit_αsf_initial; // time before init for food sorting behavioral intention
    int timeBeforeInit_αsg_initial; // time before init for green sorting behavioral intention

    double Kc_initial; // initial capacity of home composting 
    double Ks_initial; // initial dedicated collection capacity 
    double[] Kct; // linear evolution of home composter capacity
    double[] Kst; // linear evolution of dedicated collection capacity
    double αc_target; // planned maximum capacity of home composter 
    double αs_target; // planned maximum capacity of dedicated collection
    int yearRef; // reference year

    double[] LinearHomeComposter; // linear function for home composter capacity evolution
    double[] sigmoide_mcf; // sigmoid for food composting behavioral intention
    double[] sigmoide_mcg; // sigmoid for green composting behavioral intention
    double[] LinearDedicatedCollection; // linear function for dedicated collection capacity
    double[] sigmoide_msf; // sigmoid for food sorting behavioral intention
    double[] sigmoide_msg; // sigmoid for green sorting behavioral intention
    double[] sigmoide_mpg; // sigmoid for green waste reduction behavior
    
    double αcf_initial; // initial behavioral intention of food composting 
    double αcg_initial; // initial behavioral intention of green composting
    double αcf_max; // maximum evolution of food composting behavioral intention
    double αcg_max; // maximum evolution of green composting behavioral intention
    double αsf_initial; // initial behavioral intention of food sorting for collection
    double αsf_max; // maximum evolution of food sorting behavioral intention
    double αsg_initial; // initial behavioral intention of green sorting for collection
    double αsg_max; // maximum evolution of green sorting behavioral intention

    double b_pf; // baseline food waste production per capita (tonnes/person/year)
    double b_pg; // baseline green waste production per capita (tonnes/person/year)

    double αv; // volume of green waste sent to valorisation centre 
    double r; // annual population growth rate
    int sizePop; // population size

    double duraImplemCompo; // duration for home composter capacity development
    double mc; // inflexion point of home composting sigmoid
    double duraImplemCollect; // duration for dedicated collection capacity development
    double ms; // inflexion point of sorting sigmoid
    double mpg; // inflexion point of green waste reduction sigmoid
    double αpg_target; // target reduction for green waste
    double αpf_target; // target reduction for food waste

    // Aggregate arrays (computed from population dynamics)
    double[] P; // population size at year t
    double[] B; // total biowaste produced
    double[] Bpg; // green waste production
    double[] Bpf; // food waste production
    double[] ABP; // food waste reduction from anti-biowaste plan
    double[] R; // rate of food waste reduction
    double[] G; // quantity reduction in food waste
    double[] αcf; // food composting behavioral intention over time
    double[] αcg; // green composting behavioral intention over time
    double[] αvg; // volume fraction to valorisation centre
    double[] αsf; // food sorting behavioral intention over time
    double[] αsg; // green sorting behavioral intention over time
    double[] C_log; // composting logistics evolution
    double[] C_pop; // composting population evolution
    double[] Bcg; // green waste for home composting
    double[] Bcf; // food waste for home composting
    double[] Bcf_composted; // actually composted food waste
    double[] Bcg_composted; // actually composted green waste
    double[] Bc_composted; // total composted biowaste
    double[] Uc; // home composting surplus
    double[] Ucg; // green composting surplus
    double[] Ucf; // food composting surplus
    double[] sLbis; // intermediate composting surplus calculation
    double[] Bv; // green waste to valorisation centres
    double[] Bsg; // green waste for dedicated collection
    double[] Bsf; // food waste for dedicated collection
    double[] Bs_sorted; // total sorted biowaste
    double[] Bsf_sorted; // sorted food waste
    double[] Bsg_sorted; // sorted green waste
    double[] Usf; // food collection surplus
    double[] Usg; // green collection surplus
    double[] sAa_bis; // intermediate food collection surplus
    double[] sAv_bis; // intermediate green collection surplus
    double[] Us; // total collection surplus
    double[] sAbis; // intermediate collection surplus calculation
    double[] Br; // food waste to residual household waste

    int subTerritoryName; // territory identifier
    double[] propPopDesserviCollDA; // proportion of population served by collection
    double[] nbKgCollectHabDesservi; // kg collected per served inhabitant
    double[] nbKgOMRHab; // kg of residual waste per inhabitant
    double[] tauxReductionDechetVert; // green waste reduction rate
    int ident; // territory ID

    // ============== CONSTRUCTOR ==============
    
    public CollectionTerritory(Territory mt, int id) {
        myTerre = mt;
        ident = id;
        households = new ArrayList<>(); // NEW: Initialize household list
    }
    
    // ============== NEW: HOUSEHOLD INITIALIZATION ==============
    
    /**
     * Initialize households based on population size and household size
     * Called at end of init() after all parameters are set
     */
    private void initializeHouseholds() {
    numberOfHouseholds = (int) Math.round(sizePop / householdSize);
    
    households.clear();
    
    // NEW: Create random number generator for reproducible thresholds (15 October)
    java.util.Random rng = new java.util.Random(42 + ident); // Seed = 42 + territory ID
    
    for (int i = 0; i < numberOfHouseholds; i++) {
        // NEW: Sample random threshold for each household
        // Range: [0.0, 1.0] with more households having middle values
        // Using triangular distribution: most around 0.3-0.5, fewer at extremes
        double threshold = sampleThreshold(rng);
        
        // CREATE household with threshold
        Household hh = new Household(i, this, householdSize, threshold);
        households.add(hh);
    }
    
    System.err.println("Territory " + ident + ": Created " + numberOfHouseholds + 
                      " households with social norm thresholds");
}

/**
 * Sample adoption threshold for a household
 * Uses simple uniform distribution for now
 * 
 * @param rng Random number generator
 * @return Threshold between 0.0 and 1.0
 */
private double sampleThreshold(java.util.Random rng) {
    // Simple approach: uniform random between 0.1 and 0.9
    // This creates mix of early adopters (0.1), mainstream (0.5), late adopters (0.9)
    return 0.1 + rng.nextDouble() * 0.8;  // Range: [0.1, 0.9]
}
    
    // ============== MAIN ITERATION METHOD (MODIFIED) ==============
    
    /**
     * Main iteration method - called each year
     * Modified to add household distribution at the end
     */
    public void iterate(int year) {
    LinearHomeComposter[year] = linear(year, duraImplemCompo);
    LinearDedicatedCollection[year] = linear(year, duraImplemCollect);
    
    if (myTerre.useSocialDynamics) {
        sigmoide_mcf[year] = sigmoide(year + timeBeforeInit_αcf_initial, mc);
        sigmoide_mcg[year] = sigmoide(year + timeBeforeInit_αcg_initial, mc);
        sigmoide_msf[year] = sigmoide(year + timeBeforeInit_αsf_initial, ms);
        sigmoide_msg[year] = sigmoide(year + timeBeforeInit_αsg_initial, ms);
        sigmoide_mpg[year] = sigmoide(year, mpg);
    }

    computeProducedBioWaste(year);
    computeFluxRates(year);
    
    // ========== NEW: SOCIAL NORM LOGIC (3 lines) ==========
    updateTerritoryAdoptionRate(year);      // Line 1: Calculate territory adoption rate
    updateHouseholdAdoptions(year);          // Line 2: Each household checks if they adopt
    updateAlphaFromHouseholds(year);         // Line 3: Update αcf based on actual adoptions
    // ======================================================
    
    localCompost(year);
    collect(year);
    recyclingCentre(year);
    residualHouseholdWaste(year);
    
    distributeFlowsToHouseholds(year);
}
    
    // ============== NEW: DISTRIBUTE FLOWS TO HOUSEHOLDS ==============
    
    /**
     * After all SD calculations, divide aggregate flows equally among households
     * This ensures exact equivalence: sum of household flows = territory totals
     * 
     * @param year Current simulation year
     */
    private void distributeFlowsToHouseholds(int year) {
        if (numberOfHouseholds == 0) {
            System.err.println("Warning: Territory " + ident + " has no households!");
            return;
        }
        
        // Calculate per-household shares (equal distribution in Phase 1)
        double foodProduced_perHH = Bpf[year] / numberOfHouseholds;
        double greenProduced_perHH = Bpg[year] / numberOfHouseholds;
        
        // Note: Use Bcf[year] and Bcg[year] which already have surplus removed
        double foodCompost_perHH = Bcf[year] / numberOfHouseholds;
        double greenCompost_perHH = Bcg[year] / numberOfHouseholds;
        
        // Collection flows (already adjusted for capacity)
        double foodCollection_perHH = Bsf[year] / numberOfHouseholds;
        double greenCollection_perHH = Bsg[year] / numberOfHouseholds;
        
        // Other destinations
        double foodResidual_perHH = Br[year] / numberOfHouseholds;
        double greenValor_perHH = Bv[year] / numberOfHouseholds;
        
        // Assign flows to all households
        for (Household hh : households) {
            hh.assignFlows(
                foodProduced_perHH, greenProduced_perHH,
                foodCompost_perHH, foodCollection_perHH, foodResidual_perHH,
                greenCompost_perHH, greenCollection_perHH, greenValor_perHH
            );
        }
    }
    
    // ============== ORIGINAL SD MODEL METHODS (UNCHANGED) ==============
    
    /**
     * Calculate biowaste production with ABP effect
     * ORIGINAL METHOD - NO CHANGES
     */
    public void computeProducedBioWaste(int y) {
        P[y] = P[y - 1] * (1 + r); // Population size at time t
       
        R[y] = αpf_target * myTerre.sigmoideABP[y]; // food waste reduction rate
        ABP[y] = R[y] * G[y]; // food waste amount removed
        
        // Green waste production with reduction
        Bpg[y] = b_pg * (1 - αpg_target * myTerre.sigmoideABP[y]) * P[y];
        
        // Food waste production with reduction
        Bpf[y] = b_pf * (1 - αpf_target * myTerre.sigmoideABP[y] * myTerre.einit) * P[y];
        
        B[y] = Bpg[y] + Bpf[y]; // Total biowaste produced
    }

    /**
     * Compute behavioral intentions (alpha values) for this year
     * ORIGINAL METHOD - NO CHANGES
     */
    public void computeFluxRates(int y) {
        double trucDa;
        double trucDv;
        
        // Food composting intention
        αcf[y] = Math.min((αcf_initial + ((1 - αcf_initial) * sigmoide_mcf[y - 1])), 1.0);
        
        // Green composting intention
        αcg[y] = Math.min((αcg_initial + ((1 - αcg_initial) * sigmoide_mcg[y - 1])), 1.0);
        
        // Food sorting intention (prioritize composting over collection)
        αsf[y] = αsf_initial + ((1 - αsf_initial) * sigmoide_msf[y]);
        trucDa = αcf[y] + αsf[y];
        if (trucDa > 1.0) {
            αsf[y] = (1 - αcf[y]); // Reduce sorting if sum exceeds 1
        }
        
        // Green sorting intention
        αsg[y] = αsg_initial + ((αsg_max - αsg_initial) * sigmoide_msg[y]);
        trucDv = αcg[y] + αsg[y];
        if (trucDv > 1.0) {
            αsg[y] = 1.0 - αcg[y]; // Reduce sorting if sum exceeds 1
        }
        
        // Remainder goes to valorisation centre
        αvg[y] = 1 - αcg[y] - αsg[y];
    }

    /**
     * Calculate local composting flows with capacity constraints
     * ORIGINAL METHOD - NO CHANGES
     */
    public void localCompost(int y) {
        Bcg[y] = αcg[y] * Bpg[y]; // Green waste intended for composting
        Bcf[y] = αcf[y] * Bpf[y]; // Food waste intended for composting
        
        // Calibration: set initial capacity equal to initial demand
        if (y == yearRef) {
            Kc_initial = Bcg[y] + Bcf[y];
        }
        
        // Capacity grows linearly
        Kct[y] = Kc_initial + ((αc_target - Kc_initial) * LinearHomeComposter[y]);
        
        // Handle surplus if demand exceeds capacity
        if ((Bcg[y] + Bcf[y]) > Kct[y]) {
            Uc[y] = Bcg[y] + Bcf[y] - Kct[y]; // Total surplus
            
            // First remove green waste
            Bcg_composted[y] = Math.max(Bcg[y] - Uc[y], 0.0);
            
            // Check if surplus remains
            sLbis[y] = Math.max(0.0, (Bcg_composted[y] + Bcf[y] - Kct[y]));
            
            // Then remove food waste
            Bcf_composted[y] = Math.max(Bcf[y] - sLbis[y], 0.0);
            
            // Track individual surpluses
            Ucf[y] = Math.min(sLbis[y], Bcf[y]); // Food surplus → goes to collection
            Ucg[y] = Math.min(Uc[y], Bcg[y]); // Green surplus → goes to valorisation
            
            // Update actual composted amounts
            Bcg[y] = Bcg_composted[y];
            Bcf[y] = Bcf_composted[y];
        }
        
        Bc_composted[y] = Bcf[y] + Bcg[y]; // Total actually composted
    }

    /**
     * Calculate dedicated collection flows with capacity constraints
     * ORIGINAL METHOD - NO CHANGES
     */
    public void collect(int y) {
        Bsg[y] = αsg[y] * Bpg[y]; // Green waste intended for collection
        Bsf[y] = (αsf[y] * Bpf[y]) + Ucf[y]; // Food waste + composting food surplus
        
        // Capacity grows linearly
        Kst[y] = Ks_initial + ((αs_target - Ks_initial) * LinearDedicatedCollection[y]);
        
        // Handle surplus if demand exceeds capacity
        if ((Bsg[y] + Bsf[y]) > Kst[y]) {
            Us[y] = Bsf[y] + Bsg[y] - Kst[y]; // Total surplus
            
            // First remove green waste
            Bsg_sorted[y] = Math.max(Bsg[y] - Us[y], 0.0);
            
            // Check if surplus remains
            sAbis[y] = Math.max(0.0, (Bsf[y] + Bsg_sorted[y] - Kst[y]));
            
            // Then remove food waste
            Bsf_sorted[y] = Math.max(Bsf[y] - sAbis[y], 0.0);
            
            // Track individual surpluses
            Usg[y] = Math.min(Us[y], Bsg[y]); // Green surplus → goes to valorisation
            Usf[y] = Math.min(sAbis[y], Bsf[y]); // Food surplus → goes to residual
            
            // Update actual collected amounts
            Bsg[y] = Bsg_sorted[y];
            Bsf[y] = Bsf_sorted[y];
        }
        
        Bs_sorted[y] = Bsg[y] + Bsf[y]; // Total actually collected
    }

    /**
     * Calculate flow to recycling centre (green waste only)
     * ORIGINAL METHOD - NO CHANGES
     */
    public void recyclingCentre(int y) {
        // Green waste to valorisation = intended + composting surplus + collection surplus
        Bv[y] = αvg[y] * Bpg[y] + Ucg[y] + Usg[y];
    }

    /**
     * Calculate residual household waste (food waste only)
     * ORIGINAL METHOD - NO CHANGES
     */
    public void residualHouseholdWaste(int y) {
        // Food waste to residual = not composted, not collected + collection surplus
        Br[y] = (1 - αcf[y] - αsf[y]) * Bpf[y] + Usf[y];
        
        if (Br[y] < 0) {
            System.err.println("ERROR: Negative residual waste at year " + y + 
                             " - αsf=" + αsf[y] + " αcf=" + αcf[y] + 
                             " Bpf=" + Bpf[y] + " Usf=" + Usf[y]);
        }
    }

    /**
     * Sigmoid function for behavioral diffusion
     * ORIGINAL METHOD - NO CHANGES
     */
    public double sigmoide(double x, double ti) {
        double t = Math.pow(x, 5);
        double z = t / (t + Math.pow(ti, 5)); // ti is inflexion point
        return z;
    }

    /**
     * Linear growth function for capacity expansion
     * ORIGINAL METHOD - NO CHANGES
     */
    public double linear(double t, double duration) {
        return Math.min(t / duration, 1.0);
    }

    /**
     * Calculate time before initial observation for behavioral intention
     * ORIGINAL METHOD - NO CHANGES
     */
    public int calculateTimeBeforeInit(double alpha_base, double ti) {
        int timeBeforeInit = 0;
        
        if (alpha_base > 0) {
            double sigmoideValue = sigmoide(timeBeforeInit, ti);
            while (sigmoideValue < alpha_base) {
                timeBeforeInit++;
                sigmoideValue = sigmoide(timeBeforeInit, ti);
            }
        }
        
        return timeBeforeInit;
    }

    /**
     * Initialize territory with parameters
     * MODIFIED: Added householdSize parameter (params[24])
     */
    public void init(int sizeData, double[] params, int refYear) {
        yearRef = refYear;
        subTerritoryName = (int) params[0];
        duraImplemCompo = params[1];
        duraImplemCollect = params[2];
        mc = params[3];
        ms = params[4];
        b_pf = params[5];
        b_pg = params[6];
        αcf_initial = params[7];
        αcg_initial = params[8];
        αsf_initial = params[9];
        αsf_max = params[10];
        αcf_max = params[11];
        αcg_max = params[12];
        αsg_initial = params[13];
        αsg_max = params[14];
        Kc_initial = params[15];
        αc_target = params[16];
        Ks_initial = params[17];
        αs_target = params[18];
        sizePop = (int) params[19];
        r = params[20]; // Can be set to 0 in input file for Phase 1
        mpg = params[21];
        αpg_target = params[22];
        αpf_target = params[23];
        householdSize = params[24]; // NEW: Read household size from input file
        
        // Calculate time before initialization for behavioral intentions
        timeBeforeInit_αcf_initial = calculateTimeBeforeInit(αcf_initial, mc);
        timeBeforeInit_αcg_initial = calculateTimeBeforeInit(αcg_initial, mc);
        timeBeforeInit_αsf_initial = calculateTimeBeforeInit(αsf_initial, ms);
        timeBeforeInit_αsg_initial = calculateTimeBeforeInit(αsg_initial, ms);

        // Initialize all arrays
        P = new double[sizeData]; //expeTerritoryValtomSocioTechnicalOriginData
        Arrays.fill(P, 0.0);
        P[0] = sizePop;
        
        R = new double[sizeData];
        Arrays.fill(R, 0.0);
        
        ABP = new double[sizeData];
        Arrays.fill(ABP, 0.0);
        
        G = new double[sizeData];
        Arrays.fill(G, 0.0);
        
        B = new double[sizeData];
        Arrays.fill(B, 0.0);
        
        Bpg = new double[sizeData];
        Arrays.fill(Bpg, 0.0);
        
        Bpf = new double[sizeData];
        Arrays.fill(Bpf, 0.0);
        
        αcf = new double[sizeData];
        Arrays.fill(αcf, 0.0);
        
        αcg = new double[sizeData];
        Arrays.fill(αcg, 0.0);
        
        αvg = new double[sizeData];
        Arrays.fill(αvg, 0.0);
        
        C_log = new double[sizeData];
        Arrays.fill(C_log, 0.0);
        
        C_pop = new double[sizeData];
        Arrays.fill(C_pop, 0.0);
        
        Bc_composted = new double[sizeData];
        Arrays.fill(Bc_composted, 0.0);
        
        Bcg = new double[sizeData];
        Arrays.fill(Bcg, 0.0);
        
        Bcf = new double[sizeData];
        Arrays.fill(Bcf, 0.0);
        
        Uc = new double[sizeData];
        Arrays.fill(Uc, 0.0);
        
        Ucf = new double[sizeData];
        Arrays.fill(Ucf, 0.0);
        
        Ucg = new double[sizeData];
        Arrays.fill(Ucg, 0.0);
        
        Bcg_composted = new double[sizeData];
        Arrays.fill(Bcg_composted, 0.0);
        
        Bcf_composted = new double[sizeData];
        Arrays.fill(Bcf_composted, 0.0);
        
        sLbis = new double[sizeData];
        Arrays.fill(sLbis, 0.0);
        
        Bv = new double[sizeData];
        Arrays.fill(Bv, 0.0);
        
        Usg = new double[sizeData];
        Arrays.fill(Usg, 0.0);
        
        Br = new double[sizeData];
        Arrays.fill(Br, 0.0);
        
        Kst = new double[sizeData];
        Arrays.fill(Kst, 0.0);
        
        Kct = new double[sizeData];
        Arrays.fill(Kct, 0.0);

        LinearHomeComposter = new double[sizeData];
        Arrays.fill(LinearHomeComposter, 0.0);
        
        sigmoide_mcf = new double[sizeData];
        Arrays.fill(sigmoide_mcf, 0.0);
        
        sigmoide_mcg = new double[sizeData];
        Arrays.fill(sigmoide_mcg, 0.0);
        
        LinearDedicatedCollection = new double[sizeData];
        Arrays.fill(LinearDedicatedCollection, 0.0);
        
        sigmoide_msf = new double[sizeData];
        Arrays.fill(sigmoide_msf, 0.0);
        
        sigmoide_msg = new double[sizeData];
        Arrays.fill(sigmoide_msg, 0.0);
        
        sigmoide_mpg = new double[sizeData];
        Arrays.fill(sigmoide_mpg, 0.0);
        
        Bsg = new double[sizeData];
        Arrays.fill(Bsg, 0.0);
        
        Bsf = new double[sizeData];
        Arrays.fill(Bsf, 0.0);
        
        Bsf_sorted = new double[sizeData];
        Arrays.fill(Bsf_sorted, 0.0);
        
        Bsg_sorted = new double[sizeData];
        Arrays.fill(Bsg_sorted, 0.0);
        
        Bs_sorted = new double[sizeData];
        Arrays.fill(Bs_sorted, 0.0);
        
        Us = new double[sizeData];
        Arrays.fill(Us, 0.0);
        
        sAbis = new double[sizeData];
        Arrays.fill(sAbis, 0.0);
        
        Usf = new double[sizeData];
        Arrays.fill(Usf, 0.0);

        propPopDesserviCollDA = new double[sizeData];
        Arrays.fill(propPopDesserviCollDA, 0.0);
        
        nbKgCollectHabDesservi = new double[sizeData];
        Arrays.fill(nbKgCollectHabDesservi, 0.0);
        
        nbKgOMRHab = new double[sizeData];
        Arrays.fill(nbKgOMRHab, 0.0);
        
        tauxReductionDechetVert = new double[sizeData];
        Arrays.fill(tauxReductionDechetVert, 0.0);
        
        αsg = new double[sizeData];
        Arrays.fill(αsg, 0.0);
        
        αsf = new double[sizeData];
        Arrays.fill(αsf, 0.0);
        territoryAdoptionRate_foodComposting = new double[sizeData];  // NEW LINE 15 october 
Arrays.fill(territoryAdoptionRate_foodComposting, 0.0);       // NEW LINE 15 october 
        // Initial year calculations
        Bpf[0] = b_pf * P[0];
        Bpg[0] = b_pg * P[0];
        Bcg[0] = Bpg[0] * αcg_initial;
        Bcf[0] = Bpf[0] * αcf_initial;
        Bsf[0] = Bpf[0] * αsf_initial;
        Bsg[0] = Bpg[0] * αsg_initial;
        Bv[0] = Bpg[0] - Bcg[0] - Bsg[0];
        Br[0] = Bpf[0] - Bcf[0] - Bsf[0];
        
        // NEW: Initialize households after all parameters are set
        initializeHouseholds();
    }

    /**
     * Print vector for debugging
     * ORIGINAL METHOD - NO CHANGES
     */
    public void printVector(double[] edit) {
        for (int i = 0; i < edit.length; i++) {
            System.err.print(edit[i] + "\t");
        }
        System.err.println();
    }

    /**
     * Calculate territory indicators
     * ORIGINAL METHOD - NO CHANGES
     */
    public void indicSubTerritories(int year) {
        // Number of inhabitants served (based on 39 kg/person collection target)
        double nbHabDesservi = Math.min(P[year], (double) Kst[year] / (39.0 / 1000.0));
        
        propPopDesserviCollDA[year] = nbHabDesservi / P[year];
        
        if (nbHabDesservi > 0) {
            nbKgCollectHabDesservi[year] = (Bsf[year] * 1000.0) / nbHabDesservi;
        }
        
        nbKgOMRHab[year] = (Br[year] * 1000.0) / P[year];
        
        // Evolution rate of green waste in recycling centre
        tauxReductionDechetVert[year] = (Bv[year] - Bv[0]) / Bv[0];
    }

    /**
     * Print trajectory for output file
     * ORIGINAL METHOD - NO CHANGES
     */
    public void printTrajectory(int year) {
        System.out.print(ident + ";");
        System.out.print(P[year] + ";");
        System.out.print(Bpf[year] + ";");
        System.out.print(Bpg[year] + ";");
        System.out.print(αcf[year] + ";");
        System.out.print(αcg[year] + ";");
        System.out.print(Bcf[year] + ";");
        System.out.print(Ucf[year] + ";");
        System.out.print(Bcg[year] + ";");
        System.out.print(Ucg[year] + ";");
        System.out.print(Kct[year] + ";");
        System.out.print(αsf[year] + ";");
        System.out.print(αsg[year] + ";");
        System.out.print(Bsf[year] + ";");
        System.out.print(Usf[year] + ";");
        System.out.print(Bsg[year] + ";");
        System.out.print(Usg[year] + ";");
        System.out.print(Kst[year] + ";");
        System.out.print(Br[year] + ";");
        System.out.print(αvg[year] + ";");
        System.out.print(Bv[year] + ";");
    }
    
// ============== NEW: GETTERS FOR HOUSEHOLDS ==============
    
    /**
     * Get list of all households (returns copy for safety)
     */
    public List<Household> getHouseholds() {
        return new ArrayList<>(households);
    }
    
    /**
     * Get number of households in this territory
     */
    public int getNumberOfHouseholds() {
        return numberOfHouseholds;
    }
    
    /**
     * Get household size parameter
     */
    public double getHouseholdSize() {
        return householdSize;
    }
    
    // ============== GETTERS FOR HOUSEHOLD ACCESS TO TERRITORY DATA ==============
    
    /**
     * Get food composting behavioral intention for given year
     */
    public double getAlphaCf(int year) {
        if (year < 0 || year >= αcf.length) return 0.0;
        return αcf[year];
    }
    
    /**
     * Get green composting behavioral intention for given year
     */
    public double getAlphaCg(int year) {
        if (year < 0 || year >= αcg.length) return 0.0;
        return αcg[year];
    }
    
    /**
     * Get food sorting behavioral intention for given year
     */
    public double getAlphaSf(int year) {
        if (year < 0 || year >= αsf.length) return 0.0;
        return αsf[year];
    }
    
    /**
     * Get green sorting behavioral intention for given year
     */
    public double getAlphaSg(int year) {
        if (year < 0 || year >= αsg.length) return 0.0;
        return αsg[year];
    }
    
    /**
     * Get baseline food waste per capita
     */
    public double getBaselineFoodWastePerCapita() {
        return b_pf;
    }
    
    /**
     * Get baseline green waste per capita
     */
    public double getBaselineGreenWastePerCapita() {
        return b_pg;
    }
    
    /**
     * Get food waste reduction target (Anti-Biowaste Plan)
     */
    public double getAlphaPfTarget() {
        return αpf_target;
    }
    
    /**
     * Get green waste reduction target (Anti-Biowaste Plan)
     */
    public double getAlphaPgTarget() {
        return αpg_target;
    }
    
    /**
     * Get territory identifier
     */
    public int getIdent() {
        return ident;
    }
    
    // ============== VALIDATION METHODS (FOR DEBUGGING) ==============
    
    /**
     * Validate that household flows sum to territory totals
     * Call this after distributeFlowsToHouseholds() for debugging
     * 
     * @param year Year to validate
     * @return true if validation passes
     */
    public boolean validateHouseholdAggregation(int year) {
        if (households.isEmpty()) {
            System.err.println("Warning: No households to validate");
            return false;
        }
        
        // Sum household flows
        double sum_foodProduced = 0.0;
        double sum_greenProduced = 0.0;
        double sum_foodCompost = 0.0;
        double sum_greenCompost = 0.0;
        double sum_foodCollection = 0.0;
        double sum_greenCollection = 0.0;
        double sum_foodResidual = 0.0;
        double sum_greenValor = 0.0;
        
        for (Household hh : households) {
            sum_foodProduced += hh.getFoodWasteProduced();
            sum_greenProduced += hh.getGreenWasteProduced();
            sum_foodCompost += hh.getFoodWaste_homeComposted();
            sum_greenCompost += hh.getGreenWaste_homeComposted();
            sum_foodCollection += hh.getFoodWaste_dedicatedCollection();
            sum_greenCollection += hh.getGreenWaste_dedicatedCollection();
            sum_foodResidual += hh.getFoodWaste_residualBin();
            sum_greenValor += hh.getGreenWaste_valorisationCenter();
        }
        
        // Tolerance for floating point comparison
        double epsilon = 0.001;
        
        boolean valid = true;
        
        // Check food waste production
        if (Math.abs(sum_foodProduced - Bpf[year]) > epsilon) {
            System.err.println("ERROR: Food production mismatch at year " + year);
            System.err.println("  Sum of households: " + sum_foodProduced);
            System.err.println("  Territory total: " + Bpf[year]);
            valid = false;
        }
        
        // Check green waste production
        if (Math.abs(sum_greenProduced - Bpg[year]) > epsilon) {
            System.err.println("ERROR: Green production mismatch at year " + year);
            System.err.println("  Sum of households: " + sum_greenProduced);
            System.err.println("  Territory total: " + Bpg[year]);
            valid = false;
        }
        
        // Check food composting
        if (Math.abs(sum_foodCompost - Bcf[year]) > epsilon) {
            System.err.println("ERROR: Food composting mismatch at year " + year);
            System.err.println("  Sum of households: " + sum_foodCompost);
            System.err.println("  Territory total: " + Bcf[year]);
            valid = false;
        }
        
        // Check green composting
        if (Math.abs(sum_greenCompost - Bcg[year]) > epsilon) {
            System.err.println("ERROR: Green composting mismatch at year " + year);
            System.err.println("  Sum of households: " + sum_greenCompost);
            System.err.println("  Territory total: " + Bcg[year]);
            valid = false;
        }
        
        // Check food collection
        if (Math.abs(sum_foodCollection - Bsf[year]) > epsilon) {
            System.err.println("ERROR: Food collection mismatch at year " + year);
            System.err.println("  Sum of households: " + sum_foodCollection);
            System.err.println("  Territory total: " + Bsf[year]);
            valid = false;
        }
        
        // Check green collection
        if (Math.abs(sum_greenCollection - Bsg[year]) > epsilon) {
            System.err.println("ERROR: Green collection mismatch at year " + year);
            System.err.println("  Sum of households: " + sum_greenCollection);
            System.err.println("  Territory total: " + Bsg[year]);
            valid = false;
        }
        
        // Check food residual
        if (Math.abs(sum_foodResidual - Br[year]) > epsilon) {
            System.err.println("ERROR: Food residual mismatch at year " + year);
            System.err.println("  Sum of households: " + sum_foodResidual);
            System.err.println("  Territory total: " + Br[year]);
            valid = false;
        }
        
        // Check green valorisation
        if (Math.abs(sum_greenValor - Bv[year]) > epsilon) {
            System.err.println("ERROR: Green valorisation mismatch at year " + year);
            System.err.println("  Sum of households: " + sum_greenValor);
            System.err.println("  Territory total: " + Bv[year]);
            valid = false;
        }
        
        if (valid) {
            System.err.println("✓ Validation passed for territory " + ident + " year " + year);
        }
        
        return valid;
    }
    
    /**
     * Print household-level summary statistics
     * Useful for debugging and understanding distribution
     * 
     * @param year Year to summarize
     */
    public void printHouseholdSummary(int year) {
        if (households.isEmpty()) {
            System.err.println("No households in territory " + ident);
            return;
        }
        
        Household hh = households.get(0); // All households identical in Phase 1
        
        System.err.println("\n=== Territory " + ident + " Household Summary (Year " + year + ") ===");
        System.err.println("Number of households: " + numberOfHouseholds);
        System.err.println("Household size: " + householdSize + " persons");
        System.err.println("\nPer-household waste flows (tonnes/year):");
        System.err.println("  Food waste produced: " + String.format("%.6f", hh.getFoodWasteProduced()));
        System.err.println("  Green waste produced: " + String.format("%.6f", hh.getGreenWasteProduced()));
        System.err.println("  Total biowaste: " + String.format("%.6f", hh.getTotalBiowasteProduced()));
        System.err.println("\nFood waste destinations:");
        System.err.println("  Home composted: " + String.format("%.6f", hh.getFoodWaste_homeComposted()));
        System.err.println("  Dedicated collection: " + String.format("%.6f", hh.getFoodWaste_dedicatedCollection()));
        System.err.println("  Residual bin: " + String.format("%.6f", hh.getFoodWaste_residualBin()));
        System.err.println("\nGreen waste destinations:");
        System.err.println("  Home composted: " + String.format("%.6f", hh.getGreenWaste_homeComposted()));
        System.err.println("  Dedicated collection: " + String.format("%.6f", hh.getGreenWaste_dedicatedCollection()));
        System.err.println("  Valorisation center: " + String.format("%.6f", hh.getGreenWaste_valorisationCenter()));
        System.err.println("\nBehavioral intentions:");
        System.err.println("  αcf (food composting): " + String.format("%.3f", hh.getAlphaCf(year)));
        System.err.println("  αcg (green composting): " + String.format("%.3f", hh.getAlphaCg(year)));
        System.err.println("  αsf (food sorting): " + String.format("%.3f", hh.getAlphaSf(year)));
        System.err.println("  αsg (green sorting): " + String.format("%.3f", hh.getAlphaSg(year)));
        System.err.println("=====================================\n");
    }
    
    
    
    // ============== NEW: Social norm methods ============== 15 October 

/**
 * Calculate territory-wide adoption rate from sigmoid curve
 * This rate influences individual household decisions
 */
private void updateTerritoryAdoptionRate(int year) {
    // Use existing sigmoid curve as "social norm signal"
    // This represents visibility of the behavior in the territory
    if (year > 0) {
        territoryAdoptionRate_foodComposting[year] = 
            Math.min((αcf_initial + ((1 - αcf_initial) * sigmoide_mcf[year - 1])), 1.0);
    } else {
        territoryAdoptionRate_foodComposting[year] = αcf_initial;
    }
}

/**
 * Each household checks if they should adopt based on territory rate
 */
private void updateHouseholdAdoptions(int year) {
    double territoryRate = territoryAdoptionRate_foodComposting[year];
    
    for (Household hh : households) {
        hh.checkAdoption_foodComposting(territoryRate);
    }
}

/**
 * Update territory's αcf based on ACTUAL household adoptions
 * This may differ from sigmoid prediction due to individual thresholds
 */
private void updateAlphaFromHouseholds(int year) {
    // Count how many households have actually adopted
    int adoptedCount = 0;
    for (Household hh : households) {
        if (hh.hasAdoptedFoodComposting()) {
            adoptedCount++;
        }
    }
    
    // Calculate actual adoption rate
    double actualRate = (double) adoptedCount / numberOfHouseholds;
    
    // UPDATE αcf to reflect actual household behavior
    αcf[year] = actualRate;
    
    // DEBUG: Print comparison
    if (year % 5 == 0) { // Print every 5 years
        System.err.println("Territory " + ident + " Year " + year + 
                          ": Sigmoid predicted αcf=" + 
                          String.format("%.3f", territoryAdoptionRate_foodComposting[year]) + 
                          ", Actual from households=" + String.format("%.3f", actualRate) +
                          " (" + adoptedCount + "/" + numberOfHouseholds + " adopted)");
    }
}
    
    
    
    
}
