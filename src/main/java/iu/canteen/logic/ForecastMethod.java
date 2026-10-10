package iu.canteen.logic;

public enum ForecastMethod implements ForecastStrategy {
    MOVING_AVERAGE("Trung bình động 7 ngày") {
        @Override
        public double demandMultiplier(int days) {
            return 1.0;
        }
    },
    WEEKDAY_AVERAGE("Trung bình theo thứ trong tuần") {
        @Override
        public double demandMultiplier(int days) {
            return 1.10;
        }
    };

    private final String label;

    ForecastMethod(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
