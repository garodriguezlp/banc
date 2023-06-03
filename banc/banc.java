///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17+

//DEPS io.quarkus:quarkus-bom:2.16.7.Final@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS org.apache.commons:commons-csv:1.10.0

//Q:CONFIG quarkus.banner.enabled=false
//Q:CONFIG quarkus.log.level=INFO
//Q:CONFIG quarkus.log.category."io.quarkus".level=ERROR

import static org.apache.commons.csv.CSVFormat.TDF;

import io.quarkus.logging.Log;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jboss.logging.Logger;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

// -- ------------------------------------------------------------------------------------------------------------------
// -- Driving Adapters
// -- ------------------------------------------------------------------------------------------------------------------
@Command(
    name = "banc",
    mixinStandardHelpOptions = true,
    description = "Transforms financial data to my own format",
    version = "banc 0.1",
    showDefaultValues = true,
    sortOptions = false,
    headerHeading = "Usage:%n%n",
    synopsisHeading = "%n",
    descriptionHeading = "%nDescription:%n%n",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n",
    commandListHeading = "%nCommands:%n",
    footerHeading = "%n",
    header = "Transforms financial data from one format to another"
)
public class banc implements Runnable {

  @Parameters(index = "0", description = "The input file with finacial data")
  private File inputFile;

  @ArgGroup(exclusive = true, multiplicity = "1")
  Institution institution;

  static class Institution {

    @Option(names = {"--bancolombia", "-b"}, required = true)
    boolean bancolombia;

    @Option(names = {"--peoplepass", "-p"}, required = true)
    boolean peoplepass;
  }

  @Option(names = {"-c", "--clipboard"},
      negatable = true,
      defaultValue = "true",
      fallbackValue = "true",
      description = "copy token to clipboard")
  private boolean copyToClipboard = true;

  @Inject
  TransformationService transformationService;

  @Override
  public void run() {
    transformationService.transform(inputFile,
        financialInstitution(),
        new TransformationOptions(copyToClipboard));
  }

  private FinancialInstitution financialInstitution() {
    FinancialInstitution fInstitution = null;
    if (institution.bancolombia) {
      fInstitution = FinancialInstitution.BANCOLOMBIA;
    } else if (institution.peoplepass) {
      fInstitution = FinancialInstitution.PEOPLEPASS;
    }
    return fInstitution;
  }

}

// -- ------------------------------------------------------------------------------------------------------------------
// -- Driven Adapters
// -- ------------------------------------------------------------------------------------------------------------------
@ApplicationScoped
class BancolombiaReader implements RecordReader {

  private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
  private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);

  enum Headers {
    DATE, DOCUMENT, OFFICE, DESCRIPTION, REFERENCE, VALUE
  }

  @Override
  public boolean supports(FinancialInstitution fInstitution) {
    return fInstitution == FinancialInstitution.BANCOLOMBIA;
  }

  @Override
  public List<OriginRecord> read(File inputFile) throws IOException {
    Log.infov("Reading file {0} for {1}", inputFile, FinancialInstitution.BANCOLOMBIA);
    try {
      return read(new FileInputStream(inputFile));
    } catch (IOException e) {
      Log.errorv(e, "Error reading file {0}", inputFile);
      throw e;
    }
  }

  public List<OriginRecord> read(InputStream inputStream) throws IOException {
    return parseCvsFile(inputStream)
        .stream()
        .map(this::toRecord)
        .toList();
  }

  private List<CSVRecord> parseCvsFile(InputStream in) throws IOException {
    try (Reader reader = new InputStreamReader(in, WINDOWS_1252)) {
      return CSVFormat.Builder.create(TDF)
          .setHeader(Headers.class)
          .setSkipHeaderRecord(true)
          .setAllowMissingColumnNames(true)
          .build()
          .parse(reader)
          .getRecords();
    }
  }

  private OriginRecord toRecord(CSVRecord csvRecord) {
    return new OriginRecord(
        parseDate(csvRecord.get(Headers.DATE)),
        csvRecord.get(Headers.DESCRIPTION),
        parseAmount(csvRecord.get(Headers.VALUE)),
        FinancialInstitution.BANCOLOMBIA.accountName());
  }

  private static Double parseAmount(String amount) {
    try {
      return NUMBER_FORMAT.parse(amount).doubleValue();
    } catch (ParseException e) {
      Log.errorv(e, "Error parsing amount {0}", amount);
      throw new IllegalArgumentException("Error parsing amount " + amount);
    }
  }

  private static LocalDate parseDate(String date) {
    return LocalDate.parse(date, DATE_TIME_FORMATTER);
  }
}

@ApplicationScoped
class PeoplepassReader implements RecordReader {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DecimalFormat NUMBER_FORMAT = buildDecimalFormat();

  enum Headers {
    CARD, DATE, TIME, CONCEPT, COMMERCE, VALUE, STATUS
  }

  @Override
  public boolean supports(FinancialInstitution fInstitution) {
    return fInstitution == FinancialInstitution.PEOPLEPASS;
  }

  @Override
  public List<OriginRecord> read(File inputFile) throws IOException {
    Log.infov("Reading file {0} for {1}", inputFile, FinancialInstitution.PEOPLEPASS);
    try {
      return read(new FileInputStream(inputFile));
    } catch (IOException e) {
      Log.errorv(e, "Error reading file {0}", inputFile);
      throw e;
    }
  }

  public List<OriginRecord> read(InputStream inputStream) throws IOException {
    return parseCvsFile(inputStream)
        .stream()
        .map(this::toRecord)
        .toList();
  }

