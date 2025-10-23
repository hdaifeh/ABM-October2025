import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * CollectionTerritory - Refactored from CollectionTerritories for ABM Phase 1
 * 
 * Complete implementation with:
 * - Proper initialization sequencing
 * - Bounds checking to prevent negative flows
 * - Error handling for numerical stability
 * - Three-threshold household system
 * 
 * @author shuet (original SD model)
 * @author [Your Name] (ABM refactoring)
 * @date 2024
 */
public class CollectionTerritory {

    Territory myTerre;
    
    // ============== HOUSEHOLD MANAGEMENT ==============
    private List<Household> households;
    private int numberOfHouseholds;
    private double householdSize;
    
    // Adopter category percentages
    private double percentEarlyAdopters = 15.0;
    private double percentMainstream = 50.0;
    
    // Threshold boundaries
    private double thresholdEarlyMin = 0.0;
    private double thresholdEarlyMax = 0.3;
    private double thresholdMainstreamMin = 0.3;
    private double thresholdMainstreamMax = 0.7;
    private double thresholdLateMin = 0.7;
    private double thresholdLateMax = 1.0;
    
    // ============== ORIGINAL SD MODEL VARIABLES ==============
    
    // Starting points
    int timeBeforeInit_αcf_initial;
    int timeBeforeInit_αcg_initial;
    int timeBeforeInit_αsf_initial;
    int timeBeforeInit_αsg_initial;

    double Kc_initial;
    double Ks_initial;
    double[] Kct;
    double[] Kst;
    double Kc_target;
    double Ks_target;
    int yearRef;

    double[] LinearHomeComposter;
    double[] sigmoide_mcf;
    double[] sigmoide_mcg;
    double[] LinearDedicatedCollection;
    double[] sigmoide_msf;
    double[] sigmoide_msg;
    double[] sigmoide_mpg;
    
    double αcf_initial;
    double αcg_initial;
    double αcf_max;
    double αcg_max;
    double αsf_initial;
    double αsf_max;
    double αsg_initial;
    double αsg_max;

    double b_pf;
    double b_pg;

    double αv;
    double r;
    int sizePop;

    double duraImplemCompo;
    double mc;
    double duraImplemCollect;
    double ms;
    double mpg;
    double αpg_target;
    double αpf_target;

    // Aggregate arrays
    double[] P;
    double[] B;
    double[] Bpg;
    double[] Bpf;
    double[] ABP;
    double[] R;
    double[] G;
    double[] αcf;
    double[] αcg;
    double[] αvg;
    double[] αsf;
    double[] αsg;
    double[] C_log;
    double[] C_pop;
    double[] Bcg;
    double[] Bcf;
    double[] Bcf_composted;
    double[] Bcg_composted;
    double[] Bc_composted;
    double[] Uc;
    double[] Ucg;
    double[] Ucf;
    double[] sLbis;
    double[] Bv;
    double[] Bsg;
    double[] Bsf;
    double[] Bs_sorted;
    double[] Bsf_sorted;
    double[] Bsg_sorted;
    double[] Usf;
    double[] Usg;
    double[] sAa_bis;
    double[] sAv_bis;
    double[] Us;
    double[] sAbis;
    double[] Br;

    int subTerritoryName;
    double[] propPopDesserviCollDA;
    double[] nbKgCollectHabDesservi;
    double[] nbKgOMRHab;
    double[] tauxReductionDechetVert;
    int ident;

    // ============== CONSTRUCTOR ==============
    
    public CollectionTerritory(Territory mt, int id) {
        myTerre = mt;
        ident = id;
        households = new ArrayList<>();
    }
    
    // ============== INITIALIZATION ==============
    
