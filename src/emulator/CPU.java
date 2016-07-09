package emulator;


import util.CpuUtil;

class CPU {

    private long cycleNumber;
    private short PC;
    private short SP;
    private byte[] reg = new byte[16];
    private byte[] mem = new byte[4096];
    private byte[] vMem = new byte[64 * 32];

    CPU(byte[] romData) {
        System.arraycopy(romData, 0, mem, 0x200, romData.length);
        PC = 0x200;
        cycleNumber = 0;
    }

    void executeCycle() throws Exception {
        cycleNumber++;
        short opCode = CpuUtil.shortFromBytes(mem[PC], mem[PC + 1]);
        PC += 2;
        System.out.println("Current opcode is " + String.format("%04X", opCode) + " (cycle " + cycleNumber + ")");
    }

    public byte[] getvMem() {
        return vMem;
    }
}
