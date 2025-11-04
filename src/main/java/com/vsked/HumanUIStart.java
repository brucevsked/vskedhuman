package com.vsked;

import com.vsked.human.HumanHead;
import com.vsked.human.HumanID;
import com.vsked.human.HumanLowLimbs;
import com.vsked.human.HumanName;
import com.vsked.human.HumanNeck;
import com.vsked.human.HumanUI;
import com.vsked.human.HumanUpperLimbs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HumanUIStart {

    private static final Logger log = LoggerFactory.getLogger(HumanUIStart.class);

    public static void main(String[] args) {

        HumanID humanID = new HumanID("37010531");
        HumanName humanName = new HumanName("bruce vsked");

        HumanHead humanHead = new HumanHead();
        HumanNeck humanNeck = new HumanNeck();
        HumanUpperLimbs humanUpperLimbs = new HumanUpperLimbs();
        HumanLowLimbs humanLowLimbs = new HumanLowLimbs();


        HumanUI humanUI = new HumanUI(humanID,  humanName,  humanHead, humanNeck, humanUpperLimbs, humanLowLimbs);

        log.info("{}", humanUI.getId().getId());
        log.info("{}", humanUI.getName().getName());
    }
}