    public void init(int sizeData, double[] params, int yearR) {
        yearRef = yearR;
        
        // Read all parameters
        subTerritoryName = (int) params[0];
        sizePop = (int) params[1];
        r = params[2];
        b_pg = params[3] / 1000.0; // kg to tonnes
        b_pf = params[4] / 1000.0; // kg to tonnes
        αcf_initial = Math.max(0.0, Math.min(1.0, params[5])); // Ensure [0,1]
        αcg_initial = Math.max(0.0, Math.min(1.0, params[6]));
        αsf_initial = Math.max(0.0, Math.min(1.0, params[7]));
        αsg_initial = Math.max(0.0, Math.min(1.0, params[8]));
        Kc_initial = Math.max(0.0, params[9]);
        Ks_initial = Math.max(0.0, params[10]);
        αv = Math.max(0.0, Math.min(1.0, params[11]));
        αcf_max = Math.max(0.0, Math.min(1.0, params[12]));
        αcg_max = Math.max(0.0, Math.min(1.0, params[13]));
        αsf_max = Math.max(0.0, Math.min(1.0, params[14]));
        αsg_max = Math.max(0.0, Math.min(1.0, params[15]));
        αpf_target = Math.max(0.0, Math.min(1.0, params[16]));
        αpg_target = Math.max(0.0, Math.min(1.0, params[17]));
        mc = Math.max(0.1, params[18]); // Avoid division by zero
        ms = Math.max(0.1, params[19]);
        mpg = Math.max(0.1, params[20]);
        duraImplemCompo = Math.max(1.0, params[21]);
        duraImplemCollect = Math.max(1.0, params[22]);
        Kc_target = Math.max(0.0, params[23]);
        
        // Read household size
        householdSize = Math.max(1.0, params[24]); // Avoid division by zero
        
        // Read adopter percentages if provided
        if (params.length > 25) {
            percentEarlyAdopters = Math.max(0.0, Math.min(100.0, params[25]));
            percentMainstream = Math.max(0.0, Math.min(100.0, params[26]));
        }
        
        // Initialize all arrays
        initializeArrays(sizeData);
        
        // Initialize capacity evolution
        initializeCapacityEvolution(sizeData);
        
        // Initialize sigmoid curves
        initializeSigmoids(sizeData);
        
        // Set initial values
        P[0] = sizePop;
        αcf[0] = αcf_initial;
        αcg[0] = αcg_initial;
        αsf[0] = αsf_initial;
        αsg[0] = αsg_initial;
        
        // Initialize households AFTER all parameters are set
        initializeHouseholds();
    }
    
