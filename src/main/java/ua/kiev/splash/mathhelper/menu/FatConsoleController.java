package ua.kiev.splash.mathhelper.menu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.kiev.splash.mathhelper.api.EquationRepository;
import ua.kiev.splash.mathhelper.api.Evaluator;
import ua.kiev.splash.mathhelper.dto.EquationDto;
import ua.kiev.splash.mathhelper.dto.RootDto;
import ua.kiev.splash.mathhelper.exceptions.DuplicateKeyException;
import ua.kiev.splash.mathhelper.exceptions.EvaluationException;
import ua.kiev.splash.mathhelper.utils.Utils;

import java.util.*;

public class FatConsoleController {
    private static final Logger log = LoggerFactory.getLogger(FatConsoleController.class);

    private static final String ANSI_YELLOW = "\u001B[33m"; // yellow console text color
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ZERO_WIDTH_LOOKAHEAD_ASSERTION = "(?=\\S)"; // "(?=\S)" does not consume data

    // sorry, no time for perfect resource localization
    private static final String SHORT_HELP_RESOURCE = "help.txt";
    private static final String EQUATION_ENTERING_MESSAGE = "Введення математичного рівняння";
    private static final String EQUATION_SELECTION_MESSAGE = "Вибір поточного рівняння";
    private static final String ROOTS_ENTERING_MESSAGE = "Введення коренів";
    private static final String EQUATION_SEARCH_MESSAGE = "Пошук рівнянь у БД за їхніми коренями";
    private static final String HELP_MESSAGE = "Коротка довідка";
    private static final String QUIT_MESSAGE = "Вихід";

    private final Evaluator evaluator;
    private final EquationRepository equationRepository;

    private enum Menu {
        EQUATION_ENTERING, EQUATION_SELECTION, ROOTS_ENTERING, EQUATION_SEARCH, HELP, QUIT;

        public static Menu getMenuItemByNaturalOrder(int naturalOrderNumber) {
            return Menu.values()[naturalOrderNumber - 1];
        }

        public int getNaturalOrder() {
            return ordinal() + 1;
        }
    }

    private static final String MAIN_MENU_TEXT = "Меню: \n" +
            Menu.EQUATION_ENTERING.getNaturalOrder() + ". " + EQUATION_ENTERING_MESSAGE + "\n" +
            Menu.EQUATION_SELECTION.getNaturalOrder() + ". " + EQUATION_SELECTION_MESSAGE + "\n" +
            Menu.ROOTS_ENTERING.getNaturalOrder() + ". " + ROOTS_ENTERING_MESSAGE + "\n" +
            Menu.EQUATION_SEARCH.getNaturalOrder() + ". " + EQUATION_SEARCH_MESSAGE + "\n" +
            Menu.HELP.getNaturalOrder() + ". " + HELP_MESSAGE + "\n" +
            Menu.QUIT.getNaturalOrder() + ". " + QUIT_MESSAGE + "\n";

    private static final Set<String> DEFAULT_EQUATION_PARAMETERS = Collections.singleton(Evaluator.DEFAULT_ROOT_NAME);
    private EquationDto selectedEquation;
    private final Scanner consoleScanner = new Scanner(System.in); // do not close this scanner

    public FatConsoleController(Evaluator evaluator, EquationRepository equationRepository) {
        this.evaluator = evaluator;
        this.equationRepository = equationRepository;
    }

    public void executeMainMenuLoop() {
        writeln("Математичний помічник вітає вас");
        Menu menuItem;
        do {
            int defaultInput = Menu.EQUATION_ENTERING.getNaturalOrder();
            int userInput = askForInt(MAIN_MENU_TEXT + "Введіть номер пункту меню (або за замовченням буде " +
                    defaultInput + ") та натисніть клавішу Enter: ", 1, Menu.values().length, defaultInput);
            menuItem = Menu.getMenuItemByNaturalOrder(userInput);
            writeln("Ви обрали пункт " + userInput);
            log.debug("User selected {} menu item", menuItem);

            switch (menuItem) {
                case EQUATION_ENTERING: executeEquationEntering(); break;
                case EQUATION_SELECTION: executeEquationSelection(); break;
                case ROOTS_ENTERING: executeRootsEntering(); break;
                case EQUATION_SEARCH: executeEquationSearch(); break;
                case HELP: executeHelp(); break;
                case QUIT: executeExitMainMenu(); break;
            }
        } while (menuItem != Menu.QUIT);
    }

    private void executeEquationEntering() {
        writeln(EQUATION_ENTERING_MESSAGE);
        String equationString = askForAnyString(
                "Введіть математичне рівняння (або порожній рядок для відміни) та натисніть Enter:\n");
        log.debug("User has entered equationString: \"{}\"", equationString);
        if (equationString != null && !equationString.trim().isEmpty()) {
            try {
                evaluator.validateEquationSyntax(equationString, DEFAULT_EQUATION_PARAMETERS);
                EquationDto equation = new EquationDto(equationString);
                equationRepository.saveNewEquation(equation);
                writeln("Рівняння було збережено в базі даних під обліковим номером " + equation.getId());
                selectedEquation = equation;
                writeln("Також це рівняння зараз є поточним і для нього можна ввести корені");
            } catch (EvaluationException e) {
                writeln("На жаль, введене рівняння не пройшло перевірку:\n" + e.getMessage());
            }
        } else {
            writeln("Математичне рівняння не було введено, повертаємось до головного меню.");
        }
    }

