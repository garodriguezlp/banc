///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+

//DEPS org.junit.jupiter:junit-jupiter-api:5.7.2
//DEPS org.junit.jupiter:junit-jupiter-engine:5.7.2
//DEPS org.junit.platform:junit-platform-launcher:1.7.2
//DEPS org.assertj:assertj-core:3.24.2
//DEPS commons-io:commons-io:2.12.0

//SOURCES banc.java
//FILES bancolombia.csv

import static java.lang.System.out;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.IOException;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.LoggingListener;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

public class bancTest {

  @Nested
  class BancolombiaReaderTest {

    @Test
    void testRead() throws IOException {
      final var stream = getClass().getResourceAsStream("bancolombia.csv");
      final var records = new BancolombiaReader().read(stream);

      assertThat(records)
          .hasSize(6)
          .extracting(BancolombiaRecord::date, BancolombiaRecord::description, BancolombiaRecord::amount)
          .contains(
              tuple(LocalDate.of(2021, 1, 16), "COMPRA PTO.VTA", -327900L),
              tuple(LocalDate.of(2021, 1, 15), "ABONO INTERESES AHORROS", 2.09),
              tuple(LocalDate.of(2021, 1, 14), "COMPRA INTL  PAYPAL  NY TIMES", -3652.27),
              tuple(LocalDate.of(2021, 1, 14), "ABONO INTERESES AHORROS", 2.1),
              tuple(LocalDate.of(2021, 1, 12), "PAGO PSE PAYU COLOMBIA S.A.S", -42500L),
              tuple(LocalDate.of(2021, 1, 11), "COMPRA EN  RAPPI PAY", -25000L));
    }
  }

  // Run all Unit tests with JBang with ./banc.java
  public static void main(final String... args) {
    final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder
        .request()
        .selectors(selectClass(bancTest.class))
        .build();
    final Launcher launcher = LauncherFactory.create();
    final LoggingListener logListener = LoggingListener.forBiConsumer((t, m) -> {
      System.out.println(m.get());
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
}
