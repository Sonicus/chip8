package emulator;


import util.CpuUtil;

import java.util.Stack;

class CPU {

    private long cycleNumber;
    private short PC;
    private Stack<Short> SP;
    private byte[] reg = new byte[16];
    private byte[] mem = new byte[4096];
    private byte[] vMem = new byte[64 * 32];
    private boolean drawFlag;

    CPU(byte[] romData) {
        System.arraycopy(romData, 0, mem, 0x200, romData.length);
        PC = 0x200;
        cycleNumber = 0;
        drawFlag = false;
    }

    void executeCycle() throws Exception {
        cycleNumber++;
        if (PC + 1 > mem.length - 1) {
            throw new RuntimeException("PC outside memory range");
        }
        short opcodeShort = CpuUtil.shortFromBytes(mem[PC], mem[PC + 1]);
        System.out.println("Current opcode is " + String.format("%04X", opcodeShort) + " (cycle " + cycleNumber + ")");
        byte[] opcodeNibbles = CpuUtil.nibblesFromShort(opcodeShort);

        switch (opcodeNibbles[0]) {
            case 0x6:
                LD(opcodeNibbles[1], CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3]));
                break;
            default:
                throw new RuntimeException("Unknown opcode " + String.format("%04X", opcodeShort));
        }

        PC += 2;
    }

    //6XNN
    private void LD(byte targetRegister, byte value) {
        reg[targetRegister] = value;
    }

    long getCycleNumber() {
        return cycleNumber;
    }

    short getPC() {
        return PC;
    }

    public byte[] getvMem() {
        return vMem;
    }

    public boolean isDrawFlag() {
        return drawFlag;
    }
}
