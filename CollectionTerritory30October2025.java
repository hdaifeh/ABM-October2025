import static java.lang.Float.max;
import static java.lang.Float.min;
import java.util.*;

/**
 * CollectionTerritory: Hybrid Agent-Based Model for Biowaste Management with Social Norm Dynamics
 * 
 * THEORETICAL FRAMEWORK:
 * This class implements a coupled system combining:
 * 1. System Dynamics (SD): Population-level biowaste flows and infrastructure constraints
 * 2. Agent-Based Model (ABM): Household agents with heterogeneous adoption thresholds
 * 3. Social Network Theory: Erdős-Rényi random graphs for peer influence
 * 
 * MATHEMATICAL MODEL:
 * 
 * Population Growth:
 *   P(y) = P_0 * (1 + r)^y
 * 
 * Biowaste Production:
 *   B_pf(y) = b_pf * P(y) * [1 - a_pf * ABP(y)]  (food biowaste)
 *   B_pg(y) = b_pg * P(y) * [1 - a_pg * ABP(y)]  (green biowaste)
 * 
 * Social Norm for Agent i:
 *   α_i(t) = (1/|N_i|) * Σ_{j∈N_i} S_j(t)
 *   where |N_i| = neighborhood size (variable, avg ≈ 5)
 *         S_j(t) ∈ {0,1} = neighbor j's sorting status
 * 
 * Threshold-Based Adoption:
 *   S_i(t) = 1  if α_i(t) ≥ θ_i, else 0
 *   where θ_i ~ Uniform(0,1) = agent i's adoption threshold
 * 
 * Population-Level Behavioral Intention:
 *   α_c(y) = α_c(0) + [1 - α_c(0)] * AdoptionRate(y)
 *   where AdoptionRate(y) = (1/N) * Σ_i S_i(y)
 * 
 * Infrastructure Constraints:
 *   B_c^composted(y) = min(B_c^intended(y), K_c(y))
 *   B_s^sorted(y) = min(B_s^intended(y), K_s(y))
 * 
 * Mass Balance:
 *   B(y) = B_c^composted(y) + B_s^sorted(y) + B_r(y) + B_v(y)
 * 
 * @author Research Team
 * @version 2.0 - Social Dynamics Extension (October 2024)
 */
public class CollectionTerritory {

    // ========================================
    // TERRITORY ATTRIBUTES
    // ========================================
    
    Territory myTerritory;          // Parent territory object
    int territoryName;              // Numeric identifier for this sub-territory
    
    // ========================================
    // BASELINE PARAMETERS (Per Capita Rates)
    // ========================================
    
    double bpf;                     // Baseline food biowaste production (tonnes/person/year)
    double bpg;                     // Baseline green biowaste production (tonnes/person/year)
    double r;                       // Annual population growth rate (e.g., 0.01 = 1% growth)
    double einit;                   // Edible portion of food waste (unused legacy parameter)
    double apf;                     // Anti-biowaste plan effectiveness for food (0 to 1)
    double apg;                     // Anti-biowaste plan effectiveness for green (0 to 1)
    double objGaspi;                // Food waste reduction objective (unused legacy parameter)
    
    // ========================================
    // SOCIAL NORM PARAMETERS (Agent-Based Model Component)
    // ========================================
    
    int numberOfAgents;                                // N = total number of household agents
    double[] agentThresholds;                          // θ_i for each agent (adoption threshold)
    boolean[] agentSortingStatusCompost;               // S_i^compost(t) - current composting status
    boolean[] agentSortingStatusCollection;            // S_i^collection(t) - current collection status
    int avgNeighborhood;                               // n_b = 5 (average neighborhood size)
    List<List<Integer>> neighborNetwork;               // Adjacency list: neighborNetwork.get(i) = N_i
    Random random = new Random();                      // Random number generator for stochastic processes
    
    // ========================================
    // TIME SERIES: POPULATION AND BIOWASTE FLOWS (System Dynamics Component)
    // ========================================
    
    double[] P;                     // P(y) = population at year y
    double[] B;                     // B(y) = total biowaste production
    double[] Bpf;                   // B_pf(y) = food biowaste production
    double[] Bpg;                   // B_pg(y) = green biowaste production
    double[] ABP;                   // ABP(y) = anti-biowaste plan intensity (0 to 1)
    
    // ========================================
    // BIOWASTE ALLOCATION FLOWS
    // ========================================
    
    double[] Br;                    // B_r(y) = food biowaste to residual household waste
    double[] Bc;                    // B_c(y) = intended composting quantity (before capacity limit)
    double[] Bs;                    // B_s(y) = intended collection quantity (before capacity limit)
    double[] Bv;                    // B_v(y) = green waste to valorization centre (waste collection facility)
    
    // Actual flows (after infrastructure capacity constraints)
    double[] Bc_composted;          // B_c^composted(y) = actual composted (limited by K_c)
    double[] Bs_sorted;             // B_s^sorted(y) = actual collected (limited by K_s)
    
    // Detailed flows by waste type
    double[] Bcf;                   // B_cf(y) = composted food biowaste
    double[] Bcg;                   // B_cg(y) = composted green biowaste
    double[] Bsf;                   // B_sf(y) = collected food biowaste
    double[] Bsg;                   // B_sg(y) = collected green biowaste
    
