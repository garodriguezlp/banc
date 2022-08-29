///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS org.apache.commons:commons-csv:1.8

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Locale.forLanguageTag;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;

@Command(name = "peoplepass", mixinStandardHelpOptions = true, version = "peoplepass 0.1",
        description = "peoplepass made with jbang")
class peoplepass implements Callable<Integer> {

    private static final String DATE_REGEX = "\\d{2}\\/\\d{2}\\/\\d{4}";
    private static final String DESCRIPTION_REGEX = "[\\w\\s]+";
    private static final String AMOUNT_REGEX = "\\$.*";
    private static final String TWO_LINES_WE_DO_NOT_CARE_REGEX = "(.*[\\n\\r]{1,2}){2}";

    private final Pattern recordPattern = compile("(" + DATE_REGEX + ")" +
            TWO_LINES_WE_DO_NOT_CARE_REGEX +
            "(" + DESCRIPTION_REGEX + ")(" + AMOUNT_REGEX + ")");

    private final DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance(forLanguageTag("es-CO"));
    private final DecimalFormat originAmountFormat = new DecimalFormat("$###,###.00", decimalFormatSymbols);
    private final DateTimeFormatter originDateFormat = ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter destDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    @Parameters(index = "0", description = "data file")
    private File data;

    public static void main(String... args) {
        int exitCode = new CommandLine(new peoplepass()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        String recordsRawData = Files.readString(data.toPath());
        List<Record> records = parseRecords(recordsRawData);
        printRecords(records);
        return 0;
    }

    private List<Record> parseRecords(String recordsRawData) {
        List<String> list = List.of(recordsRawData.split("\\d{5,}"));
        return list.stream()
                .map(this::parseRecord)
                .flatMap(Optional::stream)
                .collect(toList());
    }

    private Optional<Record> parseRecord(String rawRecord) {
        Matcher matcher = recordPattern.matcher(rawRecord);
        if (matcher.find()) {
            String date = matcher.group(1).trim();
            String description = matcher.group(3).trim();
            String amount = matcher.group(4).trim();

            LocalDate dateParsed = parseDate(date);
            Number amountParsed = parseAmount(amount);

            return Optional.of(new Record(dateParsed, description, amountParsed));
        }
        return Optional.empty();
    }

    private LocalDate parseDate(String date) {
        return LocalDate.parse(date, originDateFormat);
    }

    private Number parseAmount(String amount) {
        try {
            return originAmountFormat.parse(amount);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void printRecords(List<Record> records) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(System.out, CSVFormat.TDF)) {
            for (Record record : records) {
                printer.printRecord(formatDate(record.getDate()),
                        record.getDescription(),
                        record.getAmount(),
                        "11. PeoplePass");
            }
        }
    }

    private String formatDate(LocalDate date) {
        return date.format(destDateFormat);
    }

    public static class Record {
        private final LocalDate date;
        private final String description;
        private final Number amount;

        public Record(LocalDate date, String description, Number amount) {
            this.date = date;
            this.description = description;
            this.amount = amount;
        }

        public LocalDate getDate() {
            return date;
        }

        public String getDescription() {
            return description;
        }

        public Number getAmount() {
            return amount;
        }
    }
}
