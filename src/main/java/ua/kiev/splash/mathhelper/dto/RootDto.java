package ua.kiev.splash.mathhelper.dto;

// next time I will use Lombok
public class RootDto {
    private long id;
    private long equationId;
    private double value;

    public RootDto(long equationId, double value) {
        this.equationId = equationId;
        this.value = value;
    }

    public RootDto(long id, long equationId, double value) {
        this.id = id;
        this.equationId = equationId;
        this.value = value;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getEquationId() {
        return equationId;
    }

    public void setEquationId(long equationId) {
        this.equationId = equationId;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "RootDto{" +
                "id=" + id +
                ", equationId=" + equationId +
                ", value=" + value +
                '}';
    }
}
