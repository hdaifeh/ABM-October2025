import static java.lang.Float.max;
import static java.lang.Float.min;
import java.util.*;

public class CollectionTerritory {

    Territory myTerritory;
    int territoryName;
    double bpf; // Per capita food biowaste production
    double bpg; // Per capita green biowaste production
    double r; // population annual growth
    double einit; // edible part
    double apf; // Anti-biowaste plan for food
    double apg; // Anti-biowaste plan for green
    double objGaspi;
    
    // NEW ADDITION 24/10 - Social norm parameters
    int numberOfAgents; // Number of household agents
    double[] agentThresholds; // Individual thresholds for adoption
    boolean[] agentSortingStatusCompost; // Current composting status of agents
    boolean[] agentSortingStatusCollection; // Current collection sorting status
    int avgNeighborhood = 5; // Average number of neighbors (nb parameter)
    List<List<Integer>> neighborNetwork; // Network structure: who knows whom
    Random random = new Random();
    
    // Time series arrays
    double[] P; // population
    double[] B; // total biowaste production
    double[] Bpf; // production of food biowaste
    double[] Bpg; // production of green biowaste
    double[] ABP; // Anti-biowaste plan value over time
    double[] Br; // biowaste that goes to residual household waste
    double[] Bc; // composted biowaste (without infrastructure limit)
    double[] Bs; // sorted biowaste (without infrastructure limit)
    double[] Bv; // green waste that goes to valorisation centre
    double[] Bc_composted; // composted biowaste (limited by infrastructure)
    double[] Bs_sorted; // sorted biowaste (limited by infrastructure)
    double[] Bcf; // composted food biowaste
    double[] Bcg; // composted green biowaste
    double[] Bsf; // sorted food biowaste
    double[] Bsg; // sorted green biowaste
    
    // Behavioral parameters (intentions)
    double[] αcf; // sorting for home composting intention - food
    double[] αcg; // sorting for home composting intention - green  
    double[] αsf; // sorting for dedicated collection intention - food
    double[] αsg; // sorting for dedicated collection intention - green
    
    // Infrastructure capacities
    double[] Kc; // home composter capacity
    double[] Ks; // dedicated collection capacity
    
    // Surplus arrays
    double[] Ucf; // food home composting-part surplus
    double[] Ucg; // green home composting-part surplus
    double[] Usf; // food sorting-part surplus
    double[] Usg; // green sorting-part surplus

    public CollectionTerritory(Territory territ, int indice) {
        myTerritory = territ;
        territoryName = indice;
    }

    public void init(int sizeData, double[] params, int refYear) {
        // Initialize all arrays
        P = new double[sizeData];
        B = new double[sizeData];
        Bpf = new double[sizeData];
        Bpg = new double[sizeData];
        ABP = new double[sizeData];
        Br = new double[sizeData];
        Bc = new double[sizeData];
        Bs = new double[sizeData];
        Bv = new double[sizeData];
        Bc_composted = new double[sizeData];
        Bs_sorted = new double[sizeData];
        Bcf = new double[sizeData];
        Bcg = new double[sizeData];
        Bsf = new double[sizeData];
        Bsg = new double[sizeData];
        αcf = new double[sizeData];
        αcg = new double[sizeData];
        αsf = new double[sizeData];
        αsg = new double[sizeData];
        Kc = new double[sizeData];
        Ks = new double[sizeData];
        Ucf = new double[sizeData];
        Ucg = new double[sizeData];
        Usf = new double[sizeData];
        Usg = new double[sizeData];
        
        // Initialize parameters
        P[0] = params[0];
        r = params[1];
        bpg = params[2];
        bpf = params[3];
        αcg[0] = params[4];
        αcf[0] = params[5];
        αsg[0] = params[6];
        αsf[0] = params[7];
        Kc[0] = params[8];
        if (params.length > 9) {
            Ks[0] = params[9];
        } else {
            Ks[0] = 0.0;
        }
        
        // Initialize all arrays with zeros
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
        
        // Set initial values
        P[0] = params[0];
        αcg[0] = params[4];
        αcf[0] = params[5];
        αsg[0] = params[6];
        αsf[0] = params[7];
        Kc[0] = params[8];
        if (params.length > 9) {
            Ks[0] = params[9];
        }
        
        // NEW ADDITION 24/10 - Initialize social norm agents
        initializeSocialNormAgents();
        
        // Initialize initial flows
        ABP[0] = 0.0;
        Bpf[0] = bpf * P[0] * (1 - apf * ABP[0]);
        Bpg[0] = bpg * P[0] * (1 - apg * ABP[0]);
        B[0] = Bpg[0] + Bpf[0];
        
        // Compute initial composting flows
        if (Kc[0] > 0) {
            double quantiteCompostableFood = αcf[0] * Bpf[0];
            double quantiteCompostableGreen = αcg[0] * Bpg[0];
            Bc[0] = quantiteCompostableFood + quantiteCompostableGreen;
            
            Bcf[0] = Math.min(Kc[0], quantiteCompostableFood);
            Bcg[0] = Math.min((Kc[0] - Bcf[0]), quantiteCompostableGreen);
            
            Ucf[0] = Math.max((quantiteCompostableFood - Kc[0]), 0.0);
            Ucg[0] = Math.max((quantiteCompostableFood + quantiteCompostableGreen - Kc[0]), 0.0);
            
            Bc_composted[0] = Bcf[0] + Bcg[0];
        }
        
        // Compute initial collection flows
        if (Ks[0] > 0) {
            double quantiteSortableFood = αsf[0] * Bpf[0] + Ucf[0];
            double quantiteSortableGreen = αsg[0] * Bpg[0];
            Bs[0] = quantiteSortableFood + quantiteSortableGreen;
            
            Bsf[0] = Math.min(Ks[0], quantiteSortableFood);
            Bsg[0] = Math.min((Ks[0] - Bsf[0]), quantiteSortableGreen);
            
            Usf[0] = Math.max((quantiteSortableFood - Ks[0]), 0.0);
            Usg[0] = Math.max((quantiteSortableFood + quantiteSortableGreen - Ks[0]), 0.0);
            
            Bs_sorted[0] = Bsf[0] + Bsg[0];
        }
        
        // Compute initial residual flows
        Bv[0] = (1 - αcg[0] - αsg[0]) * Bpg[0] + Ucg[0] + Usg[0];
        Br[0] = (1 - αcf[0] - αsf[0]) * Bpf[0] + Usf[0];
    }
    