    private void executeEquationSelection() {
        writeln(EQUATION_SELECTION_MESSAGE);
        writeCurrentEquation();
        Long equationId = askForAnyLong("Введіть обліковий номер збереженого рівняння: ");
        if (equationId != null) {
            EquationDto equation = equationRepository.findEquationById(equationId);
            if (equation != null) {
                selectedEquation = equation;
                writeln("Рівняння #" + equation.getId() + " знайдено і зроблено поточним: " + equation.getExpression());

                List<RootDto> existingRoots = equationRepository.findRootsByEquationId(selectedEquation.getId());
                writeExistingRoots(existingRoots);

                writeln("Для поточного рівняння можна ввести нові корені.");
            } else {
                writeln("Не знайдено рівняння з таким номером: " + equationId);
            }
        } else {
            writeln("Номер рівняння не було введено, повертаємось до головного меню.");
        }
    }

    private void executeRootsEntering() {
        writeln(ROOTS_ENTERING_MESSAGE);
        writeCurrentEquation();
        if (selectedEquation != null) {
            Double enteredRoot;
            do {
                enteredRoot = askForAnyDouble(
                        "Введіть корінь \"x\" для поточного рівняння або порожній рядок для завершення вводу: ");
                if (enteredRoot != null) {
                    boolean equals = evaluator.checkEquationEquality(selectedEquation.getExpression(),
                            Map.of(Evaluator.DEFAULT_ROOT_NAME, enteredRoot), Evaluator.DEFAULT_EPSILON);
                    if (equals) {
                        writeln("Введенне число є коренем рівняння.");
                        try {
                            equationRepository.saveNewRoot(new RootDto(selectedEquation.getId(), enteredRoot));
                            writeln("Цей корінь було збережено у базі даних.");
                        } catch (DuplicateKeyException dke) {
                            writeln("Помилка! Такий корінь вже існує у базі даних для поточного рівняння.");
                        }
                    } else {
                        writeln("Введенне число не є коренем рівняння, тому воно не було збережено.");
                    }
                } else {
                    writeln("Введення коренів завершено.");
                }
            } while (enteredRoot != null);
        } else {
            writeln("Щоб мати можливість вводити корені, спочатку треба ввести рівняння або знайти його за номером.");
        }
    }

    private void executeEquationSearch() {
        writeln(EQUATION_SEARCH_MESSAGE);
        writeln("Варіанти пошуку:");
        writeln("  1: знайти рівняння за збереженими коренями (режим за замовчуванням);");
        writeln("  2: знайти всі рівняння, які мають рівно один корінь, збережений у БД.");
        boolean enterRoots = askForInt("Введіть номер режима пошуку " +
                "(або порожній рядок для режиму за замовчуванням): ", 1, 2, 1) == 1;
        if (enterRoots) {
            writeln("Обрано пошук рівнянь за коренями.");
            Double enteredRoot;
            do {
                enteredRoot = askForAnyDouble(
                        "Введіть корінь для пошуку або порожній рядок для завершення пошуку за коренями: ");
                if (enteredRoot != null) {
                    write("Введено корінь: " + enteredRoot + ". ");
                    List<EquationDto> foundEquations = equationRepository.findEquationsByRootValue(enteredRoot);
                    writeFoundEquations(foundEquations);
                } else {
                    writeln("Пошук за коренями завершено.");
                }
            } while (enteredRoot != null);
        } else {
            writeln("Обрано пошук рівнянь, які мають рівно один збережений корінь.");
            List<EquationDto> foundEquations = equationRepository.findEquationsWithSingleSavedRoot();
            writeFoundEquations(foundEquations);
        }
    }

    private void executeHelp() {
        writeln(HELP_MESSAGE);
        writeln();
        writeln(Utils.getResourceFileAsString(SHORT_HELP_RESOURCE));
    }

    private void executeExitMainMenu() {
        writeln("До побачення!");
        try {
            Thread.sleep(1000);
        } catch(InterruptedException e) {
            log.error("Exception occurred in delay before exit", e);
        }
    }

    private void writeCurrentEquation() {
        if (selectedEquation != null) {
            writeln("Поточне рівняння (#" + selectedEquation.getId() + "): " + selectedEquation.getExpression());
        } else {
            writeln("Поточне рівняння не встановлено.");
        }
    }

    private void writeFoundEquations(List<EquationDto> foundEquations) {
        if (!foundEquations.isEmpty()) {
            writeln("Знайдені рівняння:");
            for (EquationDto equation : foundEquations) {
                writeln("#" + equation.getId() + ": " + equation.getExpression());
            }
        } else {
            writeln("Рівняння за вашим запитом не знайдено.");
        }
    }

