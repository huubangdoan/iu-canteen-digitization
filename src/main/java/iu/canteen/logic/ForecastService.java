package iu.canteen.logic;

import iu.canteen.logic.Store.Dish;
import iu.canteen.logic.Store.ForecastRow;
import iu.canteen.logic.Store.Ingredient;
import iu.canteen.logic.Store.RecipeLine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Computes ingredient requirements from dish demand and recipes. */
public final class ForecastService {
    private ForecastService() {}

    public static List<ForecastRow> forecast(int days, ForecastStrategy strategy) {
        double multiplier = strategy.demandMultiplier(days);
        Map<Ingredient, Double> need = new LinkedHashMap<>();
        for (Ingredient ingredient : Store.ingredients) {
            need.put(ingredient, 0.0);
        }
        for (Dish dish : Store.menu) {
            for (RecipeLine recipeLine : Store.recipeFor(dish)) {
                need.merge(recipeLine.ingredient,
                        dish.avgDaily * recipeLine.qty.get() * days * multiplier,
                        Double::sum);
            }
        }

        List<ForecastRow> rows = new ArrayList<>();
        for (Map.Entry<Ingredient, Double> entry : need.entrySet()) {
            Ingredient ingredient = entry.getKey();
            double required = round1(entry.getValue());
            double stock = ingredient.stock.get();
            double buy = Math.max(0, round1(required + ingredient.minStock - stock));
            String level = buy <= 0 ? "Đủ" : (stock < required ? "Khẩn cấp" : "Nên mua");
            rows.add(new ForecastRow(ingredient.name, ingredient.unit, stock, required, buy, level));
        }
        return rows;
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
