package com.ucar.growth.analysis.orderanalysis.driverweb;
import org.slf4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverPosition;
import com.ucar.growth.analysis.orderanalysis.driverquery.DB;
import org.slf4j.LoggerFactory;

@RequestMapping("/driverposition/{driverId}")
@RestController
public class DriverPositionController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @RequestMapping(value = "/{timeStamp}",method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public DriverPosition get(@PathVariable String driverId,
                              @PathVariable String timeStamp) {
        logger.error(driverId + "_" + timeStamp);
        DriverPosition dp = DB.getInstance().getLocation(driverId,timeStamp);
        logger.error(dp.toString());
        return dp;
    }
}