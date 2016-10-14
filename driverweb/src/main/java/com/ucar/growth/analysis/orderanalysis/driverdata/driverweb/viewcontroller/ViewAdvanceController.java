package com.ucar.growth.analysis.orderanalysis.driverdata.driverweb.viewcontroller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by zfx on 2016/9/6.
 */
@RequestMapping("/advance/show")
@Controller
public class ViewAdvanceController {
    @RequestMapping(method= RequestMethod.GET)
    public String show() {

        return "advance";
    }
}
