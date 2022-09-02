///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS org.apache.commons:commons-csv:1.8

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "banc",
    mixinStandardHelpOptions = true,
    version = "banc 0.1",
    description = "A Bancolombia CSV mapper made with jbang")
class banc implements Callable<Integer> {

    enum Headers {
        DATE, DOCUMENT, OFFICE, DESCRIPTION, REFERENCE, VALUE
    }

    @Parameters(index = "0", description = "The csv file")
    private File csvFile;

    public static void main(String... args) {
        int exitCode = new CommandLine(new banc()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        Iterable<CSVRecord> records = parseCvsFile();
        printNewFormattedCsv(records);
        return 0;
    }

    private Iterable<CSVRecord> parseCvsFile() throws IOException {
        Reader reader = new InputStreamReader(new FileInputStream(csvFile), Charset.forName("windows-1252"));
        return CSVFormat.TDF.withHeader(Headers.class).withSkipHeaderRecord().withAllowMissingColumnNames()
                .parse(reader);
    }

    private void printNewFormattedCsv(Iterable<CSVRecord> records) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(System.out, CSVFormat.TDF)) {
            for (CSVRecord csvRecord : records) {
                printer.printRecord(reformatDate(csvRecord.get(Headers.DATE)), csvRecord.get(Headers.DESCRIPTION),
                        csvRecord.get(Headers.VALUE), "1. Bancolombia");
            }
        }
    }

    private String reformatDate(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                .format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
    }
}
