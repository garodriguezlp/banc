///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+

//DEPS org.junit.jupiter:junit-jupiter-api:5.7.2
//DEPS org.junit.jupiter:junit-jupiter-engine:5.7.2
//DEPS org.junit.platform:junit-platform-launcher:1.7.2
//DEPS org.assertj:assertj-core:3.24.2
//DEPS commons-io:commons-io:2.12.0
//DEPS org.mockito:mockito-core:3.11.2
//DEPS org.mockito:mockito-junit-jupiter:5.3.1
//DEPS io.quarkus:quarkus-junit5

//SOURCES banc.java

//FILES bancolombia.csv
//FILES peoplepass.csv

import static java.lang.System.out;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.LoggingListener;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

public class bancTest {

  // Run all Unit tests with JBang with ./banc.java
  public static void main(final String... args) {
    final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder
        .request()
        .selectors(selectClass(bancTest.class))
        .build();
    final Launcher launcher = LauncherFactory.create();
    final LoggingListener logListener = LoggingListener.forBiConsumer((t, m) -> {
      out.println(m.get());
      if (t != null) {
        t.printStackTrace();
      }
      ;
    });
    final SummaryGeneratingListener execListener = new SummaryGeneratingListener();
    launcher.registerTestExecutionListeners(execListener, logListener);
    launcher.execute(request);
    execListener.getSummary().printTo(new java.io.PrintWriter(out));
  }

  @Nested
  class BancolombiaReaderTest {

    @Test
    void testRead() throws IOException {
      final var stream = getClass().getResourceAsStream("bancolombia.csv");
      final var records = new BancolombiaReader().read(stream);

      assertThat(records)
          .usingRecursiveComparison()
          .isEqualTo(List.of(
              OriginRecord.of(2021, 1, 16, "COMPRA PTO.VTA", -327900.00, "Bancolombia"),
              OriginRecord.of(2021, 1, 15, "ABONO INTERESES AHORROS", 2.09, "Bancolombia"),
              OriginRecord.of(2021, 1, 14, "COMPRA INTL  PAYPAL  NY TIMES", -3652.27, "Bancolombia"),
              OriginRecord.of(2021, 1, 14, "ABONO INTERESES AHORROS", 2.1, "Bancolombia"),
              OriginRecord.of(2021, 1, 12, "PAGO PSE PAYU COLOMBIA S.A.S", -42500.00, "Bancolombia"),
              OriginRecord.of(2021, 1, 11, "COMPRA EN  RAPPI PAY", -25000.00, "Bancolombia")));
    }
  }

  @Nested
  class PeoplepassReaderTest {

    @Test
    void testRead() throws IOException {
      final var stream = getClass().getResourceAsStream("peoplepass.csv");
      final var records = new PeoplepassReader().read(stream);

      assertThat(records)
          .usingRecursiveComparison()
          .isEqualTo(List.of(
              OriginRecord.of(2023, 6, 2, "TIENDA D1 CALI DARK ST", -70330.00, "Peoplepass"),
              OriginRecord.of(2023, 6, 2, "SUPERTIENDAS CANAVERAL", -379991.00, "Peoplepass"),
              OriginRecord.of(2023, 5, 28, "ALIMENTOS ARBEL", -75350.00, "Peoplepass"),
              OriginRecord.of(2023, 5, 28, "GOMEZ RAMIREZ OLGA LUZ", -34700.00, "Peoplepass"),
              OriginRecord.of(2023, 5, 27, "LA TRATTORINA 1", -135371.00, "Peoplepass")
          ));
    }
  }

  @Nested
  class OriginRecordMapperTest {

    @Test
    void testMap() {
      OriginRecord originRecord = OriginRecord.of(2021,
          1,
          16,
          "COMPRA PTO.VTA",
          -327900.00,
          "Bancolombia");
      final var targetRecord = new RecordMapper().map(originRecord);

      assertThat(targetRecord)
          .extracting(TargetRecord::date, TargetRecord::description, TargetRecord::amount, TargetRecord::account)
          .contains("01/16/2021", "COMPRA PTO.VTA", "-$327,900.00", "Bancolombia");
    }
  }

  @Nested
  class DefaultRecordCSVWriterTest {

    @Test
    void testWrite() throws IOException {
      final var targetRecord = new TargetRecord(
          "01/16/2021",
          "COMPRA PTO.VTA",
          "-$327,900.00",
          "Bancolombia"
      );
      final var stringWriter = new java.io.StringWriter();
      new DefaultRecordCSVWriter().write(singletonList(targetRecord), stringWriter);
      assertThat(stringWriter.toString())
          .contains("01/16/2021\tCOMPRA PTO.VTA\t-$327,900.00\tBancolombia");
    }

  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class TransformationServiceImplTest {

    @Mock
    private RecordReaderFactory recordReaderFactory;

    @Mock
    private RecordReader reader;

    @Mock
    private RecordMapper mapper;

    @Mock
    private RecordCSVWriter recordCSVWriter;

    @Mock
    private ClipboardService clipboardService;

    @InjectMocks
    private TransformationServiceImpl transformationService;

    @Test
    void testTransform() throws IOException {
      File inputFile = new File("input.csv");
      OriginRecord originRecord = OriginRecord.of(2021, 1, 16, "COMPRA PTO.VTA", -327900.00, "Bancolombia");
      List<OriginRecord> records = List.of(originRecord);
      TargetRecord targetRecord = new TargetRecord(
          "01/16/2021",
          "COMPRA PTO.VTA",
          "-$327,900.00",
          "Bancolombia"
      );

      when(recordReaderFactory.readerFor(FinancialInstitution.BANCOLOMBIA)).thenReturn(reader);
      when(reader.read(inputFile)).thenReturn(records);
      when(mapper.map(originRecord)).thenReturn(targetRecord);

      // Call the method to be tested
      transformationService.transform(
          inputFile,
          FinancialInstitution.BANCOLOMBIA,
          new TransformationOptions(true));

      verify(recordCSVWriter).write(eq(List.of(targetRecord)), any());
      verify(clipboardService).setContent(any());
    }

  }
}