    private void initializeArrays(int sizeData) {
        // Initialize all arrays with proper size
        P = new double[sizeData];
        B = new double[sizeData];
        Bpg = new double[sizeData];
        Bpf = new double[sizeData];
        ABP = new double[sizeData];
        R = new double[sizeData];
        G = new double[sizeData];
        αcf = new double[sizeData];
        αcg = new double[sizeData];
        αvg = new double[sizeData];
        αsf = new double[sizeData];
        αsg = new double[sizeData];
        C_log = new double[sizeData];
        C_pop = new double[sizeData];
        Bcg = new double[sizeData];
        Bcf = new double[sizeData];
        Bcf_composted = new double[sizeData];
        Bcg_composted = new double[sizeData];
        Bc_composted = new double[sizeData];
        Uc = new double[sizeData];
        Ucg = new double[sizeData];
        Ucf = new double[sizeData];
        sLbis = new double[sizeData];
        Bv = new double[sizeData];
        Bsg = new double[sizeData];
        Bsf = new double[sizeData];
        Bs_sorted = new double[sizeData];
        Bsf_sorted = new double[sizeData];
        Bsg_sorted = new double[sizeData];
        Usf = new double[sizeData];
        Usg = new double[sizeData];
        sAa_bis = new double[sizeData];
        sAv_bis = new double[sizeData];
        Us = new double[sizeData];
        sAbis = new double[sizeData];
        Br = new double[sizeData];
        
        // Capacity arrays
        Kct = new double[sizeData];
        Kst = new double[sizeData];
        
        // Indicator arrays
        propPopDesserviCollDA = new double[sizeData];
        nbKgCollectHabDesservi = new double[sizeData];
        nbKgOMRHab = new double[sizeData];
        tauxReductionDechetVert = new double[sizeData];
        
        // Fill with zeros
        Arrays.fill(P, 0.0);
        Arrays.fill(B, 0.0);
        Arrays.fill(Bpg, 0.0);
        Arrays.fill(Bpf, 0.0);
        Arrays.fill(ABP, 0.0);
        Arrays.fill(R, 0.0);
        Arrays.fill(G, 0.0);
        Arrays.fill(αcf, 0.0);
        Arrays.fill(αcg, 0.0);
        Arrays.fill(αvg, 0.0);
        Arrays.fill(αsf, 0.0);
        Arrays.fill(αsg, 0.0);
        Arrays.fill(C_log, 0.0);
        Arrays.fill(C_pop, 0.0);
        Arrays.fill(Bcg, 0.0);
        Arrays.fill(Bcf, 0.0);
        Arrays.fill(Bcf_composted, 0.0);
        Arrays.fill(Bcg_composted, 0.0);
        Arrays.fill(Bc_composted, 0.0);
        Arrays.fill(Uc, 0.0);
        Arrays.fill(Ucg, 0.0);
        Arrays.fill(Ucf, 0.0);
        Arrays.fill(sLbis, 0.0);
        Arrays.fill(Bv, 0.0);
        Arrays.fill(Bsg, 0.0);
        Arrays.fill(Bsf, 0.0);
        Arrays.fill(Bs_sorted, 0.0);
        Arrays.fill(Bsf_sorted, 0.0);
        Arrays.fill(Bsg_sorted, 0.0);
        Arrays.fill(Usf, 0.0);
        Arrays.fill(Usg, 0.0);
        Arrays.fill(sAa_bis, 0.0);
        Arrays.fill(sAv_bis, 0.0);
        Arrays.fill(Us, 0.0);
        Arrays.fill(sAbis, 0.0);
        Arrays.fill(Br, 0.0);
        Arrays.fill(Kct, 0.0);
        Arrays.fill(Kst, 0.0);
        Arrays.fill(propPopDesserviCollDA, 0.0);
        Arrays.fill(nbKgCollectHabDesservi, 0.0);
        Arrays.fill(nbKgOMRHab, 0.0);
        Arrays.fill(tauxReductionDechetVert, 0.0);
    }
    
    private void initializeCapacityEvolution(int sizeData) {
        // Initialize capacity at year 0
        Kct[0] = Kc_initial;
        Kst[0] = Ks_initial;
        
        // Linear evolution of capacities
        for (int year = 1; year < sizeData; year++) {
            // Home composter capacity evolution
            if (duraImplemCompo > 0 && year <= duraImplemCompo) {
                double progress = year / duraImplemCompo;
                Kct[year] = Kc_initial + (Kc_target - Kc_initial) * progress;
            } else {
                Kct[year] = (duraImplemCompo > 0) ? Kc_target : Kc_initial;
            }
            
            // Dedicated collection capacity evolution
            if (duraImplemCollect > 0 && year <= duraImplemCollect) {
                double progress = year / duraImplemCollect;
                Kst[year] = Ks_initial + (Ks_target - Ks_initial) * progress;
            } else {
                Kst[year] = (duraImplemCollect > 0) ? Ks_target : Ks_initial;
            }
            
            // Ensure non-negative
            Kct[year] = Math.max(0.0, Kct[year]);
            Kst[year] = Math.max(0.0, Kst[year]);
        }
    }
    
    private void initializeSigmoids(int sizeData) {
        sigmoide_mcf = new double[sizeData];
        sigmoide_mcg = new double[sizeData];
        sigmoide_msf = new double[sizeData];
        sigmoide_msg = new double[sizeData];
        sigmoide_mpg = new double[sizeData];
        
        for (int i = 0; i < sizeData; i++) {
            sigmoide_mcf[i] = sigmoide(i, mc);
            sigmoide_mcg[i] = sigmoide(i, mc);
            sigmoide_msf[i] = sigmoide(i, ms);
            sigmoide_msg[i] = sigmoide(i, ms);
            sigmoide_mpg[i] = sigmoide(i, mpg);
        }
    }
    
    // ============== HOUSEHOLD INITIALIZATION ==============
    
