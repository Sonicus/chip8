package emulator;


import util.CpuUtil;

import java.util.Stack;

class CPU {

    private long cycleNumber;
    private short PC;
    private Stack<Short> STACK;
    private short I;
    private byte[] reg = new byte[16];
    private byte[] mem = new byte[4096];
    private int[][] vMem = new int[64][32];
    private boolean drawFlag;

    CPU(byte[] romData) {
        System.arraycopy(romData, 0, mem, 0x200, romData.length);
        initializeFont();
        PC = 0x200;
        I = 0;
        STACK = new Stack<>();
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
            case 0x2:
                CALL(CpuUtil.addressFromNibbles(opcodeNibbles[1], opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0x6:
                LD(opcodeNibbles[1], CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0x7:
                ADDN(opcodeNibbles[1], CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0xA:
                LDI(CpuUtil.addressFromNibbles(opcodeNibbles[1], opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0xD:
                DRW(opcodeNibbles[1], opcodeNibbles[2], opcodeNibbles[3]);
                break;
            case 0xF:
                switch (opcodeNibbles[2]) {
                    case 0x3:
                        LDB(opcodeNibbles[1]);
                        break;
                }
                break;
            default:
                throw new RuntimeException("Unknown opcode " + String.format("%04X", opcodeShort));
        }

        PC += 2;
    }

    //2NNN
    private void CALL(short address) {
        STACK.push(PC);
        PC = address;
    }

    //6XNN
    private void LD(byte targetRegister, byte value) {
        reg[targetRegister] = value;
    }

    //7XNN
    private void ADDN(byte targetRegister, byte value) {
        mem[targetRegister] = (byte) ((mem[targetRegister] + value) % 256);
    }

    //ANNN
    private void LDI(short address) {
        I = address;
    }

    //DXYN
    private void DRW(byte x, byte y, byte n) {
        byte collisionFlag = 0x00;
        for (int j = 0; j < n; j++) {
            int screenYPos = (j + y) % 32;
            for (int i = 0; i < 8; i++) {
                int screenXPos = (x + i) % 64;
                if (vMem[screenXPos][screenYPos] == 1 && ((mem[I + j] >> (7 - i)) & 0b00000001) == 1) {
                    collisionFlag = 0x01;
                }
                vMem[screenXPos][screenYPos] = vMem[screenXPos][screenYPos] ^ (mem[I + j] >> i & 1);
            }
        }
        reg[0xF] = collisionFlag;
        drawFlag = true;
    }

    private void LDB(byte sourceRegister) {
        if (I + 2 > mem.length) {
            throw new RuntimeException("I value outside of memory range");
        }
        mem[I] = (byte) (reg[sourceRegister] / 100);
        mem[I + 1] = (byte) ((reg[sourceRegister] % 100) / 10);
        mem[I + 2] = (byte) (reg[sourceRegister] % 10);
    }

    private void initializeFont() {
        byte[] font = {
                (byte) 0xF0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xF0, //0
                (byte) 0x20, (byte) 0x60, (byte) 0x20, (byte) 0x20, (byte) 0x70, //1
                (byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, //2
                (byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, //3
                (byte) 0x90, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0x10, //4
                (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, //5
                (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, //6
                (byte) 0xF0, (byte) 0x10, (byte) 0x20, (byte) 0x40, (byte) 0x40, //7
                (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, //8
                (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, //9
                (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0x90, //A
                (byte) 0xE0, (byte) 0x90, (byte) 0xE0, (byte) 0x90, (byte) 0xE0, //B
                (byte) 0xF0, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0xF0, //C
                (byte) 0xE0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xE0, //D
                (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, //E
                (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0x80  //F
        };
        System.arraycopy(font, 0, mem, 0, font.length);
    }

    long getCycleNumber() {
        return cycleNumber;
    }

    short getPC() {
        return PC;
    }

    public int[][] getvMem() {
        return vMem;
    }

    public boolean isDrawFlag() {
        return drawFlag;
    }
}
