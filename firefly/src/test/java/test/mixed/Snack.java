package test.mixed;

/**
 * @author Pengtao Qiu
 */
public class Snack extends Food {

    private String productionPlace;
    private String relish;

    public String getProductionPlace() {
        return productionPlace;
    }

    public void setProductionPlace(String productionPlace) {
        this.productionPlace = productionPlace;
    }

    public String getRelish() {
        return relish;
    }

    public void setRelish(String relish) {
        this.relish = relish;
    }

    @Override
    public String toString() {
        return "Snack{" +
                "name='" + name + '\'' +
                ", price=" + price +
                ", productionPlace='" + productionPlace + '\'' +
                ", relish='" + relish + '\'' +
                '}';
    }
}