    private void initializeHouseholds() {
        numberOfHouseholds = (int) Math.round(sizePop / householdSize);
        
        households.clear();
        
        // Calculate number of households in each category
        int n1 = (int) (numberOfHouseholds * percentEarlyAdopters / 100.0);
        int n2 = (int) (numberOfHouseholds * percentMainstream / 100.0);
        int n3 = numberOfHouseholds - n1 - n2;
        
        // Create random number generator
        Random rng = new Random(42 + ident);
        
        // Create households with appropriate thresholds
        for (int i = 0; i < numberOfHouseholds; i++) {
            double threshold;
            
            if (i < n1) {
                threshold = sampleThreshold(rng, thresholdEarlyMin, thresholdEarlyMax);
            } else if (i < (n1 + n2)) {
                threshold = sampleThreshold(rng, thresholdMainstreamMin, thresholdMainstreamMax);
            } else {
                threshold = sampleThreshold(rng, thresholdLateMin, thresholdLateMax);
            }
            
            Household hh = new Household(i, this, householdSize, threshold);
            households.add(hh);
        }
        
        System.out.println("Territory " + ident + ": Created " + numberOfHouseholds + 
                          " households (Early: " + n1 + ", Mainstream: " + n2 + 
                          ", Late: " + n3 + ")");
    }

    private double sampleThreshold(Random rng, double min, double max) {
        return min + rng.nextDouble() * (max - min);
    }
    
    // ============== MAIN ITERATION METHOD ==============
    
    public void iterate(int year) {
        // Compute flows with bounds checking
        computeProduction(year);
        computeHomeComposting(year);
        computeSorting(year);
        computeResidualAndValorisation(year);
        
        // Validate all flows are non-negative
        validateFlows(year);
        
        // Calculate indicators
        indicSubTerritories(year);
        
        // Distribute to households
        if (!households.isEmpty()) {
            distributeFlowsToHouseholds(year);
        }
    }
    
    // ============== FLOW COMPUTATION METHODS ==============
    
    private void computeProduction(int year) {
        // Population growth
        if (year == 0) {
            P[year] = sizePop;
        } else {
            P[year] = P[year-1] * (1 + r);
        }
        P[year] = Math.max(0.0, P[year]);
        
        // Anti-biowaste plan effect
        if (year > 0 && myTerre != null && myTerre.sigmoideABP != null) {
            ABP[year] = Math.min(1.0, myTerre.sigmoideABP[year] * αpf_target);
        } else {
            ABP[year] = 0.0;
        }
        
        // Food waste production
        Bpf[year] = b_pf * P[year] * (1 - ABP[year]);
        Bpf[year] = Math.max(0.0, Bpf[year]);
        
        // Green waste production
        Bpg[year] = b_pg * P[year];
        Bpg[year] = Math.max(0.0, Bpg[year]);
        
        // Total biowaste
        B[year] = Bpf[year] + Bpg[year];
    }
    
    private void computeHomeComposting(int year) {
        // Update behavioral intentions
        if (year > 0) {
            αcf[year] = αcf_initial + ((αcf_max - αcf_initial) * sigmoide_mcf[year - 1]);
            αcg[year] = αcg_initial + ((αcg_max - αcg_initial) * sigmoide_mcg[year - 1]);
        } else {
            αcf[year] = αcf_initial;
            αcg[year] = αcg_initial;
        }
        
        // Ensure intentions are in [0,1]
        αcf[year] = Math.max(0.0, Math.min(1.0, αcf[year]));
        αcg[year] = Math.max(0.0, Math.min(1.0, αcg[year]));
        
        // Compostable quantities
        Bcf[year] = αcf[year] * Bpf[year];
        Bcg[year] = αcg[year] * Bpg[year];
        
        // Apply capacity constraints
        if (Kct[year] > 0) {
            Bcf_composted[year] = Math.min(Bcf[year], Kct[year]);
            double remainingCapacity = Math.max(0.0, Kct[year] - Bcf_composted[year]);
            Bcg_composted[year] = Math.min(Bcg[year], remainingCapacity);
        } else {
            Bcf_composted[year] = 0.0;
            Bcg_composted[year] = 0.0;
        }
        
        // Calculate surplus
        Ucf[year] = Math.max(0.0, Bcf[year] - Bcf_composted[year]);
        Ucg[year] = Math.max(0.0, Bcg[year] - Bcg_composted[year]);
        Bc_composted[year] = Bcf_composted[year] + Bcg_composted[year];
    }
    
