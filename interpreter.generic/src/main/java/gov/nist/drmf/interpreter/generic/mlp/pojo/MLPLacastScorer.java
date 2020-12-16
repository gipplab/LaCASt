package gov.nist.drmf.interpreter.generic.mlp.pojo;

/**
 * @author Andre Greiner-Petter
 */
public class MLPLacastScorer {

    private double maxEsScore = 0;

    public MLPLacastScorer(double maxEsScore) {
        this.maxEsScore = maxEsScore;
    }

    public double getScore(
            double mlpScore,
            double esScore,
            double dlmfScore) {
        return mlpScore * (esScore/maxEsScore) * dlmfScore;
    }
}
