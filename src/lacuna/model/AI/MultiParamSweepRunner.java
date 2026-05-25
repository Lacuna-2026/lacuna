package lacuna.model.AI;

import lacuna.model.*;
import java.io.*;
import java.util.*;

public class MultiParamSweepRunner {

    public static void main(String[] args) throws Exception {
        int gamesPerSetting = 10;
        int[] tunedSides = new int[] {0, 1};

        // Parametre par défaut
        int majorityBase = 200;
        int diffBonus = 10;
        int majorityWinThreshold = 4;
        int majorityWinBase = 50000;
        int majorityWinStep = 5000;
        int majorityThreeBonus = 1000;

        int quickMyCountGt3 = 500;
        int quickMyCountEq3 = 200;
        int quickOppCountGe3 = 300;
        int quickFlowerWeight = 50;

        double cfWeightDefault = 1.0;
        double cfWeightLess = 3.0;
        double cfWeightEqual = 2.0;
        double cfWeightMyThree = 2.5;

        // Les rangs des parametres
        int[] diffMults = new int[] {0,10,20,30,40,50};
        int[] majorityBases = new int[] {0,100,200,300,400,500,600,700,800,900,
            1000,1100,1200,1300,1400,1500};
        int[] majorityWinBases = new int[] {0,10000,50000,100000};
        int[] quickMyCountGt3s = new int[] {0,100,200,300,400,500,600,700,800,900,1000};
        int[] quickMyCountEq3s = new int[] {0,50,100,150,200,250,300,350,400};
        int[] quickOppCountGe3s = new int[] {0,50,100,150,200,250,300,350,400,450,500};
        int[] quickFlowerWeights = new int[] {0,10,20,30,40,50,60,70,80,90,100};

        if (args.length > 0 && "diffMult".equals(args[0])) {
            majorityBase = 0;
            runSweep("diffMult", diffMults, gamesPerSetting, tunedSides,
                    majorityBase, diffBonus, majorityWinThreshold, majorityWinBase, majorityWinStep, majorityThreeBonus,
                    quickMyCountGt3, quickMyCountEq3, quickOppCountGe3, quickFlowerWeight,
                    cfWeightDefault, cfWeightLess, cfWeightEqual, cfWeightMyThree);
            System.out.println("DiffMult sweep complete.");
            return;
        }

        if (args.length > 0 && "quick".equals(args[0])) {
            runSweep("quickMyCountGt3", quickMyCountGt3s, gamesPerSetting, tunedSides,
                majorityBase, diffBonus, majorityWinThreshold, majorityWinBase, majorityWinStep, majorityThreeBonus,
                quickMyCountGt3, quickMyCountEq3, quickOppCountGe3, quickFlowerWeight,
                cfWeightDefault, cfWeightLess, cfWeightEqual, cfWeightMyThree);

            runSweep("quickMyCountEq3", quickMyCountEq3s, gamesPerSetting, tunedSides,
                majorityBase, diffBonus, majorityWinThreshold, majorityWinBase, majorityWinStep, majorityThreeBonus,
                quickMyCountGt3, quickMyCountEq3, quickOppCountGe3, quickFlowerWeight,
                cfWeightDefault, cfWeightLess, cfWeightEqual, cfWeightMyThree);

            runSweep("quickOppCountGe3", quickOppCountGe3s, gamesPerSetting, tunedSides,
                majorityBase, diffBonus, majorityWinThreshold, majorityWinBase, majorityWinStep, majorityThreeBonus,
                quickMyCountGt3, quickMyCountEq3, quickOppCountGe3, quickFlowerWeight,
                cfWeightDefault, cfWeightLess, cfWeightEqual, cfWeightMyThree);

            runSweep("quickFlowerWeight", quickFlowerWeights, gamesPerSetting, tunedSides,
                majorityBase, diffBonus, majorityWinThreshold, majorityWinBase, majorityWinStep, majorityThreeBonus,
                quickMyCountGt3, quickMyCountEq3, quickOppCountGe3, quickFlowerWeight,
                cfWeightDefault, cfWeightLess, cfWeightEqual, cfWeightMyThree);

            System.out.println("Quick sweeps complete.");
            return;
        }

        runSweep("diffMult", diffMults, gamesPerSetting, tunedSides,
                majorityBase, diffBonus, majorityWinThreshold, majorityWinBase, majorityWinStep, majorityThreeBonus,
                quickMyCountGt3, quickMyCountEq3, quickOppCountGe3, quickFlowerWeight,
                cfWeightDefault, cfWeightLess, cfWeightEqual, cfWeightMyThree);

        runSweep("majorityBase", majorityBases, gamesPerSetting, tunedSides,
                majorityBase, diffBonus, majorityWinThreshold, majorityWinBase, majorityWinStep, majorityThreeBonus,
                quickMyCountGt3, quickMyCountEq3, quickOppCountGe3, quickFlowerWeight,
                cfWeightDefault, cfWeightLess, cfWeightEqual, cfWeightMyThree);

        runSweep("majorityWinBase", majorityWinBases, gamesPerSetting, tunedSides,
                majorityBase, diffBonus, majorityWinThreshold, majorityWinBase, majorityWinStep, majorityThreeBonus,
                quickMyCountGt3, quickMyCountEq3, quickOppCountGe3, quickFlowerWeight,
                cfWeightDefault, cfWeightLess, cfWeightEqual, cfWeightMyThree);

        System.out.println("All sweeps complete.");
    }

