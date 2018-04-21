package forex_guru.controllers;

import forex_guru.exceptions.OandaException;
import forex_guru.model.oanda.OandaResponse;
import forex_guru.services.GuruService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class guruController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    GuruService guruService;

    @GetMapping("/prices")
    public OandaResponse getPrices() throws OandaException {
        logger.info("API Call: /prices");
        return guruService.getPrices();
    }

}