    // ========================================
    // BEHAVIORAL PARAMETERS (Population-Level Intentions)
    // ========================================
    // These parameters are driven by agent-level adoption when social dynamics are enabled
    
    double[] αcf;                   // α_cf(y) = fraction of food biowaste intended for home composting
    double[] αcg;                   // α_cg(y) = fraction of green biowaste intended for home composting
    double[] αsf;                   // α_sf(y) = fraction of food biowaste intended for collection sorting
    double[] αsg;                   // α_sg(y) = fraction of green biowaste intended for collection sorting
    
    // ========================================
    // INFRASTRUCTURE CAPACITIES
    // ========================================
    
    double[] Kc;                    // K_c(y) = home composting infrastructure capacity (tonnes/year)
    double[] Ks;                    // K_s(y) = dedicated collection infrastructure capacity (tonnes/year)
    
    // ========================================
    // SURPLUS QUANTITIES (Overflow from Infrastructure Constraints)
    // ========================================
    
    double[] Ucf;                   // U_cf(y) = food waste surplus from composting (exceeds K_c)
    double[] Ucg;                   // U_cg(y) = green waste surplus from composting (exceeds K_c)
    double[] Usf;                   // U_sf(y) = food waste surplus from collection (exceeds K_s)
    double[] Usg;                   // U_sg(y) = green waste surplus from collection (exceeds K_s)
    
    // ========================================
    // TRACKING INDICATORS (Per Capita Metrics)
    // ========================================
    
    double[] nbKgCollectHabDesservi;        // kg collected per served inhabitant
    double[] propPopDesserviCollDA;         // proportion of population served by collection
    double[] nbKgOMRHab;                    // kg of food waste in residual waste per inhabitant
    double[] Kct;                           // Current composting capacity (tracking array)
    double[] Kst;                           // Current collection capacity (tracking array)
    double[] tauxReductionDechetVert;       // Green waste reduction rate relative to baseline

    // ========================================
    // CONSTRUCTOR
    // ========================================
    
    /**
     * Constructor for CollectionTerritory
     * 
     * @param territ Parent Territory object
     * @param indice Numeric identifier for this sub-territory
     */
    public CollectionTerritory(Territory territ, int indice) {
        myTerritory = territ;
        territoryName = indice;
        avgNeighborhood = 5;        // Default average neighborhood size for social network
    }

    // ========================================
    // INITIALIZATION METHOD
    // ========================================
    
