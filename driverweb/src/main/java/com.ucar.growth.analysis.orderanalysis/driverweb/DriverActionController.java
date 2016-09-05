package com.ucar.growth.analysis.orderanalysis.driverweb;

import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverAction;
import com.ucar.growth.analysis.orderanalysis.driverquery.DB;
import com.ucar.growth.analysis.orderanalysis.driverquery.DriverQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by zfx on 2016/8/24.
 */
@RequestMapping("/driveraction/{driverId}")
@RestController
public class DriverActionController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @RequestMapping(value = "/{timeStamp}",method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public DriverAction[] getOne(@PathVariable String driverId,
                              @PathVariable String timeStamp) {
        logger.error(driverId + "_" + timeStamp);
        DriverAction[] da = DB.getInstance().getAction(driverId,timeStamp);
        logger.error(da.toString());
        return da;
    }

    @RequestMapping(value = "/list",method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public DriverAction[] getAll(@PathVariable String driverId
                              ) {
        logger.error(driverId + "_");
        DriverAction[] da = DriverQuery.getDriverAction(driverId);
        logger.error(da.toString());
        return da;
    }
}
