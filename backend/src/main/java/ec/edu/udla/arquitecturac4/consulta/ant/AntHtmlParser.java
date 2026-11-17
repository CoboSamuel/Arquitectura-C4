package ec.edu.udla.arquitecturac4.consulta.ant;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
public class AntHtmlParser {

    private static final Pattern SUMMARY_LABEL = Pattern.compile("(?i)\\bPuntos\\s*:");
    private static final Pattern POINTS_VALUE = Pattern.compile(
            "(?i)\\bPuntos\\s*:\\s*([0-9]+(?:[,.][0-9]+)?)(?![0-9.,])\\b");
    private static final String NO_RECORDS_MARKER = "no se encontraron registros";

    public Optional<BigDecimal> parse(String html) {
        if (html == null || html.isBlank()) {
            throw new AntRespuestaInvalidaException();
        }

        String text = Jsoup.parse(html).text().replaceAll("\\s+", " ").trim();
        String normalized = text.toLowerCase(Locale.ROOT);
        boolean noRecords = normalized.equals(NO_RECORDS_MARKER);

        Matcher labels = SUMMARY_LABEL.matcher(text);
        int labelCount = 0;
        while (labels.find()) {
            labelCount++;
        }
        if (noRecords && labelCount == 0) {
            return Optional.empty();
        }
        if (noRecords || labelCount != 1) {
            throw new AntRespuestaInvalidaException();
        }

        Matcher valueMatcher = POINTS_VALUE.matcher(text);
        if (!valueMatcher.find()) {
            throw new AntRespuestaInvalidaException();
        }
        String value = valueMatcher.group(1).replace(',', '.');
        if (valueMatcher.find()) {
            throw new AntRespuestaInvalidaException();
        }

        try {
            BigDecimal points = new BigDecimal(value);
            if (points.signum() < 0) {
                throw new AntRespuestaInvalidaException();
            }
            return Optional.of(points.stripTrailingZeros());
        } catch (NumberFormatException exception) {
            throw new AntRespuestaInvalidaException();
        }
    }
}