    /**
     * Initialize all arrays and parameters for simulation
     * 
     * This method:
     * 1. Allocates all time-indexed arrays
     * 2. Extracts parameters from input vector
     * 3. Initializes all arrays to zero
     * 4. Sets initial parameter values
     * 5. Creates agent population and social network
     * 6. Calculates initial biowaste flows
     * 
     * @param sizeData Length of time-indexed arrays (number of years in simulation)
     * @param params Parameter vector containing:
     *               [0] P_0 = initial population
     *               [1] r = population growth rate
     *               [2] b_pg = per capita green biowaste production
     *               [3] b_pf = per capita food biowaste production
     *               [4] α_cg(0) = initial green composting intention
     *               [5] α_cf(0) = initial food composting intention
     *               [6] α_sg(0) = initial green collection intention
     *               [7] α_sf(0) = initial food collection intention
     *               [8] K_c(0) = initial composting capacity
     *               [9] K_s(0) = initial collection capacity (optional)
     * @param refYear Reference year for output (typically 2017)
     */
    public void init(int sizeData, double[] params, int refYear) { 
        
        // ========================================
        // STEP 1: ALLOCATE ALL TIME-INDEXED ARRAYS
        // ========================================
        
        // Population and total biowaste
        P = new double[sizeData];
        B = new double[sizeData];
        Bpf = new double[sizeData];
        Bpg = new double[sizeData];
        ABP = new double[sizeData];
        
        // Flow allocation arrays
        Br = new double[sizeData];
        Bc = new double[sizeData];
        Bs = new double[sizeData];
        Bv = new double[sizeData];
        Bc_composted = new double[sizeData];
        Bs_sorted = new double[sizeData];
        
        // Detailed flows by waste type
        Bcf = new double[sizeData];
        Bcg = new double[sizeData];
        Bsf = new double[sizeData];
        Bsg = new double[sizeData];
        
        // Behavioral intention parameters
        αcf = new double[sizeData];
        αcg = new double[sizeData];
        αsf = new double[sizeData];
        αsg = new double[sizeData];
        
        // Infrastructure capacities
        Kc = new double[sizeData];
        Ks = new double[sizeData];
        
        // Surplus quantities
        Ucf = new double[sizeData];
        Ucg = new double[sizeData];
        Usf = new double[sizeData];
        Usg = new double[sizeData];
        
        // Tracking indicators (CRITICAL: these were missing and caused NullPointerException)
        nbKgCollectHabDesservi = new double[sizeData];
        propPopDesserviCollDA = new double[sizeData];
        nbKgOMRHab = new double[sizeData];
        Kct = new double[sizeData];
        Kst = new double[sizeData];
        tauxReductionDechetVert = new double[sizeData];
        
        // ========================================
        // STEP 2: EXTRACT PARAMETERS FROM INPUT VECTOR
        // ========================================
        
        P[0] = params[0];           // Initial population
        r = params[1];              // Population growth rate
        bpg = params[2];            // Per capita green biowaste production
        bpf = params[3];            // Per capita food biowaste production
        αcg[0] = params[4];         // Initial green composting intention
        αcf[0] = params[5];         // Initial food composting intention
        αsg[0] = params[6];         // Initial green collection intention
        αsf[0] = params[7];         // Initial food collection intention
        Kc[0] = params[8];          // Initial composting capacity
        
        // Optional parameter: collection capacity (may not exist in all territories)
        if (params.length > 9) {
            Ks[0] = params[9];
        } else {
            Ks[0] = 0.0;            // No collection infrastructure if not specified
        }
        
        // ========================================
        // STEP 3: INITIALIZE ALL ARRAYS WITH ZEROS
        // ========================================
        // This ensures no undefined values in unused array positions
        
        Arrays.fill(P, 0.0);
        Arrays.fill(B, 0.0);
        Arrays.fill(Bpf, 0.0);
        Arrays.fill(Bpg, 0.0);
        Arrays.fill(ABP, 0.0);
        Arrays.fill(Bc, 0.0);
        Arrays.fill(Br, 0.0);
        Arrays.fill(Bs, 0.0);
        Arrays.fill(Bv, 0.0);
        Arrays.fill(Bc_composted, 0.0);
        Arrays.fill(Bs_sorted, 0.0);
        Arrays.fill(Bcf, 0.0);
        Arrays.fill(Bcg, 0.0);
        Arrays.fill(Bsf, 0.0);
        Arrays.fill(Bsg, 0.0);
        Arrays.fill(αcf, 0.0);
        Arrays.fill(αcg, 0.0);
        Arrays.fill(αsf, 0.0);
        Arrays.fill(αsg, 0.0);
        Arrays.fill(Kc, 0.0);
        Arrays.fill(Ks, 0.0);
        Arrays.fill(Ucf, 0.0);
        Arrays.fill(Ucg, 0.0);
        Arrays.fill(Usf, 0.0);
        Arrays.fill(Usg, 0.0);
        
        // Initialize tracking indicators
        Arrays.fill(nbKgCollectHabDesservi, 0.0);
        Arrays.fill(propPopDesserviCollDA, 0.0);
        Arrays.fill(nbKgOMRHab, 0.0);
        Arrays.fill(Kct, 0.0);
        Arrays.fill(Kst, 0.0);
        Arrays.fill(tauxReductionDechetVert, 0.0);
        
        // ========================================
        // STEP 4: RE-SET INITIAL VALUES (after fill operations)
        // ========================================
        // Arrays.fill() overwrote our initial values, so restore them
        
        P[0] = params[0];
        αcg[0] = params[4];
        αcf[0] = params[5];
        αsg[0] = params[6];
        αsf[0] = params[7];
        Kc[0] = params[8];
        if (params.length > 9) {
            Ks[0] = params[9];
        }
        
        // Set initial values for tracking arrays
        Kct[0] = Kc[0];             // Track composting capacity
        Kst[0] = Ks[0];             // Track collection capacity
        
        // ========================================
        // STEP 5: INITIALIZE AGENT-BASED MODEL COMPONENT
        // ========================================
        // Creates household agents and constructs social network
        
        initializeSocialNormAgents();
        
        // ========================================
        // STEP 6: CALCULATE INITIAL BIOWASTE PRODUCTION
        // ========================================
        
        ABP[0] = 0.0;               // No anti-biowaste plan at baseline (year 0)
        
        // Biowaste production = per capita rate * population * (1 - plan effectiveness)
        Bpf[0] = bpf * P[0] * (1 - apf * ABP[0]);
        Bpg[0] = bpg * P[0] * (1 - apg * ABP[0]);
        B[0] = Bpg[0] + Bpf[0];
        
        // ========================================
        // STEP 7: COMPUTE INITIAL HOME COMPOSTING FLOWS
        // ========================================
        // Home composting is capacity-constrained
        
        if (Kc[0] > 0) {
            // Calculate intended quantities based on behavioral intentions
            double quantiteCompostableFood = αcf[0] * Bpf[0];
            double quantiteCompostableGreen = αcg[0] * Bpg[0];
            Bc[0] = quantiteCompostableFood + quantiteCompostableGreen;
            
            // Allocate with capacity constraint (PRIORITY: food biowaste first)
            Bcf[0] = Math.min(Kc[0], quantiteCompostableFood);
            Bcg[0] = Math.min((Kc[0] - Bcf[0]), quantiteCompostableGreen);
            
            // Compute surpluses (overflow beyond capacity)
            Ucf[0] = Math.max((quantiteCompostableFood - Kc[0]), 0.0);
            Ucg[0] = Math.max((quantiteCompostableFood + quantiteCompostableGreen - Kc[0]), 0.0);
            
            Bc_composted[0] = Bcf[0] + Bcg[0];
        }
        
        // ========================================
        // STEP 8: COMPUTE INITIAL COLLECTION FLOWS
        // ========================================
        // Collection is also capacity-constrained
        
        if (Ks[0] > 0) {
            // Intended collection includes composting surplus
            double quantiteSortableFood = αsf[0] * Bpf[0] + Ucf[0];
            double quantiteSortableGreen = αsg[0] * Bpg[0];
            Bs[0] = quantiteSortableFood + quantiteSortableGreen;
            
            // Allocate with capacity constraint (PRIORITY: food biowaste first)
            Bsf[0] = Math.min(Ks[0], quantiteSortableFood);
            Bsg[0] = Math.min((Ks[0] - Bsf[0]), quantiteSortableGreen);
            
            // Compute surpluses
            Usf[0] = Math.max((quantiteSortableFood - Ks[0]), 0.0);
            Usg[0] = Math.max((quantiteSortableFood + quantiteSortableGreen - Ks[0]), 0.0);
            
            Bs_sorted[0] = Bsf[0] + Bsg[0];
        }
        
        // ========================================
        // STEP 9: COMPUTE RESIDUAL AND VALORIZATION FLOWS
        // ========================================
        
        // Green waste to valorization = not composted, not collected, plus surpluses
        Bv[0] = (1 - αcg[0] - αsg[0]) * Bpg[0] + Ucg[0] + Usg[0];
        
        // Food waste to residual = not composted, not collected, plus surplus
        Br[0] = (1 - αcf[0] - αsf[0]) * Bpf[0] + Usf[0];
        
        // ========================================
        // STEP 10: CALCULATE INITIAL PER CAPITA INDICATORS
        // ========================================
        
        if (P[0] > 0) {
            // Residual waste per inhabitant (in kg)
            nbKgOMRHab[0] = (Br[0] * 1000.0) / P[0];
            
            // Collection metrics (if collection infrastructure exists)
            if (Ks[0] > 0 && Bs_sorted[0] > 0) {
                // Proportion of population served by collection
                double totalIntended = αsf[0] * Bpf[0] + Ucf[0] + αsg[0] * Bpg[0];
                if (totalIntended > 0) {
                    propPopDesserviCollDA[0] = Math.min(1.0, Ks[0] / totalIntended);
                }
                
                double populationServed = P[0] * propPopDesserviCollDA[0];
                if (populationServed > 0) {
                    nbKgCollectHabDesservi[0] = (Bs_sorted[0] * 1000.0) / populationServed;
                }
            }
        }
    }
    
