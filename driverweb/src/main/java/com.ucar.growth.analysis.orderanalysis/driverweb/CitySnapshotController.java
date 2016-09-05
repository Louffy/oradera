package com.ucar.growth.analysis.orderanalysis.driverweb;

import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverPosition;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverSnapshot;
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
@RequestMapping("/citysnapshot")
@RestController
public class CitySnapshotController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @RequestMapping(value = "/{timeStamp}",method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public DriverSnapshot[] get(@PathVariable String timeStamp) {
        logger.error(timeStamp);
        DriverSnapshot[] dsl = DriverQuery.getSnapshotList(timeStamp);
        logger.error(dsl.toString());
        return dsl;
    }
}