    // NEW ADDITION 24/10 - Initialize agents and create network
    private void initializeSocialNormAgents() {
        // Calculate number of agents (assuming 2.5 people per household)
        numberOfAgents = (int)(P[0] / 2.5);
        
        agentThresholds = new double[numberOfAgents];
        agentSortingStatusCompost = new boolean[numberOfAgents];
        agentSortingStatusCollection = new boolean[numberOfAgents];
        neighborNetwork = new ArrayList<>();
        
        // Initialize agent thresholds randomly between 0 and 1 // its a set of value // 
        for (int i = 0; i < numberOfAgents; i++) {
            agentThresholds[i] = random.nextDouble();
            agentSortingStatusCompost[i] = false; // Initially not composting
            agentSortingStatusCollection[i] = false; // Initially not sorting for collection
            neighborNetwork.add(new ArrayList<>());
        }
        
        // Create neighbor network following Sylvie algorithm
        double p = (double)avgNeighborhood / numberOfAgents;
        
        // For every agent i, for every agent j (except i)
        for (int i = 0; i < numberOfAgents; i++) {
            for (int j = 0; j < numberOfAgents; j++) {
                if (i != j) { // except i
                    double x = random.nextDouble(); // Pick out a random = x
                    if (x < p) { // If (x < p)
                        neighborNetwork.get(i).add(j); // J is a neighbourhood of i
                    }
                }
            }
        }
    }
    
    // NEW ADDITION 24/10 - Calculate social norm for an agent
    private double calculateSocialNorm(int agentId, boolean[] sortingStatus) {
        List<Integer> neighbors = neighborNetwork.get(agentId);
        
        if (neighbors.isEmpty()) {
            return 0.0;
        }
        
        // Calculate α = (1/|Ni|) × Σ Sj(t)
        int sortingNeighbors = 0;
        for (int neighborId : neighbors) {
            if (sortingStatus[neighborId]) {
                sortingNeighbors++;
            }
        }
        
        return (double)sortingNeighbors / neighbors.size();
    }
    
    // NEW ADDITION 24/10 - Update social norms and behavioral intentions
    private void updateSocialNorms(int year) {
        // Update composting behavior
        boolean[] newCompostingStatus = new boolean[numberOfAgents];
        for (int i = 0; i < numberOfAgents; i++) {
            double socialNormCompost = calculateSocialNorm(i, agentSortingStatusCompost);
            newCompostingStatus[i] = (socialNormCompost >= agentThresholds[i]);
        }
        agentSortingStatusCompost = newCompostingStatus;
        
        // Update collection sorting behavior (if infrastructure exists)
        if (Ks[year] > 0) {
            boolean[] newCollectionStatus = new boolean[numberOfAgents];
            for (int i = 0; i < numberOfAgents; i++) {
                double socialNormCollection = calculateSocialNorm(i, agentSortingStatusCollection);
                newCollectionStatus[i] = (socialNormCollection >= agentThresholds[i]);
            }
            agentSortingStatusCollection = newCollectionStatus;
        }
        
        // Calculate territory-level adoption rates
        int compostingAgents = 0;
        int collectionAgents = 0;
        for (int i = 0; i < numberOfAgents; i++) {
            if (agentSortingStatusCompost[i]) compostingAgents++;
            if (agentSortingStatusCollection[i]) collectionAgents++;
        }
        
        double compostAdoptionRate = (double)compostingAgents / numberOfAgents;
        double collectionAdoptionRate = (double)collectionAgents / numberOfAgents;
        
        // Update behavioral intentions based on social norms
        if (myTerritory.useSocialDynamics) {
            // For composting
            αcf[year] = αcf[0] + (1 - αcf[0]) * compostAdoptionRate;
            αcg[year] = αcg[0] + (1 - αcg[0]) * compostAdoptionRate;
            
            // For dedicated collection (ensure consistency)
            if (Ks[year] > 0) {
                αsf[year] = αsf[0] + (1 - αsf[0]) * collectionAdoptionRate;
                αsg[year] = αsg[0] + (1 - αsg[0]) * collectionAdoptionRate;
                
                // Ensure sum doesn't exceed 1
                if (αcf[year] + αsf[year] > 1) {
                    αsf[year] = 1 - αcf[year];
                }
                if (αcg[year] + αsg[year] > 1) {
                    αsg[year] = 1 - αcg[year];
                }
            } else {
                αsf[year] = 0;
                αsg[year] = 0;
            }
        }
    }