    // ========================================
    // AGENT-BASED MODEL METHODS
    // ========================================
    
    /**
     * Initialize agent population and construct social network
     * 
     * AGENT CREATION:
     * - Number of agents: N = floor(P_0 / 2.1)
     *   Assumes 2.1 people per household on average
     * 
     * NETWORK MODEL: Erdős-Rényi Random Graph
     * - Edge probability: p = n_b / N
     *   where n_b = 5 (average expected degree)
     * - Results in Poisson degree distribution with mean ≈ 5
     * - Each agent i has variable |N_i| (neighborhood size)
     * 
     * HETEROGENEOUS THRESHOLDS:
     * - θ_i ~ Uniform(0, 1) for each agent
     * - Interpretation:
     *   θ_i ≈ 0.0: Early adopter (adopts with minimal peer influence)
     *   θ_i ≈ 0.5: Typical agent (requires ≥50% neighbors adopting)
     *   θ_i ≈ 1.0: Laggard (requires near-universal adoption)
     */
    private void initializeSocialNormAgents() {
        // Calculate number of agents (2.1 people per household)
        numberOfAgents = (int)(P[0] / 2.1);
        
        // Allocate agent attribute arrays
        agentThresholds = new double[numberOfAgents];
        agentSortingStatusCompost = new boolean[numberOfAgents];
        agentSortingStatusCollection = new boolean[numberOfAgents];
        neighborNetwork = new ArrayList<>();
        
        // Initialize each agent's attributes
        for (int i = 0; i < numberOfAgents; i++) {
            // Heterogeneous adoption threshold: θ_i ~ Uniform(0, 1)
            agentThresholds[i] = random.nextDouble();
            
            // Initially no adoption of sorting behaviors
            agentSortingStatusCompost[i] = false;
            agentSortingStatusCollection[i] = false;
            
            // Initialize empty neighbor list for agent i
            neighborNetwork.add(new ArrayList<>());
        }
        
        // ========================================
        // CONSTRUCT SOCIAL NETWORK (Erdős-Rényi Model)
        // ========================================
        
        // Edge probability for desired average degree
        double p = (double)avgNeighborhood / numberOfAgents;
        
        // For each potential edge (i,j)
        for (int i = 0; i < numberOfAgents; i++) {
            for (int j = 0; j < numberOfAgents; j++) {
                if (i != j) {                       // Exclude self-loops
                    if (random.nextDouble() < p) {  // Add edge with probability p
                        neighborNetwork.get(i).add(j);  // j ∈ N_i
                    }
                }
            }
        }
    }
    
