package com.ucar.growth.analysis.orderanalysis.driverweb;

import com.google.gson.Gson;
import com.sun.xml.internal.bind.v2.runtime.reflect.Accessor;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.CitySnapshot;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverSnapshot;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.OrderSnapshot;
import com.ucar.growth.analysis.orderanalysis.driverquery.DriverQuery;
import com.ucar.growth.analysis.orderanalysis.driverquery.OrderAnalysis;
import com.ucar.growth.analysis.orderanalysis.driverquery.OrderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by zfx on 2016/8/25.
 */
@RequestMapping("/invalidorder")
@RestController
public class OrderContextController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @RequestMapping(value = "/{orderNo}",method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public OrderContext get(@PathVariable String orderNo) {

        OrderSnapshot orderSnapshot = DriverQuery.getOrderSnapshot(orderNo,"0");
        OrderContext orderContext = OrderAnalysis.initOC(orderSnapshot);
        return orderContext;
    }
}
