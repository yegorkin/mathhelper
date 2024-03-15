package ua.kiev.splash.mathhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.kiev.splash.mathhelper.api.EquationRepository;
import ua.kiev.splash.mathhelper.api.Evaluator;
import ua.kiev.splash.mathhelper.api.StorageSupport;
import ua.kiev.splash.mathhelper.calculator.HandMadeEvaluator;
import ua.kiev.splash.mathhelper.menu.FatConsoleController;
import ua.kiev.splash.mathhelper.db.StorageService;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    StorageService storageService = new StorageService();
    EquationRepository equationRepository = storageService;
    StorageSupport storageSupport = storageService;
    Evaluator evaluator = new HandMadeEvaluator();
    FatConsoleController consoleController = new FatConsoleController(evaluator, equationRepository);

    public static void main(String[] args) {
        try {
            new Main().run();
        } catch (Exception e) {
            String msg = "Exception occurred: " + e;
            System.err.println(msg);
            log.error(msg);
        }
    }

    private void run() {
        log.debug("Application started");

        if (!storageSupport.isDataBaseTableCreated()) {
            storageSupport.createDataBaseTables();
        }

        consoleController.executeMainMenuLoop();

        log.debug("Application closed normally.");
    }
}
