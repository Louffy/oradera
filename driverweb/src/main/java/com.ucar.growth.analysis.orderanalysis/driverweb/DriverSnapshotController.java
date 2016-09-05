package com.ucar.growth.analysis.orderanalysis.driverweb;

import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverSnapshot;
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
@RequestMapping("/driversnapshot/{driverId}")
@RestController
public class DriverSnapshotController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @RequestMapping(value = "/{timeStamp}",method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public DriverSnapshot get(@PathVariable String driverId,
                              @PathVariable String timeStamp) {
        logger.error(driverId + "_" + timeStamp);
        DriverSnapshot ds = DriverQuery.getSnapshot(
                driverId,timeStamp);
        logger.error(ds.toString());
        return ds;
    }
}
