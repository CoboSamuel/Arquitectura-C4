package ec.edu.udla.arquitecturac4.consulta.ant;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AntHtmlParserTest {

    private final AntHtmlParser parser = new AntHtmlParser();

    @Test
    void parsesSyntheticPositivePointsSummary() {
        assertEquals(Optional.of(new BigDecimal("25")), parser.parse("""
                <html><body><div>Consulta de licencia</div><p>Puntos: 25</p></body></html>
                """));
    }

    @Test
    void zeroPointsIsAValidSummary() {
        assertEquals(Optional.of(BigDecimal.ZERO), parser.parse("<p>Puntos: 0</p>"));
    }

    @Test
    void acceptsCommaAndDotDecimalSeparators() {
        assertEquals(Optional.of(new BigDecimal("19.5")), parser.parse("<p>Puntos: 19,5</p>"));
        assertEquals(Optional.of(new BigDecimal("19.5")), parser.parse("<p>Puntos: 19.5</p>"));
    }

    @Test
    void exactNoRecordsMarkerReturnsEmpty() {
        assertEquals(Optional.empty(), parser.parse("<p>NO SE ENCONTRARON REGISTROS</p>"));
    }

    @Test
    void noRecordsPhraseWithAdditionalTextIsNotTreatedAsConfirmedAbsence() {
        assertThrows(AntRespuestaInvalidaException.class,
                () -> parser.parse("<p>No se encontraron registros para esta consulta</p>"));
    }

    @Test
    void missingSummaryIsInvalidRatherThanNoRecords() {
        assertThrows(AntRespuestaInvalidaException.class,
                () -> parser.parse("<html><body>Bienvenido al portal</body></html>"));
    }

    @Test
    void challengePageIsInvalid() {
        assertThrows(AntRespuestaInvalidaException.class,
                () -> parser.parse("<title>Verificación</title><p>Confirme que no es un robot</p>"));
    }

    @Test
    void multipleSummaryValuesAreAmbiguousAndInvalid() {
        assertThrows(AntRespuestaInvalidaException.class,
                () -> parser.parse("<p>Puntos: 25</p><p>Puntos: 20</p>"));
    }

    @Test
    void negativeOrMalformedPointsAreInvalid() {
        assertThrows(AntRespuestaInvalidaException.class, () -> parser.parse("<p>Puntos: -1</p>"));
        assertThrows(AntRespuestaInvalidaException.class, () -> parser.parse("<p>Puntos: muchos</p>"));
        assertThrows(AntRespuestaInvalidaException.class, () -> parser.parse("<p>Puntos: 19,5.6</p>"));
    }
}
