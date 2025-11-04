package com.vsked.human;

public class HumanUI {

    private HumanID id;
    private HumanName name;
    private HumanHead head;
    private HumanNeck neck;
    private HumanUpperLimbs upperLimbs;
    private HumanLowLimbs lowLimbs;

    public HumanUI(HumanID id, HumanName name, HumanHead head, HumanNeck neck, HumanUpperLimbs upperLimbs, HumanLowLimbs lowLimbs) {
        this.id = id;
        this.name = name;
        this.head = head;
        this.neck = neck;
        this.upperLimbs = upperLimbs;
        this.lowLimbs = lowLimbs;
    }

    public HumanID getId() {
        return id;
    }

    public HumanName getName() {
        return name;
    }

    public HumanHead getHead() {
        return head;
    }

    public HumanNeck getNeck() {
        return neck;
    }

    public HumanUpperLimbs getUpperLimbs() {
        return upperLimbs;
    }

    public HumanLowLimbs getLowLimbs() {
        return lowLimbs;
    }


}
