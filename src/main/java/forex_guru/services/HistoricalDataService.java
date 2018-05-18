package forex_guru.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.ta4j.core.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
public class HistoricalDataService {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    RestTemplate restTemplate;

    /**
     * Gets a daily TimeSeries for the last 365 business days
     * @param symbol the currency pair
     * @return a TimeSeries containing all available daily data from Kibot API
     */
    public TimeSeries getDailySeries(String symbol) {

        // range for last 365 days
        long enddate = System.currentTimeMillis() / 1000;
        long startdate = enddate - 31536000;

        // convert dates to Kibot formatting (MM/DD/YYYY)
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
        String start = df.format(new Date(startdate * 1000)).toString();
        String end = df.format(new Date(enddate * 1000)).toString();

        // build query
        StringBuilder queryBuilder = new StringBuilder("http://api.kibot.com/?action=history");
        queryBuilder.append("&user=guest");
        queryBuilder.append("&password=guest");
        queryBuilder.append("&type=forex");
        queryBuilder.append("&symbol=" + symbol);
        queryBuilder.append("&interval=daily");
        queryBuilder.append("&startdate=" + start);
        queryBuilder.append("&enddate=" + end);
        String query =  queryBuilder.toString();

        // make API call
        String response = null;

        try {
            ResponseEntity<String> fullResponse = restTemplate.exchange(query, HttpMethod.GET, new HttpEntity(new HttpHeaders()), String.class);
            response = fullResponse.getBody();
        }
        // catch bad API call
        catch (HttpClientErrorException ex) {
            logger.error("bad external api request");
        }

        // map response to ExchangeRate Objects
        return buildTimeSeries(response, symbol);
    }

    /**
     * Maps String response data to a TimeSeries containing Bars
     * @param rates the Kibot Response
     * @param symbol the currency pair
     * @return a TimeSeries of the given data
     */
    private TimeSeries buildTimeSeries(String rates, String symbol) {

        TimeSeries series = new BaseTimeSeries(symbol);

        // read response
        try (BufferedReader reader = new BufferedReader(new StringReader(rates))) {
            String line;

            DateTimeFormatter df = DateTimeFormatter.ofPattern("MM/dd/yyyy");

            // read lines
            while ((line = reader.readLine()) != null) {

                String[] data = line.split(",");

                // parse timestamp
                LocalDate local = LocalDate.parse(data[0], df);
                long epoch = local.atStartOfDay(ZoneId.of("GMT")).toInstant().getEpochSecond();
                Instant temp = Instant.ofEpochSecond(epoch);
                ZonedDateTime date = ZonedDateTime.ofInstant(temp, ZoneOffset.UTC);

                // populate Bar Object
                Decimal open = Decimal.valueOf(data[1]);
                Decimal high = Decimal.valueOf(data[2]);
                Decimal low = Decimal.valueOf(data[3]);
                Decimal close = Decimal.valueOf(data[4]);
                Decimal volume = Decimal.valueOf(data[5]);

                // create a bar for rate data (date, open, high, low, close, volume)
                Bar bar = new BaseBar(date, open, high, low, close, volume);

                // add bar to series
                series.addBar(bar);
            }

        } catch (IOException ex) {
            logger.error("could not map response");
            return null;
        }

        return series;
    }

}