    /**
     * Calculate social norm strength for agent i
     * 
     * MATHEMATICAL FORMULA:
     *   α_i(t) = (1/|N_i|) * Σ_{j∈N_i} S_j(t)
     * 
     * where:
     *   |N_i| = actual neighborhood size for agent i (variable across agents)
     *   S_j(t) ∈ {0,1} = binary sorting status of neighbor j
     * 
     * INTERPRETATION:
     *   α_i(t) = fraction of neighbors currently adopting the behavior
     *   α_i(t) = 0.0 → no neighbors sorting
     *   α_i(t) = 0.5 → half of neighbors sorting
     *   α_i(t) = 1.0 → all neighbors sorting
     * 
     * NOTE: Average |N_i| ≈ 5, but varies per agent due to random network
     * 
     * @param agentId The agent i for whom to calculate social norm
     * @param sortingStatus Boolean array S(t) indicating current sorting status
     * @return α_i(t) ∈ [0, 1], the fraction of neighbors currently sorting
     */
    private double calculateSocialNorm(int agentId, boolean[] sortingStatus) {
        // Get neighborhood N_i for agent i
        List<Integer> neighbors = neighborNetwork.get(agentId);
        
        // Handle isolated agents (no neighbors)
        if (neighbors.isEmpty()) {
            return 0.0;
        }
        
        // Count neighbors currently sorting: Σ_{j∈N_i} S_j(t)
        int sortingNeighbors = 0;
        for (int neighborId : neighbors) {
            if (sortingStatus[neighborId]) {
                sortingNeighbors++;
            }
        }
        
        // Calculate fraction: (1/|N_i|) * Σ_{j∈N_i} S_j(t)
        double socialNorm = (double)sortingNeighbors / neighbors.size();
        
        return socialNorm;
    }
    
    /**
     * Update all social norms and behavioral statuses for year y
     * 
     * PROCESS (executed at beginning of each year):
     * 1. For each agent i, calculate social norm α_i(y) from year-1 status
     * 2. Agent i adopts if α_i(y) ≥ θ_i (threshold-based adoption)
     * 3. Aggregate: AdoptionRate(y) = (1/N) * Σ_i S_i(y)
     * 4. Update population intentions: α_c(y), α_s(y) based on adoption
     * 
     * BEHAVIORAL INTENTION UPDATE EQUATION:
     *   α_c(y) = α_c(0) + [1 - α_c(0)] * AdoptionRate(y)
     * 
     * This equation ensures:
     * - If AdoptionRate = 0, then α_c(y) = α_c(0) (no change)
     * - If AdoptionRate = 1, then α_c(y) = 1 (full adoption)
     * - Smooth interpolation between baseline and full adoption
     * 
     * CONSTRAINT ENFORCEMENT:
     *   α_cf(y) + α_sf(y) ≤ 1  (food biowaste fractions cannot exceed 100%)
     *   α_cg(y) + α_sg(y) ≤ 1  (green biowaste fractions cannot exceed 100%)
     * 
     * @param year Year index y for which to update norms
     */
    private void updateSocialNorms(int year) {
        
        // ========================================
        // COMPOSTING BEHAVIOR UPDATE
        // ========================================
        
        boolean[] newCompostingStatus = new boolean[numberOfAgents];
        
        for (int i = 0; i < numberOfAgents; i++) {
            // Calculate social norm: α_i(y) based on year-1 status
            double socialNormCompost = calculateSocialNorm(i, agentSortingStatusCompost);
            
            // Threshold-based adoption: S_i^compost(y) = 1 if α_i(y) ≥ θ_i
            newCompostingStatus[i] = (socialNormCompost >= agentThresholds[i]);
        }
        
        // Update composting status for all agents
        agentSortingStatusCompost = newCompostingStatus;
        
        // ========================================
        // COLLECTION SORTING BEHAVIOR UPDATE
        // ========================================
        // Only if collection infrastructure exists
        
        if (Ks[year] > 0) {
            boolean[] newCollectionStatus = new boolean[numberOfAgents];
            
            for (int i = 0; i < numberOfAgents; i++) {
                // Calculate social norm: α_i(y) based on year-1 status
                double socialNormCollection = calculateSocialNorm(i, agentSortingStatusCollection);
                
                // Threshold-based adoption: S_i^collection(y) = 1 if α_i(y) ≥ θ_i
                newCollectionStatus[i] = (socialNormCollection >= agentThresholds[i]);
            }
            
            // Update collection status for all agents
            agentSortingStatusCollection = newCollectionStatus;
        }
        
        // ========================================
        // AGGREGATE ADOPTION RATES
        // ========================================
        // Calculate population-level adoption: AdoptionRate(y) = (1/N) * Σ_i S_i(y)
        
        int compostingAgents = 0;
        int collectionAgents = 0;
        
        for (int i = 0; i < numberOfAgents; i++) {
            if (agentSortingStatusCompost[i]) compostingAgents++;
            if (agentSortingStatusCollection[i]) collectionAgents++;
        }
        
        double compostAdoptionRate = (double)compostingAgents / numberOfAgents;
        double collectionAdoptionRate = (double)collectionAgents / numberOfAgents;
        
        // ========================================
        // UPDATE POPULATION-LEVEL BEHAVIORAL INTENTIONS
        // ========================================
        // Link agent-level adoption to population-level parameters
        
        if (myTerritory.useSocialDynamics) {
            
            // COMPOSTING INTENTIONS
            // α_c(y) = α_c(0) + [1 - α_c(0)] * AdoptionRate(y)
            αcf[year] = αcf[0] + (1 - αcf[0]) * compostAdoptionRate;
            αcg[year] = αcg[0] + (1 - αcg[0]) * compostAdoptionRate;
            
            // COLLECTION INTENTIONS (only if infrastructure exists)
            if (Ks[year] > 0) {
                // α_s(y) = α_s(0) + [1 - α_s(0)] * AdoptionRate(y)
                αsf[year] = αsf[0] + (1 - αsf[0]) * collectionAdoptionRate;
                αsg[year] = αsg[0] + (1 - αsg[0]) * collectionAdoptionRate;
                
                // CONSTRAINT ENFORCEMENT: α_c(y) + α_s(y) ≤ 1
                if (αcf[year] + αsf[year] > 1) {
                    αsf[year] = 1 - αcf[year];  // Reduce collection intention
                }
                if (αcg[year] + αsg[year] > 1) {
                    αsg[year] = 1 - αcg[year];  // Reduce collection intention
                }
            } else {
                // No infrastructure → no collection sorting possible
                αsf[year] = 0;
                αsg[year] = 0;
            }
        }
    }