    private void computeSorting(int year) {
        // Update sorting intentions
        if (year > 0) {
            double potential_αsf = αsf_initial + ((αsf_max - αsf_initial) * sigmoide_msf[year - 1]);
            double potential_αsg = αsg_initial + ((αsg_max - αsg_initial) * sigmoide_msg[year - 1]);
            
            // Ensure total intentions don't exceed 1
            if ((αcf[year] + potential_αsf) > 1.0) {
                αsf[year] = 1.0 - αcf[year];
            } else {
                αsf[year] = potential_αsf;
            }
            
            if ((αcg[year] + potential_αsg) > 1.0) {
                αsg[year] = 1.0 - αcg[year];
            } else {
                αsg[year] = potential_αsg;
            }
        } else {
            αsf[year] = αsf_initial;
            αsg[year] = αsg_initial;
        }
        
        // Ensure intentions are in [0,1]
        αsf[year] = Math.max(0.0, Math.min(1.0, αsf[year]));
        αsg[year] = Math.max(0.0, Math.min(1.0, αsg[year]));
        
        // Sortable quantities (including composting surplus)
        Bsf[year] = αsf[year] * Bpf[year] + Ucf[year];
        Bsg[year] = αsg[year] * Bpg[year];
        
        // Apply collection capacity constraints
        if (Kst[year] > 0) {
            Bsf_sorted[year] = Math.min(Bsf[year], Kst[year]);
            double remainingCapacity = Math.max(0.0, Kst[year] - Bsf_sorted[year]);
            Bsg_sorted[year] = Math.min(Bsg[year], remainingCapacity);
        } else {
            Bsf_sorted[year] = 0.0;
            Bsg_sorted[year] = 0.0;
        }
        
        // Calculate surplus
        Usf[year] = Math.max(0.0, Bsf[year] - Bsf_sorted[year]);
        Usg[year] = Math.max(0.0, Bsg[year] - Bsg_sorted[year]);
        Bs_sorted[year] = Bsf_sorted[year] + Bsg_sorted[year];
    }
    
    private void computeResidualAndValorisation(int year) {
        // Food waste to residual bin
        double residualIntention = Math.max(0.0, 1.0 - αcf[year] - αsf[year]);
        Br[year] = residualIntention * Bpf[year] + Usf[year];
        Br[year] = Math.max(0.0, Br[year]);
        
        // Green waste to valorisation centre
        double valorisationIntention = Math.max(0.0, 1.0 - αcg[year] - αsg[year]);
        Bv[year] = valorisationIntention * Bpg[year] + Ucg[year] + Usg[year];
        Bv[year] = Math.max(0.0, Bv[year]);
    }
    
    private void validateFlows(int year) {
        // Ensure all flows are non-negative
        Bpf[year] = Math.max(0.0, Bpf[year]);
        Bpg[year] = Math.max(0.0, Bpg[year]);
        Bcf_composted[year] = Math.max(0.0, Bcf_composted[year]);
        Bcg_composted[year] = Math.max(0.0, Bcg_composted[year]);
        Bsf_sorted[year] = Math.max(0.0, Bsf_sorted[year]);
        Bsg_sorted[year] = Math.max(0.0, Bsg_sorted[year]);
        Br[year] = Math.max(0.0, Br[year]);
        Bv[year] = Math.max(0.0, Bv[year]);
        
        // Check mass balance
        double totalProduced = Bpf[year] + Bpg[year];
        double totalAllocated = Bcf_composted[year] + Bcg_composted[year] + 
                               Bsf_sorted[year] + Bsg_sorted[year] + 
                               Br[year] + Bv[year];
        
        if (Math.abs(totalProduced - totalAllocated) > 0.001) {
            System.err.println("WARNING: Mass balance error at year " + year + 
                             " in territory " + ident + 
                             ": produced=" + totalProduced + 
                             ", allocated=" + totalAllocated);
        }
    }
    