  private List<CSVRecord> parseCvsFile(InputStream in) throws IOException {
    try (Reader reader = new InputStreamReader(in)) {
      return CSVFormat.Builder.create(TDF)
          .setHeader(Headers.class)
          .setSkipHeaderRecord(true)
          .setAllowMissingColumnNames(true)
          .build()
          .parse(reader)
          .getRecords();
    }
  }

  private OriginRecord toRecord(CSVRecord csvRecord) {
    return new OriginRecord(
        parseDate(csvRecord.get(Headers.DATE)),
        csvRecord.get(Headers.COMMERCE),
        parseAmount(csvRecord.get(Headers.VALUE)),
        FinancialInstitution.PEOPLEPASS.accountName());
  }

  private static Double parseAmount(String amount) {
    try {
      return -NUMBER_FORMAT.parse(amount).doubleValue();
    } catch (ParseException e) {
      Log.errorv(e, "Error parsing amount {0}", amount);
      throw new IllegalArgumentException("Error parsing amount " + amount);
    }
  }

  public static DecimalFormat buildDecimalFormat() {
    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    symbols.setDecimalSeparator(',');
    symbols.setGroupingSeparator('.');
    symbols.setCurrencySymbol("$ ");
    DecimalFormat decimalFormat = new DecimalFormat("$ #,##0.00", symbols);
    decimalFormat.setParseBigDecimal(true);
    return decimalFormat;
  }


  private static LocalDate parseDate(String date) {
    return LocalDate.parse(date, DATE_TIME_FORMATTER);
  }
}

@ApplicationScoped
class RecordReaderFactory {

  @Any
  @Inject
  Instance<RecordReader> recordReaders;

  public RecordReader readerFor(FinancialInstitution fInstitution) {
    return recordReaders.stream()
        .filter(reader -> reader.supports(fInstitution))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No reader found for " + fInstitution));
  }
}

@ApplicationScoped
class DefaultRecordCSVWriter implements RecordCSVWriter {

  @Override
  public void write(List<TargetRecord> records, Writer writer) throws IOException {
    try (CSVPrinter printer = new CSVPrinter(writer, TDF)) {
      for (TargetRecord record : records) {
        printer.printRecord(
            record.date(),
            record.description(),
            record.amount(),
            record.account()
        );
      }
    }
  }
}

// -- ------------------------------------------------------------------------------------------------------------------
// -- Ports
// -- ------------------------------------------------------------------------------------------------------------------
interface TransformationService {

  <T> void transform(File inputFile, FinancialInstitution fInstitution, TransformationOptions options);
}

interface RecordReader {

  boolean supports(FinancialInstitution fInstitution);

  List<OriginRecord> read(File inputFile) throws IOException;
}

interface RecordCSVWriter {

  void write(List<TargetRecord> records, Writer writer) throws IOException;
}

// -- ------------------------------------------------------------------------------------------------------------------
// -- Domain
// -- ------------------------------------------------------------------------------------------------------------------
enum FinancialInstitution {
  BANCOLOMBIA("Bancolombia"),
  PEOPLEPASS("Peoplepass");

  private final String accountName;

  FinancialInstitution(String name) {
    this.accountName = name;
  }

  public String accountName() {
    return accountName;
  }
}

record TargetRecord(
    String date,
    String description,
    String amount,
    String account
) {

}

record OriginRecord(
    LocalDate date,
    String description,
    Double amount,
    String source
) {

  public static OriginRecord of(int year,
      int month,
      int dayOfMonth,
      String description,
      double amount,
      String source) {
    return new OriginRecord(LocalDate.of(year, month, dayOfMonth), description, amount, source);
  }

}

record TransformationOptions(
    boolean toClipboard
) {

}

@ApplicationScoped
class RecordMapper {

  public TargetRecord map(OriginRecord originRecord) {
    return new TargetRecord(
        formatDate(originRecord.date()),
        originRecord.description(),
        formatAmount(originRecord.amount()),
        "Bancolombia"
    );
  }

  private String formatAmount(Number amount) {
    return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
  }

  private String formatDate(LocalDate date) {
    return date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
  }

}

// -- ------------------------------------------------------------------------------------------------------------------
// -- Application: Use Cases
// -- ------------------------------------------------------------------------------------------------------------------
@ApplicationScoped
class TransformationServiceImpl implements TransformationService {

  private static final Logger LOGGER = Logger.getLogger(TransformationServiceImpl.class);

  @Inject
  RecordReaderFactory recordReaderFactory;

  @Inject
  RecordMapper mapper;

  @Inject
  RecordCSVWriter recordCSVWriter;

  @Inject
  ClipboardService clipboardService;

  @Override
  public <T> void transform(File inputFile, FinancialInstitution fInstitution, TransformationOptions options) {
    LOGGER.infov("Transforming {0} file {1}", fInstitution, inputFile);
    try {
      List<TargetRecord> targetRecords = toTargetRecords(inputFile, fInstitution);
      StringWriter writer = new StringWriter();
      recordCSVWriter.write(targetRecords, writer);
      System.out.println(writer);

      if (options.toClipboard()) {
        clipboardService.setContent(writer.toString());
      }

    } catch (Exception e) {
      LOGGER.errorv(e, "Error transforming file {0}", inputFile);
    }
  }

  private <T> List<TargetRecord> toTargetRecords(File inputFile, FinancialInstitution fInstitution) throws IOException {
    RecordReader reader = recordReaderFactory.readerFor(fInstitution);
    List<OriginRecord> records = reader.read(inputFile);
    return records.stream()
        .map(mapper::map)
        .toList();
  }
}

@ApplicationScoped
class ClipboardService {

  public void setContent(String data) {
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    StringSelection selection = new StringSelection(data);
    clipboard.setContents(selection, null);
  }
}