    public void iterate(int y) {
        // NEW ADDITION 24/10 - Update social norms first
        updateSocialNorms(y);
        
        // Then continue with existing calculations
        computeProductionOfBiowaste(y);
        computeComposters(y);
        if (Ks[y] > 0) {
            computeCollectors(y);
        }
        computeFluxToValoCenterAndOMR(y);
    }
    
    public void computeProductionOfBiowaste(int y) {
        P[y] = P[0] * Math.pow(1 + r, y);
        ABP[y] = (myTerritory.sigmoideABP[y]);
        Bpf[y] = bpf * P[y] * (1 - apf * ABP[y]);
        Bpg[y] = bpg * P[y] * (1 - apg * ABP[y]);
        B[y] = Bpg[y] + Bpf[y];
    }
    
    public void computeComposters(int y) {
        if (Kc[y] > 0) {
            double quantiteCompostableFood = αcf[y] * Bpf[y];
            double quantiteCompostableGreen = αcg[y] * Bpg[y];
            Bc[y] = quantiteCompostableFood + quantiteCompostableGreen;
            
            Bcf[y] = Math.min(Kc[y], quantiteCompostableFood);
            Bcg[y] = Math.min((Kc[y] - Bcf[y]), quantiteCompostableGreen);
            
            Ucf[y] = Math.max((quantiteCompostableFood - Kc[y]), 0.0);
            Ucg[y] = Math.max((quantiteCompostableFood + quantiteCompostableGreen - Kc[y]), 0.0);
            
            Bc_composted[y] = Bcf[y] + Bcg[y];
        }
    }
    
    public void computeCollectors(int y) {
        double quantiteSortableFood = αsf[y] * Bpf[y] + Ucf[y];
        double quantiteSortableGreen = αsg[y] * Bpg[y];
        Bs[y] = quantiteSortableFood + quantiteSortableGreen;
        
        Bsf[y] = Math.min(Ks[y], quantiteSortableFood);
        Bsg[y] = Math.min((Ks[y] - Bsf[y]), quantiteSortableGreen);
        
        Usf[y] = Math.max((quantiteSortableFood - Ks[y]), 0.0);
        Usg[y] = Math.max((quantiteSortableFood + quantiteSortableGreen - Ks[y]), 0.0);
        
        Bs_sorted[y] = Bsf[y] + Bsg[y];
    }
    
    public void computeFluxToValoCenterAndOMR(int y) {
        Bv[y] = (1 - αcg[y] - αsg[y]) * Bpg[y] + Ucg[y] + Usg[y];
        Br[y] = (1 - αcf[y] - αsf[y]) * Bpf[y] + Usf[y];
    }
    
    public void printTrajectory(int y) {
        System.out.print((y+2017)+";");
        System.out.print(territoryName+";");
        System.out.print(P[y]+";");
        System.out.print(Bpf[y]+";");
        System.out.print(Bpg[y]+";");
        System.out.print(Bcf[y]+";");
        System.out.print(Bcg[y]+";");
        System.out.print(Bsf[y]+";");
        System.out.print(Bsg[y]+";");
        System.out.print(Br[y]+";");
        System.out.print(Bv[y]+";");
        System.out.print(Bc_composted[y]+";");
        System.out.print(Bs_sorted[y]+";");
        System.out.print(Bc[y]+";");
        System.out.print(Bs[y]+";");
        System.out.print(αcf[y]+";");
        System.out.print(αcg[y]+";");
        System.out.print(αsf[y]+";");
        System.out.print(αsg[y]+";");
        System.out.print(Kc[y]+";");
        System.out.print(Ks[y]+";");
        System.out.print(Ucf[y]+";");
        System.out.print(Ucg[y]+";");
        System.out.print(Usf[y]+";");
        System.out.print(Usg[y]+";");
        System.out.println();
    }
}
