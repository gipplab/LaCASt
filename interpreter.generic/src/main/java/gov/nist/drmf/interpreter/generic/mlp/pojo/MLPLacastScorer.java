package gov.nist.drmf.interpreter.generic.mlp.pojo;

/**
 * @author Andre Greiner-Petter
 */
public class MLPLacastScorer {

    private double maxEsScore = 0;

    private double esScore = 0;
    private double dlmfScore = 0;
    private double mlpScore = 0;

    public MLPLacastScorer(double maxEsScore) {
        this.maxEsScore = maxEsScore;
    }

    public void setMacroESScore(double esScore) {
        this.esScore = esScore;
    }

    public void setMacroLikelihoodScore(double dlmfScore) {
        this.dlmfScore = dlmfScore;
    }

    public void setMlpScore(double mlpScore) {
        this.mlpScore = mlpScore;
    }

    public double getScore() {
        if ( maxEsScore <= 0 ) return 0;
        return mlpScore * (esScore/maxEsScore) * dlmfScore;
    }
}
