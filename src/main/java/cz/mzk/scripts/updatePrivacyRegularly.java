package cz.mzk.scripts;

import cz.mzk.services.FileIO;
import cz.mzk.services.SolrUtils;

import java.util.List;
import java.util.Properties;

public class updatePrivacyRegularly implements Script{
    private final boolean DEBUG = true;
    private final int maxPidsToRead = 1000000;
    private final boolean K7SolrNames = false;
    SolrUtils solrUtils = new SolrUtils();
    FileIO fileIO = new FileIO();

    public void start(Properties prop) {
        String rok_vydani = "datum_begin";
        String duvernost = "dostupnost";
        String typ_dokumentu = "fedora.model";

        if (K7SolrNames){
            rok_vydani = "date_range_start.year";
            duvernost = "accessibility";
            typ_dokumentu = "model";
        }

        //TODO VSECHNY DOKUMENTY, KTERE NEJSOU PERIODIKA --> ktere nemaji root pid periodical
        //TODO u kazdeho takoveho dokumentu zjistit, jestli to ma child, ktery ma vetsi rok, nez 1900, pokud ne, tak zmenit cely dokument
        //TODO tyto dokumenty, ktere maji nejaky takovy child menit nebudu, jen si vyfiltruju tyto children a ty pak zmenim
        String query = rok_vydani + ":[250 TO 1900] AND " + duvernost + ":private";
        List <String> privatePidsAllTypes = solrUtils.getPids(query, maxPidsToRead, true);
        //fileIO.toOutputFile(pidsToMakePublic, "IO/438/pids.txt");

        //TODO PERIODIKA pokud nebudou mit zadne rocniky, ktere jsou vetsi, nez 1910, tak zmenit cela periodika
        query = rok_vydani + ":[1901 TO 1910] AND " + typ_dokumentu + ":periodical AND " + duvernost + ":private";
        List<String> privatePidsPer = solrUtils.getPids(query, maxPidsToRead, true);

        //TODO periodika, ktere budou mit nejake takove rocniky, tak u nich jen zmenim rocniky
        query = rok_vydani + ":[1901 TO 1910] AND " + typ_dokumentu + ":periodicalvolume AND " + duvernost + ":private";
        List<String> privatePidsPerVol = solrUtils.getPids(query, maxPidsToRead, true);

    }
}
