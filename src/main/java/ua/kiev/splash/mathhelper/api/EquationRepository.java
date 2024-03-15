package ua.kiev.splash.mathhelper.api;

import ua.kiev.splash.mathhelper.dto.EquationDto;
import ua.kiev.splash.mathhelper.dto.RootDto;

import java.util.List;

public interface EquationRepository {
    void saveNewEquation(EquationDto equation);
    EquationDto findEquationById(long equationId);
    List<RootDto> findRootsByEquationId(long equationId);
    void saveNewRoot(RootDto root);
    List<EquationDto> findEquationsByRootValue(double rootValue);
    List<EquationDto> findEquationsWithSingleSavedRoot();
}
