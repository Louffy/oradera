package com.ucar.growth.analysis.orderanalysis.driverweb.viewcontroller;
import com.google.gson.Gson;
import com.ucar.growth.analysis.orderanalysis.driverdata.Beans.DriverSnapshot;
import com.ucar.growth.analysis.orderanalysis.driverquery.DriverQuery;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by zfx on 2016/8/25.
 */
@RequestMapping("/driversnapshot/{driverId}/{timeStamp}")
@Controller
public class ViewDriverSnapshotController {
    @RequestMapping(value="/show", method=RequestMethod.GET)
    public String show(@PathVariable String driverId,
            @PathVariable String timeStamp, Model model) {
        DriverSnapshot ds = DriverQuery.getSnapshot(
                driverId,timeStamp);
        String jsonDriverSnapshot = new Gson().toJson(ds);
        System.out.println(jsonDriverSnapshot);
        model.addAttribute("driver", jsonDriverSnapshot);
        return "showDriverSnapshot";
    }
}