    // ============== INDICATOR CALCULATION ==============
    
    public void indicSubTerritories(int year) {
        if (P[year] > 0) {
            // Per capita waste production (kg/person/year)
            nbKgOMRHab[year] = (Br[year] * 1000) / P[year];
            
            // Diversion rate
            if (B[year] > 0) {
                double diverted = Bc_composted[year] + Bs_sorted[year] + Bv[year];
                propPopDesserviCollDA[year] = diverted / B[year];
            }
            
            // Collection coverage
            if (Ks_initial > 0 && B[year] > 0) {
                nbKgCollectHabDesservi[year] = (Bs_sorted[year] * 1000) / P[year];
            }
            
            // Green waste reduction rate
            if (year > 0 && Bv[year-1] > 0) {
                tauxReductionDechetVert[year] = (Bv[year-1] - Bv[year]) / Bv[year-1];
            }
        }
    }
    
    // ============== OUTPUT METHODS ==============
    
    public void printTrajectory(int year) {
        if (year == 0) {
            System.out.println("Year;TerritoryID;Population;FoodWaste;GreenWaste;" +
                             "HomeComposted;Collected;Residual;Valorisation;" +
                             "AlphaCf;AlphaCg;AlphaSf;AlphaSg");
        }
        
        System.out.println(
            (2017 + year) + ";" +
            ident + ";" +
            P[year] + ";" +
            Bpf[year] + ";" +
            Bpg[year] + ";" +
            Bc_composted[year] + ";" +
            Bs_sorted[year] + ";" +
            Br[year] + ";" +
            Bv[year] + ";" +
            αcf[year] + ";" +
            αcg[year] + ";" +
            αsf[year] + ";" +
            αsg[year]
        );
    }
    
    // ============== HOUSEHOLD DISTRIBUTION ==============
    
    private void distributeFlowsToHouseholds(int year) {
        if (households.isEmpty() || numberOfHouseholds == 0) {
            return;
        }
        
        // Calculate per-household flows
        double perHH_foodProduced = Bpf[year] / numberOfHouseholds;
        double perHH_greenProduced = Bpg[year] / numberOfHouseholds;
        double perHH_foodCompost = Bcf_composted[year] / numberOfHouseholds;
        double perHH_foodCollection = Bsf_sorted[year] / numberOfHouseholds;
        double perHH_foodResidual = Br[year] / numberOfHouseholds;
        double perHH_greenCompost = Bcg_composted[year] / numberOfHouseholds;
        double perHH_greenCollection = Bsg_sorted[year] / numberOfHouseholds;
        double perHH_greenValor = Bv[year] / numberOfHouseholds;
        
        // Assign to each household
        for (Household hh : households) {
            hh.assignFlows(perHH_foodProduced, perHH_greenProduced,
                          perHH_foodCompost, perHH_foodCollection, perHH_foodResidual,
                          perHH_greenCompost, perHH_greenCollection, perHH_greenValor);
        }
    }
    
    // ============== HELPER METHODS ==============
    
    private double sigmoide(double x, double ti) {
        if (ti <= 0) return 1.0; // Avoid division by zero
        double t = Math.pow(x, 5);
        double z = t / (t + Math.pow(ti, 5));
        return z;
    }
    
    // ============== GETTERS ==============
    
    public int getIdent() {
        return ident;
    }
    
    public double[] getAlphaCf() {
        return αcf;
    }
    
    public double[] getAlphaCg() {
        return αcg;
    }
    
    public double[] getAlphaSf() {
        return αsf;
    }
    
    public double[] getAlphaSg() {
        return αsg;
    }
    
    public List<Household> getHouseholds() {
        return households;
    }
    
    public int getNumberOfHouseholds() {
        return numberOfHouseholds;
    }
}