    // ========================================
    // MAIN ITERATION METHOD
    // ========================================
    
    /**
     * Main iteration procedure for year y
     * 
     * EXECUTION SEQUENCE:
     * 1. updateSocialNorms(y) - Update agent behaviors and α parameters
     * 2. computeProductionOfBiowaste(y) - Calculate population growth and biowaste production
     * 3. Update infrastructure capacity tracking (Kct, Kst)
     * 4. computeComposters(y) - Allocate biowaste to composting (capacity-limited)
     * 5. computeCollectors(y) - Allocate biowaste to collection (capacity-limited)
     * 6. computeFluxToValoCenterAndOMR(y) - Calculate residual flows
     * 7. updateYearlyIndicators(y) - Calculate per capita and coverage metrics
     * 
     * @param y Year index
     */
    public void iterate(int y) {
        // Step 1: Update agent-based model and behavioral parameters
        updateSocialNorms(y);
        
        // Step 2: Compute population growth and biowaste production
        computeProductionOfBiowaste(y);
        
        // Step 3: Update infrastructure capacity tracking
        Kct[y] = Kc[y];
        Kst[y] = Ks[y];
        
        // Step 4: Compute composting flows (capacity-constrained)
        computeComposters(y);
        
        // Step 5: Compute collection flows (if infrastructure exists)
        if (Ks[y] > 0) {
            computeCollectors(y);
        }
        
        // Step 6: Compute residual and valorization flows
        computeFluxToValoCenterAndOMR(y);
        
        // Step 7: Calculate yearly indicators
        updateYearlyIndicators(y);
    }
    
    // ========================================
    // SYSTEM DYNAMICS COMPUTATION METHODS
    // ========================================
    
    /**
     * Compute population and biowaste production for year y
     * 
     * POPULATION DYNAMICS:
     *   P(y) = P_0 * (1 + r)^y
     * 
     * BIOWASTE PRODUCTION (with anti-waste plan reduction):
     *   B_pf(y) = b_pf * P(y) * [1 - a_pf * ABP(y)]
     *   B_pg(y) = b_pg * P(y) * [1 - a_pg * ABP(y)]
     *   B(y) = B_pf(y) + B_pg(y)
     * 
     * where ABP(y) ∈ [0,1] is the anti-biowaste plan intensity
     * 
     * @param y Year index
     */
    public void computeProductionOfBiowaste(int y) {
        // Exponential population growth model
        P[y] = P[0] * Math.pow(1 + r, y);
        
        // Anti-biowaste plan level (typically sigmoid curve from external data)
        ABP[y] = (myTerritory.sigmoideABP[y]);
        
        // Biowaste production with plan effectiveness reduction
        Bpf[y] = bpf * P[y] * (1 - apf * ABP[y]);
        Bpg[y] = bpg * P[y] * (1 - apg * ABP[y]);
        B[y] = Bpg[y] + Bpf[y];
    }
    
    /**
     * Compute home composting flows (capacity-constrained)
     * 
     * INTENDED COMPOSTING:
     *   Q_cf = α_cf(y) * B_pf(y)      (intended food composting)
     *   Q_cg = α_cg(y) * B_pg(y)      (intended green composting)
     *   B_c(y) = Q_cf + Q_cg           (total intended)
     * 
     * ACTUAL COMPOSTING (with capacity constraint, food priority):
     *   B_cf(y) = min(K_c(y), Q_cf)
     *   B_cg(y) = min(K_c(y) - B_cf(y), Q_cg)
     *   B_c^composted(y) = B_cf(y) + B_cg(y)
     * 
     * SURPLUS (excess flows to collection):
     *   U_cf(y) = max(Q_cf - K_c(y), 0)
     *   U_cg(y) = max(Q_cf + Q_cg - K_c(y), 0)
     * 
     * PRIORITY RULE: Food biowaste has priority over green biowaste
     * 
     * @param y Year index
     */
    public void computeComposters(int y) {
        if (Kc[y] > 0) {
            // Calculate intended composting quantities
            double quantiteCompostableFood = αcf[y] * Bpf[y];
            double quantiteCompostableGreen = αcg[y] * Bpg[y];
            Bc[y] = quantiteCompostableFood + quantiteCompostableGreen;
            
            // Allocate with capacity constraint (food priority)
            Bcf[y] = Math.min(Kc[y], quantiteCompostableFood);
            Bcg[y] = Math.min((Kc[y] - Bcf[y]), quantiteCompostableGreen);
            
            // Calculate surpluses (overflow beyond capacity)
            Ucf[y] = Math.max((quantiteCompostableFood - Kc[y]), 0.0);
            Ucg[y] = Math.max((quantiteCompostableFood + quantiteCompostableGreen - Kc[y]), 0.0);
            
            // Total actual composted
            Bc_composted[y] = Bcf[y] + Bcg[y];
        }
    }
    
