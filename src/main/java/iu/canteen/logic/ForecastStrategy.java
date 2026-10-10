package iu.canteen.logic;

/** Supplies the demand adjustment used by an ingredient forecast method. */
public interface ForecastStrategy {
    double demandMultiplier(int days);
}
