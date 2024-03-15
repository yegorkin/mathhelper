package ua.kiev.splash.mathhelper.dto;

// next time I will use Lombok
public class EquationDto {
    private long id;
    private String expression;

    public EquationDto(String expression) {
        this.expression = expression;
    }

    public EquationDto(long id, String expression) {
        this.id = id;
        this.expression = expression;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "EquationDto{" +
                "id=" + id +
                ", expression='" + expression + '\'' +
                '}';
    }
}