    /**
     * Compute dedicated collection flows (capacity-constrained)
     * 
     * INTENDED COLLECTION (includes composting surplus):
     *   Q_sf = α_sf(y) * B_pf(y) + U_cf(y)    (food: intended + composting surplus)
     *   Q_sg = α_sg(y) * B_pg(y)               (green: intended only)
     *   B_s(y) = Q_sf + Q_sg                   (total intended)
     * 
     * ACTUAL COLLECTION (with capacity constraint, food priority):
     *   B_sf(y) = min(K_s(y), Q_sf)
     *   B_sg(y) = min(K_s(y) - B_sf(y), Q_sg)
     *   B_s^sorted(y) = B_sf(y) + B_sg(y)
     * 
     * SURPLUS (excess flows to residual/valorization):
     *   U_sf(y) = max(Q_sf - K_s(y), 0)
     *   U_sg(y) = max(Q_sf + Q_sg - K_s(y), 0)
     * 
     * PRIORITY RULE: Food biowaste has priority over green biowaste
     * 
     * @param y Year index
     */
    public void computeCollectors(int y) {
        // Intended collection quantities (includes composting surplus)
        double quantiteSortableFood = αsf[y] * Bpf[y] + Ucf[y];
        double quantiteSortableGreen = αsg[y] * Bpg[y];
        Bs[y] = quantiteSortableFood + quantiteSortableGreen;
        
        // Allocate with capacity constraint (food priority)
        Bsf[y] = Math.min(Ks[y], quantiteSortableFood);
        Bsg[y] = Math.min((Ks[y] - Bsf[y]), quantiteSortableGreen);
        
        // Calculate surpluses (overflow beyond capacity)
        Usf[y] = Math.max((quantiteSortableFood - Ks[y]), 0.0);
        Usg[y] = Math.max((quantiteSortableFood + quantiteSortableGreen - Ks[y]), 0.0);
        
        // Total actual sorted
        Bs_sorted[y] = Bsf[y] + Bsg[y];
    }
    
    /**
     * Compute residual waste and valorization flows
     * 
     * MASS BALANCE EQUATIONS:
     * 
     * RESIDUAL FOOD WASTE (to household waste bin):
     *   B_r(y) = [1 - α_cf(y) - α_sf(y)] * B_pf(y) + U_sf(y)
     *   = food not composted, not collected + collection surplus
     * 
     * GREEN WASTE TO VALORIZATION CENTER:
     *   B_v(y) = [1 - α_cg(y) - α_sg(y)] * B_pg(y) + U_cg(y) + U_sg(y)
     *   = green not composted, not collected + all surpluses
     * 
     * VERIFICATION:
     *   B_pf(y) = B_cf(y) + B_sf(y) + B_r(y)  (food mass balance)
     *   B_pg(y) = B_cg(y) + B_sg(y) + B_v(y)  (green mass balance)
     * 
     * @param y Year index
     */
    public void computeFluxToValoCenterAndOMR(int y) {
        // Green waste to valorization (waste collection center)
        Bv[y] = (1 - αcg[y] - αsg[y]) * Bpg[y] + Ucg[y] + Usg[y];
        
        // Food waste to residual household waste
        Br[y] = (1 - αcf[y] - αsf[y]) * Bpf[y] + Usf[y];
    }
    
    // ========================================
    // INDICATOR CALCULATION METHODS
    // ========================================
    