    private static void runSweep(String paramName, int[] values, int gamesPerSetting, int[] tunedSides,
            int majorityBase, int diffBonus, int majorityWinThreshold, int majorityWinBase, int majorityWinStep, int majorityThreeBonus,
            int quickMyCountGt3, int quickMyCountEq3, int quickOppCountGe3, int quickFlowerWeight,
            double cfWeightDefault, double cfWeightLess, double cfWeightEqual, double cfWeightMyThree) throws Exception {

        String outCsv = "src/lacuna/model/AI/stats/tuning_" + paramName + ".csv";
        PrintWriter pw = new PrintWriter(new FileWriter(outCsv));
        pw.println(paramName + ",wins,losses,draws,win_rate");

        for (int v : values) {
            int wins = 0, losses = 0, draws = 0;
            for (int tunedPlayerIndex : tunedSides) {
                for (int g = 0; g < gamesPerSetting; g++) {
                    long seed = System.nanoTime() + (g * 31L) + (v * 131L) + tunedPlayerIndex;
                    GameModel model = new GameModel("Tuned", "Base", seed);

                    while (model.getPhase() == GameModel.GamePhase.PLACING) {
                        int current = model.getJoueurCourant().getIndex();

                        int useDiffMult = 30;
                        int useMajorityBase = majorityBase;
                        int useMajorityWinBase = majorityWinBase;

                        if (paramName.equals("diffMult")) useDiffMult = v;
                        if (paramName.equals("majorityBase")) useMajorityBase = v;
                        if (paramName.equals("majorityWinBase")) useMajorityWinBase = v;

                        IAIEvaluator evaluator = (current == tunedPlayerIndex)
                                ? new TunableAIEvaluator(useMajorityBase, useDiffMult, diffBonus,
                                        majorityWinThreshold, useMajorityWinBase, majorityWinStep, majorityThreeBonus,
                                        quickMyCountGt3, quickMyCountEq3, quickOppCountGe3, quickFlowerWeight,
                                        cfWeightDefault, cfWeightLess, cfWeightEqual, cfWeightMyThree)
                                : new DefaultAIEvaluator();

                        Minimax ai = new Minimax(model, current, 1, 200, evaluator, new DefaultAIMoveGenerator());
                        AIMove move = ai.findBestMove();
                        if (move == null) { break; }

                        boolean ok = model.placerPionEtCapturer(model.getFleurs().get(move.flower1Index),
                                model.getFleurs().get(move.flower2Index), move.pawnX, move.pawnY);
                        if (!ok) { break; }
                    }

                    model.resoudreProximite();
                    Player winner = model.getVainqueur();
                    if (winner == null) draws++;
                    else if (winner.getIndex() == tunedPlayerIndex) wins++;
                    else losses++;
                }
            }
            double winRate = (double) wins / (gamesPerSetting * tunedSides.length);
            pw.printf(Locale.US, "%d,%d,%d,%d,%.4f%n", v, wins, losses, draws, winRate);
            pw.flush();
            System.out.println("Completed " + paramName + "=" + v + " winRate=" + winRate);
        }
        pw.close();
        System.out.println("Results written to " + outCsv);
    }
}
