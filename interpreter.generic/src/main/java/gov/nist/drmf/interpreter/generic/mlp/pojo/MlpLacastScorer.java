package gov.nist.drmf.interpreter.generic.mlp.pojo;

/**
 * @author Andre Greiner-Petter
 */
public class MlpLacastScorer {

    private double maxEsScore = 0;

    public MlpLacastScorer(double maxEsScore) {
        this.maxEsScore = maxEsScore;
    }

    public double getScore(
            double mlpScore,
            double esScore,
            double dlmfScore) {
        return mlpScore * (esScore/maxEsScore) * dlmfScore;
    }
}
