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
        if(PC + 1 > mem.length-1){
            throw new RuntimeException("PC outside memory range");
        }
        short opCodeShort = CpuUtil.shortFromBytes(mem[PC], mem[PC + 1]);
        System.out.println("Current opcode is " + String.format("%04X", opCodeShort) + " (cycle " + cycleNumber + ")");
        byte[] opCodeNibbles = CpuUtil.nibblesFromShort(opCodeShort);

        switch(opCodeNibbles[0]) {
            case 0x6:
                break;
            default:
                throw new RuntimeException("Unknown OpCode " + String.format("%04X", opCodeShort));
        }

        PC += 2;
    }

    public long getCycleNumber() {
        return cycleNumber;
    }

    public short getPC(){
        return PC;
    }

    public byte[] getvMem() {
        return vMem;
    }

    public boolean isDrawFlag() {
        return drawFlag;
    }
}
