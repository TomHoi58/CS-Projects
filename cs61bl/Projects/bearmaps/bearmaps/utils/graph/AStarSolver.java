package bearmaps.utils.graph;

import bearmaps.utils.pq.MinHeapPQ;
import edu.princeton.cs.algs4.Stopwatch;

import java.util.*;

public class AStarSolver<Vertex> implements ShortestPathsSolver<Vertex> {
    private SolverOutcome outcome;
    private double solutionWeight;
    private List<Vertex> solution;
    private double timeSpent;
    private int numStatesExplored;


    public AStarSolver(AStarGraph<Vertex> input, Vertex start, Vertex end, double timeout){
        /**
         * start the timer
         */
        Stopwatch sw = new Stopwatch();

        /**
         * initialize dist,parents and pq
         */
        HashMap<Vertex,Double> dist = new HashMap<>();
        HashMap<Vertex,Vertex> parents = new HashMap<>();
        MinHeapPQ<Vertex> PQ = new MinHeapPQ<>();

        /**
         * insert the source vertex into the PQ
         */
        PQ.insert(start,0+input.estimatedDistanceToGoal(start,start));
        dist.put(start,0.0);
        parents.put(start,null);

        /**
         * A* process
         */
        while ( PQ.size()> 0 && !PQ.peek().equals(end) && sw.elapsedTime()< timeout){
            Vertex removed = PQ.poll();
            numStatesExplored++;
            /**
             * Relax all edges outgoing from removed
             */
            List<WeightedEdge<Vertex>> neighborEdges = input.neighbors(removed);
            for (WeightedEdge<Vertex> e: neighborEdges){
                Vertex p = e.from();
                Vertex q = e.to();
                Double w = e.weight();
                if (!dist.containsKey(q) || dist.get(p)+ w < dist.get(q)){
                    dist.put(q,dist.get(p)+ w);
                    parents.put(q,p);
                    if (PQ.contains(q)){
                        PQ.changePriority(q,dist.get(q)+input.estimatedDistanceToGoal(q,end));
                    }else {
                        PQ.insert(q,dist.get(q)+input.estimatedDistanceToGoal(q,end));
                    }
                }
            }

        }
        if (sw.elapsedTime() >= timeout){
            outcome = SolverOutcome.TIMEOUT;
            solution = new ArrayList<>();
            solutionWeight = 0;
            timeSpent = sw.elapsedTime();
            return;
        }
        if (PQ.size()<= 0){
            outcome = SolverOutcome.UNSOLVABLE;
            solution = new ArrayList<>();
            solutionWeight = 0;
            timeSpent = sw.elapsedTime();
            return;
        }else {
            outcome = SolverOutcome.SOLVED;
            Vertex intermediate = PQ.peek();
            solution = new ArrayList<>();
            while (intermediate != null) {
                solution.add(intermediate);
                intermediate = parents.get(intermediate);
            }
            solutionWeight = dist.get(PQ.peek());
            Collections.reverse(solution);
            timeSpent = sw.elapsedTime();
        }
    }


    @Override
    public SolverOutcome outcome() {
        return outcome;
    }

    @Override
    public List<Vertex> solution() {
        return solution;
    }

    @Override
    public double solutionWeight() { return solutionWeight;}

    @Override
    public int numStatesExplored() { return numStatesExplored;}

    @Override
    public double explorationTime() {
        return timeSpent;
    }
}