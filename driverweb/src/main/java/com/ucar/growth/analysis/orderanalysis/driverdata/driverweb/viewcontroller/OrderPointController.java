package com.ucar.growth.analysis.orderanalysis.driverdata.driverweb.viewcontroller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by zfx on 2016/8/29.
 */
@RequestMapping("/invalidorder")
@Controller
public class OrderPointController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = "/{orderNo}/show",method = RequestMethod.GET)

    public String get(@PathVariable String orderNo,Model model) {
        model.addAttribute("orderno",orderNo);
        return "point";
    }
}
