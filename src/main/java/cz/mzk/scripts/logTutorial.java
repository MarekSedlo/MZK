package cz.mzk.scripts;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.*;
import java.util.logging.FileHandler;

public class logTutorial implements Script{
    private static final Logger logger = Logger.getLogger(logTutorial.class.getName());
    @Override
    public void start(Properties prop) {
        FileHandler fileHandler = null;
        try {
            fileHandler = new FileHandler("logs/log.log");
            logger.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
        } catch (IOException e) {
            e.printStackTrace();
        }


        logger.info("info log");
        logger.warning("warning log");
    }
}
