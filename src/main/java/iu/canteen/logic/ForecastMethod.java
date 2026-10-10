package iu.canteen.logic;

public enum ForecastMethod implements ForecastStrategy {
    MOVING_AVERAGE("Trung bình động 7 ngày (SMA)") {
        @Override
        public double demandMultiplier(int days) {
            return 1.0;
        }
    },
    WEEKDAY_AVERAGE("Trung bình theo thứ trong tuần") {
        @Override
        public double demandMultiplier(int days) {
            return 1.05;
        }
    },
    WEIGHTED_MOVING_AVG("Trung bình động có trọng số (WMA)") {
        @Override
        public double demandMultiplier(int days) {
            return 1.02;
        }
    },
    EXPONENTIAL_SMOOTHING("San bằng số mũ (Exponential Smoothing)") {
        @Override
        public double demandMultiplier(int days) {
            return 1.03;
        }
    },
    LINEAR_REGRESSION("Hồi quy xu hướng (Linear Regression)") {
        @Override
        public double demandMultiplier(int days) {
            return 1.06;
        }
    },
    SMART_AI_ENSEMBLE("AI Đa yếu tố (Ensemble: Doanh số + Thời tiết + Ngày lễ)") {
        @Override
        public double demandMultiplier(int days) {
            return 1.08;
        }
    };

    private final String label;

    ForecastMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
