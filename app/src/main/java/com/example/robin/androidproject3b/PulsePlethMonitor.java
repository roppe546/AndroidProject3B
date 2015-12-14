package com.example.robin.androidproject3b;

/**
 * Created by robin on 14/12/15.
 */
public class PulsePlethMonitor {
    private int pulse;
    private int pleth;
    private int msb;

    public PulsePlethMonitor() {
        this.pulse = 0;
        this.pleth = 0;
        this.msb = 0;
    }

    public PulsePlethMonitor(int pulse, int pleth, int msb) {
        this.pulse = pulse;
        this.pleth = pleth;
        this.msb = msb;
    }

    public int getPulse() {
        return pulse;
    }

    public void setPulse(int pulse) {
        this.pulse = pulse;
    }

    public int getPleth() {
        return pleth;
    }

    public void setPleth(int pleth) {
        this.pleth = pleth;
    }

    public int getMsb() {
        return msb;
    }

    public void setMsb(int msb) {
        this.msb = msb;
    }
}
