package emulator;


import util.CpuUtil;

import java.util.Stack;
import java.util.stream.IntStream;

class CPU {

    private long cycleNumber;
    private short PC;
    private Stack<Short> STACK;
    private short I;
    private byte DT = 0x0;
    private byte ST = 0x0;
    private double timerRefreshRate = 1000000000 / 60;
    private long timerTickTime = 0;
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

        //TODO! Make the system refresh rate adjustable. For now use the same as the DT and ST
        if (System.nanoTime() < timerTickTime + timerRefreshRate) {
            return;
        }

        cycleNumber++;
        if (System.nanoTime() > timerTickTime + timerRefreshRate) {
            DT = (byte) (Math.max(0, DT--));
            ST = (byte) (Math.max(0, ST--));
            timerTickTime = System.nanoTime();
        }
        if (PC + 1 >= mem.length) {
            throw new RuntimeException("PC outside memory range");
        }
        short opcodeShort = CpuUtil.shortFromBytes(mem[PC], mem[PC + 1]);
        System.out.println("Current opcode is " + String.format("%04X", opcodeShort) + " (cycle " + cycleNumber + ")");
        byte[] opcodeNibbles = CpuUtil.nibblesFromShort(opcodeShort);

        switch (opcodeNibbles[0]) {
            case 0x0:
                switch (CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3])) {
                    case (byte) 0xE0:
                        CLS();
                        break;
                    case (byte) 0xEE:
                        RET();
                        break;
                    default:
                        unknownOpcode(opcodeShort);
                }
                break;
            case 0x1:
                JMP(CpuUtil.addressFromNibbles(opcodeNibbles[1], opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0x2:
                CALL(CpuUtil.addressFromNibbles(opcodeNibbles[1], opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0x3:
                SE_N(opcodeNibbles[1], CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0x6:
                LD(opcodeNibbles[1], CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0x7:
                ADD_N(opcodeNibbles[1], CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0x9:
                SNE_R(opcodeNibbles[1], opcodeNibbles[2]);
                break;
            case 0xA:
                LDI(CpuUtil.addressFromNibbles(opcodeNibbles[1], opcodeNibbles[2], opcodeNibbles[3]));
                break;
            case 0xD:
                DRW(opcodeNibbles[1], opcodeNibbles[2], opcodeNibbles[3]);
                break;
            case 0xF:
                switch (CpuUtil.byteFromNibbles(opcodeNibbles[2], opcodeNibbles[3])) {
                    case 0x07:
                        LDRDT(opcodeNibbles[1]);
                        break;
                    case 0x15:
                        LDDT(opcodeNibbles[1]);
                        break;
                    case 0x29:
                        LDF(opcodeNibbles[1]);
                        break;
                    case 0x33:
                        LDB(opcodeNibbles[1]);
                        break;
                    case 0x65:
                        LDR(opcodeNibbles[1]);
                        break;
                    default:
                        unknownOpcode(opcodeShort);
                }
                break;
            default:
                unknownOpcode(opcodeShort);
        }

        PC += 2;
    }

    private void unknownOpcode(short opcodeShort) {
        throw new RuntimeException("Unknown opcode " + String.format("%04X", opcodeShort));
    }

    //00E0
    private void CLS() {
        vMem = new int[64][32];
    }

    //00EE
    private void RET() {
        if (STACK.empty()) {
            throw new RuntimeException("Tried to return from a subroutine but the stack is empty");
        }
        PC = STACK.pop();
        if (PC < 0) {
            throw new RuntimeException("Stack underflow");
        }
    }

    //1NNN
    private void JMP(short address) {
        PC = address;
        PC -= 2; //Since the cycle execution adds +2
    }

    //2NNN
    private void CALL(short address) {
        STACK.push((short) (PC + 2));
        PC = address;
        PC -= 2;
    }

    //3XNN
    private void SE_N(byte sourceRegister, byte value) {
        if (mem[sourceRegister] == value) {
            PC += 2;
        }
    }

    //6XNN
    private void LD(byte targetRegister, byte value) {
        reg[targetRegister] = value;
    }

    //7XNN
    private void ADD_N(byte targetRegister, byte value) {
        mem[targetRegister] = (byte) ((mem[targetRegister] + value) % 256);
    }

    //9XY0
    private void SNE_R(byte regX, byte regY) {
        if (mem[regX] != mem[regY]) {
            PC += 2;
        }
    }

    //ANNN
    private void LDI(short address) {
        I = address;
    }

    //DXYN
    private void DRW(byte xSource, byte ySource, byte n) {
        byte x = reg[xSource];
        byte y = reg[ySource];
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

    //FX07
    private void LDRDT(byte targetRegister) {
        reg[targetRegister] = DT;
    }

    //FX15
    private void LDDT(byte sourceRegister) {
        DT = reg[sourceRegister];
    }

    //FX29
    private void LDF(byte sourceRegister) {
        I = (short) (reg[sourceRegister] * 5);
    }

    //FX33
    private void LDB(byte sourceRegister) {
        if (I + 2 >= mem.length) {
            segfault();
        }
        mem[I] = (byte) (reg[sourceRegister] / 100);
        mem[I + 1] = (byte) ((reg[sourceRegister] % 100) / 10);
        mem[I + 2] = (byte) (reg[sourceRegister] % 10);
    }

    //FX65
    private void LDR(byte targetRegister) {
        if (I + targetRegister >= mem.length) {
            segfault();
        }
        IntStream.rangeClosed(0, targetRegister).forEach(index -> reg[index] = mem[I + index]);
        I = (short) (I + targetRegister + 1);
    }

    private void segfault() {
        throw new RuntimeException("Segmentation fault! I: " + String.format("%04X", I) + " Memory length: " + mem.length);
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
