package com.ws.backend.tools;


import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PciMaskingConverter extends CompositeConverter<ILoggingEvent> {

    // Regex za kartice (13-19 cifara) i CVV/Lozinke
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:\\d{4}[ -]?){3,4}\\d{1,4}\\b");
    private static final Pattern CVV_PATTERN = Pattern.compile("(?i)(cvv|cvc|securityCode|password|merchantPassword)[\"']?\\s*[:=]\\s*[\"']?(\\w+?)[\"']?(?=\\s|,|}|\\b)");

    @Override
    protected String transform(ILoggingEvent event, String in) {
        if (in == null || in.isEmpty()) {
            return in;
        }

        String message = in;

        // 1. Maskiranje CVV, lozinki i merchantPassword-a
        Matcher cvvMatcher = CVV_PATTERN.matcher(message);
        if (cvvMatcher.find()) {
            message = cvvMatcher.replaceAll("$1: ***");
        }

        // 2. Maskiranje kartice (Prvih 6 i zadnja 4)
        Matcher cardMatcher = CARD_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;
        while (cardMatcher.find()) {
            sb.append(message, lastIndex, cardMatcher.start());
            String card = cardMatcher.group().replaceAll("[ -]", "");
            if (card.length() >= 13) {
                sb.append(card.substring(0, 6)).append("******").append(card.substring(card.length() - 4));
            } else {
                sb.append("****");
            }
            lastIndex = cardMatcher.end();
        }
        sb.append(message.substring(lastIndex));

        return sb.toString();
    }
}