    private void writeExistingRoots(List<RootDto> existingRoots) {
        if (!existingRoots.isEmpty()) {
            writeln("Збережені корені:");
            int i = 0;
            for (RootDto root : existingRoots) {
                writeln((++i) + ": " + root.getValue()); // no need to show root.getId()
            }
        } else {
            writeln("Немає збережених коренів.");
        }
    }

    public int askForInt(String requestToUser, int minValue, int maxValue, int defaultValue) {
        Integer userInput = null;

        do {
            writeln();
            write(requestToUser);

            if (consoleScanner.hasNextLine()) {
                if (consoleScanner.findInLine(ZERO_WIDTH_LOOKAHEAD_ASSERTION) == null) { // does not consume data here
                    log.debug("User pressed Enter without any additional input and accepted default value {}", defaultValue);
                    userInput = defaultValue;
                    String line = consoleScanner.nextLine();
                    log.trace("Empty user input: \"{}\"", line);
                } else if (consoleScanner.hasNextInt()) {
                    int input = consoleScanner.nextInt();
                    if (input >= minValue && input <= maxValue) {
                        userInput = input;
                    } else {
                        log.debug("User input is out of bounds: {}", input);
                    }
                    String line = consoleScanner.nextLine();
                    log.trace("Unneeded user input discarded: \"{}\"", line);
                } else {
                    String line = consoleScanner.nextLine();
                    log.debug("Wrong user input: \"{}\"", line);
                }
            }
        } while (userInput == null);

        return userInput;
    }

    public Long askForAnyLong(String requestToUser) {
        Long userInput = null;

        do {
            writeln();
            write(requestToUser);

            if (consoleScanner.hasNextLine()) {
                if (consoleScanner.findInLine(ZERO_WIDTH_LOOKAHEAD_ASSERTION) == null) { // does not consume data here
                    log.debug("User pressed Enter without any additional input");
                    String line = consoleScanner.nextLine();
                    log.debug("Empty user input: \"{}\"", line);
                    break;
                } else if (consoleScanner.hasNextLong()) {
                    userInput = consoleScanner.nextLong();
                    String line = consoleScanner.nextLine();
                    log.debug("Unneeded user input discarded: \"{}\"", line);
                } else {
                    String line = consoleScanner.nextLine();
                    log.debug("Wrong user input: \"{}\"", line);
                }
            }
        } while (userInput == null);

        return userInput;
    }

    public Double askForAnyDouble(String requestToUser) {
        Double userInput = null;

        do {
            writeln();
            write(requestToUser);

            if (consoleScanner.hasNextLine()) {
                if (consoleScanner.findInLine(ZERO_WIDTH_LOOKAHEAD_ASSERTION) == null) { // does not consume data here
                    log.debug("User pressed Enter without any additional input");
                    String line = consoleScanner.nextLine();
                    log.debug("Empty user input: \"{}\"", line);
                    break;
                } else if (consoleScanner.hasNextDouble()) {
                    userInput = consoleScanner.nextDouble();
                    String line = consoleScanner.nextLine();
                    log.debug("Unneeded user input discarded: \"{}\"", line);
                } else {
                    String line = consoleScanner.nextLine();
                    log.debug("Wrong user input: \"{}\"", line);
                }
            }
        } while (userInput == null);

        return userInput;
    }

    public String askForAnyString(String requestToUser) {
        String userInput = null;

        writeln();
        write(requestToUser);

        if (consoleScanner.hasNextLine()) {
            userInput = consoleScanner.nextLine();
            log.debug("User input string: \"{}\"", userInput);
        }

        return userInput;
    }

    public void writeln() {
        System.out.println();
    }

    public void write(String text) {
        // fix UA console output by replacing Ukrainian letter with Latin letter "i"
        String fixedText = text.replaceAll("і", "i");
        System.out.print(ANSI_YELLOW + fixedText + ANSI_RESET);
    }

    public void writeln(String text) {
        write(text);
        writeln();
    }
}


//InputStreamReader reader = new InputStreamReader(System.in);
//BufferedReader br = new BufferedReader(reader);
//System.out.println("Whis is our name?");
//var input = br.readLine();
//System.out.println("Your input was: " + input);
//
//writeln("::: YOU ENTERED " + a);
//
//a = askForInt("Введіть ціле число [1...3]: ", 1, 3, 2);
//
//
//a = askForInt("Введіть ціле число [10...11]: ", 10, 11, -1);
//writeln("::: YOU ENTERED " + a);
//
//Double d = askForAnyDouble("Введіть корень рівняння 1:");
//writeln("::: YOU ENTERED " + d);
//
//d = askForAnyDouble("Введіть корень рівняння 2:");
//writeln("::: YOU ENTERED " + d);
//
//d = askForAnyDouble("Введіть корень рівняння 3:");
//writeln("::: YOU ENTERED " + d);
