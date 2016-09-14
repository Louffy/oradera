package com.ucar.growth.analysis.orderanalysis.driverweb;

import com.ucar.growth.analysis.orderanalysis.driverquery.OrderAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

/**
 * Created by zfx on 2016/9/6.
 */
@RequestMapping("/advance")
@RestController
public class AdvanceCountController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @RequestMapping(method = RequestMethod.GET,produces = {MediaType.APPLICATION_JSON_VALUE})
    public HashMap<String,Double[][]> get() {

        return OrderAnalysis.read("result/count0801");
    }
}