    /**
     * Update yearly tracking indicators for year y
     * 
     * CALCULATES:
     * 1. nbKgOMRHab[y] - Residual waste per capita (kg/person/year)
     * 2. propPopDesserviCollDA[y] - Population coverage by collection service (0 to 1)
     * 3. nbKgCollectHabDesservi[y] - Collection per served inhabitant (kg/person/year)
     * 4. tauxReductionDechetVert[y] - Green waste reduction rate relative to baseline
     * 
     * FORMULAS:
     *   nbKgOMRHab = (B_r * 1000) / P
     *   propPopDesserviCollDA = min(1, K_s / IntendedSorting)
     *   nbKgCollectHabDesservi = (B_s^sorted * 1000) / (P * propPopDesservi)
     *   tauxReductionDechetVert = (B_pg[0] - B_pg[y]) / B_pg[0]
     * 
     * @param y Year index
     */
    private void updateYearlyIndicators(int y) {
        // Avoid division by zero
        if (P[y] > 0) {
            
            // ========================================
            // RESIDUAL WASTE PER CAPITA
            // ========================================
            // Convert from tonnes to kg: multiply by 1000
            nbKgOMRHab[y] = (Br[y] * 1000.0) / P[y];
            
            // ========================================
            // COLLECTION SERVICE COVERAGE
            // ========================================
            // Only calculate if collection infrastructure exists
            
            if (Ks[y] > 0 && Bs_sorted[y] > 0) {
                
                // Calculate total intended sorting quantity
                double intendedSorting = αsf[y] * Bpf[y] + Ucf[y] + αsg[y] * Bpg[y];
                
                if (intendedSorting > 0) {
                    // Proportion served = capacity / intended demand
                    // Capped at 1.0 (cannot serve more than 100%)
                    propPopDesserviCollDA[y] = Math.min(1.0, Ks[y] / intendedSorting);
                } else {
                    propPopDesserviCollDA[y] = 0.0;
                }
                
                // Calculate kg collected per served inhabitant
                double populationServed = P[y] * propPopDesserviCollDA[y];
                
                if (populationServed > 0) {
                    nbKgCollectHabDesservi[y] = (Bs_sorted[y] * 1000.0) / populationServed;
                } else {
                    nbKgCollectHabDesservi[y] = 0.0;
                }
                
            } else {
                // No collection infrastructure or no collection
                propPopDesserviCollDA[y] = 0.0;
                nbKgCollectHabDesservi[y] = 0.0;
            }
            
            // ========================================
            // GREEN WASTE REDUCTION RATE
            // ========================================
            // Reduction relative to baseline (year 0)
            
            if (y > 0 && Bpg[0] > 0) {
                tauxReductionDechetVert[y] = (Bpg[0] - Bpg[y]) / Bpg[0];
            } else {
                tauxReductionDechetVert[y] = 0.0;
            }
            
        } else {
            // Population is zero - set all indicators to zero
            nbKgOMRHab[y] = 0.0;
            propPopDesserviCollDA[y] = 0.0;
            nbKgCollectHabDesservi[y] = 0.0;
            tauxReductionDechetVert[y] = 0.0;
        }
    }
    
    /**
     * Calculate indicators for sub-territory at year i
     * 
     * This method is called from ExperimentalDesign.indicatorsObjectives()
     * Most indicators are already calculated in updateYearlyIndicators()
     * This method is kept for compatibility with the experimental design framework
     * 
     * Additional sub-territory specific calculations can be added here if needed
     * 
     * @param i Year index
     */
    void indicSubTerritories(int i) {
        // Most indicators are already calculated in updateYearlyIndicators()
        // which is called automatically during iterate()
        
        // This method is kept for compatibility with ExperimentalDesign class
        // Add any additional sub-territory specific calculations here if needed
        
        // Currently: no additional calculations needed
        // All main indicators are computed in:
        // - updateYearlyIndicators() - per capita metrics
        // - ExperimentalDesign.indicatorsObjectives() - territory-level objectives
    }
    
    // ========================================
    // OUTPUT METHOD
    // ========================================
    
    /**
     * Print trajectory for year y in semicolon-delimited format
     * 
     * OUTPUT COLUMNS (semicolon-separated):
     * Year | Territory | Population | B_pf | B_pg | B_cf | B_cg | B_sf | B_sg | 
     * B_r | B_v | B_c^composted | B_s^sorted | B_c | B_s | 
     * α_cf | α_cg | α_sf | α_sg | K_c | K_s | U_cf | U_cg | U_sf | U_sg
     * 
     * Used for detailed trajectory output when printTrajectory = true
     * 
     * @param y Year index
     */
    public void printTrajectory(int y) {
        System.out.print((y+2017)+";");                 // Calendar year
        System.out.print(territoryName+";");            // Territory identifier
        System.out.print(P[y]+";");                     // Population
        System.out.print(Bpf[y]+";");                   // Food biowaste production
        System.out.print(Bpg[y]+";");                   // Green biowaste production
        System.out.print(Bcf[y]+";");                   // Composted food
        System.out.print(Bcg[y]+";");                   // Composted green
        System.out.print(Bsf[y]+";");                   // Collected food
        System.out.print(Bsg[y]+";");                   // Collected green
        System.out.print(Br[y]+";");                    // Residual food waste
        System.out.print(Bv[y]+";");                    // Green to valorization
        System.out.print(Bc_composted[y]+";");          // Total composted
        System.out.print(Bs_sorted[y]+";");             // Total collected
        System.out.print(Bc[y]+";");                    // Intended composting
        System.out.print(Bs[y]+";");                    // Intended collection
        System.out.print(αcf[y]+";");                   // Food composting intention
        System.out.print(αcg[y]+";");                   // Green composting intention
        System.out.print(αsf[y]+";");                   // Food collection intention
        System.out.print(αsg[y]+";");                   // Green collection intention
        System.out.print(Kc[y]+";");                    // Composting capacity
        System.out.print(Ks[y]+";");                    // Collection capacity
        System.out.print(Ucf[y]+";");                   // Food composting surplus
        System.out.print(Ucg[y]+";");                   // Green composting surplus
        System.out.print(Usf[y]+";");                   // Food collection surplus
        System.out.print(Usg[y]+";");                   // Green collection surplus
        System.out.println();
    }
}
